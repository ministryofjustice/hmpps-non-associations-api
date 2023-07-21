package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation

@Schema(description = "Migrate Request")
data class MigrateRequest(
  @Schema(description = "Prisoner number to not associate", required = true, example = "A1234BC")
  val firstPrisonerNumber: String,
  @Schema(
    description = "Reason why this prisoner should be kept apart from the other",
    required = true,
    example = "VICTIM",
  )
  val firstPrisonerReason: NonAssociationReason,
  @Schema(description = "Prisoner number to not associate", required = true, example = "D5678EF")
  val secondPrisonerNumber: String,
  @Schema(
    description = "Reason why this prisoner should be kept apart from the other",
    required = true,
    example = "PERPETRATOR",
  )
  val secondPrisonerReason: NonAssociationReason,

  @Schema(description = "Type of restriction, e.g. don't locate in the same cell", required = true, example = "CELL")
  val restrictionType: NonAssociationRestrictionType,

  @Schema(
    description = "Type of restriction, e.g. don't locate in the same cell",
    required = false,
    example = "John and Luke always end up fighting",
  )
  val comment: String? = null,

  @Schema(description = "Who authorised the non-association", required = false, example = "John Smith")
  val authorisedBy: String? = null,
) {
  fun toNewEntity(): NonAssociation {
    return NonAssociation(
      id = null,
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerReason = firstPrisonerReason,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerReason = secondPrisonerReason,
      restrictionType = restrictionType,
      comment = comment ?: "NO COMMENT",
      authorisedBy = authorisedBy ?: SYSTEM_USERNAME,
    )
  }

  override fun toString(): String {
    return "MigrateRequest(firstPrisonerNumber='$firstPrisonerNumber', secondPrisonerNumber='$secondPrisonerNumber')"
  }
}
