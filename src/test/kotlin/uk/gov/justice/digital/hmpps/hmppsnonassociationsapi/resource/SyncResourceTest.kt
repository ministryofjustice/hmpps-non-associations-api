package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.test.context.support.WithMockUser
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.DeleteSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyRestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.UpsertSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NO_CLOSURE_REASON_PROVIDED
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NO_COMMENT_PROVIDED
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.genNonAssociation
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@WithMockUser(username = expectedUsername)
class SyncResourceTest : SqsIntegrationTestBase() {
  @TestConfiguration
  class FixedClockConfig {
    @Primary
    @Bean
    fun fixedClock(): Clock = clock
  }

  @Nested
  inner class `Upsert Sync a non-association` {
    private val now = LocalDateTime.now(clock)
    private val today = LocalDate.now(clock).atStartOfDay()
    private val url = "/sync/upsert"

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.post()
        .uri(url)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role and scope responds 403 Forbidden`() {
      val request = UpsertSyncRequest(
        firstPrisonerNumber = "A7777XX",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "B7777XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        comment = "They keep fighting",
        authorisedBy = "Me",
        effectiveFromDate = LocalDate.now(clock).minusDays(1),
      )

      // correct role, missing write scope
      webTestClient.put()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY")))
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `without a valid request body responds 400 Bad Request`() {
      // no request body
      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isBadRequest

      // unsupported Content-Type
      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isBadRequest

      // request body missing some fields
      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString("firstPrisonerNumber" to "A1234BC"))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `can sync non-association by ID`() {
      val existingNa = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "A7777XT",
          secondPrisonerNumber = "B7777XT",
        ),
      )
      val request = UpsertSyncRequest(
        id = existingNa.id,
        firstPrisonerNumber = "Z1111ZZ",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "X1111XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        effectiveFromDate = LocalDate.now(clock).minusDays(10),
        authorisedBy = "The free text field for a staff name",
        lastModifiedByUsername = "A_NOMIS_USER",
      )

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${existingNa.firstPrisonerNumber}",
          "firstPrisonerRole": "VICTIM",
          "firstPrisonerRoleDescription": "Victim",
          "secondPrisonerNumber": "${existingNa.secondPrisonerNumber}",
          "secondPrisonerRole": "PERPETRATOR",
          "secondPrisonerRoleDescription": "Perpetrator",
          "reason": "OTHER",
          "reasonDescription": "Other",
          "restrictionType": "${request.restrictionType.toRestrictionType()}",
          "restrictionTypeDescription": "${request.restrictionType.toRestrictionType().description}",
          "comment": "$NO_COMMENT_PROVIDED",
          "updatedBy": "${request.lastModifiedByUsername}",
          "isClosed": false,
          "closedReason": null,
          "closedBy": null,
          "closedAt": null,
          "whenCreated": "${request.effectiveFromDate.atStartOfDay().format(dtFormat)}",
          "whenUpdated": "$now"
        }
        """

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(expectedResponse, false)
    }

    @Test
    fun `can sync a future non-association`() {
      val existingNa = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "A7777XT",
          secondPrisonerNumber = "B7777XT",
        ),
      )
      val request = UpsertSyncRequest(
        id = existingNa.id,
        firstPrisonerNumber = "Z1111ZZ",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "X1111XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        effectiveFromDate = LocalDate.now(clock).plusDays(10),
        authorisedBy = "The free text field for a staff name",
        lastModifiedByUsername = "A_NOMIS_USER",
      )

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${existingNa.firstPrisonerNumber}",
          "firstPrisonerRole": "VICTIM",
          "firstPrisonerRoleDescription": "Victim",
          "secondPrisonerNumber": "${existingNa.secondPrisonerNumber}",
          "secondPrisonerRole": "PERPETRATOR",
          "secondPrisonerRoleDescription": "Perpetrator",
          "reason": "OTHER",
          "reasonDescription": "Other",
          "restrictionType": "${request.restrictionType.toRestrictionType()}",
          "restrictionTypeDescription": "${request.restrictionType.toRestrictionType().description}",
          "comment": "$NO_COMMENT_PROVIDED",
          "updatedBy": "${request.lastModifiedByUsername}",
          "isClosed": true,
          "closedReason": "$NO_CLOSURE_REASON_PROVIDED",
          "closedBy": "A_NOMIS_USER",
          "closedAt": "${today.format(dtFormat)}",
          "whenCreated": "${today.format(dtFormat)}",
          "whenUpdated": "${today.format(dtFormat)}"
        }
        """

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(expectedResponse, false)
    }

    @Test
    fun `cannot sync open non-association by ID when another exists that is open`() {
      val existingClosedNa = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "A7777XT",
          secondPrisonerNumber = "B7777XT",
          closed = true,
        ),
      )
      repository.save(
        genNonAssociation(
          firstPrisonerNumber = "A7777XT",
          secondPrisonerNumber = "B7777XT",
        ),
      )
      val request = UpsertSyncRequest(
        id = existingClosedNa.id,
        firstPrisonerNumber = "Z1111ZZ",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "X1111XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        effectiveFromDate = LocalDate.now(clock).minusDays(2),
      )

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isEqualTo(409)
    }

    @Test
    fun `cannot sync non-association by ID that doesn't exist`() {
      val request = UpsertSyncRequest(
        id = -111111,
        firstPrisonerNumber = "Z1111ZZ",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "X1111XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        effectiveFromDate = LocalDate.now(clock),
      )

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `can sync non-association in open state`() {
      repository.save(
        genNonAssociation(
          firstPrisonerNumber = "A7777XX",
          secondPrisonerNumber = "B7777XX",
        ),
      )
      val request = UpsertSyncRequest(
        firstPrisonerNumber = "A7777XX",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "B7777XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        expiryDate = LocalDate.now(clock).minusDays(4),
        effectiveFromDate = LocalDate.now(clock).minusDays(10),
      )

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `fails validation on an non-association too long`() {
      val request = UpsertSyncRequest(
        firstPrisonerNumber = "A7777XX",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "B7777XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        authorisedBy = "1234567890123456789012345678901234567890123456789012345678901",
        effectiveFromDate = LocalDate.now(clock).minusDays(5),
      )

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `fails validation on an non-association incorrect prisoner number format`() {
      val request = UpsertSyncRequest(
        firstPrisonerNumber = "A7777XX2",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "B7777XX2342342342",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        authorisedBy = "HI",
        effectiveFromDate = LocalDate.now(clock).minusDays(5),
      )

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `creating an non-association`() {
      val request = UpsertSyncRequest(
        firstPrisonerNumber = "A7777XX",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "B7777XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        expiryDate = LocalDate.now(clock).minusDays(4),
        effectiveFromDate = LocalDate.now(clock).minusDays(5),
      )

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${request.firstPrisonerNumber}",
          "firstPrisonerRole": "VICTIM",
          "firstPrisonerRoleDescription": "Victim",
          "secondPrisonerNumber": "${request.secondPrisonerNumber}",
          "secondPrisonerRole": "PERPETRATOR",
          "secondPrisonerRoleDescription": "Perpetrator",
          "reason": "OTHER",
          "reasonDescription": "Other",
          "restrictionType": "${request.restrictionType.toRestrictionType()}",
          "restrictionTypeDescription": "${request.restrictionType.toRestrictionType().description}",
          "comment": "$NO_COMMENT_PROVIDED",
          "updatedBy": "$SYSTEM_USERNAME",
          "isClosed": true,
          "closedReason": "$NO_CLOSURE_REASON_PROVIDED",
          "closedBy": "$SYSTEM_USERNAME",
          "closedAt": "${request.expiryDate?.atStartOfDay()?.format(dtFormat)}",
          "whenCreated": "${request.effectiveFromDate.atStartOfDay().format(dtFormat)}",
          "whenUpdated": "${request.expiryDate?.atStartOfDay()?.format(dtFormat)}"
        }
        """

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(expectedResponse, false)
    }

    @Test
    fun `closing a non-association`() {
      val naUpdate = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
        ),
      )

      val request = UpsertSyncRequest(
        firstPrisonerNumber = "C1234AA",
        secondPrisonerNumber = "D1234AA",
        firstPrisonerReason = LegacyReason.RIV,
        secondPrisonerReason = LegacyReason.RIV,
        restrictionType = LegacyRestrictionType.WING,
        expiryDate = LocalDate.now(clock),
        effectiveFromDate = LocalDate.now(clock).minusDays(5),
        comment = "Its ok now",
        authorisedBy = "TEST",
        lastModifiedByUsername = "A_NOMIS_USER_THAT_CLOSES",
      )

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "C1234AA",
          "firstPrisonerRole": "NOT_RELEVANT",
          "firstPrisonerRoleDescription": "Not relevant",
          "secondPrisonerNumber": "D1234AA",
          "secondPrisonerRole": "NOT_RELEVANT",
          "secondPrisonerRoleDescription": "Not relevant",
          "reason": "GANG_RELATED",
          "reasonDescription": "Gang related",
          "restrictionType": "${request.restrictionType.toRestrictionType()}",
          "restrictionTypeDescription": "${request.restrictionType.toRestrictionType().description}",
          "comment": "${request.comment}",
          "updatedBy": "A_NOMIS_USER_THAT_CLOSES",
          "isClosed": true,
          "closedReason": "$NO_CLOSURE_REASON_PROVIDED",
          "closedBy": "A_NOMIS_USER_THAT_CLOSES",
          "closedAt": "${request.expiryDate?.atStartOfDay()?.format(dtFormat)}"
        }
        """

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(expectedResponse, false)

      repository.deleteById(naUpdate.id!!)
    }

    @Test
    fun `re-opening a non-association`() {
      val stayClosed = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          createTime = LocalDateTime.now(clock).minusDays(30),
          closed = true,
          closedReason = "All fine now oldest",
        ),
      )

      val reopened = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          createTime = LocalDateTime.now(clock).minusHours(1),
          closed = true,
          closedReason = "All fine now newest",
        ),
      )

