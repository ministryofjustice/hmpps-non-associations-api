package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.offendersearch.OffenderSearchPrisoner
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

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

/**
 * Converts a list of non-associations (JPA) into the `PrisonerNonAssociations` format
 *
 * @param prisonerNumber prisoner number of the "main" prisoner
 * @param prisoners is a dictionary with the information about the prisoners
 *                  from Offender Search API (e.g first name, etc...)
 *
 * @return an instance of `PrisonerNonAssociations`
 */
fun List<NonAssociationJPA>.toPrisonerNonAssociations(
  prisonerNumber: String,
  prisoners: Map<String, OffenderSearchPrisoner>,
): PrisonerNonAssociations {
  return PrisonerNonAssociations(
    prisonerNumber = prisonerNumber,
    firstName = prisoners[prisonerNumber]!!.firstName,
    lastName = prisoners[prisonerNumber]!!.lastName,
    prisonId = prisoners[prisonerNumber]!!.prisonId,
    prisonName = prisoners[prisonerNumber]!!.prisonName,
    cellLocation = prisoners[prisonerNumber]!!.cellLocation,
    nonAssociations = this.toNonAssociationsDetails(prisonerNumber, prisoners),
  )
}

private fun List<NonAssociationJPA>.toNonAssociationsDetails(
  prisonerNumber: String,
  prisoners: Map<String, OffenderSearchPrisoner>,
): List<NonAssociationDetails> {
  data class PrisonersInfo(
    val prisoner: OffenderSearchPrisoner,
    val otherPrisoner: OffenderSearchPrisoner,
    val reason: NonAssociationReason,
    val otherReason: NonAssociationReason,
  )

  return this.map { nonna ->
    val prisonersInfo = if (nonna.firstPrisonerNumber == prisonerNumber) {
      PrisonersInfo(
        prisoners[nonna.firstPrisonerNumber]!!,
        prisoners[nonna.secondPrisonerNumber]!!,
        nonna.firstPrisonerReason,
        nonna.secondPrisonerReason,
      )
    } else if (nonna.secondPrisonerNumber == prisonerNumber) {
      PrisonersInfo(
        prisoners[nonna.secondPrisonerNumber]!!,
        prisoners[nonna.firstPrisonerNumber]!!,
        nonna.secondPrisonerReason,
        nonna.firstPrisonerReason,
      )
    } else {
      throw Exception("One of the non-association is not for the desired prisoner $prisonerNumber")
    }
    val (_, otherPrisoner, reason, otherReason) = prisonersInfo

    NonAssociationDetails(
      reasonCode = reason,
      reasonDescription = reason.description,
      restrictionTypeCode = nonna.restrictionType,
      restrictionTypeDescription = nonna.restrictionType.description,
      comment = nonna.comment,
      authorisedBy = nonna.authorisedBy ?: "",
      whenCreated = nonna.whenCreated,
      otherPrisonerDetails = OtherPrisonerDetails(
        prisonerNumber = otherPrisoner.prisonerNumber,
        reasonCode = otherReason,
        reasonDescription = otherReason.description,
        firstName = otherPrisoner.firstName,
        lastName = otherPrisoner.lastName,
        prisonId = otherPrisoner.prisonId,
        prisonName = otherPrisoner.prisonName,
        cellLocation = otherPrisoner.cellLocation,
      ),
    )
  }
}
