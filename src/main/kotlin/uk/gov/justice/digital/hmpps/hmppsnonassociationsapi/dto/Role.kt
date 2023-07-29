package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

enum class Role(val description: String) {
  VICTIM("Victim"),
  PERPETRATOR("Perpetrator"),
  NOT_RELEVANT("Not relevant"),
  UNKNOWN("Unknown"),
  ;

  fun toLegacyRole() = when (this) {
    VICTIM -> LegacyReason.VIC
    PERPETRATOR -> LegacyReason.PER

    // following reasons need to be added into NOMIS reference data
    NOT_RELEVANT -> LegacyReason.NOT_RELEVANT
    UNKNOWN -> LegacyReason.UNKNOWN
  }
}