      val request = UpsertSyncRequest(
        firstPrisonerNumber = "C1234AA",
        secondPrisonerNumber = "D1234AA",
        firstPrisonerReason = LegacyReason.PER,
        secondPrisonerReason = LegacyReason.BUL,
        restrictionType = LegacyRestrictionType.WING,
        effectiveFromDate = LocalDate.now(clock),
        comment = "Its kicked off again",
        authorisedBy = "A free text field for staff name",
      )

      val expectedResponse =
        // language=json
        """
        {
          "id": ${reopened.id},
          "firstPrisonerNumber": "C1234AA",
          "firstPrisonerRole": "PERPETRATOR",
          "firstPrisonerRoleDescription": "Perpetrator",
          "secondPrisonerNumber": "D1234AA",
          "secondPrisonerRole": "UNKNOWN",
          "secondPrisonerRoleDescription": "Unknown",
          "reason": "BULLYING",
          "reasonDescription": "Bullying",
          "restrictionType": "${request.restrictionType.toRestrictionType()}",
          "restrictionTypeDescription": "${request.restrictionType.toRestrictionType().description}",
          "comment": "${request.comment}",
          "updatedBy": "$SYSTEM_USERNAME",
          "isClosed": false,
          "closedReason": null,
          "closedBy": null,
          "closedAt": null
        }
        """

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(expectedResponse, false)

