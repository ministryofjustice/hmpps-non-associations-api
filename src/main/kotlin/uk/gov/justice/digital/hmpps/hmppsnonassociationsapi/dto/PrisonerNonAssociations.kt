package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.offendersearch.OffenderSearchPrisoner
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

/**
 * A list of non-associations for a given prisoner
 */
@Schema(description = "List of non-associations for a given prisoner")
data class PrisonerNonAssociations(
  @Schema(description = "Prisoner number", required = true, example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "First name", required = true, example = "James")
  val firstName: String,
  @Schema(description = "Last name", required = true, example = "Hall")
  val lastName: String,
  @Schema(description = "ID of the prison the prisoner is assigned to", required = false, example = "MDI")
  val prisonId: String?,
  @Schema(description = "Name of the prison the prisoner is assigned to", required = false, example = "Moorland (HMP & YOI)")
  val prisonName: String?,
  @Schema(description = "Cell the prisoner is assigned to", required = false, example = "A-1-002")
  val cellLocation: String?,
  @Schema(description = "Number of open non-associations (follows includeOtherPrisons filter)", required = true, example = "1", minimum = "0", type = "integer", format = "int32")
  val openCount: Int,
  @Schema(description = "Number of closed non-associations (follows includeOtherPrisons filter)", required = true, example = "0", minimum = "0", type = "integer", format = "int32")
  val closedCount: Int,
  @Schema(description = "Non-associations with other prisoners", required = true)
  val nonAssociations: List<PrisonerNonAssociation>,
)

/**
 * Details about a single non-association in a list and link to the other prisoner involved
 */
@Schema(description = "An item in a list of non-associations for a given prisoner")
data class PrisonerNonAssociation(
  @Schema(description = "ID of the non-association", required = true, example = "42")
  val id: Long,

  @Schema(description = "This prisoner’s role code in the non-association", required = true, example = "VICTIM")
  val role: Role,
  @Schema(description = "This prisoner’s role description in the non-association", required = true, example = "Victim")
  val roleDescription: String,
  @Schema(description = "Reason code why these prisoners should be kept apart", required = true, example = "BULLYING")
  val reason: Reason,
  @Schema(description = "Reason description why these prisoners should be kept apart", required = true, example = "Bullying")
  val reasonDescription: String,
  @Schema(description = "Location-based restriction code", required = true, example = "CELL")
  val restrictionType: RestrictionType,
  @Schema(description = "Location-based restriction description", required = true, example = "Cell only")
  val restrictionTypeDescription: String,

  @Schema(description = "Explanation of why prisoners are non-associated", required = true, example = "John and Luke always end up fighting")
  val comment: String,
  @Schema(description = "User ID of the person who created the non-association. NOTE: For records migrated from NOMIS/Prison API this is free text and may not be a valid User ID. Additionally, migrated records might use an internal system username. It can be an empty string", required = true, example = "OFF3_GEN")
  val authorisedBy: String,
  @Schema(description = "When the non-association was created", required = true, example = "2021-12-31T12:34:56.789012")
  val whenCreated: LocalDateTime,
  @Schema(description = "When the non-association was last updated", required = true, example = "2022-01-03T12:34:56.789012")
  val whenUpdated: LocalDateTime,
  @Schema(description = "User ID of the person who last updated the non-association", required = true, example = "OFF3_GEN")
  val updatedBy: String,

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
) {
  val isOpen: Boolean
    get() = !isClosed
}

/**
 * Details about the other prisoner to non-associate with
 */
@Schema(description = "Other prisoner’s details for an item in a list of non-associations")
data class OtherPrisonerDetails(
  @Schema(description = "Prisoner number", required = true, example = "D5678EF")
  val prisonerNumber: String,
  @Schema(description = "Other prisoner’s role code in the non-association", required = true, example = "PERPETRATOR")
  val role: Role,
  @Schema(description = "Other prisoner’s role description in the non-association", required = true, example = "Perpetrator")
  val roleDescription: String,
  @Schema(description = "First name", required = true, example = "Joseph")
  val firstName: String,
  @Schema(description = "Last name", required = true, example = "Bloggs")
  val lastName: String,
  @Schema(description = "ID of the prison the prisoner is assigned to", required = false, example = "MDI")
  val prisonId: String?,
  @Schema(description = "Name of the prison the prisoner is assigned to", required = false, example = "Moorland (HMP & YOI)")
  val prisonName: String?,
  @Schema(description = "Cell the prisoner is assigned to", required = false, example = "B-2-007")
  val cellLocation: String?,
)

/**
 * Converts a list of non-associations (JPA) into the `PrisonerNonAssociations` format
 *
 * @param prisonerNumber prisoner number of the "main" prisoner
 * @param prisoners is a dictionary with the information about the prisoners
 *                  from Offender Search API (e.g first name, etc...)
 * @param options sorting options
 *
 * @return an instance of `PrisonerNonAssociations`
 */
fun List<NonAssociationJPA>.toPrisonerNonAssociations(
  prisonerNumber: String,
  prisoners: Map<String, OffenderSearchPrisoner>,
  options: NonAssociationListOptions,
  openCount: Int,
  closedCount: Int,
): PrisonerNonAssociations {
  val nonAssociations = mapPrisonerNonAssociationItems(prisonerNumber, prisoners)
    .sortedWith(options.comparator)
  val mainPrisoner = prisoners[prisonerNumber]!!
  return PrisonerNonAssociations(
    prisonerNumber = prisonerNumber,
    firstName = mainPrisoner.firstName,
    lastName = mainPrisoner.lastName,
    prisonId = mainPrisoner.prisonId,
    prisonName = mainPrisoner.prisonName,
    cellLocation = mainPrisoner.cellLocation,
    openCount = openCount,
    closedCount = closedCount,
    nonAssociations = nonAssociations,
  )
}

private fun List<NonAssociationJPA>.mapPrisonerNonAssociationItems(
  prisonerNumber: String,
  prisoners: Map<String, OffenderSearchPrisoner>,
): List<PrisonerNonAssociation> {
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

    PrisonerNonAssociation(
      id = nonna.id ?: throw Exception("Only persisted non-associations can by used to build a PrisonerNonAssociations instance"),
      role = role,
      roleDescription = role.description,
      reason = nonna.reason,
      reasonDescription = nonna.reason.description,
      restrictionType = nonna.restrictionType,
      restrictionTypeDescription = nonna.restrictionType.description,
      comment = nonna.comment,
      authorisedBy = nonna.authorisedBy ?: "",
      whenCreated = nonna.whenCreated,
      whenUpdated = nonna.whenUpdated,
      updatedBy = nonna.updatedBy,

      isClosed = nonna.isClosed,
      closedBy = nonna.closedBy,
      closedReason = nonna.closedReason,
      closedAt = nonna.closedAt,

      otherPrisonerDetails = OtherPrisonerDetails(
        prisonerNumber = otherPrisoner.prisonerNumber,
        role = otherRole,
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
