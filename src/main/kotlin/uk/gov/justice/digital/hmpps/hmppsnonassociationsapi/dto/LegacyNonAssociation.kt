package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Non-association in NOMIS/Prison API format")
data class LegacyNonAssociation(
  @param:Schema(description = "ID of the non-association", required = true, example = "42")
  val id: Long,

  @param:Schema(description = "Prisoner number", required = true, example = "A1234BC")
  val offenderNo: String,
  @param:Schema(description = "Reason code for the non-association", required = true, example = "VIC")
  val reasonCode: LegacyReason,
  @param:Schema(description = "Reason for the non-association", required = true, example = "Victim")
  val reasonDescription: String,
  @param:Schema(description = "The non-association type code", required = true, example = "WING")
  val typeCode: LegacyRestrictionType,
  @param:Schema(
    description = "The non-association type description",
    required = true,
    example = "Do Not Locate on Same Wing",
  )
  val typeDescription: String,
  @param:Schema(
    description = "Date and time the non-association is effective from. " +
      "In Europe/London (ISO 8601) format without timezone offset e.g. YYYY-MM-DDTHH:MM:SS.",
    required = true,
    example = "2021-07-05T00:00:00",
  )
  val effectiveDate: LocalDateTime,
  @param:Schema(
    description = "Date and time the non-association expires. " +
      "In Europe/London (ISO 8601) format without timezone offset e.g. YYYY-MM-DDTHH:MM:SS.",
    required = false,
    example = "2021-07-05T00:00:00",
  )
  val expiryDate: LocalDateTime?,
  @param:Schema(
    description = "The person who authorised the non-association (free text).",
    required = false,
    example = "Officer Alice B.",
  )
  val authorisedBy: String?,
  @param:Schema(
    description = "Additional free text comments related to the non-association.",
    required = false,
    example = "Mr. Bloggs assaulted Mr. Hall",
  )
  val comments: String,

  @param:Schema(description = "Details about the other non-association person", required = true)
  val offenderNonAssociation: LegacyNonAssociationOtherPrisonerDetails,
)

@Schema(description = "Details about the other non-association person in NOMIS/Prison API format")
data class LegacyNonAssociationOtherPrisonerDetails(
  @param:Schema(description = "Prisoner number", required = true, example = "B1234CD")
  val offenderNo: String,
  @param:Schema(description = "Reason code for the non-association", required = true, example = "PER")
  val reasonCode: LegacyReason,
  @param:Schema(description = "Reason for the non-association", required = true, example = "Perpetrator")
  val reasonDescription: String,
)
