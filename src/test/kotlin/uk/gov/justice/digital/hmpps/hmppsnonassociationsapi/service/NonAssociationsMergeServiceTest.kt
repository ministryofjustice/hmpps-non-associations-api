package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase.Companion.clock
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.genNonAssociation
import java.time.LocalDateTime

@DisplayName("Non-associations merge service, unit tests")
class NonAssociationsMergeServiceTest {

  private val nonAssociationsRepository: NonAssociationsRepository = mock()
  private val service = NonAssociationsMergeService(
    nonAssociationsRepository = nonAssociationsRepository,
    clock = clock,
    telemetryClient = mock(),
  )

  @Test
  fun mergeNonAssociationPrisonerNumbers() {
    val now = LocalDateTime.now(clock)

    whenever(nonAssociationsRepository.findAllByPrisonerNumber("A1234AA")).thenReturn(
      listOf(
        genNonAssociation(1, "A1234AA", "X1234AA", now),
        genNonAssociation(2, "A1234AA", "X1234AB", now),
        genNonAssociation(3, "A1234AA", "X1234AC", now),
        genNonAssociation(4, "A1234AA", "X1234AD", now),
        genNonAssociation(5, "A1234AA", "X1234AE", now),
        genNonAssociation(11, "Y1234AA", "A1234AA", now),
        genNonAssociation(22, "Y1234AB", "A1234AA", now),
        genNonAssociation(33, "Y1234AC", "A1234AA", now),
      ),
    )

    val nonAssociationMap = service.mergePrisonerNumbers("A1234AA", "A1234BB")

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

    assertThat(nonAssociationMap[MergeResult.MERGED]).hasSize(8)

    assertThat(nonAssociationMap[MergeResult.MERGED]).isEqualTo(resultantNonAssociations)
  }
}
