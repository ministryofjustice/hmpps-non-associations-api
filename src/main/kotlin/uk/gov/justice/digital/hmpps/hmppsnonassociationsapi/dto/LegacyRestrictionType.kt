package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

/**
 * Type of non-association restriction, e.g. "Do not locate in same cell"
 *
 * Values taken from Prison API `references_codes` table: https://github.com/ministryofjustice/prison-api/blob/acb4076b98c706c49edd61f21061c84eb6a837bf/src/main/resources/db/migration/data/R__2_3__REFERENCE_CODES.sql#LL5185-L5189
 */
enum class LegacyRestrictionType(val description: String) {
  CELL("Do Not Locate in Same Cell"),
  LAND("Do Not Locate on Same Landing"),
  NONEX("Do Not Exercise Together"),
  TNA("Total Non Association"),
  WING("Do Not Locate on Same Wing"),
  ;

  fun toRestrictionType(): RestrictionType = when (this) {
    CELL -> RestrictionType.CELL
    LAND -> RestrictionType.LANDING
    WING -> RestrictionType.WING

    // following legacy restriction types do not map clearly
    NONEX -> RestrictionType.WING
    TNA -> RestrictionType.WING
  }
}
