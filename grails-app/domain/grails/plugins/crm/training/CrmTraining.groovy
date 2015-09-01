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

import grails.plugins.crm.core.AuditEntity
import grails.plugins.crm.core.TenantEntity
import grails.plugins.sequence.SequenceEntity

/**
 * Training metadata.
 */
@TenantEntity
@AuditEntity
@SequenceEntity
class CrmTraining {

    String number
    String name
    String description
    String url
    String scope // Number of days
    Integer maxAttendees // Max attender for this training
    Integer autoConfirm // Confirm reservations automatically until number of attendees reach above this number
    Integer overbook // Total number of people to book = maxAttendees + overbook
    Double price
    Double vat

    CrmTrainingType type

    static constraints = {
        number(maxSize: 20, blank: false, unique: 'tenantId')
        name(maxSize: 255, blank: false)
        description(maxSize: 2000, nullable: true, widget: 'textarea')
        url(maxSize: 255, nullable: true)
        scope(maxSize: 40, nullable: true)
        type()
        maxAttendees(min: 0, nullable: true)
        autoConfirm(min: 0, nullable: true)
        overbook(min: 0, nullable: true)
        price(min: -999999d, max: 999999d, scale: 2, nullable: true)
        vat(min: 0d, max: 1d, scale: 2)
    }

    static mapping = {
        sort 'number'
        number index: 'crm_training_number_idx'
        url index: 'crm_training_url_idx'
    }

    static taggable = true
    static attachmentable = true
    static dynamicProperties = true
    static relatable = true
    static auditable = true

    public static final List BIND_WHITELIST = ['number', 'name', 'description', 'url', 'scope', 'type',
                                               'maxAttendees', 'autoConfirm', 'overbook', 'price', 'vat']

    def beforeValidate() {
        if(vat == null) {
            vat = 0
        }
    }

    transient Double getPriceVAT() {
        def p = price ?: 0
        def v = vat ?: 0
        return p + (p * v)
    }

    transient Map<String, Object> getDao() {
        def info = BIND_WHITELIST.inject([:]) {map, p ->
            def value = this[p]
            if(value != null) {
                map[p] = value
            }
            map
        }
        info.id = ident()
        info.type = type?.dao
        info.tags = getTagValue()
        info
    }

    String toString() {
        name.toString()
    }
}