      assertThat(repository.findById(reopened.id!!).get().isClosed).isFalse()
      assertThat(repository.findById(stayClosed.id!!).get().isClosed).isTrue()
      repository.deleteById(reopened.id!!)
    }
  }

  @Nested
  inner class `Delete Sync a non-association` {

    private val url = "/sync/delete"

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.post()
        .uri(url)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role and scope responds 403 Forbidden`() {
      val request = DeleteSyncRequest(
        firstPrisonerNumber = "A7777XX",
        secondPrisonerNumber = "B7777XX",
      )

      // correct role, missing write scope
      webTestClient.put()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY")))
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `without a valid request body responds 400 Bad Request`() {
      // no request body
      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isBadRequest

      // unsupported Content-Type
      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isBadRequest

      // request body missing some fields
      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString("firstPrisonerNumber" to "A1234BC"))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `deleting a non-association`() {
      val naActive = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          updatedBy = "TEST",
        ),
      )
      val naClosed1 = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          createTime = LocalDateTime.now(clock).minusDays(1),
          closedReason = "OK now again",
          closed = true,
          updatedBy = "TEST",
        ),
      )
      val naClosed2 = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          createTime = LocalDateTime.now(clock).minusMonths(1),
          closedReason = "OK now",
          closed = true,
          updatedBy = "TEST",
        ),
      )
      val request = DeleteSyncRequest(
        firstPrisonerNumber = "C1234AA",
        secondPrisonerNumber = "D1234AA",
      )

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .bodyValue(jsonString(request))
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.findById(naActive.id!!)).isNotPresent
      assertThat(repository.findById(naClosed1.id!!)).isNotPresent
      assertThat(repository.findById(naClosed2.id!!)).isNotPresent
    }

    @Test
    fun `deleting a non-association by ID`() {
      val toDelete = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          updatedBy = "TEST",
        ),
      )

      webTestClient.delete()
        .uri("/sync/${toDelete.id}")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.findById(toDelete.id!!)).isNotPresent
    }
  }
}