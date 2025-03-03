package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper

import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.PostgresTestcontainer
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@ActiveProfiles("test")
abstract class TestBase {
  companion object {
    // All tests should use a frozen instant for “now”: 12:34:56 on 15 July 2023 in London
    val zoneId: ZoneId = ZoneId.of("Europe/London")
    val clock: Clock = Clock.fixed(
      Instant.parse("2023-07-15T12:34:56+00:00"),
      zoneId,
    )
    val now: LocalDateTime = LocalDateTime.now(clock)

    private val postgresInstance = PostgresTestcontainer.instance

    @Suppress("unused")
    @JvmStatic
    @DynamicPropertySource
    fun postgresProperties(registry: DynamicPropertyRegistry) {
      postgresInstance?.let { PostgresTestcontainer.setupProperties(postgresInstance, registry) }
    }
  }

  fun String.readResourceAsText(): String {
    return TestBase::class.java.getResource(this)?.readText()
      ?: throw AssertionError("Cannot find file $this")
  }
}
