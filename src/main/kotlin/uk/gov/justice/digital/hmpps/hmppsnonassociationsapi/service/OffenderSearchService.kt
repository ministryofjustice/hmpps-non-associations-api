package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.MissingPrisonersInSearchException
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
  fun searchByPrisonerNumbers(prisonerNumbers: Collection<String>): Map<String, OffenderSearchPrisoner> {
    if (prisonerNumbers.isEmpty()) {
      return emptyMap()
    }

    val requests = prisonerNumbers
      .toSet()
      .chunked(900)
      .map { pageOfPrisonerNumbers ->
        val requestBody = mapOf("prisonerNumbers" to pageOfPrisonerNumbers)
        offenderSearchWebClient
          .post()
          .uri(
            "/prisoner-search/prisoner-numbers?responseFields=prisonerNumber,firstName,lastName,prisonId,prisonName,cellLocation",
          )
          .header("Content-Type", "application/json")
          .bodyValue(requestBody)
          .retrieve()
          .bodyToMono<List<OffenderSearchPrisoner>>()
      }
    val foundPrisoners = Flux.merge(requests)
      .collectList()
      .block()!!
      .flatten()
      .associateBy(OffenderSearchPrisoner::prisonerNumber)

    // Throw an exception if any of the prisoners searched were not found
    val missingPrisoners = prisonerNumbers.subtract(foundPrisoners.keys)
    if (missingPrisoners.any()) {
      throw MissingPrisonersInSearchException(missingPrisoners)
    }

    return foundPrisoners
  }
}
