package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.MigrateRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationRestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.SyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase

@WithMockUser
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
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = "D7777XX",
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
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
    fun `for a valid request migrates the non-association`() {
      val request = MigrateRequest(
        firstPrisonerNumber = "C7777XX",
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = "D7777XX",
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
        comment = "This is a comment",
      )

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${request.firstPrisonerNumber}",
          "firstPrisonerReason": "${request.firstPrisonerReason}",
          "secondPrisonerNumber": "${request.secondPrisonerNumber}",
          "secondPrisonerReason": "${request.secondPrisonerReason}",
          "restrictionType": "${request.restrictionType}",
          "comment": "${request.comment}",
          "authorisedBy": "$SYSTEM_USERNAME",
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
      val request = SyncRequest(
        firstPrisonerNumber = "A7777XX",
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = "B7777XX",
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
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
    fun `for a valid request sync's the non-association`() {
      val request = SyncRequest(
        firstPrisonerNumber = "A7777XX",
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = "B7777XX",
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
      )

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${request.firstPrisonerNumber}",
          "firstPrisonerReason": "${request.firstPrisonerReason}",
          "secondPrisonerNumber": "${request.secondPrisonerNumber}",
          "secondPrisonerReason": "${request.secondPrisonerReason}",
          "restrictionType": "${request.restrictionType}",
          "comment": "NO COMMENT",
          "authorisedBy": "$SYSTEM_USERNAME",
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
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json(expectedResponse, false)
    }
  }
}
