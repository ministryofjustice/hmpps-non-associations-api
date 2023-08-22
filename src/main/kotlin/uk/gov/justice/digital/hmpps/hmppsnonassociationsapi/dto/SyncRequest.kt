package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import java.time.LocalDate

@Schema(description = "Create Sync Request")
data class CreateSyncRequest(
  @Schema(description = "Prisoner number to not associate", required = true, example = "A1234BC")
  val firstPrisonerNumber: String,
  @Schema(
    description = "Reason why this prisoner should be kept apart from the other",
    required = true,
    example = "VIC",
  )
  val firstPrisonerReason: LegacyReason,
  @Schema(description = "Prisoner number to not associate", required = true, example = "D5678EF")
  val secondPrisonerNumber: String,
  @Schema(
    description = "Reason why this prisoner should be kept apart from the other",
    required = true,
    example = "PER",
  )
  val secondPrisonerReason: LegacyReason,

  @Schema(description = "Type of restriction, e.g. don't locate in the same cell", required = true, example = "CELL")
  val restrictionType: LegacyRestrictionType,

  @Schema(
    description = "Explanation of why prisoners are non-associated",
    required = false,
    example = "John and Luke always end up fighting",
  )
  val comment: String? = null,

  @Schema(description = "Who authorised the non-association", required = false, example = "John Smith")
  val authorisedBy: String? = null,

  @Schema(description = "Indicates that the NA is active", required = true, example = "false", defaultValue = "true")
  val active: Boolean = true,

  @Schema(description = "The date that the NA became active", required = false, example = "2023-05-09", defaultValue = "today")
  val effectiveFromDate: LocalDate? = null,

  @Schema(description = "The date that the NA became inactive", required = false, example = "2026-05-09")
  val expiryDate: LocalDate? = null,
) {
  fun toNewEntity(): NonAssociation {
    val (firstPrisonerRole, secondPrisonerRole, reason) = translateToRolesAndReason(firstPrisonerReason, secondPrisonerReason)
    return NonAssociation(
      id = null,
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerRole = firstPrisonerRole,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerRole = secondPrisonerRole,
      reason = reason,
      restrictionType = restrictionType.toRestrictionType(),
      comment = comment ?: "No comment provided", // TODO: can we have a better message?
      authorisedBy = authorisedBy,
      isClosed = !active,
      closedAt = if (active) { null } else { expiryDate?.atStartOfDay() }, // TODO: can this be in the future?
      closedBy = if (active) { null } else { SYSTEM_USERNAME },
      closedReason = if (active) { null } else { "UNDEFINED" }, // TODO: can we have a better message?
      updatedBy = SYSTEM_USERNAME,
    )
  }

  override fun toString(): String {
    return "CreateSyncRequest(firstPrisonerNumber='$firstPrisonerNumber', secondPrisonerNumber='$secondPrisonerNumber')"
  }
}

@Schema(description = "Update Sync Request")
data class UpdateSyncRequest(

  @Schema(description = "Unique reference to NA", required = true, example = "123341321")
  val id: Long,

  @Schema(
    description = "Reason why this prisoner should be kept apart from the other",
    required = true,
    example = "VIC",
  )
  val firstPrisonerReason: LegacyReason,

  @Schema(
    description = "Reason why this prisoner should be kept apart from the other",
    required = true,
    example = "PER",
  )
  val secondPrisonerReason: LegacyReason,

  @Schema(description = "Type of restriction, e.g. don't locate in the same cell", required = true, example = "CELL")
  val restrictionType: LegacyRestrictionType,

  @Schema(
    description = "Explanation of why prisoners are non-associated",
    required = false,
    example = "John and Luke always end up fighting",
  )
  val comment: String? = null,

  @Schema(description = "Who authorised the non-association", required = false, example = "John Smith")
  val authorisedBy: String? = null,

  @Schema(description = "Indicates that the NA is active", required = true, example = "false", defaultValue = "true")
  val active: Boolean = true,

  @Schema(description = "The date that the NA became active", required = false, example = "2023-05-09", defaultValue = "today")
  val effectiveFromDate: LocalDate? = null,

  @Schema(description = "The date that the NA became inactive", required = false, example = "2026-05-09")
  val expiryDate: LocalDate? = null,
) {
  override fun toString(): String {
    return "UpdateSyncRequest(id=$id)"
  }
}
