package grails.plugins.crm.training

/**
 * Tests for CrmTrainingService.
 */
class CrmTrainingServiceSpec extends grails.test.spock.IntegrationSpec {

    def crmTrainingService

    def "create training instance"() {
        given:
        def training = crmTrainingService.createTrainingType(name: 'Training', true)

        when:
        def instance = crmTrainingService.createTraining(number: "GROOVY-01",
                type: training,
                name: "Introduction to the Groovy programming language",
                description: "Groovy is a powerful, optionally typed and dynamic language, with static-typing and static compilation capabilities, for the Java platform aimed at multiplying developers’ productivity thanks to a concise, familiar and easy to learn syntax.",
                maxAttendees: 20,
                overbook: 5,
                scope: '2 days',
                price: 12000,
                vat: 0.25,
                true)

        then:
        !instance.hasErrors()

        when:
        def dao = instance.dao

        then: "check getDao() result"
        dao.id != null
        dao.type.param == 'training'
        dao.name == 'Introduction to the Groovy programming language'
        dao.maxAttendees == 20
        dao.overbook == 5
        dao.autoConfirm == null
        dao.price == 12000
        dao.vat == 0.25
    }
}
