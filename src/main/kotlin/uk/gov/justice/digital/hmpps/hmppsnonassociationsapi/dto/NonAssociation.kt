package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

/**
 * Non-association between two prisoners
 */
@Schema(description = "Non-association")
data class NonAssociation(
  @Schema(description = "ID of the non-association", required = true, example = "42")
  val id: Long,

  @Schema(description = "Prisoner number to not associate", required = true, example = "A1234BC")
  val firstPrisonerNumber: String,
  @Schema(description = "This prisoner’s role code in the non-association", required = true, example = "VICTIM")
  val firstPrisonerRole: Role,
  @Schema(description = "This prisoner’s role description in the non-association", required = true, example = "Victim")
  val firstPrisonerRoleDescription: String,
  @Schema(description = "Prisoner number to not associate", required = true, example = "D5678EF")
  val secondPrisonerNumber: String,
  @Schema(description = "Other prisoner’s role code in the non-association", required = true, example = "PERPETRATOR")
  val secondPrisonerRole: Role,
  @Schema(description = "Other prisoner’s role description in the non-association", required = true, example = "Perpetrator")
  val secondPrisonerRoleDescription: String,

  @Schema(description = "Reason code why these prisoners should be kept apart", required = true, example = "BULLYING")
  val reason: Reason,
  @Schema(description = "Reason description why these prisoners should be kept apart", required = true, example = "Bullying")
  val reasonDescription: String,
  @Schema(description = "Location-based restriction code", required = true, example = "CELL")
  val restrictionType: RestrictionType,
  @Schema(description = "Location-based restriction description", required = true, example = "Cell only")
  val restrictionTypeDescription: String,

  @Schema(description = "Explanation of why prisoners are non-associated", required = true, example = "John and Luke always end up fighting")
  val comment: String,
  @Schema(description = "User ID of the person who created the non-association. NOTE: For records migrated from NOMIS/Prison API this is free text and may not be a valid User ID. Additionally, migrated records might use an internal system username", required = true, example = "OFF3_GEN")
  val authorisedBy: String,

  @Schema(description = "When the non-association was created", required = true, example = "2021-12-31T12:34:56.789012")
  val whenCreated: LocalDateTime,
  @Schema(description = "When the non-association was last updated", required = true, example = "2022-01-03T12:34:56.789012")
  val whenUpdated: LocalDateTime,
  @Schema(description = "User ID of the person who last updated the non-association", required = true, example = "OFF3_GEN")
  val updatedBy: String,

  @Schema(description = "Whether the non-association is closed or is in effect", required = true, example = "false")
  val isClosed: Boolean = false,
  @Schema(description = "User ID of the person who closed the non-association. Only present when the non-association is closed, null for open non-associations", required = false, example = "null")
  val closedBy: String? = null,
  @Schema(description = "Reason why the non-association was closed. Only present when the non-association is closed, null for open non-associations", required = false, example = "null")
  val closedReason: String? = null,
  @Schema(description = "Date and time of when the non-association was closed. Only present when the non-association is closed, null for open non-associations", required = false, example = "null")
  val closedAt: LocalDateTime? = null,
) {
  val isOpen: Boolean
    get() = !isClosed
}

/**
 * Request format for creating a new, open, non-association between two prisoners
 */
@Schema(description = "Request format for creating a new, open, non-association between two prisoners")
data class CreateNonAssociationRequest(
  @Schema(description = "Prisoner number to not associate", required = true, example = "A1234BC")
  val firstPrisonerNumber: String,
  @Schema(description = "This prisoner’s role in the non-association", required = true, example = "VICTIM")
  val firstPrisonerRole: Role,
  @Schema(description = "Prisoner number to not associate", required = true, example = "D5678EF")
  val secondPrisonerNumber: String,
  @Schema(description = "Other prisoner’s role in the non-association", required = true, example = "PERPETRATOR")
  val secondPrisonerRole: Role,

  @Schema(description = "Reason why these prisoners should be kept apart", required = true, example = "BULLYING")
  val reason: Reason,
  @Schema(description = "Location-based restriction code", required = true, example = "CELL")
  val restrictionType: RestrictionType,

  @Schema(description = "Explanation of why prisoners are non-associated", required = true, example = "John and Luke always end up fighting")
  val comment: String,
) {
  fun toNewEntity(authorisedBy: String): NonAssociationJPA {
    return NonAssociationJPA(
      id = null,
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerRole = firstPrisonerRole,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerRole = secondPrisonerRole,
      reason = reason,
      restrictionType = restrictionType,
      comment = comment,
      authorisedBy = authorisedBy,
      updatedBy = authorisedBy,
    )
  }
}

/**
 * Request format for updating a non-association between two prisoners
 */
@Schema(description = "Request format for updating a non-association between two prisoners")
data class PatchNonAssociationRequest(
  @Schema(description = "This prisoner’s role in the non-association", required = true, example = "VICTIM")
  val firstPrisonerRole: Role? = null,
  @Schema(description = "Other prisoner’s role in the non-association", required = true, example = "PERPETRATOR")
  val secondPrisonerRole: Role? = null,

  @Schema(description = "Reason why these prisoners should be kept apart", required = true, example = "BULLYING")
  val reason: Reason? = null,
  @Schema(description = "Location-based restriction code", required = true, example = "CELL")
  val restrictionType: RestrictionType? = null,

  @Schema(description = "Explanation of why prisoners are non-associated", required = true, example = "John and Luke always end up fighting")
  val comment: String? = null,
)

/**
 * Request format to close a non-association between two prisoners
 */
@Schema(description = "Request to close a non-association")
data class CloseNonAssociationRequest(
  @Schema(description = "Reason for closing the non-association", required = true, example = "They are friends now")
  val closedReason: String,
  @Schema(description = "Date and time of the closure, if not provided will default to today's time", required = false, example = "2023-06-07", defaultValue = "now")
  val closedAt: LocalDateTime? = null,
  @Schema(description = "The username of the member of staff requesting the closure, if not provided will use the user in the JWT access token", required = false, example = "ASMITH")
  val closedBy: String? = null,
)

/**
 * Request format to delete a non-association between two prisoners
 */
@Schema(description = "Request to delete a non-association")
data class DeleteNonAssociationRequest(
  @Schema(description = "Reason for deleting the non-association", required = true, example = "Created in error and removed on requested from OMU team")
  val deletionReason: String,
  @Schema(description = "The username of the member of staff requesting the deletion", required = true, example = "AJONES")
  val staffUserNameRequestingDeletion: String,
)

fun NonAssociationJPA.updateWith(patch: PatchNonAssociationRequest): NonAssociationJPA {
  this.firstPrisonerRole = patch.firstPrisonerRole ?: this.firstPrisonerRole
  this.secondPrisonerRole = patch.secondPrisonerRole ?: this.secondPrisonerRole
  this.reason = patch.reason ?: this.reason
  this.restrictionType = patch.restrictionType ?: this.restrictionType
  this.comment = patch.comment ?: this.comment

  return this
}
