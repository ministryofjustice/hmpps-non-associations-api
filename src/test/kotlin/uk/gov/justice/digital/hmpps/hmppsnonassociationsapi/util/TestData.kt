package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util

import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationRestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.offendersearch.OffenderSearchPrisoner

/**
 * Returns a test CreateNonAssociationRequest
 *
 * Arguments are all optional with good (test) defaults so that you don't have to
 * pass all non-association fields when you just need a valid value.
 */
fun createNonAssociationRequest(
  firstPrisonerNumber: String = "A1234BC",
  firstPrisonerReason: NonAssociationReason = NonAssociationReason.VICTIM,
  secondPrisonerNumber: String = "D5678EF",
  secondPrisonerReason: NonAssociationReason = NonAssociationReason.PERPETRATOR,
  comment: String = "test comment",
  restrictionType: NonAssociationRestrictionType = NonAssociationRestrictionType.CELL,
): CreateNonAssociationRequest {
  return CreateNonAssociationRequest(
    firstPrisonerNumber = firstPrisonerNumber,
    firstPrisonerReason = firstPrisonerReason,
    secondPrisonerNumber = secondPrisonerNumber,
    secondPrisonerReason = secondPrisonerReason,
    comment = comment,
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
)
