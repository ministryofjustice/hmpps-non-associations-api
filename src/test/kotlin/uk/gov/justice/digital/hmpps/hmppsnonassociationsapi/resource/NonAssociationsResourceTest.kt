package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase

class NonAssociationsResourceTest : SqsIntegrationTestBase() {

  final val prisonerNumber = "A1234BC"

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
