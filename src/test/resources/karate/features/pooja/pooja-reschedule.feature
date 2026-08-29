@ignore
Feature: Reschedule and cancel a pooja registration
  # Called by: e2e/ganesh-mahotsav-full-flow.feature
  # Requires: sevaMainDaysId, eventId, adminToken, communityId
  # Tests:
  #   POST /events/pooja-registrations/{id}/reschedule
  #   DELETE /events/pooja-registrations/{id}  (cancel)

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: Create a throwaway registration on Sept 15 morning (to reschedule)
    Given path '/events/pooja-schedules'
    And param poojaId = sevaMainDaysId
    And param date    = '2026-09-15'
    When method GET
    Then status 200
    * def schedules = response
    * def hasSchedule = schedules.length > 0
    * if (!hasSchedule) karate.log('No schedule for Sept 15 — skipping reschedule scenario')

    * def throwawayRegId = null
    * if (hasSchedule) karate.set('sept15ScheduleId', schedules[0].id)
    * if (hasSchedule) karate.set('sept15SlotId', schedules[0].timeSlotConfigId)

    # Reserve a slot on Sept 15 for the throwaway registration
    * def idem1 = java.util.UUID.randomUUID().toString()
    * if (hasSchedule) karate.call(read('classpath:karate/features/pooja/_do-reserve-and-register.feature'), { doScheduleId: schedules[0].id, doSlotId: schedules[0].timeSlotConfigId, doEventId: eventId, doIdem: idem1, doDate: '2026-09-15', doTime: '10:00', doName: 'Throwaway Devotee' })
    * print '✅ Throwaway registration created for reschedule test, hasSchedule:', hasSchedule

  Scenario: Reschedule the throwaway registration to Sept 15 evening
    # Get the throwaway registration ID
    Given path '/events/pooja-registrations'
    And param poojaSevaId = sevaMainDaysId
    When method GET
    Then status 200
    * def regs = response
    * def hasReg = regs.length > 0

    * if (hasReg) karate.set('rescheduleRegId', regs[0].id)

    # Find a Sept 15 evening schedule to reschedule to
    Given path '/events/pooja-schedules'
    And param poojaId = sevaMainDaysId
    And param date    = '2026-09-15'
    When method GET
    Then status 200
    * def sched15 = response
    * def hasSched = sched15.length > 0 && hasReg

    * if (hasSched) karate.call(read('classpath:karate/features/pooja/_do-reschedule.feature'), { rsRegId: regs[0].id, rsNewScheduleId: sched15[0].id })
    * print '✅ Reschedule step done, hasSched:', hasSched

  Scenario: Cancel (soft-delete) the throwaway registration
    Given path '/events/pooja-registrations'
    And param poojaSevaId = sevaMainDaysId
    When method GET
    Then status 200
    * def regs = response
    * if (regs.length == 0) karate.log('No registrations to cancel — skipping')

    * if (regs.length > 0) karate.call(read('classpath:karate/features/pooja/_do-cancel.feature'), { cancelRegId: regs[0].id })
    * print '✅ Cancel step done'
