# GR8 CRM - Training Plugin

CRM = [Customer Relationship Management](http://en.wikipedia.org/wiki/Customer_relationship_management)

GR8 CRM is a set of [Grails Web Application Framework](http://www.grails.org/)
plugins that makes it easy to develop web application with CRM functionality.
With CRM we mean features like:

- Contact Management
- Task/Todo Lists
- Project Management

## Course / Event Management Plugin
This plugin provides services and persistence for course management in GR8 CRM applications.
Although the plugin was originally designed for administration of course/training events it
ended up being a very generic plugin capable of managing any type of events where people attend.
It can be traditional training classes, seminars, small conferences or user group meetings.

This is a "headless" plugin. For event management user interface see the
[crm-training-ui](https://github.com/technipelago/grails-crm-training-ui.git) plugin.

## Examples

    def conferenceType = crmTrainingService.createTrainingType(name: 'Conference', true)
    def gr8conf = crmTrainingService.createTraining(name: "GR8Conf EU", type: conferenceType, true)
    def taskType = crmTaskService.createTaskType(name: "Event", true)
    
    def startDate = new GregorianCalendar(2015, Calendar.JUNE, 2, 8, 0, 0).time
    def endDate = new GregorianCalendar(2015, Calendar.JUNE, 4, 17, 15, 0).time
    def gr8confeu2015 = crmTaskService.createTask(number: 'GR8CONF-EU-2015', name: "GR8Conf EU", type: taskType,
        startTime: startDate, endTime: endDate, displayDate: "June 2nd - 4th, 2015", location: "IT-University, Rued Langgards Vej 7, Copenhagen, Denmark", username: 'admin', reference: gr8conf,
        description: "GR8Conf is an independent, affordable series of conferences in Denmark and the US. It's dedicated to the technologies in the Groovy ecosystem.", true)             
    
    crmTrainingService.addAttender(gr8confeu2015, [firstName: "Göran", lastName: "Ehrsson", status: "confirmed"])
    crmTrainingService.addAttender(gr8confeu2015, [firstName: "Søren", lastName: "Berg Glasius", status: "confirmed"])
    crmTrainingService.addAttender(gr8confeu2015, [firstName: "Guillaume", lastName: "Laforge", status: "confirmed"])
    
### Documentation

Complete documentation for this plugin can be found at [gr8crm.github.io](http://gr8crm.github.io/plugins/crm-training/)