package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config

import com.nimbusds.jwt.SignedJWT
import io.opentelemetry.api.trace.Span
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.kotlin.clienttracking.HmppsClientTrackingInterceptor
import java.text.ParseException

/**
 * Replaces the default client tracking from hmpps-kotlin-lib, which records only `username` and `clientId`,
 * so that the HMPPS Auth user UUID is also recorded in App Insights as `userUuid` for audit purposes.
 *
 * HMPPS Auth only includes `user_uuid` in tokens issued when a user signs in,
 * not in system tokens requested on behalf of a user, so it will be missing for many requests.
 *
 * NB: this bean must not be named `clientTrackingInterceptor` as that disables the library's registration of it.
 */
@Configuration
class AppInsightsClientTrackingConfiguration {
  @Bean
  fun hmppsClientTrackingInterceptor() = HmppsClientTrackingInterceptor(setTrackingDetails = { token ->
    setTrackingDetails(token)
  })

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun setTrackingDetails(token: String) {
      if (!token.startsWith("Bearer ")) return
      try {
        val span = Span.current()
        val claims = SignedJWT.parse(token.removePrefix("Bearer ")).jwtClaimsSet
        claims.getStringClaim("user_name")?.let {
          span.setAttribute("username", it) // username in customDimensions
          span.setAttribute("enduser.id", it) // user_Id at the top level of the request
        }
        claims.getStringClaim("user_uuid")?.let { span.setAttribute("userUuid", it) }
        claims.getStringClaim("client_id")?.let { span.setAttribute("clientId", it) }
      } catch (e: ParseException) {
        log.warn("Problem decoding JWT for application insights", e)
      }
    }
  }
}
