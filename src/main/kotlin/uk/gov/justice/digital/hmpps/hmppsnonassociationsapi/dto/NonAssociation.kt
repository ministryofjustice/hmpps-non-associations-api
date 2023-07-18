package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

/**
 * Non-association between two prisoners
 */
data class NonAssociation(
  @Schema(description = "ID of the non-association", required = false, example = "42")
  val id: Long,

  @Schema(description = "Prisoner number to not associate", required = true, example = "A1234BC")
  val firstPrisonerNumber: String,
  @Schema(description = "Reason why this prisoner should be kept apart from the other", required = true, example = "VICTIM")
  val firstPrisonerReason: NonAssociationReason,
  @Schema(description = "Prisoner number to not associate", required = true, example = "D5678EF")
  val secondPrisonerNumber: String,
  @Schema(description = "Reason why this prisoner should be kept apart from the other", required = true, example = "PERPETRATOR")
  val secondPrisonerReason: NonAssociationReason,

  @Schema(description = "Type of restriction, e.g. don't locate in the same cell", required = true, example = "CELL")
  val restrictionType: NonAssociationRestrictionType,

  @Schema(description = "Explanation of why prisoners are non-associated", required = true, example = "John and Luke always end up fighting")
  val comment: String,
  @Schema(description = "User ID of the person who created the non-association. NOTE: For records migrated from NOMIS/Prison API this is free text and may not be a valid User ID", required = true, example = "OFF3_GEN")
  val authorisedBy: String,
  @Schema(description = "When the non-association was created", required = true, example = "2021-12-31T12:34:56.789012")
  val whenCreated: LocalDateTime,

  @Schema(description = "Whether the non-association is closed or is in effect", required = true, example = "false")
  val isClosed: Boolean = false,
  @Schema(description = "User ID of the person who closed the non-association. Only present when the non-association is closed, null for open non-associations", required = false, example = "null")
  val closedBy: String? = null,
  @Schema(description = "Reason why the non-association was closed. Only present when the non-association is closed, null for open non-associations", required = false, example = "null")
  val closedReason: String? = null,
  @Schema(description = "Date and time of when the non-association was closed. Only present when the non-association is closed, null for open non-associations", required = false, example = "null")
  val closedAt: String? = null,
)

/**
 * Request format for creating a new, open, non-association between two prisoners
 */
data class CreateNonAssociationRequest(
  @Schema(description = "Prisoner number to not associate", required = true, example = "A1234BC")
  val firstPrisonerNumber: String,
  @Schema(description = "Reason why this prisoner should be kept apart from the other", required = true, example = "VICTIM")
  val firstPrisonerReason: NonAssociationReason,
  @Schema(description = "Prisoner number to not associate", required = true, example = "D5678EF")
  val secondPrisonerNumber: String,
  @Schema(description = "Reason why this prisoner should be kept apart from the other", required = true, example = "PERPETRATOR")
  val secondPrisonerReason: NonAssociationReason,

  @Schema(description = "Type of restriction, e.g. don't locate in the same cell", required = true, example = "CELL")
  val restrictionType: NonAssociationRestrictionType,

  @Schema(description = "Type of restriction, e.g. don't locate in the same cell", required = true, example = "John and Luke always end up fighting")
  val comment: String,
) {
  fun toNewEntity(authorisedBy: String): NonAssociationJPA {
    return NonAssociationJPA(
      id = null,
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerReason = firstPrisonerReason,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerReason = secondPrisonerReason,
      restrictionType = restrictionType,
      comment = comment,
      authorisedBy = authorisedBy,
    )
  }
}
