package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

enum class RestrictionType(val description: String) {
  CELL("Cell only"),
  LANDING("Cell and landing"),
  WING("Cell, landing and wing"),
}
