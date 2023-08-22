package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

enum class Role(val description: String) {
  VICTIM("Victim"),
  PERPETRATOR("Perpetrator"),
  NOT_RELEVANT("Not relevant"),
  UNKNOWN("Unknown"),
  ;
}
