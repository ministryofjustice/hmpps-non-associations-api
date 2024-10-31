package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.transaction.TestTransaction
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ApplicationInsightsConfiguration
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ClockConfiguration
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAnyBetweenPrisonerNumbers
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.genNonAssociation
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@DisplayName("Non-associations merge service, integration tests")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(HmppsAuthenticationHolder::class, NonAssociationsMergeService::class, ClockConfiguration::class, ApplicationInsightsConfiguration::class)
@WithMockAuthUser(username = "A_DPS_USER")
@DataJpaTest
class NonAssociationMergeServiceIntTest : TestBase() {

  val objectMapper = ObjectMapper()

  @Autowired
  lateinit var service: NonAssociationsMergeService

  @Autowired
  lateinit var repository: NonAssociationsRepository

  @Test
  fun testJsonDeserialization() {
    val eventListener = PrisonOffenderEventListener(mapper = objectMapper, nonAssociationsMergeService = service)
    eventListener.onPrisonOffenderEvent(MERGE_MESSAGE)

    assertThat(repository.findAllByPrisonerNumber("B1234AA")).hasSize(6)
    assertThat(repository.findAllByPrisonerNumber("A1234AA")).hasSize(0)
  }

  @Test
  fun testMerge() {
    assertThat(repository.findAllByPrisonerNumber("A1234AA")).hasSize(6)
    assertThat(repository.findAllByPrisonerNumber("B1234AA")).hasSize(4)

    assertThat(repository.findAnyBetweenPrisonerNumbers(listOf("X1234AA", "A1234AA"), NonAssociationListInclusion.CLOSED_ONLY)).hasSize(0)
    assertThat(repository.findAnyBetweenPrisonerNumbers(listOf("X1234AA", "B1234AA"), NonAssociationListInclusion.CLOSED_ONLY)).hasSize(0)

    val mergedAssociations = service.mergePrisonerNumbers("A1234AA", "B1234AA")

    assertThat(mergedAssociations[MergeResult.CLOSED]).hasSize(2)
    assertThat(mergedAssociations[MergeResult.MERGED]).hasSize(2)
    assertThat(mergedAssociations[MergeResult.DELETED]).hasSize(2)

    assertThat(mergedAssociations[MergeResult.MERGED]).isEqualTo(
      listOf(
        genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "C1234FF"),
        genNonAssociation(firstPrisonerNumber = "D1234RR", secondPrisonerNumber = "B1234AA"),
      ),
    )

    assertThat(mergedAssociations[MergeResult.CLOSED]).isEqualTo(
      listOf(
        genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "X1234AA"),
        genNonAssociation(firstPrisonerNumber = "X1234AB", secondPrisonerNumber = "B1234AA"),
      ),
    )

    assertThat(mergedAssociations[MergeResult.DELETED]).isEqualTo(
      listOf(
        genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "B1234AA"),
        genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "B1234AA"),
      ),
    )

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    assertThat(repository.findAllByPrisonerNumber("B1234AA")).hasSize(6)
    assertThat(repository.findAllByPrisonerNumber("A1234AA")).hasSize(0)

    assertThat(repository.findAnyBetweenPrisonerNumbers(listOf("X1234AA", "B1234AA"), NonAssociationListInclusion.CLOSED_ONLY)).hasSize(1)
  }

  @BeforeEach
  fun setupData() {
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "A1234AA",
        secondPrisonerNumber = "X1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "X1234AB",
        secondPrisonerNumber = "A1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "B1234AA",
        secondPrisonerNumber = "X1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "X1234AB",
        secondPrisonerNumber = "B1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "A1234AA",
        secondPrisonerNumber = "B1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "A1234AA",
        secondPrisonerNumber = "C1234FF",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "D1234RR",
        secondPrisonerNumber = "A1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "B1234AA",
        secondPrisonerNumber = "A1234AA",
      ),
    )
  }

  @AfterEach
  fun tearDown() {
    repository.deleteAll()
  }
}

const val MERGE_MESSAGE = """
  {
  "Type": "Notification",
  "MessageId": "ee46cb90-a2de-57bf-86ba-9d2eba64647a",
  "TopicArn": "arn:aws:sns:eu-west-2:754256621582:cloud-platform-Digital-Prison-Services-f221e27fcfcf78f6ab4f4c3cc165eee7",
  "Message":"{\"version\":\"1.0\", \"occurredAt\":\"2024-10-12T15:14:24.125533+00:00\", \"publishedAt\":\"2024-10-12T15:15:09.902048716+00:00\", \"description\":\"A prisoner has been merged from A1234AA to B1234AA\", \"additionalInformation\":{\"nomsNumber\":\"B1234AA\", \"removedNomsNumber\":\"A1234AA\", \"reason\":\"MERGE\"}}",
  "Timestamp": "2020-02-12T15:15:06.239Z",
  "SignatureVersion": "1",
  "Signature": "E0oesISQOBGaDjgOg3wEFfCFcIMNN4GyOdCtLRuhXB8QOzFt5XhzhfhcypPyXvIN+G5+Ky79BK0SlXDWxv9vSw2tOSojNwH1vvbXApInAiqyAgIBNYgUk3l1MzKmkqoH5lWmgmo5U4szk5jKbL0LVVc4BYRY6pIq2ZWt4pPoX47Z5oibjfXZZhKsR6k5VCTnUD7lqa2hkWWqaqZIsoeCG5g83Xb5d7s+LlN5iV74gwP/lgZT0E/uSnRCk8Nx0UUPEvpk/04V5yaW6W9YP/hwKMNep873tYzTcFGilyKoU5ucy4vVMulwT+EL3iOmumQEoFcCd/BQotjU2+wQ4wL3/Q==",
  "SigningCertURL": "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-a86cb10b4e1f29c941702d737128f7b6.pem",
  "UnsubscribeURL": "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:754256621582:cloud-platform-Digital-Prison-Services-f221e27fcfcf78f6ab4f4c3cc165eee7:92545cfe-de5d-43e1-8339-c366bf0172aa",
  "MessageAttributes": {
    "eventType": {
      "Type": "String",
      "Value": "prison-offender-events.prisoner.merged"
    },
    "id": {
      "Type": "String",
      "Value": "11c19083-520d-5d7e-c91f-918a7b214ef2"
    },
    "contentType": {
      "Type": "String",
      "Value": "text/plain;charset=UTF-8"
    },
    "timestamp": {
      "Type": "Number.java.lang.Long",
      "Value": "1581520506294"
    }
  }
}
"""
