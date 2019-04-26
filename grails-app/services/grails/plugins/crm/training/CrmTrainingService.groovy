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
import grails.plugins.crm.core.CrmContactInformation
import grails.plugins.crm.core.PagedResultList
import grails.plugins.crm.core.SearchUtils
import grails.plugins.crm.core.TenantUtils
import grails.plugins.crm.task.CrmTask
import grails.plugins.crm.task.CrmTaskAttenderStatus
import grails.plugins.crm.task.CrmTaskBooking
import grails.plugins.crm.task.CrmTaskAttender
import grails.plugins.crm.task.CrmTaskType
import grails.plugins.selection.Selectable
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.grails.databinding.SimpleMapDataBindingSource

/**
 * Training services.
 */
class CrmTrainingService {

    def crmSecurityService
    def crmContactService
    def crmTaskService
    def crmTagService
    def messageSource
    def grailsWebDataBinder

    @Listener(namespace = "crmTraining", topic = "enableFeature")
    def enableFeature(event) {
        // event = [feature: feature, tenant: tenant, role:role, expires:expires]
        Map tenant = crmSecurityService.getTenantInfo(event.tenant)
        Locale locale = tenant.locale ?: Locale.getDefault()

        TenantUtils.withTenant(tenant.id) {
            crmTagService.createTag(name: CrmTraining.name, multiple: true)

            createTrainingType(name: 'Event', true)

            crmTaskService.createAttenderStatus(orderIndex: 2, param: "registered",
                    name: messageSource.getMessage("crmTaskAttenderStatus.name.registered", null, "Registered", locale), true)
            crmTaskService.createAttenderStatus(orderIndex: 3, param: "confirm",
                    name: messageSource.getMessage("crmTaskAttenderStatus.name.confirm", null, "Confirm *", locale), true)
            crmTaskService.createAttenderStatus(orderIndex: 4, param: "confirmed",
                    name: messageSource.getMessage("crmTaskAttenderStatus.name.confirmed", null, "Confirmed", locale), true)
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
                ilike('number', SearchUtils.wildcard(query.number))
            }
            if (query.name) {
                ilike('name', SearchUtils.wildcard(query.name))
            }
            if (query.type) {
                type {
                    or {
                        ilike('name', SearchUtils.wildcard(query.type))
                        eq('param', query.type)
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

    CrmTraining getTraining(CrmTask task) {
        def reference = task.reference
        return (reference instanceof CrmTraining) ? reference : null
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

    CrmTaskType createTaskType(Map params, boolean save = false) {
        crmTaskService.createTaskType(params, save)
    }

    CrmTaskAttenderStatus createAttenderStatus(Map params, boolean save = false) {
        crmTaskService.createAttenderStatus(params, save)
    }

    CrmTask getTrainingEvent(String number) {
        def tenant = TenantUtils.tenant
        def ev = CrmTask.findByNumberAndTenantId(number, tenant)
        if (!ev) {
            ev = CrmTask.findByGuidAndTenantId(number, tenant)
            if (!ev && number.isNumber()) {
                ev = CrmTask.get(Long.valueOf(number))
            }
            if (ev?.tenantId != tenant) {
                ev = null
            }
        }
        return ev
    }

    CrmTask createTrainingEvent(Map params, boolean save = false) {
        CrmTraining crmTraining
        if (params.training) {
            params.reference = params.training
        }
        if (params.reference instanceof CrmTraining) {
            crmTraining = params.reference
        } else {
            throw new IllegalArgumentException("No CrmTraining instance found in params")
        }
        if (!params.name) {
            params.name = crmTraining.name
        }
        if (params.scope == null) {
            params.scope = crmTraining.scope
        }
        if (params.description == null) {
            params.description = crmTraining.description
        }

        crmTaskService.createTask(params, save)
    }

    List<CrmTask> listTrainingEvents(Map query = [:], Map params = [:]) {
        if (query.reference == null && query.ref == null) {
            query.referenceType = CrmTraining.class
        }
        crmTaskService.list(query, params)
    }

    /**
     * Find a booking with a specific booking reference.
     * @param crmTask the task/activity where bookings are searched
     * @param bookingReference if null the last created booking will be returned
     * @return a CrmTaskBooking instance or null if not found
     */
    CrmTaskBooking getBooking(CrmTask crmTask, String bookingReference) {
        CrmTaskBooking.createCriteria().get() {
            eq('task', crmTask)
            if (bookingReference) {
                eq('bookingRef', bookingReference)
            }
            order 'dateCreated', 'desc'
            maxResults 1
        }
    }

    /**
     * Add an attender to an event (CrmTask).
     * @param task the task to add the attender to
     * @param contact the attender to add
     * @param params optional attender properties to set
     * @return the created attender or null if CrmTaskBooking cannot be created
     */
    CrmTaskAttender addAttender(CrmTask task, CrmContactInformation contact, Map params) {
        def booking = crmTaskService.createBooking([task: task, bookingDate: params.bookingDate ?: new Date(), bookingRef: params.bookingRef], true)
        if (booking.hasErrors()) {
            return null
        }
        def notes = params.notes ?: (params.description ?: params.msg)
        def attender = crmTaskService.addAttender(booking, contact, params.status, notes)

        grailsWebDataBinder.bind(attender, params as SimpleMapDataBindingSource, null, CrmTaskAttender.BIND_WHITELIST, null, null)

        task.save(flush: true)

        event(for: "crmTaskAttender", topic: "created", data: attender.getDao())

        attender
    }

    /**
     * Add an attender to an event (CrmTask).
     * @param params
     * @return
     */
    CrmTaskAttender addAttender(Map params) {
        def trainingEvent = getTrainingEvent(params.id ?: params.number)
        if (!trainingEvent) {
            throw new IllegalArgumentException("No training found with number or id [${params.id ?: params.number}]")
        }
        def booking
        if (params.booking) {
            booking = getBooking(trainingEvent, params.booking)
        }
        if (!booking) {
            booking = crmTaskService.createBooking(params + [task: trainingEvent], true)
            if (booking.hasErrors()) {
                throw new IllegalArgumentException("Cannot create booking with $params due to ${booking.errors.allErrors}")
            }
        }
        def contact = crmContactService.createContactInformation(params)
        def notes = params.notes ?: (params.description ?: params.msg)
        def attender = crmTaskService.addAttender(booking, contact, params.status, notes ?: params.msg)
        if (attender.hasErrors()) {
            log.error("Cannot register attender with $params due to ${attender.errors.allErrors}")
        } else {
            trainingEvent.save(flush: true)
            def data = attender.getDao()
            event(for: "crmTaskAttender", topic: "created", data: data)
            log.debug "Successfully registered attender $attender"
        }
        attender
    }

    /**
     * Add an attender to an event (CrmTask).
     * @deprecated Use CrmTaskService#addAttender()
     * @param task
     * @param params
     * @return
     */
    CrmTaskAttender addAttender(CrmTask task, Map params) {
        def contactInfo = crmContactService.createContactInformation(params)
        def notes = params.notes ?: (params.description ?: params.msg)
        def a = crmTaskService.addAttender(task, contactInfo, params.status, notes)
        if (params.bookingRef) {
            a.bookingRef = params.bookingRef
        }
        if (params.externalRef) {
            a.externalRef = params.externalRef
        }
        return a
    }

    /**
     * Trigger an application event with same topic/name as the current attender status.
     *
     * @param attender
     */
    void triggerStatusEvent(CrmTaskAttender attender, String newStatus) {
        final String topic = attender.status.param
        final CrmTaskBooking booking = attender.booking
        final CrmTask task = booking.task
        final Map model = attender.getDao()
        model.booking = booking.getDao()
        model.task = task.getDao()
        model.event = model.task
        try {
            model.training = task.reference?.getDao()
        } catch(Exception e) {
            log.error(e.message, e)
        }
        model.newStatus = newStatus

        event(for: "crmTaskAttender", topic: topic, data: model)
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

    boolean isBookingPossible(CrmTask event) {
        CrmTraining training = getTraining(event)
        if(training == null) {
            return true // TODO should it be possible if it's not a training event?
        }

        Integer max = training.maxAttendees
        if(max == null) {
            return true
        }

        if(training.overbook) {
            max += training.overbook
        }

        int booked = crmTaskService.countBookedAttenders(event.id)
        if(booked < max) {
            return true
        }

        return false
    }

    boolean isAutoConfirmPossible(CrmTask event) {
        CrmTraining training = getTraining(event)
        if(training == null) {
            return true // TODO should it be possible if it's not a training event?
        }

        Integer autoConfirm = training.autoConfirm
        if(autoConfirm == null) {
            return false
        }

        int booked = crmTaskService.countBookedAttenders(event.id)
        if(booked < autoConfirm) {
            return true
        }

        return false
    }
}
