package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.MissingPrisonersInSearchException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.prisonerSearchPrisoners

@DisplayName("Prisoner search service")
class PrisonerSearchServiceTest {
  private val mapper = ObjectMapper().findAndRegisterModules()

  private fun createWebClientMockResponses(vararg responses: List<Prisoner>): WebClient {
    val responseIterator = responses.iterator()
    return WebClient.builder()
      .exchangeFunction {
        val response = if (!responseIterator.hasNext()) {
          ClientResponse.create(HttpStatus.NOT_FOUND).build()
        } else {
          ClientResponse.create(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(mapper.writeValueAsString(responseIterator.next()))
            .build()
        }
        Mono.just(response)
      }
      .build()
  }

  @Test
  fun `returns an empty map when passed no prisoner numbers`() {
    val webClient = createWebClientMockResponses()
    val prisonerSearchService = PrisonerSearchService(webClient, mapper)
    val prisoners = prisonerSearchService.searchByPrisonerNumbers(emptyList())
    assertThat(prisoners).isEmpty()
  }

  @Test
  fun `calls prisoner search once if passed few prisoner numbers`() {
    val webClient = createWebClientMockResponses(prisonerSearchPrisoners.values.toList())
    val prisonerSearchService = PrisonerSearchService(webClient, mapper)
    val prisoners = prisonerSearchService.searchByPrisonerNumbers(prisonerSearchPrisoners.keys.toList())
    assertThat(prisoners).hasSize(prisonerSearchPrisoners.size)
  }

  @Test
  fun `calls prisoner search repeatedly in pages of 900`() {
    // generate 900 prisoners for page 1
    val response1 = (1..900).map {
      Prisoner(
        String.format("A%04dAA", it),
        "First name",
        "Surname",
        null,
        null,
        null,
      )
    }
    // generate 900 prisoners for page 2
    val response2 = (900..<1800).map {
      Prisoner(
        String.format("A%04dAA", it),
        "First name",
        "Surname",
        null,
        null,
        null,
      )
    }
    // generate 200 prisoners for page 3
    val response3 = (1800..2000).map {
      Prisoner(
        String.format("A%04dAA", it),
        "First name",
        "Surname",
        null,
        null,
        null,
      )
    }

    val webClient = createWebClientMockResponses(response1, response2, response3)
    val prisonerSearchService = PrisonerSearchService(webClient, mapper)
    val prisoners = prisonerSearchService.searchByPrisonerNumbers((1..2000).map { String.format("A%04dAA", it) })
    assertThat(prisoners).hasSize(2000)
  }

  @Test
  fun `throws an error if any prisoners are not found`() {
    val webClient = createWebClientMockResponses(emptyList())
    val prisonerSearchService = PrisonerSearchService(webClient, mapper)
    assertThatThrownBy {
      prisonerSearchService.searchByPrisonerNumbers(listOf("A1234AA"))
    }.isInstanceOf(MissingPrisonersInSearchException::class.java).hasMessageContaining("A1234AA")
  }
}
