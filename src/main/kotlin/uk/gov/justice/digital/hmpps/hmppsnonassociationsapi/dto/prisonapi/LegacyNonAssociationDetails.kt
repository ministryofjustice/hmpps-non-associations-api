package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyRestrictionType
import java.time.LocalDateTime

/**
 * Top-level type for non-association details returned by Prison API
 */
data class LegacyNonAssociationDetails(
  @Schema(description = "Prisoner number", required = true, example = "A1234BC")
  val offenderNo: String,
  @Schema(description = "First name", required = true, example = "James")
  val firstName: String,
  @Schema(description = "Last name", required = true, example = "Hall")
  val lastName: String,
  @Schema(description = "Prison ID where prisoner resides or OUT for released", required = true, example = "MDI")
  val agencyId: String,
  @Schema(description = "Description of the agency (e.g. prison) the offender is assigned to", required = true, example = "Moorland (HMP & YOI)")
  val agencyDescription: String,
  @Schema(description = "Description of living unit (e.g. cell) the offender is assigned to.", required = false, example = "MDI-1-1-3")
  val assignedLivingUnitDescription: String?,
  @Schema(description = "Non-associations with other prisoners", required = true)
  val nonAssociations: List<LegacyNonAssociation>,
)

/**
 * Prison API format for a single non-association
 */
data class LegacyNonAssociation(
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
  val offenderNonAssociation: LegacyOffenderNonAssociation,
)

/**
 * Prison API format containing the details of the other side of the non-association relation
 */
data class LegacyOffenderNonAssociation(
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
  @Schema(description = "Prison ID where prisoner resides or OUT for released", required = true, example = "MDI")
  val agencyId: String,
  @Schema(description = "Description of the agency (e.g. prison) the offender is assigned to", required = true, example = "Moorland (HMP & YOI)")
  val agencyDescription: String,
  @Schema(description = "Description of living unit (e.g. cell) the offender is assigned to.", required = false, example = "MDI-2-3-4")
  val assignedLivingUnitDescription: String?,
)
