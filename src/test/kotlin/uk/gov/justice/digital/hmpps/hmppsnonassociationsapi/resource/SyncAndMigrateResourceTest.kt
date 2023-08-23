package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyRestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.MigrateRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Reason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.UpdateSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.genNonAssociation
import java.time.LocalDate
import java.time.LocalDateTime

@WithMockUser(username = expectedUsername)
class SyncAndMigrateResourceTest : SqsIntegrationTestBase() {

  @Nested
  inner class `Migrate a non-association` {

    private val url = "/migrate"

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
      val request = MigrateRequest(
        firstPrisonerNumber = "C7777XX",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "D7777XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        comment = "They keep fighting",
        authorisedBy = "Me",
        active = true,
      )

      // correct role, missing write scope
      webTestClient.post()
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
      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_MIGRATE"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isBadRequest

      // unsupported Content-Type
      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_MIGRATE"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isBadRequest

      // request body missing some fields
      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_MIGRATE"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString("firstPrisonerNumber" to "A1234BC"))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `already ready open NA rejects request`() {
      repository.save(
        genNonAssociation(
          firstPrisonerNumber = "D7777XX",
          secondPrisonerNumber = "C7777XX",
          createTime = LocalDateTime.now(clock),
        ),
      )

      val request = MigrateRequest(
        firstPrisonerNumber = "C7777XX",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "D7777XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        comment = "This is a comment",
        authorisedBy = "Test",
        active = true,
      )

      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_MIGRATE"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `for a valid request migrates the non-association`() {
      val request = MigrateRequest(
        firstPrisonerNumber = "C7777XX",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "D7777XX",
        secondPrisonerReason = LegacyReason.BUL,
        restrictionType = LegacyRestrictionType.CELL,
        comment = "This is a comment",
        authorisedBy = "Test",
        active = true,
      )

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${request.firstPrisonerNumber}",
          "firstPrisonerRole": "VICTIM",
          "firstPrisonerRoleDescription": "Victim",
          "secondPrisonerNumber": "${request.secondPrisonerNumber}",
          "secondPrisonerRole": "UNKNOWN",
          "secondPrisonerRoleDescription": "Unknown",
          "reason": "${Reason.BULLYING}",
          "reasonDescription": "${Reason.BULLYING.description}",
          "restrictionType": "${request.restrictionType.toRestrictionType()}",
          "restrictionTypeDescription": "${request.restrictionType.toRestrictionType().description}",
          "comment": "${request.comment}",
          "authorisedBy": "${request.authorisedBy}",
          "updatedBy": "$SYSTEM_USERNAME",
          "isClosed": false,
          "closedReason": null,
          "closedBy": null,
          "closedAt": null
        }
        """

      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_MIGRATE"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json(expectedResponse, false)
    }
  }

  @Nested
  inner class `Sync a non-association` {

    private val url = "/sync"

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
      val request = CreateSyncRequest(
        firstPrisonerNumber = "A7777XX",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "B7777XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        comment = "They keep fighting",
        authorisedBy = "Me",
      )

      // correct role, missing write scope
      webTestClient.post()
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
      webTestClient.post()
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
      webTestClient.post()
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
      webTestClient.post()
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
    fun `cannot sync non-association already in open state`() {
      repository.save(
        genNonAssociation(
          firstPrisonerNumber = "A7777XX",
          secondPrisonerNumber = "B7777XX",
          createTime = LocalDateTime.now(clock),
        ),
      )
      val request = CreateSyncRequest(
        firstPrisonerNumber = "A7777XX",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "B7777XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        expiryDate = LocalDate.now(clock).minusDays(4),
        active = true,
      )

      webTestClient.post()
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
      val request = CreateSyncRequest(
        firstPrisonerNumber = "A7777XX",
        firstPrisonerReason = LegacyReason.VIC,
        secondPrisonerNumber = "B7777XX",
        secondPrisonerReason = LegacyReason.PER,
        restrictionType = LegacyRestrictionType.CELL,
        expiryDate = LocalDate.now(clock).minusDays(4),
        active = false,
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
          "comment": "No comment provided",
          "authorisedBy": "",
          "updatedBy": "$SYSTEM_USERNAME",
          "isClosed": true,
          "closedReason": "UNDEFINED",
          "closedBy": "$SYSTEM_USERNAME",
          "closedAt": "${request.expiryDate?.atStartOfDay()?.format(dtFormat)}"
        }
        """

      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json(expectedResponse, false)
    }

    @Test
    fun `closing a non-association`() {
      val naUpdate = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          createTime = LocalDateTime.now(clock),
        ),
      )

      val request = UpdateSyncRequest(
        id = naUpdate.id!!,
        firstPrisonerReason = LegacyReason.RIV,
        secondPrisonerReason = LegacyReason.RIV,
        restrictionType = LegacyRestrictionType.WING,
        expiryDate = LocalDate.now(clock),
        active = false,
        comment = "Its ok now",
        authorisedBy = "TEST",
      )

      val expectedResponse =
        // language=json
        """
        {
          "id": ${request.id},
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
          "authorisedBy": "${request.authorisedBy}",
          "updatedBy": "$expectedUsername",
          "isClosed": true,
          "closedReason": "No closure reason provided",
          "closedBy": "TEST",
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

      repository.deleteById(request.id)
    }

    @Test
    fun `re-opening a non-association`() {
      val naUpdate = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          createTime = LocalDateTime.now(clock),
          closed = true,
          closedReason = "All fine now",
        ),
      )

      val request = UpdateSyncRequest(
        id = naUpdate.id!!,
        firstPrisonerReason = LegacyReason.PER,
        secondPrisonerReason = LegacyReason.BUL,
        restrictionType = LegacyRestrictionType.WING,
        expiryDate = LocalDate.now(clock),
        active = true,
        comment = "Its kicked off again",
        authorisedBy = "STAFF1",
      )

      val expectedResponse =
        // language=json
        """
        {
          "id": ${request.id},
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
          "authorisedBy": "${request.authorisedBy}",
          "updatedBy": "$expectedUsername",
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

      repository.deleteById(request.id)
    }

    @Test
    fun `deleting a non-association`() {
      val naDelete = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          createTime = LocalDateTime.now(clock),
          closedReason = "OK now",
          authBy = "TEST",
        ),
      )

      webTestClient.delete()
        .uri("$url/${naDelete.id}")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.findById(naDelete.id!!)).isNotPresent
    }
  }
}
