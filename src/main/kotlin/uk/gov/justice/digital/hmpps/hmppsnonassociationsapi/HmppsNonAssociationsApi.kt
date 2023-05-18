package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsNonAssociationsApi

fun main(args: Array<String>) {
  runApplication<HmppsNonAssociationsApi>(*args)
}
