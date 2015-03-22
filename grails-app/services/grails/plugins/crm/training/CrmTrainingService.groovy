/*
 * Copyright (c) 2014 Goran Ehrsson.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugins.crm.training

import grails.events.Listener
import grails.plugins.crm.core.PagedResultList
import grails.plugins.crm.core.SearchUtils
import grails.plugins.crm.core.TenantUtils
import grails.plugins.crm.task.CrmTask
import grails.plugins.crm.task.CrmTaskAttender
import grails.plugins.selection.Selectable
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod

/**
 * Training services.
 */
class CrmTrainingService {

    def crmSecurityService
    def crmTaskService
    def crmTagService
    def messageSource

    @Listener(namespace = "crmTraining", topic = "enableFeature")
    def enableFeature(event) {
        // event = [feature: feature, tenant: tenant, role:role, expires:expires]
        Map tenant = crmSecurityService.getTenantInfo(event.tenant)
        Locale locale = tenant.locale ?: Locale.getDefault()

        TenantUtils.withTenant(tenant.id) {
            crmTagService.createTag(name: CrmTraining.name, multiple: true)

            createTrainingType(name: 'Event', true)

            crmTaskService.createAttenderStatus(orderIndex: 2, param: "confirm",
                    name: messageSource.getMessage("crmTaskAttenderStatus.name.confirm", null, "Confirm*", locale), true)
        }
    }

    @Listener(namespace = "crmTenant", topic = "requestDelete")
    def requestDeleteTenant(event) {
        def tenant = event.id
        def count = 0
        count += CrmTraining.countByTenantId(tenant)
        count += CrmTrainingType.countByTenantId(tenant)
        count ? [namespace: 'crmTraining', topic: 'deleteTenant'] : null
    }

    @Listener(namespace = "crmTraining", topic = "deleteTenant")
    def deleteTenant(event) {
        def tenant = event.id
        def result = CrmTraining.findAllByTenantId(tenant)
        result*.delete()
        CrmTrainingType.findAllByTenantId(tenant)*.delete()
        log.warn("Deleted ${result.size()} trainings in tenant $tenant")
    }

    /**
     * Empty query = search all records.
     *
     * @param params pagination parameters
     * @return List of CrmTraining domain instances
     */
    @Selectable
    def list(Map params = [:]) {
        listTrainings([:], params)
    }

    /**
     * Find CrmTraining instances filtered by query.
     *
     * @param query filter parameters
     * @param params pagination parameters
     * @return List of CrmTraining domain instances
     */
    @Selectable
    def list(Map query, Map params) {
        listTrainings(query, params)
    }

    /**
     * Find CrmTraining instances filtered by query.
     *
     * @param query filter parameters
     * @param params pagination parameters
     * @return List of CrmTraining domain instances
     */
    def listTrainings(Map query, Map params) {
        def ids = [] as Set

        if (query.tags) {
            def tmp = crmTagService.findAllIdByTag(CrmTraining, query.tags)
            if (tmp) {
                ids.addAll(tmp)
            } else {
                return new PagedResultList([])
            }
        }

        if (query.customer) {
            def tmp = listTrainingsForContact(query.customer)
            if (tmp) {
                ids.addAll(tmp.collect { Long.valueOf(it.ref.split('@')[1]) })
            } else {
                return new PagedResultList([])
            }
        }

        if (query.fromDate || query.toDate || query.loction) {
            def taskQuery = params.subMap(['fromDate', 'toDate', 'location'])
            taskQuery.referenceType = 'crmTraining'
            def tmp = crmTaskService.list(taskQuery, [:])
            if (tmp) {
                ids.addAll(tmp.collect { Long.valueOf(it.ref.split('@')[1]) })
            } else {
                return new PagedResultList([])
            }
        }
        CrmTraining.createCriteria().list(params) {
            eq('tenantId', TenantUtils.tenant)
            if (ids) {
                inList('id', ids)
            }
            if (query.number) {
                or {
                    ilike('number', SearchUtils.wildcard(query.number))
                    ilike('displayNumber', SearchUtils.wildcard(query.number))
                }
            }
            if (query.name) {
                or {
                    ilike('name', SearchUtils.wildcard(query.name))
                    ilike('displayName', SearchUtils.wildcard(query.name))
                }
            }
            if (query.type) {
                type {
                    or {
                        ilike('name', SearchUtils.wildcard(query.type))
                        eq('param', SearchUtils.wildcard(query.type))
                    }
                }
            }
        }
    }

