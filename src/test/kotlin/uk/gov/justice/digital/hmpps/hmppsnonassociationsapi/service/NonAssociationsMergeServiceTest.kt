package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase.Companion.clock
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase.Companion.now
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.genNonAssociation

@DisplayName("Non-associations merge service, unit tests")
class NonAssociationsMergeServiceTest {

  private val nonAssociationsRepository: NonAssociationsRepository = mock()
  private val service = NonAssociationsMergeService(
    nonAssociationsRepository = nonAssociationsRepository,
    clock = clock,
    telemetryClient = mock(),
  )

  @Test
  fun `can merge prisoner numbers`() {
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
    // No open NAs exist for the new prisoner number with any of these third parties
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners("A1234BB", "X1234AA")).thenReturn(emptyList())
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners("A1234BB", "X1234AB")).thenReturn(emptyList())
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners("A1234BB", "X1234AC")).thenReturn(emptyList())
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners("A1234BB", "X1234AD")).thenReturn(emptyList())
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners("A1234BB", "X1234AE")).thenReturn(emptyList())
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners("Y1234AA", "A1234BB")).thenReturn(emptyList())
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners("Y1234AB", "A1234BB")).thenReturn(emptyList())
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners("Y1234AC", "A1234BB")).thenReturn(emptyList())

    val nonAssociationMap = service.replacePrisonerNumber("A1234AA", "A1234BB")

    assertThat(nonAssociationMap).isEqualTo(
      mapOf(
        MergeResult.MERGED to listOf(
          genNonAssociation(1, "A1234BB", "X1234AA", now),
          genNonAssociation(2, "A1234BB", "X1234AB", now),
          genNonAssociation(3, "A1234BB", "X1234AC", now),
          genNonAssociation(4, "A1234BB", "X1234AD", now),
          genNonAssociation(5, "A1234BB", "X1234AE", now),
          genNonAssociation(11, "Y1234AA", "A1234BB", now),
          genNonAssociation(22, "Y1234AB", "A1234BB", now),
          genNonAssociation(33, "Y1234AC", "A1234BB", now),
        ),
      ),
    )
  }

  @Test
  fun `can move a booking between prisoner numbers`() {
    whenever(nonAssociationsRepository.findAllByPrisonerNumber("A1234AA")).thenReturn(
      listOf(
        genNonAssociation(1, "A1234AA", "X1234AD", now.minusDays(2)),
        genNonAssociation(2, "A1234AA", "X1234AE", now.minusDays(1)),
        genNonAssociation(3, "Y1234AA", "A1234AA", now),
        genNonAssociation(4, "Y1234AB", "A1234AA", now.plusDays(1)),
        genNonAssociation(5, "Y1234AC", "A1234AA", now.plusDays(2)),
      ),
    )
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners("A1234BB", "X1234AE")).thenReturn(emptyList())
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners("Y1234AA", "A1234BB")).thenReturn(emptyList())

    val nonAssociationMap = service.replacePrisonerNumberInDateRange(
      oldPrisonerNumber = "A1234AA",
      newPrisonerNumber = "A1234BB",
      since = now.minusDays(1),
      until = now,
    )

    assertThat(nonAssociationMap).isEqualTo(
      mapOf(
        MergeResult.MERGED to listOf(
          genNonAssociation(2, "A1234BB", "X1234AE", now.minusDays(1)),
          genNonAssociation(3, "Y1234AA", "A1234BB", now),
        ),
      ),
    )
  }

  /**
   * Bug: when both merge participants have an open NA with the same third party, and the new prisoner
   * already has a closed historical NA with that third party, `findByFirstPrisonerNumberAndSecondPrisonerNumber`
   * would return more than one row and throw IncorrectResultSizeDataAccessException.
   *
   * Fix: use `findOpenBetweenPrisoners` which only returns open records.
   */
  @Test
  fun `when new prison has both open closed NAs with the same 3rd party`() {
    val oldPrisoner = "A1111AA"
    val newPrisoner = "B2222BB"
    val thirdParty = "C3333CC"

    // Old prisoner has an open NA with the third party
    val oldOpenWithThirdParty = genNonAssociation(1, oldPrisoner, thirdParty, now)

    // New prisoner has an open NA with the third party — this is the live duplicate
    val newOpenWithThirdParty = genNonAssociation(2, newPrisoner, thirdParty, now.minusDays(5))

    whenever(nonAssociationsRepository.findAllByPrisonerNumber(oldPrisoner)).thenReturn(
      listOf(oldOpenWithThirdParty),
    )
    // findOpenBetweenPrisoners should return only the open record, ignoring any closed historical one
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners(newPrisoner, thirdParty)).thenReturn(
      listOf(newOpenWithThirdParty),
    )

    val result = service.replacePrisonerNumber(oldPrisoner, newPrisoner)

    // The old prisoner's NA should have been closed as a duplicate (not thrown an exception)
    assertThat(result[MergeResult.CLOSED]).hasSize(1)
    assertThat(result[MergeResult.CLOSED]!!.first().firstPrisonerNumber).isEqualTo(newPrisoner)
    assertThat(result[MergeResult.CLOSED]!!.first().secondPrisonerNumber).isEqualTo(thirdParty)
    assertThat(result[MergeResult.CLOSED]!!.first().isClosed).isTrue()
    assertThat(result[MergeResult.MERGED]).isNull()
  }

  /**
   * Bug: when both merge participants have an open NA with the same third party, but the new prisoner's
   * NA is stored in the reverse direction (thirdParty, newPrisoner) rather than (newPrisoner, thirdParty),
   * the directional lookup misses it and both NAs are left open after the merge.
   *
   * Fix: use `findOpenBetweenPrisoners` which checks both directions.
   */
  @Test
  fun `when prisoner has an open NA stored in reverse direction with the same 3rd party detected as a duplicate`() {
    val oldPrisoner = "A1111AA"
    val newPrisoner = "B2222BB"
    val thirdParty = "C3333CC"

    // Old prisoner's NA: oldPrisoner is first
    val oldOpenWithThirdParty = genNonAssociation(1, oldPrisoner, thirdParty, now)

    // New prisoner's NA: stored with thirdParty as FIRST, newPrisoner as SECOND (reverse direction)
    val newOpenWithThirdPartyReversed = genNonAssociation(2, thirdParty, newPrisoner, now.minusDays(3))

    whenever(nonAssociationsRepository.findAllByPrisonerNumber(oldPrisoner)).thenReturn(
      listOf(oldOpenWithThirdParty),
    )
    // findOpenBetweenPrisoners finds C-B even though we queried as (B, C)
    whenever(nonAssociationsRepository.findOpenBetweenPrisoners(newPrisoner, thirdParty)).thenReturn(
      listOf(newOpenWithThirdPartyReversed),
    )

    val result = service.replacePrisonerNumber(oldPrisoner, newPrisoner)

    // The migrated NA should be CLOSED as a duplicate — not left MERGED/open
    assertThat(result[MergeResult.CLOSED]).hasSize(1)
    assertThat(result[MergeResult.CLOSED]!!.first().isClosed).isTrue()
    assertThat(result[MergeResult.MERGED]).isNull()
  }
}
