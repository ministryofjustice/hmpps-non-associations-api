package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
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
  ): Map<String, OffenderSearchPrisoner> {
    val requestBody = objectMapper.writeValueAsString("prisonerNumbers" to prisonerNumbers)

    val foundPrisoners = getClient(useClientCredentials)
      .post()
      .uri("/prisoner-search/prisoner-numbers")
      .header("Content-Type", "application/json")
      .bodyValue(requestBody)
      .retrieve()
      .bodyToMono<List<OffenderSearchPrisoner>>()
      .block()!!
      .associateBy(OffenderSearchPrisoner::prisonerNumber)

    // Throw an exception if any of the prisoners searched were not found
    val missingPrisoners = prisonerNumbers.subtract(foundPrisoners.keys)
    if (missingPrisoners.any()) {
      throw ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Could not find the following prisoners: $missingPrisoners",
      )
    }

    return foundPrisoners
  }
}
