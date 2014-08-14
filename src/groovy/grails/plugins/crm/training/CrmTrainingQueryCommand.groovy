package grails.plugins.crm.training

import grails.validation.Validateable

/**
 * Created by goran on 2014-05-05.
 */
@Validateable
class CrmTrainingQueryCommand  implements Serializable {
    String number
    String name
    String type
    String location
    String customer
    String fromDate
    String toDate
    String tags

    String toString() {
        name.toString()
    }
}
