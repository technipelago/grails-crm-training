package grails.plugins.crm.training

import grails.events.Listener
import grails.plugins.crm.core.SearchUtils
import grails.plugins.crm.core.TenantUtils
import grails.plugins.crm.task.CrmTask
import grails.plugins.crm.task.CrmTaskAttender
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod

/**
 * Training services.
 */
class CrmTrainingService {

    def crmTaskService
    def crmCoreService
    def crmTagService

    @Listener(namespace = "crmTraining", topic = "enableFeature")
    def enableFeature(event) {
        // event = [feature: feature, tenant: tenant, role:role, expires:expires]
        def tenant = event.tenant
        TenantUtils.withTenant(tenant) {
            crmTagService.createTag(name: CrmTraining.name, multiple: true)
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
        def tagged
        if (query.tags) {
            tagged = crmTagService.findAllIdByTag(CrmTraining, query.tags) ?: [0L]
        }

        CrmTraining.createCriteria().list(params) {
            eq('tenantId', TenantUtils.tenant)
            if (tagged) {
                inList('id', tagged)
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
        CrmTask.findByNumberAndTenantId(number, TenantUtils.tenant)
    }

    CrmTask createTrainingEvent(Map params, boolean save = false) {
        if(params.training) {
            params.reference = params.training
        }
        def crmTask = crmTaskService.createTask(params, save)

        return crmTask
    }

    List<CrmTask> listTrainingEvents(Map query = [:], Map params = [:]) {
        if(query.reference == null && query.ref == null) {
            query.referenceType = CrmTraining.class
        }
        crmTaskService.list(query, params)
    }

    CrmTaskAttender addAttender(CrmTask task, Map params) {
        def contactInfo = crmTaskService.createContactInformation(params)
        crmTaskService.addAttender(task, contactInfo, params.status, params.notes ?: (params.description ?: params.msg))
    }
}
