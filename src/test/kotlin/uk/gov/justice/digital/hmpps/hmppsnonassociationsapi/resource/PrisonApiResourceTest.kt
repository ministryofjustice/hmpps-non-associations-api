package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.IntegrationTestBase

class PrisonApiResourceTest : IntegrationTestBase() {

  @Nested
  inner class `GET non associations details by bookingId` {
    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.get()
        .uri("/api/bookings/123456/non-associations-details")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `with a valid token returns the list of non-associations`() {
      webTestClient.get()
        .uri("/api/bookings/123456/non-associations-details")
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