    CrmTraining getTraining(Long id) {
        CrmTraining.findByIdAndTenantId(id, TenantUtils.tenant)
    }

    CrmTraining getTraining(String number) {
        CrmTraining.findByNumberAndTenantId(number, TenantUtils.tenant)
    }

    CrmTraining createTraining(Map params, boolean save = false) {
        def tenant = TenantUtils.tenant
        def m = CrmTraining.findByNumberAndTenantId(params.number, tenant)
        if (!m) {
            m = new CrmTraining()
            def args = [m, params, [include: CrmTraining.BIND_WHITELIST]]
            new BindDynamicMethod().invoke(m, 'bind', args.toArray())
            m.tenantId = tenant
            if (save) {
                m.save()
            } else {
                m.validate()
                m.clearErrors()
            }
        }
        return m
    }

    CrmTrainingType getTrainingType(String param) {
        CrmTrainingType.findByParamAndTenantId(param, TenantUtils.tenant)
    }

    CrmTrainingType createTrainingType(Map params, boolean save = false) {
        if (!params.param) {
            params.param = StringUtils.abbreviate(params.name?.toLowerCase(), 20)
        }
        def tenant = TenantUtils.tenant
        def m = CrmTrainingType.findByParamAndTenantId(params.param, tenant)
        if (!m) {
            m = new CrmTrainingType(params)
            m.tenantId = tenant
            if (params.enabled == null) {
                m.enabled = true
            }
            if (save) {
                m.save()
            } else {
                m.validate()
                m.clearErrors()
            }
        }
        return m
    }

    CrmTask getTrainingEvent(String number) {
        def ev = CrmTask.findByNumberAndTenantId(number, TenantUtils.tenant)
        if (!ev) {
            ev = CrmTask.findByGuid(number)
        }
        return ev
    }

    CrmTask createTrainingEvent(Map params, boolean save = false) {
        if (params.training) {
            params.reference = params.training
        }
        def crmTask = crmTaskService.createTask(params, save)

        return crmTask
    }

    List<CrmTask> listTrainingEvents(Map query = [:], Map params = [:]) {
        if (query.reference == null && query.ref == null) {
            query.referenceType = CrmTraining.class
        }
        crmTaskService.list(query, params)
    }

    CrmTaskAttender addAttender(CrmTask task, Map params) {
        def contactInfo = crmTaskService.createContactInformation(params)
        crmTaskService.addAttender(task, contactInfo, params.status, params.notes ?: (params.description ?: params.msg))
    }

    /**
     * Trigger an application event with same topic/name as the current attender status.
     *
     * @param attender
     */
    void triggerStatusEvent(CrmTaskAttender attender, String newStatus) {
        final String topic = attender.status.param
        final Map data = attender.dao
        data.newStatus = newStatus
        event(for: "crmTaskAttender", topic: topic, data: data)
    }

    List<CrmTask> listTrainingsForContact(final String contactName, final String attenderStatus = null) {
        CrmTaskAttender.createCriteria().list() {
            projections {
                property 'task'
            }
            task {
                eq('tenantId', TenantUtils.tenant)
                ilike('ref', 'crmTraining@%')
            }
            contact {
                ilike('name', SearchUtils.wildcard(contactName))
            }
            if (attenderStatus) {
                status {
                    eq('param', attenderStatus)
                }
            }
        }
    }
}
