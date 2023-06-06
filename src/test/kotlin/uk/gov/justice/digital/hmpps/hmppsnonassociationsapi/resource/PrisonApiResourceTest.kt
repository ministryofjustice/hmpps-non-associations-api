package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.IntegrationTestBase

class PrisonApiResourceTest : IntegrationTestBase() {

  @Nested
  inner class `GET non association details by bookingId` {
    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.get()
        .uri("/legacy/api/bookings/123456/non-association-details")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `with a valid token returns the non-association details`() {
      webTestClient.get()
        .uri("/legacy/api/bookings/123456/non-association-details")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
            { "offenderNo": "TODO: Implement me" }
            """,
          true,
        )
    }
  }
}
