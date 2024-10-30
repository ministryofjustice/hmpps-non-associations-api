package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.offendersearch.OffenderSearchPrisoner

class OffenderSearchMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8094
  }

  private val mapper = ObjectMapper().findAndRegisterModules()

  fun stubHealthPing(status: Int) {
    val stat = if (status == 200) "UP" else "DOWN"
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
          .withBody("""{"status": "$stat"}""")
          .withStatus(status),
      ),
    )
  }

  fun stubSearchByPrisonerNumbers(
    prisonerNumbers: List<String>,
    prisoners: List<OffenderSearchPrisoner>,
  ) {
    val requestBody = mapper.writeValueAsString(mapOf("prisonerNumbers" to prisonerNumbers.toSet()))

    stubFor(
      post("/prisoner-search/prisoner-numbers")
        .withRequestBody(
          WireMock.equalToJson(requestBody, true, false),
        )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsBytes(prisoners)),
        ),
    )
  }

  fun stubSearchFails() {
    stubFor(
      post("/prisoner-search/prisoner-numbers")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }
}
