package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NO_CLOSURE_REASON_PROVIDED
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NO_COMMENT_PROVIDED
import java.time.Clock
import java.time.LocalDate

@Schema(description = "Upsert Sync Request")
data class UpsertSyncRequest(
  @Schema(description = "ID of the non-association, if provided an update will be performed", required = false, example = "234233")
  val id: Long? = null,

  @Schema(description = "Prisoner number to not associate, this is ignored if ID is provided", required = true, example = "A1234BC", maxLength = 10)
  @field:Pattern(regexp = "[a-zA-Z][0-9]{4}[a-zA-Z]{2}", message = "Prisoner number must be in the correct format")
  val firstPrisonerNumber: String,
  @Schema(
    description = "Reason why this prisoner should be kept apart from the other",
    required = true,
    example = "VIC",
  )
  val firstPrisonerReason: LegacyReason,
  @Schema(description = "Prisoner number to not associate, this is ignored if ID is provided", required = true, example = "D5678EF", maxLength = 10)
  @field:Pattern(regexp = "[a-zA-Z][0-9]{4}[a-zA-Z]{2}", message = "Prisoner number must be in the correct format")
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

  @Schema(description = "Who authorised the non-association", required = false, example = "John Smith", minLength = 1, maxLength = 60)
  @field:Size(min = 1, max = 60, message = "Authorised by must be a maximum of 60 characters")
  val authorisedBy: String? = null,

  @Schema(description = "The last staff member who changed this record, username of 60 characters, if not provided the API credentials will be used", required = false, example = "JSMITH", minLength = 1, maxLength = 60)
  @field:Size(min = 1, max = 60, message = "Last modified by must be a maximum of 60 characters")
  val lastModifiedByUsername: String? = null,

  @Schema(description = "The date that the NA became active", required = true, example = "2023-05-09", defaultValue = "today")
  val effectiveFromDate: LocalDate,

  @Schema(description = "The date that the NA became inactive", required = false, example = "2026-05-09")
  val expiryDate: LocalDate? = null,
) {
  fun toNewEntity(clock: Clock): NonAssociation {
    val (firstPrisonerRole, secondPrisonerRole, reason) = translateToRolesAndReason(firstPrisonerReason, secondPrisonerReason)
    val currentDate = LocalDate.now(clock)
    return NonAssociation(
      id = null,
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerRole = firstPrisonerRole,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerRole = secondPrisonerRole,
      reason = reason,
      restrictionType = restrictionType.toRestrictionType(),
      comment = comment ?: NO_COMMENT_PROVIDED,
      authorisedBy = authorisedBy,
      isClosed = isClosed(clock),
      closedAt = if (isOpen(clock)) { null } else { preventFutureDate(expiryDate, currentDate).atStartOfDay() },
      closedBy = if (isOpen(clock)) { null } else { lastModifiedByUsername ?: SYSTEM_USERNAME },
      closedReason = if (isOpen(clock)) { null } else { NO_CLOSURE_REASON_PROVIDED },
      whenCreated = preventFutureDate(effectiveFromDate, currentDate).atStartOfDay(),
      whenUpdated = preventFutureDate(effectiveFromDate, currentDate).atStartOfDay(),
      updatedBy = lastModifiedByUsername ?: SYSTEM_USERNAME,
    )
  }

  fun isOpen(clock: Clock): Boolean {
    return isWithinRange(LocalDate.now(clock), effectiveFromDate, expiryDate)
  }

  fun isClosed(clock: Clock): Boolean {
    return !isOpen(clock)
  }

  private fun isWithinRange(testDate: LocalDate, startDate: LocalDate, endDate: LocalDate?): Boolean {
    return testDate >= startDate && (endDate == null || testDate < endDate)
  }

  override fun toString(): String {
    return "UpsertSyncRequest(id=$id, firstPrisonerNumber='$firstPrisonerNumber', firstPrisonerReason=$firstPrisonerReason, secondPrisonerNumber='$secondPrisonerNumber', secondPrisonerReason=$secondPrisonerReason, restrictionType=$restrictionType, comment=$comment, authorisedBy=$authorisedBy, effectiveFromDate=$effectiveFromDate, expiryDate=$expiryDate)"
  }
}

@Schema(description = "Delete Sync Request")
data class DeleteSyncRequest(
  @Schema(description = "Prisoner number to not associate", required = true, example = "A1234BC")
  @field:Pattern(regexp = "[a-zA-Z][0-9]{4}[a-zA-Z]{2}", message = "Prisoner number must be in the correct format")
  val firstPrisonerNumber: String,
  @Schema(description = "Prisoner number to not associate", required = true, example = "D5678EF")
  @field:Pattern(regexp = "[a-zA-Z][0-9]{4}[a-zA-Z]{2}", message = "Prisoner number must be in the correct format")
  val secondPrisonerNumber: String,
) {
  override fun toString(): String {
    return "DeleteSyncRequest(firstPrisonerNumber='$firstPrisonerNumber', secondPrisonerNumber='$secondPrisonerNumber')"
  }
}

fun preventFutureDate(dateToCheck: LocalDate?, currentDate: LocalDate) =
  if (dateToCheck == null || currentDate < dateToCheck) {
    currentDate
  } else {
    dateToCheck
  }
