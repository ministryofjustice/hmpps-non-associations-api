package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation

enum class NonAssociationListInclusion {
  OPEN_ONLY,
  CLOSED_ONLY,
  ALL,
  ;

  companion object {
    fun of(includeOpen: Boolean = true, includeClosed: Boolean = false): NonAssociationListInclusion? =
      if (!includeOpen && !includeClosed) {
        null
      } else if (!includeClosed) {
        OPEN_ONLY
      } else if (!includeOpen) {
        CLOSED_ONLY
      } else {
        ALL
      }
  }
}

data class NonAssociationListOptions(
  val inclusion: NonAssociationListInclusion = NonAssociationListInclusion.OPEN_ONLY,
  val includeOtherPrisons: Boolean = false,
  val sortBy: NonAssociationsSort? = null,
  val sortDirection: Sort.Direction? = null,
) {
  val filterForOpenAndClosed: (NonAssociation) -> Boolean
    get() = when (inclusion) {
      NonAssociationListInclusion.OPEN_ONLY -> NonAssociation::isOpen
      NonAssociationListInclusion.CLOSED_ONLY -> NonAssociation::isClosed
      NonAssociationListInclusion.ALL -> { _ -> true }
    }

  val comparator: Comparator<PrisonerNonAssociation>
    get() {
      val sortBy = sortBy ?: NonAssociationsSort.WHEN_CREATED
      val sortDirection = sortDirection ?: sortBy.defaultSortDirection
      return when (sortBy) {
        NonAssociationsSort.WHEN_CREATED -> compareBy(PrisonerNonAssociation::whenCreated)
        NonAssociationsSort.WHEN_UPDATED -> compareBy(PrisonerNonAssociation::whenUpdated)
        NonAssociationsSort.WHEN_CLOSED -> compareBy(PrisonerNonAssociation::closedAt)
        NonAssociationsSort.LAST_NAME -> compareBy { nonna -> nonna.otherPrisonerDetails.lastName }
        NonAssociationsSort.FIRST_NAME -> compareBy { nonna -> nonna.otherPrisonerDetails.firstName }
        NonAssociationsSort.PRISONER_NUMBER -> compareBy { nonna -> nonna.otherPrisonerDetails.prisonerNumber }
        NonAssociationsSort.PRISON_ID -> compareBy { nonna -> nonna.otherPrisonerDetails.prisonId }
        NonAssociationsSort.PRISON_NAME -> compareBy { nonna -> nonna.otherPrisonerDetails.prisonName }
        NonAssociationsSort.CELL_LOCATION -> compareBy { nonna -> nonna.otherPrisonerDetails.cellLocation }
      }.run {
        if (sortDirection == Sort.Direction.DESC) reversed() else this
      }
    }
}

enum class NonAssociationsSort(
  val defaultSortDirection: Sort.Direction,
) {
  WHEN_CREATED(Sort.Direction.DESC),
  WHEN_UPDATED(Sort.Direction.DESC),
  WHEN_CLOSED(Sort.Direction.DESC),
  LAST_NAME(Sort.Direction.ASC),
  FIRST_NAME(Sort.Direction.ASC),
  PRISONER_NUMBER(Sort.Direction.ASC),
  PRISON_ID(Sort.Direction.ASC),
  PRISON_NAME(Sort.Direction.ASC),
  CELL_LOCATION(Sort.Direction.ASC),
}
