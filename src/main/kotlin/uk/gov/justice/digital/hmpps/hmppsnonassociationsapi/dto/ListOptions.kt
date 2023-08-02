package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation

data class NonAssociationListOptions(
  val includeOpen: Boolean = true,
  val includeClosed: Boolean = false,
  val includeOtherPrisons: Boolean = false,
  val sortBy: NonAssociationsSort? = null,
  val sortDirection: Sort.Direction? = null,
) {
  val filterForOpenAndClosed: (NonAssociation) -> Boolean
    get() {
      return if (includeOpen && includeClosed) {
        { true }
      } else if (includeOpen) {
        { it.isOpen }
      } else if (includeClosed) {
        { it.isClosed }
      } else {
        { false }
      }
    }

  val comparator: Comparator<PrisonerNonAssociation>
    get() {
      val sortBy = sortBy ?: NonAssociationsSort.WHEN_CREATED
      val sortDirection = sortDirection ?: sortBy.defaultSortDirection
      return when (sortBy) {
        NonAssociationsSort.WHEN_CREATED -> compareBy(PrisonerNonAssociation::whenCreated)
        NonAssociationsSort.WHEN_UPDATED -> compareBy(PrisonerNonAssociation::whenUpdated)
        NonAssociationsSort.LAST_NAME -> compareBy { nonna -> nonna.otherPrisonerDetails.lastName }
        NonAssociationsSort.FIRST_NAME -> compareBy { nonna -> nonna.otherPrisonerDetails.firstName }
        NonAssociationsSort.PRISONER_NUMBER -> compareBy { nonna -> nonna.otherPrisonerDetails.prisonerNumber }
      }.run {
        if (sortDirection == Sort.Direction.DESC) reversed() else this
      }
    }
}

enum class NonAssociationsSort(val defaultSortDirection: Sort.Direction) {
  WHEN_CREATED(Sort.Direction.DESC),
  WHEN_UPDATED(Sort.Direction.DESC),
  LAST_NAME(Sort.Direction.ASC),
  FIRST_NAME(Sort.Direction.ASC),
  PRISONER_NUMBER(Sort.Direction.ASC),
}
