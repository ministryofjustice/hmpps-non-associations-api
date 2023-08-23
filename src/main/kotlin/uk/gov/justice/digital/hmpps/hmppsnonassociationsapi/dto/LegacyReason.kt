package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

/**
 * Reason why a prisoner should not be associated with another, e.g. Victim
 */
enum class LegacyReason(val description: String) {
  BUL("Anti Bullying Strategy"),
  PER("Perpetrator"),
  RIV("Rival Gang"),
  VIC("Victim"),
  NOT_RELEVANT("Not relevant"),
  UNKNOWN("Unknown"),
  ;
}

/**
 * Translate a pair of "legacy" reasons from NOMIS into a "modern" pair of roles and a "modern" reason
 */
fun translateToRolesAndReason(firstPrisonerReason: LegacyReason, secondPrisonerReason: LegacyReason): Triple<Role, Role, Reason> {
  var firstPrisonerRole = Role.UNKNOWN
  var secondPrisonerRole = Role.UNKNOWN
  var reason = Reason.OTHER

  if (firstPrisonerReason == LegacyReason.BUL) {
    firstPrisonerRole = Role.UNKNOWN
    reason = Reason.BULLYING
  }
  if (secondPrisonerReason == LegacyReason.BUL) {
    secondPrisonerRole = Role.UNKNOWN
    reason = Reason.BULLYING
  }

  if (firstPrisonerReason == LegacyReason.RIV) {
    firstPrisonerRole = Role.NOT_RELEVANT
    reason = Reason.GANG_RELATED
  }
  if (secondPrisonerReason == LegacyReason.RIV) {
    secondPrisonerRole = Role.NOT_RELEVANT
    reason = Reason.GANG_RELATED
  }

  if (firstPrisonerReason == LegacyReason.VIC) {
    firstPrisonerRole = Role.VICTIM
  }
  if (secondPrisonerReason == LegacyReason.VIC) {
    secondPrisonerRole = Role.VICTIM
  }

  if (firstPrisonerReason == LegacyReason.PER) {
    firstPrisonerRole = Role.PERPETRATOR
  }
  if (secondPrisonerReason == LegacyReason.PER) {
    secondPrisonerRole = Role.PERPETRATOR
  }

  if (firstPrisonerReason == LegacyReason.NOT_RELEVANT) {
    firstPrisonerRole = Role.NOT_RELEVANT
  }
  if (secondPrisonerReason == LegacyReason.NOT_RELEVANT) {
    secondPrisonerRole = Role.NOT_RELEVANT
  }

  return Triple(firstPrisonerRole, secondPrisonerRole, reason)
}

/**
 * Translate a "modern" pair of roles and a "modern" reason into a pair of "legacy" reasons for NOMIS
 */
fun translateFromRolesAndReason(firstPrisonerRole: Role, secondPrisonerRole: Role, reason: Reason): Pair<LegacyReason, LegacyReason> {
  return when (reason) {
    Reason.GANG_RELATED -> LegacyReason.RIV to LegacyReason.RIV
    Reason.BULLYING -> LegacyReason.BUL to LegacyReason.BUL
    else -> firstPrisonerRole.toLegacyRole() to secondPrisonerRole.toLegacyRole()
  }
}

private fun Role.toLegacyRole() = when (this) {
  Role.VICTIM -> LegacyReason.VIC
  Role.PERPETRATOR -> LegacyReason.PER

  // following reasons need to be added into NOMIS reference data
  Role.NOT_RELEVANT -> LegacyReason.NOT_RELEVANT
  Role.UNKNOWN -> LegacyReason.UNKNOWN
}
