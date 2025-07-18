package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Clock
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

/**
 * Non-association between two prisoners
 */
@Schema(description = "Non-association")
data class NonAssociation(
  @param:Schema(description = "ID of the non-association", required = true, example = "42")
  val id: Long,

  @param:Schema(description = "Prisoner number to not associate", required = true, example = "A1234BC")
  val firstPrisonerNumber: String,
  @param:Schema(description = "This prisoner’s role code in the non-association", required = true, example = "VICTIM")
  val firstPrisonerRole: Role,
  @param:Schema(
    description = "This prisoner’s role description in the non-association",
    required = true,
    example = "Victim",
  )
  val firstPrisonerRoleDescription: String,
  @param:Schema(description = "Prisoner number to not associate", required = true, example = "D5678EF")
  val secondPrisonerNumber: String,
  @param:Schema(
    description = "Other prisoner’s role code in the non-association",
    required = true,
    example = "PERPETRATOR",
  )
  val secondPrisonerRole: Role,
  @param:Schema(
    description = "Other prisoner’s role description in the non-association",
    required = true,
    example = "Perpetrator",
  )
  val secondPrisonerRoleDescription: String,

  @param:Schema(
    description = "Reason code why these prisoners should be kept apart",
    required = true,
    example = "BULLYING",
  )
  val reason: Reason,
  @param:Schema(
    description = "Reason description why these prisoners should be kept apart",
    required = true,
    example = "Bullying",
  )
  val reasonDescription: String,
  @param:Schema(description = "Location-based restriction code", required = true, example = "CELL")
  val restrictionType: RestrictionType,
  @param:Schema(description = "Location-based restriction description", required = true, example = "Cell only")
  val restrictionTypeDescription: String,

  @param:Schema(
    description = "Explanation of why prisoners are non-associated",
    required = true,
    example = "John and Luke always end up fighting",
  )
  val comment: String,

  @param:Schema(
    description = "When the non-association was created",
    required = true,
    example = "2021-12-31T12:34:56.789012",
  )
  val whenCreated: LocalDateTime,
  @param:Schema(
    description = "When the non-association was last updated",
    required = true,
    example = "2022-01-03T12:34:56.789012",
  )
  val whenUpdated: LocalDateTime,
  @param:Schema(
    description = "User ID of the person who last updated the non-association",
    required = true,
    example = "OFF3_GEN",
  )
  val updatedBy: String,

  @param:Schema(
    description = "Whether the non-association is closed or is in effect",
    required = true,
    example = "false",
  )
  val isClosed: Boolean = false,
  @param:Schema(
    description = "User ID of the person who closed the non-association. " +
      "Only present when the non-association is closed, null for open non-associations",
    required = false,
    example = "null",
  )
  val closedBy: String? = null,
  @param:Schema(
    description = "Reason why the non-association was closed. " +
      "Only present when the non-association is closed, null for open non-associations",
    required = false,
    example = "null",
  )
  val closedReason: String? = null,
  @param:Schema(
    description = "Date and time of when the non-association was closed. " +
      "Only present when the non-association is closed, null for open non-associations",
    required = false,
    example = "null",
  )
  val closedAt: LocalDateTime? = null,
) {
  @Suppress("unused")
  val isOpen: Boolean
    get() = !isClosed
}

/**
 * Request format for creating a new, open, non-association between two prisoners
 */
@Schema(description = "Request format for creating a new, open, non-association between two prisoners")
data class CreateNonAssociationRequest(
  @param:Schema(description = "Prisoner number to not associate", required = true, example = "A1234BC", maxLength = 10)
  @field:Pattern(regexp = "[a-zA-Z][0-9]{4}[a-zA-Z]{2}", message = "Prisoner number must be in the correct format")
  val firstPrisonerNumber: String,
  @param:Schema(description = "This prisoner’s role in the non-association", required = true, example = "VICTIM")
  val firstPrisonerRole: Role,
  @param:Schema(description = "Prisoner number to not associate", required = true, example = "D5678EF", maxLength = 10)
  @field:Pattern(regexp = "[a-zA-Z][0-9]{4}[a-zA-Z]{2}", message = "Prisoner number must be in the correct format")
  val secondPrisonerNumber: String,
  @param:Schema(description = "Other prisoner’s role in the non-association", required = true, example = "PERPETRATOR")
  val secondPrisonerRole: Role,

  @param:Schema(description = "Reason why these prisoners should be kept apart", required = true, example = "BULLYING")
  val reason: Reason,
  @param:Schema(description = "Location-based restriction code", required = true, example = "CELL")
  val restrictionType: RestrictionType,

  @param:Schema(
    description = "Explanation of why prisoners are non-associated",
    required = true,
    example = "John and Luke always end up fighting",
    minLength = 1,
  )
  @field:Size(min = 1, message = "A comment is required")
  val comment: String,
) {
  fun toNewEntity(createdBy: String, clock: Clock): NonAssociationJPA {
    return NonAssociationJPA(
      id = null,
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerRole = firstPrisonerRole,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerRole = secondPrisonerRole,
      reason = reason,
      restrictionType = restrictionType,
      comment = comment,
      authorisedBy = createdBy,
      updatedBy = createdBy,
      whenCreated = LocalDateTime.now(clock),
      whenUpdated = LocalDateTime.now(clock),
    )
  }
}

