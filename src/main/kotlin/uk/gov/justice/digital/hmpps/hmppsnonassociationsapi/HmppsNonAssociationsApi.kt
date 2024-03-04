package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

/**
 * Used as the system OAuth username when calling HMPPS APIs
 */
const val SYSTEM_USERNAME = "NON_ASSOCIATIONS_API"

@SpringBootApplication()
@ComponentScan(
  "uk.gov.justice.digital.hmpps.hmppsnonassociationsapi",
  "uk.gov.justice.digital.hmpps.digitalprisonreportinglib",
)
class HmppsNonAssociationsApi

fun main(args: Array<String>) {
  runApplication<HmppsNonAssociationsApi>(*args)
}
