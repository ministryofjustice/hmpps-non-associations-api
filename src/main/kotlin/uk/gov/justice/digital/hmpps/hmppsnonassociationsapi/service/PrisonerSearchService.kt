package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.MissingPrisonersInSearchException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonersearch.Prisoner

@Service
class PrisonerSearchService(
  private val prisonerSearchWebClient: WebClient,
  objectMapper: ObjectMapper,
) {
  private val responseFields by lazy {
    objectMapper.serializerProviderInstance.findValueSerializer(Prisoner::class.java).properties()
      .asSequence()
      .joinToString(",") { it.name }
  }

  /**
   * Search prisoners by their prisoner number
   *
   * Requires ROLE_GLOBAL_SEARCH or ROLE_PRISONER_SEARCH role.
   */
  fun searchByPrisonerNumbers(prisonerNumbers: Collection<String>): Map<String, Prisoner> {
    if (prisonerNumbers.isEmpty()) {
      return emptyMap()
    }

    val requests = prisonerNumbers
      .toSet()
      .chunked(900)
      .map { pageOfPrisonerNumbers ->
        val requestBody = mapOf("prisonerNumbers" to pageOfPrisonerNumbers)
        prisonerSearchWebClient
          .post()
          .uri(
            "/prisoner-search/prisoner-numbers?responseFields={responseFields}",
            mapOf("responseFields" to responseFields),
          )
          .header("Content-Type", "application/json")
          .bodyValue(requestBody)
          .retrieve()
          .bodyToMono<List<Prisoner>>()
      }
    val foundPrisoners = Flux.merge(requests)
      .collectList()
      .block()!!
      .flatten()
      .associateBy(Prisoner::prisonerNumber)

    // Throw an exception if any of the prisoners searched were not found
    val missingPrisoners = prisonerNumbers.subtract(foundPrisoners.keys)
    if (missingPrisoners.any()) {
      throw MissingPrisonersInSearchException(missingPrisoners)
    }

    return foundPrisoners
  }
}