/**
 * Request format for updating a non-association between two prisoners
 */
@Schema(description = "Request format for updating a non-association between two prisoners")
data class PatchNonAssociationRequest(
  @param:Schema(description = "This prisoner’s role in the non-association", required = false, example = "VICTIM")
  val firstPrisonerRole: Role? = null,
  @param:Schema(description = "Other prisoner’s role in the non-association", required = false, example = "PERPETRATOR")
  val secondPrisonerRole: Role? = null,

  @param:Schema(description = "Reason why these prisoners should be kept apart", required = false, example = "BULLYING")
  val reason: Reason? = null,
  @param:Schema(description = "Location-based restriction code", required = false, example = "CELL")
  val restrictionType: RestrictionType? = null,

  @param:Schema(
    description = "Explanation of why prisoners are non-associated",
    required = false,
    example = "John and Luke always end up fighting",
    minLength = 1,
  )
  @field:Size(min = 1, message = "Comment cannot be blank")
  val comment: String? = null,
)

/**
 * Request format to close a non-association between two prisoners
 */
@Schema(description = "Request to close a non-association")
data class CloseNonAssociationRequest(
  @param:Schema(
    description = "Reason for closing the non-association",
    required = true,
    example = "They are friends now",
    minLength = 1,
  )
  @field:Size(min = 1, message = "Comment cannot be blank")
  val closedReason: String,
  @param:Schema(
    description = "Date and time of the closure, if not provided will default to today's time",
    required = false,
    example = "2023-06-07",
    defaultValue = "now",
  )
  val closedAt: LocalDateTime? = null,
  @param:Schema(
    description = "The username of the member of staff requesting the closure, " +
      "if not provided will use the user in the JWT access token",
    required = false,
    example = "ASMITH",
    minLength = 1,
    maxLength = 60,
  )
  @field:Size(min = 1, max = 60, message = "Closed by must be a maximum of 60 characters")
  val closedBy: String? = null,
)

/**
 * Request format to delete a non-association between two prisoners
 */
@Schema(description = "Request to delete a non-association")
data class DeleteNonAssociationRequest(
  @param:Schema(
    description = "Reason for deleting the non-association",
    required = true,
    example = "Created in error and removed on requested from OMU team",
    minLength = 1,
  )
  @field:Size(min = 1, message = "Comment cannot be blank")
  val deletionReason: String,
  @param:Schema(
    description = "The username of the member of staff requesting the deletion",
    required = true,
    example = "AJONES",
    minLength = 1,
    maxLength = 60,
  )
  @field:Size(min = 1, max = 60, message = "Deleted by must be a maximum of 60 characters")
  val staffUserNameRequestingDeletion: String,
)

/**
 * Request format to re-open a non-association between two prisoners
 */
@Schema(description = "Request to re-open a non-association")
data class ReopenNonAssociationRequest(
  @param:Schema(
    description = "Reason for re-opening the non-association",
    required = true,
    example = "Prisoners are fighting again",
    minLength = 1,
  )
  @field:Size(min = 1, message = "The reason for re-opening cannot be blank")
  val reopenReason: String,
  @param:Schema(
    description = "Date and time of the re-open, if not provided will default to today's time",
    required = false,
    example = "2023-06-07T10:30:00",
    defaultValue = "now",
  )
  val reopenedAt: LocalDateTime? = null,
  @param:Schema(
    description = "The username of the member of staff requesting the re-open, " +
      "if not provided will use the user in the JWT access token",
    required = true,
    example = "AJONES",
    minLength = 1,
    maxLength = 60,
  )
  @field:Size(min = 1, max = 60, message = "Reopened by must be a maximum of 60 characters")
  val reopenedBy: String? = null,
)

fun NonAssociationJPA.updateWith(
  patch: PatchNonAssociationRequest,
  updatedBy: String,
  clock: Clock,
): NonAssociationJPA {
  this.firstPrisonerRole = patch.firstPrisonerRole ?: this.firstPrisonerRole
  this.secondPrisonerRole = patch.secondPrisonerRole ?: this.secondPrisonerRole
  this.reason = patch.reason ?: this.reason
  this.restrictionType = patch.restrictionType ?: this.restrictionType
  this.comment = patch.comment ?: this.comment
  this.updatedBy = updatedBy
  this.whenUpdated = LocalDateTime.now(clock)

  return this
}
