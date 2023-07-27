package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.offendersearch.OffenderSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NonAssociationListOptions
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
  @Schema(description = "ID of the non-association", required = true, example = "42")
  val id: Long,

  @Schema(description = "This prisoner’s role code in the non-association", required = true, example = "VICTIM")
  val roleCode: Role,
  @Schema(description = "This prisoner’s role description in the non-association", required = true, example = "Victim")
  val roleDescription: String,
  @Schema(description = "Reason code why these prisoners should be kept apart", required = true, example = "BULLYING")
  val reasonCode: Reason,
  @Schema(description = "Reason description why these prisoners should be kept apart", required = true, example = "Bullying")
  val reasonDescription: String,
  @Schema(description = "The non-association restriction type code", required = true, example = "CELL")
  val restrictionTypeCode: RestrictionType,
  @Schema(description = "The non-association restriction description", required = true, example = "Do Not Locate in Same Cell")
  val restrictionTypeDescription: String,

  @Schema(description = "Explanation of why prisoners are non-associated", required = true, example = "John and Luke always end up fighting")
  val comment: String,
  @Schema(description = "User ID of the person who created the non-association. NOTE: For records migrated from NOMIS/Prison API this is free text and may not be a valid User ID", required = true, example = "OFF3_GEN")
  val authorisedBy: String,
  @Schema(description = "When the non-association was created", required = true, example = "2021-12-31T12:34:56.789012")
  val whenCreated: LocalDateTime,
  @Schema(description = "When the non-association was last updated", required = true, example = "2022-01-03T12:34:56.789012")
  val whenUpdated: LocalDateTime,

  @Schema(description = "Whether the non-association is closed or is in effect", required = true, example = "false")
  val isClosed: Boolean = false,
  @Schema(description = "User ID of the person who closed the non-association. Only present when the non-association is closed, null for open non-associations", required = false, example = "null")
  val closedBy: String? = null,
  @Schema(description = "Reason why the non-association was closed. Only present when the non-association is closed, null for open non-associations", required = false, example = "null")
  val closedReason: String? = null,
  @Schema(description = "Date and time of when the non-association was closed. Only present when the non-association is closed, null for open non-associations", required = false, example = "null")
  val closedAt: LocalDateTime? = null,

  @Schema(description = "Details about the other person in the non-association.", required = true)
  val otherPrisonerDetails: OtherPrisonerDetails,
)

/**
 * Details about the other prisoner to non-associate with
 */
data class OtherPrisonerDetails(
  @Schema(description = "Prisoner number", required = true, example = "D5678EF")
  val prisonerNumber: String,
  @Schema(description = "Other prisoner’s role code in the non-association", required = true, example = "PERPETRATOR")
  val roleCode: Role,
  @Schema(description = "Other prisoner’s role description in the non-association", required = true, example = "Perpetrator")
  val roleDescription: String,
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
  options: NonAssociationListOptions,
): PrisonerNonAssociations {
  val sortComparator = options.sortBy.comparator(options.sortDirection)
  val nonAssociations = this.toNonAssociationsDetails(prisonerNumber, prisoners)
    .sortedWith(sortComparator)
  return PrisonerNonAssociations(
    prisonerNumber = prisonerNumber,
    firstName = prisoners[prisonerNumber]!!.firstName,
    lastName = prisoners[prisonerNumber]!!.lastName,
    prisonId = prisoners[prisonerNumber]!!.prisonId,
    prisonName = prisoners[prisonerNumber]!!.prisonName,
    cellLocation = prisoners[prisonerNumber]!!.cellLocation,
    nonAssociations = nonAssociations,
  )
}

private fun List<NonAssociationJPA>.toNonAssociationsDetails(
  prisonerNumber: String,
  prisoners: Map<String, OffenderSearchPrisoner>,
): List<NonAssociationDetails> {
  data class PrisonersInfo(
    val prisoner: OffenderSearchPrisoner,
    val otherPrisoner: OffenderSearchPrisoner,
    val role: Role,
    val otherRole: Role,
  )

  return this.map { nonna ->
    val prisonersInfo = if (nonna.firstPrisonerNumber == prisonerNumber) {
      PrisonersInfo(
        prisoners[nonna.firstPrisonerNumber]!!,
        prisoners[nonna.secondPrisonerNumber]!!,
        nonna.firstPrisonerRole,
        nonna.secondPrisonerRole,
      )
    } else if (nonna.secondPrisonerNumber == prisonerNumber) {
      PrisonersInfo(
        prisoners[nonna.secondPrisonerNumber]!!,
        prisoners[nonna.firstPrisonerNumber]!!,
        nonna.secondPrisonerRole,
        nonna.firstPrisonerRole,
      )
    } else {
      throw Exception("One of the non-association is not for the desired prisoner $prisonerNumber")
    }
    val (_, otherPrisoner, role, otherRole) = prisonersInfo

    NonAssociationDetails(
      id = nonna.id ?: throw Exception("Only persisted non-associations can used to build a PrisonerNonAssociations instance"),
      roleCode = role,
      roleDescription = role.description,
      reasonCode = nonna.reason,
      reasonDescription = nonna.reason.description,
      restrictionTypeCode = nonna.restrictionType,
      restrictionTypeDescription = nonna.restrictionType.description,
      comment = nonna.comment,
      authorisedBy = nonna.authorisedBy ?: "",
      whenCreated = nonna.whenCreated,
      whenUpdated = nonna.whenUpdated,

      isClosed = nonna.isClosed,
      closedBy = nonna.closedBy,
      closedReason = nonna.closedReason,
      closedAt = nonna.closedAt,

      otherPrisonerDetails = OtherPrisonerDetails(
        prisonerNumber = otherPrisoner.prisonerNumber,
        roleCode = otherRole,
        roleDescription = otherRole.description,
        firstName = otherPrisoner.firstName,
        lastName = otherPrisoner.lastName,
        prisonId = otherPrisoner.prisonId,
        prisonName = otherPrisoner.prisonName,
        cellLocation = otherPrisoner.cellLocation,
      ),
    )
  }
}
