package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util

import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationRestrictionType

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
