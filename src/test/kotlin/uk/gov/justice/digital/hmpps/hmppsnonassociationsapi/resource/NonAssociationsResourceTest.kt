package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationRestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.createNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase

class NonAssociationsResourceTest : SqsIntegrationTestBase() {

  final val prisonerNumber = "A1234BC"

  @Nested
  inner class `Create a non-association` {

    private val url = "/non-associations"

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
      val request = createNonAssociationRequest(
        firstPrisonerNumber = "A1234BC",
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = "D5678EF",
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
        comment = "They keep fighting",
      )

      // correct role, missing write scope
      webTestClient.post()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus()
        .isForbidden

      // correct role, missing write scope
      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("read"),
          ),
        )
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
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
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
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
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
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue(jsonString("firstPrisonerNumber" to "A1234BC"))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `for a valid request creates the non-association`() {
      val request: CreateNonAssociationRequest = createNonAssociationRequest(
        firstPrisonerNumber = "A1234BC",
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = "D5678EF",
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
        comment = "They keep fighting",
      )

      val expectedUsername = "A_TEST_USER"
      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${request.firstPrisonerNumber}",
          "firstPrisonerReason": "${request.firstPrisonerReason}",
          "secondPrisonerNumber": "${request.secondPrisonerNumber}",
          "secondPrisonerReason" = "${request.secondPrisonerReason}",
          "restrictionType": "${request.restrictionType}",
          "comment": "${request.comment}",
          "authorisedBy": "$expectedUsername",
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
            user = expectedUsername,
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json(expectedResponse, false)

      // TODO: Make request to `GET /non-associations/{id}` once it exists
    }
  }

  @Nested
  inner class `GET non associations for a prisoner` {

    private val url = "/prisoner/$prisonerNumber/non-associations"

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.get()
        .uri(url)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role responds 403 Forbidden`() {
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `with a valid token returns the non-association details`() {
      val expectedResponse = jsonString(mapOf("prisonerNumber" to prisonerNumber))
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          expectedResponse,
          true,
        )
    }
  }
}
