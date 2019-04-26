package grails.plugins.crm.training
/**
 * Tests for CrmTrainingService.
 */
class CrmTrainingServiceSpec extends grails.test.spock.IntegrationSpec {

    def crmTrainingService
    def crmContactService

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

    def "is booking possible"() {
        given:
        def person = crmContactService.&createContactInformation
        def training = crmTrainingService.createTrainingType(name: 'Training', true)
        def type = crmTrainingService.createTaskType(name: 'Training', true)
        def registered = crmTrainingService.createAttenderStatus(name: 'Registered', true)
        def confirmed = crmTrainingService.createAttenderStatus(name: 'Confirmed', true)

        when:
        def instance = crmTrainingService.createTraining(number: "GROOVY-02",
                type: training,
                name: "Groovy programming language best practices",
                description: "Groovy is a powerful, optionally typed and dynamic language, with static-typing and static compilation capabilities, for the Java platform aimed at multiplying developers’ productivity thanks to a concise, familiar and easy to learn syntax.",
                maxAttendees: 5,
                overbook: 2,
                autoConfirm: 4,
                scope: '2 days',
                price: 12000,
                vat: 0.25,
                true)

        then:
        !instance.hasErrors()

        when: "add 3 attenders"
        def event = crmTrainingService.createTrainingEvent(
                training: instance, number: 'GROOVY-02-001', type: type,
                startTime: date(2019, 10, 25, 16, 0), duration: 300,
                alarmTime: date(2019, 9, 30, 12, 0), true)

        crmTrainingService.addAttender(event, person(firstName: 'Joe', lastName: 'Average 1'), [status: confirmed])
        crmTrainingService.addAttender(event, person(firstName: 'Joe', lastName: 'Average 2'), [status: confirmed])
        crmTrainingService.addAttender(event, person(firstName: 'Joe', lastName: 'Average 3'), [status: registered])

        then: "booking is possible and auto confirm is still possible"
        crmTrainingService.isBookingPossible(event)
        crmTrainingService.isAutoConfirmPossible(event)

        when: "add fourth attender"
        crmTrainingService.addAttender(event, person(firstName: 'Joe', lastName: 'Average 4'), [status: registered])

        then: "auto confirm is not possible anymore because we reached 4 attenders"
        crmTrainingService.isBookingPossible(event)
        !crmTrainingService.isAutoConfirmPossible(event)

        when: "add a total of 7 attenders"
        crmTrainingService.addAttender(event, person(firstName: 'Joe', lastName: 'Average 5'), [status: registered])
        crmTrainingService.addAttender(event, person(firstName: 'Joe', lastName: 'Average 6'), [status: registered])
        crmTrainingService.addAttender(event, person(firstName: 'Joe', lastName: 'Average 7'), [status: registered])

        then: "it's not possible to book more people"
        !crmTrainingService.isBookingPossible(event)
        !crmTrainingService.isAutoConfirmPossible(event)
    }

    private Date date(int y, int mon, int d, int h, int min) {
        def cal = Calendar.getInstance()
        cal.clearTime()
        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, mon - 1)
        cal.set(Calendar.DAY_OF_MONTH, d)
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, min)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.getTime()
    }
}
