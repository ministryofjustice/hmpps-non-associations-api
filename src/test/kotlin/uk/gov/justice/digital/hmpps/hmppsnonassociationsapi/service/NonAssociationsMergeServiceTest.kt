package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.genNonAssociation
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
