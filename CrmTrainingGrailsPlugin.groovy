/*
 * Copyright (c) 2012 Goran Ehrsson.
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

import grails.plugins.crm.training.CrmTraining

class CrmTrainingGrailsPlugin {
    def groupId = ""
    def version = "2.0.0"
    def grailsVersion = "2.2 > *"
    def dependsOn = [:]
    def loadAfter = ['crmTags']
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/domain/test/TestEntity.groovy",
            "src/groovy/grails/plugins/crm/training/TestSecurityDelegate.groovy"
    ]
    def title = "GR8 CRM Training Administration"
    def author = "Goran Ehrsson"
    def authorEmail = "goran@technipelago.se"
    def description = "Provides domain classes and services for training administration, based on GR8 CRM."
    def documentation = "http://gr8crm.github.io/plugins/crm-training"
    def license = "APACHE"
    def organization = [name: "Technipelago AB", url: "http://www.technipelago.se/"]
    def issueManagement = [system: "github", url: "https://github.com/technipelago/grails-crm-training/issues"]
    def scm = [url: "https://github.com/technipelago/grails-crm-training"]

    def features = {
        crmTraining {
            description "GR8 CRM Training Administration"
            link controller: "crmTraining", action: "index"
            permissions {
                guest "crmTraining:index,list,show,createFavorite,deleteFavorite,clearQuery"
                partner "crmTraining:index,list,show,createFavorite,deleteFavorite,clearQuery"
                user "crmTraining:*"
                admin "crmTraining,crmTrainingType:*"
            }
            statistics { tenant ->
                def total = CrmTraining.countByTenantId(tenant)
                def updated = CrmTraining.countByTenantIdAndLastUpdatedGreaterThan(tenant, new Date() - 31)
                def usage
                if (total > 0) {
                    def tmp = updated / total
                    if (tmp < 0.1) {
                        usage = 'low'
                    } else if (tmp < 0.3) {
                        usage = 'medium'
                    } else {
                        usage = 'high'
                    }
                } else {
                    usage = 'none'
                }
                return [usage: usage, objects: total]
            }
        }
    }
}
