package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

/**
 * Reason why a prisoner should not be associated with another, e.g. Victim
 *
 * Values taken from Prison API `references_codes` table: https://github.com/ministryofjustice/prison-api/blob/acb4076b98c706c49edd61f21061c84eb6a837bf/src/main/resources/db/migration/data/R__2_3__REFERENCE_CODES.sql#LL5181-L5184
 */
enum class LegacyReason(val description: String) {
  BUL("Anti Bullying Strategy"),
  PER("Perpetrator"),
  RIV("Rival Gang"),
  VIC("Victim"),
  NOT_RELEVANT("Not relevant"),
  UNKNOWN("Unknown"),
  ;

  fun toRole() = when (this) {
    PER -> Role.PERPETRATOR
    VIC -> Role.VICTIM
    NOT_RELEVANT -> Role.NOT_RELEVANT
    UNKNOWN -> Role.UNKNOWN

    // following legacy reasons do not map clearly to a role
    BUL -> Role.UNKNOWN
    RIV -> Role.UNKNOWN
  }

  fun toReason() =
    when (this) {
      BUL -> Reason.BULLYING
      PER -> Reason.OTHER
      RIV -> Reason.GANG_RELATED
      VIC -> Reason.OTHER
      NOT_RELEVANT -> Reason.OTHER
      UNKNOWN -> Reason.OTHER
    }
}

fun translateToReason(firstPrisonerReason: LegacyReason, secondPrisonerReason: LegacyReason) =
  if (firstPrisonerReason.toReason() == Reason.OTHER) {
    secondPrisonerReason.toReason()
  } else {
    firstPrisonerReason.toReason()
  }
