package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey

class AppInsightsClientTrackingConfigurationTest {
  private val interceptor = AppInsightsClientTrackingConfiguration().hmppsClientTrackingInterceptor()
  private val tracer: Tracer = otelTesting.openTelemetry.getTracer("test")

  @Test
  fun `records username, user UUID and client id from a user sign-in token`() {
    trackRequest(
      "Bearer " + createToken(
        "user_name" to "TEST_USER",
        "user_uuid" to "d2f1c3a4-5b6e-4f70-8a9b-0c1d2e3f4a5b",
        "client_id" to "hmpps-non-associations",
      ),
    )

    assertThat(recordedAttributes()).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "username" to "TEST_USER",
        "enduser.id" to "TEST_USER",
        "userUuid" to "d2f1c3a4-5b6e-4f70-8a9b-0c1d2e3f4a5b",
        "clientId" to "hmpps-non-associations",
      ),
    )
  }

  @Test
  fun `records username and client id from a system token requested on behalf of a user, which has no UUID`() {
    trackRequest(
      "Bearer " + createToken(
        "user_name" to "TEST_USER",
        "client_id" to "hmpps-non-associations-system",
      ),
    )

    assertThat(recordedAttributes()).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "username" to "TEST_USER",
        "enduser.id" to "TEST_USER",
        "clientId" to "hmpps-non-associations-system",
      ),
    )
  }

  @Test
  fun `records only client id from a system token with no user`() {
    trackRequest("Bearer " + createToken("client_id" to "hmpps-non-associations-system"))

    assertThat(recordedAttributes()).containsExactlyInAnyOrderEntriesOf(
      mapOf("clientId" to "hmpps-non-associations-system"),
    )
  }

  @Test
  fun `records nothing when the authorisation header is not a bearer token`() {
    trackRequest("Basic dXNlcjpwYXNzd29yZA==")

    assertThat(recordedAttributes()).isEmpty()
  }

  @Test
  fun `records nothing and does not fail when the bearer token cannot be parsed`() {
    trackRequest("Bearer not-a-jwt")

    assertThat(recordedAttributes()).isEmpty()
  }

  @Test
  fun `records nothing when there is no authorisation header`() {
    trackRequest(null)

    assertThat(recordedAttributes()).isEmpty()
  }

  private fun trackRequest(authorisationHeader: String?) {
    val request = MockHttpServletRequest().apply {
      authorisationHeader?.let { addHeader(HttpHeaders.AUTHORIZATION, it) }
    }
    val span = tracer.spanBuilder("request").startSpan()
    span.makeCurrent().use {
      assertThat(interceptor.preHandle(request, MockHttpServletResponse(), "handler")).isTrue()
    }
    span.end()
  }

  private fun recordedAttributes(): Map<String, String> {
    val span = otelTesting.spans.single()
    return span.attributes.asMap().keys.associate { it.key to span.attributes.get(stringKey(it.key))!! }
  }

  private fun createToken(vararg claims: Pair<String, String>): String {
    val claimsSet = JWTClaimsSet.Builder().apply { claims.forEach { (name, value) -> claim(name, value) } }.build()
    return SignedJWT(JWSHeader(JWSAlgorithm.RS256), claimsSet)
      .apply { sign(RSASSASigner(keyPair.private as RSAPrivateKey)) }
      .serialize()
  }

  private companion object {
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    @JvmStatic
    @RegisterExtension
    val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
  }
}
