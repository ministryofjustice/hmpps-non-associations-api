package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.wiremock.OffenderSearchMockServer
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class IntegrationTestBase : TestBase() {

  @Autowired
  lateinit var repository: NonAssociationsRepository

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  @MockitoSpyBean
  protected lateinit var telemetryClient: TelemetryClient

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  companion object {

    @JvmField
    val offenderSearchMockServer = OffenderSearchMockServer()

    @JvmField
    val hmppsAuthMockServer = HmppsAuthMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      hmppsAuthMockServer.start()
      hmppsAuthMockServer.stubGrantToken()

      offenderSearchMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      hmppsAuthMockServer.stop()
    }

    val dtFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
  }

  @BeforeEach
  fun setUp() {
    offenderSearchMockServer.resetAll()
    repository.deleteAll()
  }

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  protected fun setAuthorisation(
    user: String = SYSTEM_USERNAME,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthorisationHelper.setAuthorisationHeader(
    clientId = "hmpps-non-associations-api",
    username = user,
    scope = scopes,
    roles = roles,
  )

  protected fun jsonString(any: Any) = objectMapper.writeValueAsString(any) as String
}
