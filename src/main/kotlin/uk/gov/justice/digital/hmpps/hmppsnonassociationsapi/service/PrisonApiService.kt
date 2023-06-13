package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.NonAssociationDetails

@Service
class PrisonApiService(
  private val prisonWebClient: WebClient,
  private val prisonWebClientClientCredentials: WebClient,
) {

  private fun getClient(useClientCredentials: Boolean = false): WebClient {
    return if (useClientCredentials) prisonWebClientClientCredentials else prisonWebClient
  }

  suspend fun getNonAssociationDetails(prisonerNumber: String, useClientCredentials: Boolean = false): NonAssociationDetails {
    return getClient(useClientCredentials)
      .get()
      .uri("/api/offenders/$prisonerNumber/non-association-details")
      .retrieve()
      .awaitBody()
  }
}
