package grails.plugins.crm.training

import grails.validation.Validateable

/**
 * Created by goran on 2014-05-07.
 */
@Validateable
class CrmTrainingRegisterCommand implements Serializable {

    String number
    String firstName
    String lastName
    String title
    String company
    String address1
    String postalCode
    String city
    String email
    String telephone
    String msg

    static constraints = {
        number(maxSize: 40, blank: false)
        firstName(maxSize: 40, blank: false)
        lastName(maxSize: 40, blank: false)
        title(maxSize: 40, nullable: true)
        company(maxSize: 80, nullable: true)
        address1(maxSize: 255, nullable: true)
        postalCode(maxSize: 10, nullable: true)
        city(maxSize: 40, nullable: true)
        email(maxSize: 80, blank: false)
        telephone(maxSize: 40, nullable: true)
        msg(maxSize: 2000, nullable: true, widget: 'textarea')
    }

    static transients = ['address']

    transient String getAddress() {
        final StringBuilder s = new StringBuilder()
        if (address1) {
            s << address1
        }
        if (postalCode) {
            if (s.length()) {
                s << ', '
            }
            s << postalCode
        }
        if (city) {
            if (s.length()) {
                s << ' '
            }
            s << city
        }
        s.toString()
    }

    Map<String, String> asMap() {
        ['number', 'firstName', 'lastName', 'title', 'company', 'address', 'address1', 'postalCode', 'city', 'email', 'telephone', 'msg'].inject([:]) { map, prop ->
            def value = this[prop]
            if (value != null) {
                map[prop] = value
            }
            return map
        }
    }
}
