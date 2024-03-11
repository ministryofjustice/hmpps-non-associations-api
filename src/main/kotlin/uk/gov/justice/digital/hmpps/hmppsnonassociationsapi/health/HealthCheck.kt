package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component("offenderSearchApi")
class OffenderSearchApiHealth(@Qualifier("offenderSearchHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("hmppsAuthApi")
class HmppsAuthApiHealth(@Qualifier("authHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)
