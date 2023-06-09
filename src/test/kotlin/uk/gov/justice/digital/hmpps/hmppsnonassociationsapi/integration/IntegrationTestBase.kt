package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.wiremock.PrisonApiMockServer
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class IntegrationTestBase : TestBase() {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @SpyBean
  protected lateinit var telemetryClient: TelemetryClient

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  companion object {
    val clock: Clock = Clock.fixed(
      Instant.parse("2022-03-15T12:34:56+00:00"),
      ZoneId.of("Europe/London"),
    )

    @JvmField
    val prisonApiMockServer = PrisonApiMockServer()

    @JvmField
    val hmppsAuthMockServer = HmppsAuthMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      hmppsAuthMockServer.start()
      hmppsAuthMockServer.stubGrantToken()

      prisonApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
      hmppsAuthMockServer.stop()
    }
  }

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  protected fun setAuthorisation(
    user: String = "NON_ASSOCIATIONS_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  protected fun jsonString(any: Any) = objectMapper.writeValueAsString(any) as String
}
