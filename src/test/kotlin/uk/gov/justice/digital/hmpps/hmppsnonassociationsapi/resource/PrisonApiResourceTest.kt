package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.IntegrationTestBase

class PrisonApiResourceTest : IntegrationTestBase() {

  @Nested
  inner class `GET non association details by bookingId` {
    var bookingId: Long = 123456

    @BeforeEach
    fun startMocks() {
      prisonApiMockServer.start()
      prisonApiMockServer.stubGetNonAssociationDetails(bookingId)
    }

    @AfterEach
    fun stopMocks() {
      prisonApiMockServer.stop()
    }

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.get()
        .uri("/legacy/api/bookings/$bookingId/non-association-details")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `with a valid token returns the non-association details`() {
      webTestClient.get()
        .uri("/legacy/api/bookings/$bookingId/non-association-details")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
            {
              "offenderNo": "A1234AB"
            }
            """,
          true,
        )
    }
  }
}
