package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * A list of non-associations for a given prisoner (in NOMIS/Prison API format)
 */
@Schema(description = "List of non-associations for a given prisoner in NOMIS/Prison API format")
data class LegacyPrisonerNonAssociations(
  @Schema(description = "Prisoner number", required = true, example = "A1234BC")
  val offenderNo: String,
  @Schema(description = "First name", required = true, example = "James")
  val firstName: String,
  @Schema(description = "Last name", required = true, example = "Hall")
  val lastName: String,
  @Schema(description = "Prison ID where prisoner resides or OUT for released", required = false, example = "MDI")
  val agencyId: String?,
  @Schema(description = "Description of the agency (e.g. prison) the offender is assigned to", required = false, example = "Moorland (HMP & YOI)")
  val agencyDescription: String?,
  @Schema(description = "Description of living unit (e.g. cell) the offender is assigned to.", required = false, example = "MDI-1-1-3")
  val assignedLivingUnitDescription: String?,
  @Schema(description = "Non-associations with other prisoners", required = true)
  val nonAssociations: List<LegacyPrisonerNonAssociation>,
)

/**
 * Details about a single non-association in a list and link to the other prisoner involved (in NOMIS/Prison API format)
 */
@Schema(description = "An item in a list of non-associations for a given prisoner in NOMIS/Prison API format")
data class LegacyPrisonerNonAssociation(
  @Schema(description = "Reason code for the non-association", required = true, example = "VIC")
  val reasonCode: LegacyReason,
  @Schema(description = "Reason for the non-association", required = true, example = "Victim")
  val reasonDescription: String,
  @Schema(description = "The non-association type code", required = true, example = "WING")
  val typeCode: LegacyRestrictionType,
  @Schema(description = "The non-association type description", required = true, example = "Do Not Locate on Same Wing")
  val typeDescription: String,
  @Schema(description = "Date and time the non-association is effective from. In Europe/London (ISO 8601) format without timezone offset e.g. YYYY-MM-DDTHH:MM:SS.", required = true, example = "2021-07-05T00:00:00")
  val effectiveDate: LocalDateTime,
  @Schema(description = "Date and time the non-association expires. In Europe/London (ISO 8601) format without timezone offset e.g. YYYY-MM-DDTHH:MM:SS.", required = false, example = "2021-07-05T00:00:00")
  val expiryDate: LocalDateTime?,
  @Schema(description = "The person who authorised the non-association (free text).", required = false, example = "Officer Alice B.")
  val authorisedBy: String?,
  @Schema(description = "Additional free text comments related to the non-association.", required = false, example = "Mr. Bloggs assaulted Mr. Hall")
  val comments: String?,
  @Schema(description = "Details about the other non-association person.", required = true)
  val offenderNonAssociation: LegacyOtherPrisonerDetails,
)

/**
 * Details about the other prisoner to non-associate with (in NOMIS/Prison API format)
 */
@Schema(description = "Other prisoner’s details for an item in a list of non-associations in NOMIS/Prison API format")
data class LegacyOtherPrisonerDetails(
  @Schema(description = "Prisoner number", required = true, example = "B1234CD")
  val offenderNo: String,
  @Schema(description = "First name", required = true, example = "Joseph")
  val firstName: String,
  @Schema(description = "Last name", required = true, example = "Bloggs")
  val lastName: String,
  @Schema(description = "Reason code for the non-association", required = true, example = "PER")
  val reasonCode: LegacyReason,
  @Schema(description = "Reason for the non-association", required = true, example = "Perpetrator")
  val reasonDescription: String,
  @Schema(description = "Prison ID where prisoner resides or OUT for released", required = false, example = "MDI")
  val agencyId: String?,
  @Schema(description = "Description of the agency (e.g. prison) the offender is assigned to", required = false, example = "Moorland (HMP & YOI)")
  val agencyDescription: String?,
  @Schema(description = "Description of living unit (e.g. cell) the offender is assigned to.", required = false, example = "MDI-2-3-4")
  val assignedLivingUnitDescription: String?,
)
