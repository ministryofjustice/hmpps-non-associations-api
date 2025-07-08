package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value($$"${api.base.url.oauth}") private val authBaseUri: String,
  @param:Value($$"${api.base.url.offender-search}") private val offenderSearchUri: String,
  @param:Value($$"${api.timeout:20s}") val healthTimeout: Duration,
) {

  @Bean
  fun authHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(authBaseUri, healthTimeout)

  @Bean
  fun offenderSearchHealthWebClient(builder: WebClient.Builder): WebClient =
    builder.healthWebClient(offenderSearchUri, healthTimeout)

  @Bean
  fun offenderSearchWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    registrationId = SYSTEM_USERNAME,
    url = offenderSearchUri,
    healthTimeout,
  )
}
