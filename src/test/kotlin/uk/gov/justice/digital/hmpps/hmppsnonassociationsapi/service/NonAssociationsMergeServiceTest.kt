package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationRestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import java.time.LocalDateTime

class NonAssociationsMergeServiceTest {

  private val nonAssociationsRepository: NonAssociationsRepository = mock()
  private val service = NonAssociationsMergeService(
    nonAssociationsRepository,
  )

  @Test
  fun mergeNonAssociationPrisonerNumbers() {
    val now = LocalDateTime.now()

    whenever(nonAssociationsRepository.findAllByFirstPrisonerNumber("A1234AA")).thenReturn(
      listOf(
        genNonAssociation(1, "A1234AA", "X1234AA", now),
        genNonAssociation(2, "A1234AA", "X1234AB", now),
        genNonAssociation(3, "A1234AA", "X1234AC", now),
        genNonAssociation(4, "A1234AA", "X1234AD", now),
        genNonAssociation(5, "A1234AA", "X1234AE", now),
      ),
    )

    whenever(nonAssociationsRepository.findAllBySecondPrisonerNumber("A1234AA")).thenReturn(
      listOf(
        genNonAssociation(11, "Y1234AA", "A1234AA", now),
        genNonAssociation(22, "Y1234AB", "A1234AA", now),
        genNonAssociation(33, "Y1234AC", "A1234AA", now),
      ),
    )

    val nonAssociationList = service.mergePrisonerNumbers("A1234AA", "A1234BB")

    val resultantNonAssociations = listOf(
      genNonAssociation(1, "A1234BB", "X1234AA", now),
      genNonAssociation(2, "A1234BB", "X1234AB", now),
      genNonAssociation(3, "A1234BB", "X1234AC", now),
      genNonAssociation(4, "A1234BB", "X1234AD", now),
      genNonAssociation(5, "A1234BB", "X1234AE", now),
      genNonAssociation(11, "Y1234AA", "A1234BB", now),
      genNonAssociation(22, "Y1234AB", "A1234BB", now),
      genNonAssociation(33, "Y1234AC", "A1234BB", now),
    )

    assertThat(nonAssociationList).hasSize(8)

    assertThat(nonAssociationList).isEqualTo(resultantNonAssociations)
  }
}

fun genNonAssociation(
  id: Long? = null,
  firstPrisonerNumber: String,
  secondPrisonerNumber: String,
  createTime: LocalDateTime = LocalDateTime.now(),
  closed: Boolean = false,
  closedReason: String? = "Ok Now",
  authBy: String? = "TEST",

) = NonAssociation(
  id = id,
  firstPrisonerNumber = firstPrisonerNumber,
  firstPrisonerReason = NonAssociationReason.BULLYING,
  secondPrisonerNumber = secondPrisonerNumber,
  secondPrisonerReason = NonAssociationReason.VICTIM,
  comment = "Comment",
  restrictionType = NonAssociationRestrictionType.WING,
  authorisedBy = authBy,
  whenUpdated = createTime,
  whenCreated = createTime,
  isClosed = closed,
  closedAt = if (closed) {
    LocalDateTime.now()
  } else {
    null
  },
  closedBy = if (closed) {
    "A USER"
  } else {
    null
  },
  closedReason = if (closed) {
    closedReason
  } else {
    null
  },
)
