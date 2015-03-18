package grails.plugins.crm.training

import grails.plugins.crm.core.TenantUtils

/**
 * Created by goran on 14-11-01.
 */
class CrmTrainingTagLib {

    static namespace = "crm"

    def crmContentService
    def crmTrainingService

    def eachTraining = { attrs, body ->
        def tenant = attrs.tenant ? Long.valueOf(attrs.tenant) : (grailsApplication.config.crm.training.defaultTenant ?: TenantUtils.tenant)
        TenantUtils.withTenant(tenant) {
            def query = attrs.query ?: [:]
            def params = attrs.params ?: [:]
            if (!params.sort) {
                params.sort = 'number'
            }
            if (!params.order) {
                params.order = 'asc'
            }
            def result = crmTrainingService.list(query, params)
            int i = 0
            for (CrmTraining crmTraining in result) {
                def map = crmTraining.dao
                map.presentation = {
                    crmContentService.findResourcesByReference(crmTraining, [name: (attrs.presentation ?: 'index.html')]).find {
                        it
                    }
                }
                def model = [(attrs.var ?: 'it'): map]
                if (attrs.status) {
                    model[attrs.status] = i++
                }
                out << body(model)
            }
        }
    }
}
