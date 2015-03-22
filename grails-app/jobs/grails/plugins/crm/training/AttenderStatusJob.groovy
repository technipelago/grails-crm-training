package grails.plugins.crm.training

import grails.plugins.crm.core.TenantUtils
import grails.plugins.crm.task.CrmTaskAttender

/**
 * Search CrmTaskAttender with status 'confirm' and trigger 'confirm' event.
 */
class AttenderStatusJob {
    static triggers = {
        simple name: 'attenderStatus', startDelay: 30000, repeatInterval: 600000 // every ten minutes
    }

    def group = 'training'

    def grailsApplication
    def crmTrainingService

    def execute() {
        def config = grailsApplication.config
        if (config.crm.training.job.confirmation.enabled) {
            def statusMap = config.crm.training.job.confirmation.status ?: [confirm: 'confirmed']
            def result = CrmTaskAttender.createCriteria().list() {
                status {
                    inList('param', statusMap.keySet().toList())
                }
            }
            for (a in result) {
                TenantUtils.withTenant(a.task.tenantId) {
                    crmTrainingService.triggerStatusEvent(a, statusMap[a.status.param])
                }
            }
        } else {
            log.debug "${getClass().getName()} is disabled because config [crm.training.job.confirmation.enabled] is not true"
        }
    }
}
