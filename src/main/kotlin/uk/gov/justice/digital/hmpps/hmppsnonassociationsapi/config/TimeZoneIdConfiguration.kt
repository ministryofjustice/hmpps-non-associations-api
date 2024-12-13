package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.ZoneId

@Configuration
class TimeZoneIdConfiguration(
  @Value("\${spring.jackson.time-zone}") private val timeZone: String,
) {
  @Bean
  fun timeZoneId(): ZoneId = ZoneId.of(timeZone)
}
