package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Used as the system OAuth username when calling HMPPS APIs
 */
const val SYSTEM_USERNAME = "NON_ASSOCIATIONS_API"

@SpringBootApplication()
class HmppsNonAssociationsApi

fun main(args: Array<String>) {
  runApplication<HmppsNonAssociationsApi>(*args)
}
