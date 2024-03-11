package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.offendersearch.OffenderSearchPrisoner

@Service
class OffenderSearchService(
  private val offenderSearchWebClient: WebClient,
) {
  /**
   * Search prisoners by their prisoner number
   *
   * Requires ROLE_GLOBAL_SEARCH or ROLE_PRISONER_SEARCH role.
   */
  fun searchByPrisonerNumbers(
    prisonerNumbers: Collection<String>,
  ): Map<String, OffenderSearchPrisoner> {
    val requestBody = mapOf("prisonerNumbers" to prisonerNumbers.toSet())

    val foundPrisoners = offenderSearchWebClient
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
