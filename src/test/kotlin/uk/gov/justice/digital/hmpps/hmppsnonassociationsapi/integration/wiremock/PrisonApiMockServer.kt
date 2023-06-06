package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get

class PrisonApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8093
  }

  fun getCountFor(url: String) = this.findAll(WireMock.getRequestedFor(WireMock.urlEqualTo(url))).count()

  fun stubGetNonAssociationDetails(bookingId: Long) {
    stubFor(
      get("/api/bookings/$bookingId/non-association-details").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            // language=json
            """
                {
                  "offenderNo": "A1234AB",
                  "nonAssociations": []
                }
            """,
          ),
      ),
    )
  }

  fun stubGetNonAssociationDetails(prisonerNumber: String) {
    stubFor(
      get("/api/offenders/$prisonerNumber/non-association-details").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            // language=json
            """
                {
                  "offenderNo": "A1234AB",
                  "nonAssociations": []
                }
            """,
          ),
      ),
    )
  }


  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status),
      ),
    )
  }

  fun stubGetPrisonerInfoByBooking(bookingId: Long, prisonerNumber: String, locationId: Long) {
    stubFor(
      get("/api/bookings/$bookingId?basicInfo=true").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            // language=json
            """
              {
                "agencyId": "MDI",
                "assignedLivingUnitId": $locationId,
                "bookingId": $bookingId,
                "bookingNo": "A12121",
                "firstName": "JOHN",
                "lastName": "SMITH",
                "offenderNo": "$prisonerNumber"
              }
            """,
          ),
      ),
    )
  }

}
