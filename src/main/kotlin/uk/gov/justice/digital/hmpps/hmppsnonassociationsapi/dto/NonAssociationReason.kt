package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

/**
 * Reason why a prisoner should not be associated with another, e.g. Victim
 *
 * Values taken from Prison API `references_codes` table: https://github.com/ministryofjustice/prison-api/blob/acb4076b98c706c49edd61f21061c84eb6a837bf/src/main/resources/db/migration/data/R__2_3__REFERENCE_CODES.sql#LL5181-L5184
 * */
enum class NonAssociationReason(val code: String, val description: String) {
  BULLYING("BUL", "Anti Bullying Strategy"),
  PERPETRATOR("PER", "Perpetrator"),
  RIVAL_GANG("RIV", "Rival Gang"),
  VICTIM("VIC", "Victim"),
  ;

  companion object {
    fun getByCode(code: String): NonAssociationReason? {
      return NonAssociationReason.values().associateBy(NonAssociationReason::code)[code]
    }
  }
}
