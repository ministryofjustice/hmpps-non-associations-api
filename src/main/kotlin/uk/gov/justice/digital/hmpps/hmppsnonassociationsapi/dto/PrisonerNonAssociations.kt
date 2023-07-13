package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * Non-associations for a given prisoner
 *
 * TODO: This is WIP at the moment. It may share some similarities with the
 * format currently returned by NOMIS/Prison API but it's a distinct type
 * and will likely differ.
 */
data class PrisonerNonAssociations(
  @Schema(description = "Prisoner number", required = true, example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "First name", required = true, example = "James")
  val firstName: String,
  @Schema(description = "Last name", required = true, example = "Hall")
  val lastName: String,
  @Schema(description = "ID of the prison the prisoner is assigned to", required = true, example = "MDI")
  val prisonId: String,
  @Schema(description = "Name of the prison the prisoner is assigned to", required = true, example = "Moorland (HMP & YOI)")
  val prisonName: String,
  @Schema(description = "Cell the prisoner is assigned to", required = true, example = "A-1-002")
  val cellLocation: String,
  @Schema(description = "Non-associations with other prisoners", required = true)
  val nonAssociations: List<NonAssociationDetails>,
)

/**
 * Details about a single non-association and link to the other prisoner involved
 */
data class NonAssociationDetails(
  @Schema(description = "Reason code for the non-association", required = true, example = "VICTIM")
  val reasonCode: NonAssociationReason,
  @Schema(description = "Reason description for the non-association", required = true, example = "Victim")
  val reasonDescription: String,
  @Schema(description = "The non-association restriction type code", required = true, example = "CELL")
  val restrictionTypeCode: NonAssociationRestrictionType,
  @Schema(description = "The non-association restriction description", required = true, example = "Do Not Locate in Same Cell")
  val restrictionTypeDescription: String,

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

  @Schema(description = "Details about the other person in the non-association.", required = true)
  val otherPrisonerDetails: OtherPrisonerDetails,
)

/**
 * Details about the other prisoner to non-associate with
 */
data class OtherPrisonerDetails(
  @Schema(description = "Prisoner number", required = true, example = "D5678EF")
  val prisonerNumber: String,
  @Schema(description = "Reason code for the non-association", required = true, example = "PERPETRATOR")
  val reasonCode: NonAssociationReason,
  @Schema(description = "Reason description for the non-association", required = true, example = "Perpetrator")
  val reasonDescription: String,
  @Schema(description = "First name", required = true, example = "Joseph")
  val firstName: String,
  @Schema(description = "Last name", required = true, example = "Bloggs")
  val lastName: String,
  @Schema(description = "ID of the prison the prisoner is assigned to", required = true, example = "MDI")
  val prisonId: String,
  @Schema(description = "Name of the prison the prisoner is assigned to", required = true, example = "Moorland (HMP & YOI)")
  val prisonName: String,
  @Schema(description = "Cell the prisoner is assigned to", required = true, example = "B-2-007")
  val cellLocation: String,
)
