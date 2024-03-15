package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util

import uk.gov.justice.digital.hmpps.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Reason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.RestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Role
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.offendersearch.OffenderSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import java.time.Clock
import java.time.LocalDateTime

/**
 * Creates a new non-association entity
 */

fun genNonAssociation(
  id: Long? = null,
  firstPrisonerNumber: String,
  secondPrisonerNumber: String,
  createTime: LocalDateTime = LocalDateTime.now(TestBase.clock).minusDays(1),
  closed: Boolean = false,
  closedReason: String? = "No closure reason provided",
  updatedBy: String? = SYSTEM_USERNAME,
  clock: Clock = TestBase.clock,
  authorisedBy: String? = null,
) = NonAssociation(
  id = id,
  firstPrisonerNumber = firstPrisonerNumber,
  firstPrisonerRole = Role.PERPETRATOR,
  secondPrisonerNumber = secondPrisonerNumber,
  secondPrisonerRole = Role.VICTIM,
  comment = "Comment",
  reason = Reason.BULLYING,
  restrictionType = RestrictionType.WING,
  authorisedBy = authorisedBy,
  whenUpdated = createTime,
  whenCreated = createTime,
  updatedBy = updatedBy ?: "A_DPS_USER",
  isClosed = closed,
  closedAt = if (closed) {
    LocalDateTime.now(clock)
  } else {
    null
  },
  closedBy = if (closed) {
    "A_DPS_USER"
  } else {
    null
  },
  closedReason = if (closed) {
    closedReason
  } else {
    null
  },
)

/**
 * Returns a test CreateNonAssociationRequest
 *
 * Arguments are all optional with good (test) defaults so that you don't have to
 * pass all non-association fields when you just need a valid value.
 */
fun createNonAssociationRequest(
  firstPrisonerNumber: String = "A1234BC",
  firstPrisonerRole: Role = Role.VICTIM,
  secondPrisonerNumber: String = "D5678EF",
  secondPrisonerRole: Role = Role.PERPETRATOR,
  comment: String = "test comment",
  reason: Reason = Reason.THREAT,
  restrictionType: RestrictionType = RestrictionType.CELL,
): CreateNonAssociationRequest {
  return CreateNonAssociationRequest(
    firstPrisonerNumber = firstPrisonerNumber,
    firstPrisonerRole = firstPrisonerRole,
    secondPrisonerNumber = secondPrisonerNumber,
    secondPrisonerRole = secondPrisonerRole,
    comment = comment,
    reason = reason,
    restrictionType = restrictionType,
  )
}

val offenderSearchPrisoners = mapOf(
  "A1234BC" to OffenderSearchPrisoner(
    prisonerNumber = "A1234BC",
    firstName = "John",
    lastName = "Doe",
    prisonId = "MDI",
    prisonName = "Moorland",
    cellLocation = "MDI-A-1",
  ),
  "D5678EF" to OffenderSearchPrisoner(
    prisonerNumber = "D5678EF",
    firstName = "Merlin",
    lastName = "Somerplumbs",
    prisonId = "MDI",
    prisonName = "Moorland",
    cellLocation = "MDI-A-2",
  ),
  "G9012HI" to OffenderSearchPrisoner(
    prisonerNumber = "G9012HI",
    firstName = "Josh",
    lastName = "Plimburkson",
    prisonId = "MDI",
    prisonName = "Moorland",
    cellLocation = "MDI-A-3",
  ),
  // Different prison
  "L3456MN" to OffenderSearchPrisoner(
    prisonerNumber = "L3456MN",
    firstName = "Edward",
    lastName = "Lillibluprs",
    prisonId = "FBI",
    prisonName = "Forest Bank",
    cellLocation = "FBI-C-2",
  ),
  // In transfer
  "C1234CC" to OffenderSearchPrisoner(
    prisonerNumber = "C1234CC",
    firstName = "MAX",
    lastName = "CLARKE",
    prisonId = "TRN",
    prisonName = "Transfer",
    cellLocation = null,
  ),
  // Outside any establishment
  "B1234BB" to OffenderSearchPrisoner(
    prisonerNumber = "B1234BB",
    firstName = "JOE",
    lastName = "PETERS",
    prisonId = "OUT",
    prisonName = "Outside - released from Moorland (HMP)",
    cellLocation = null,
  ),
  // Null location, allegedly indicates no booking
  "D1234DD" to OffenderSearchPrisoner(
    prisonerNumber = "D1234DD",
    firstName = "NATHAN",
    lastName = "LOST",
    prisonId = null,
    prisonName = null,
    cellLocation = null,
  ),
)
