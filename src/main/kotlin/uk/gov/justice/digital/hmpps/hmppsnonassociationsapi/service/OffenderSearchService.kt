package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.offendersearch.OffenderSearchPrisoner

@Service
class OffenderSearchService(
  private val offenderSearchWebClient: WebClient,
  private val offenderSearchClientCredentials: WebClient,
  private val objectMapper: ObjectMapper,
) {

  private fun getClient(useClientCredentials: Boolean = false): WebClient {
    return if (useClientCredentials) offenderSearchClientCredentials else offenderSearchWebClient
  }

  /**
   * Search prisoners by their prisoner number
   *
   * Requires ROLE_GLOBAL_SEARCH or ROLE_PRISONER_SEARCH role.
   */
  fun searchByPrisonerNumbers(
    prisonerNumbers: List<String>,
    useClientCredentials: Boolean = false,
  ): List<OffenderSearchPrisoner> {
    val requestBody = objectMapper.writeValueAsString("prisonerNumbers" to prisonerNumbers)

    return getClient(useClientCredentials)
      .post()
      .uri("/prisoner-search/prisoner-numbers")
      .header("Content-Type", "application/json")
      .bodyValue(requestBody)
      .retrieve()
      .bodyToMono<List<OffenderSearchPrisoner>>()
      .block()!!
  }
}
