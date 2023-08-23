package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NO_CLOSURE_REASON_PROVIDED
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NO_COMMENT_PROVIDED
import java.time.LocalDate

@Schema(description = "Upsert Sync Request")
data class UpsertSyncRequest(
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
    return NonAssociation(
      id = null,
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerRole = firstPrisonerReason.toRole(),
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerRole = secondPrisonerReason.toRole(),
      reason = translateToReason(firstPrisonerReason, secondPrisonerReason),
      restrictionType = restrictionType.toRestrictionType(),
      comment = comment ?: NO_COMMENT_PROVIDED,
      authorisedBy = authorisedBy,
      isClosed = !active,
      closedAt = if (active) { null } else { expiryDate?.atStartOfDay() }, // TODO: can this be in the future?
      closedBy = if (active) { null } else { SYSTEM_USERNAME },
      closedReason = if (active) { null } else { NO_CLOSURE_REASON_PROVIDED },
      updatedBy = SYSTEM_USERNAME,
    )
  }

  override fun toString(): String {
    return "UpsertSyncRequest(firstPrisonerNumber='$firstPrisonerNumber', secondPrisonerNumber='$secondPrisonerNumber')"
  }
}

@Schema(description = "Delete Sync Request")
data class DeleteSyncRequest(
  @Schema(description = "Prisoner number to not associate", required = true, example = "A1234BC")
  val firstPrisonerNumber: String,
  @Schema(description = "Prisoner number to not associate", required = true, example = "D5678EF")
  val secondPrisonerNumber: String,
)
