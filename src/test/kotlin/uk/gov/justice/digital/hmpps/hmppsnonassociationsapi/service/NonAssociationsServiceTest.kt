package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationRestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.createNonAssociationRequest
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation as NonAssociationDTO
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

class NonAssociationsServiceTest {

  private val nonAssociationsRepository: NonAssociationsRepository = mock()
  private val prisonApiService: PrisonApiService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val service = NonAssociationsService(
    nonAssociationsRepository,
    authenticationFacade,
    prisonApiService,
  )

  @Test
  fun createNonAssociation() {
    val createNonAssociationRequest: CreateNonAssociationRequest = createNonAssociationRequest()

    val authorisedBy = "TEST_USER_GEN"
    whenever(authenticationFacade.currentUsername).thenReturn(authorisedBy)

    val expectedId = 42L
    val createdNonAssociationJPA = NonAssociationJPA(
      id = expectedId,
      firstPrisonerNumber = createNonAssociationRequest.firstPrisonerNumber,
      firstPrisonerReason = createNonAssociationRequest.firstPrisonerReason,
      secondPrisonerNumber = createNonAssociationRequest.secondPrisonerNumber,
      secondPrisonerReason = createNonAssociationRequest.secondPrisonerReason,
      comment = createNonAssociationRequest.comment,
      restrictionType = createNonAssociationRequest.restrictionType,
      authorisedBy = authorisedBy,
      isClosed = false,
      closedBy = null,
      closedReason = null,
      closedAt = null,
    )

    whenever(nonAssociationsRepository.save(any()))
      .thenReturn(createdNonAssociationJPA)

    val createdNonAssociationDTO: NonAssociationDTO = service.createNonAssociation(createNonAssociationRequest)

    val expectedCreatedNonAssociationDTO = createdNonAssociationJPA.toDto()
    assertThat(createdNonAssociationDTO).isEqualTo(expectedCreatedNonAssociationDTO)
  }

  @Test
  fun mergeNonAssociationPrisonerNumbers() {
    val now = LocalDateTime.now()

    whenever(nonAssociationsRepository.findAllByFirstPrisonerNumber("A1234AA")).thenReturn(
      listOf(
        genNonAssociation(1, "A1234AA", "X12234AA", now),
        genNonAssociation(2, "A1234AA", "X12234AB", now),
        genNonAssociation(3, "A1234AA", "X12234AC", now),
        genNonAssociation(4, "A1234AA", "X12234AD", now),
        genNonAssociation(5, "A1234AA", "X12234AE", now),
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
      genNonAssociation(1, "A1234BB", "X12234AA", now),
      genNonAssociation(2, "A1234BB", "X12234AB", now),
      genNonAssociation(3, "A1234BB", "X12234AC", now),
      genNonAssociation(4, "A1234BB", "X12234AD", now),
      genNonAssociation(5, "A1234BB", "X12234AE", now),
      genNonAssociation(11, "Y1234AA", "A1234BB", now),
      genNonAssociation(22, "Y1234AB", "A1234BB", now),
      genNonAssociation(33, "Y1234AC", "A1234BB", now),
    )

    verify(nonAssociationsRepository).saveAll(resultantNonAssociations)
    assertThat(nonAssociationList).hasSize(8)

    assertThat(nonAssociationList).isEqualTo(resultantNonAssociations)
  }

  private fun genNonAssociation(
    id: Long,
    firstPrisonerNumber: String,
    secondPrisonerNumber: String,
    createTime: LocalDateTime = LocalDateTime.now(),
  ) = NonAssociation(
    id = id,
    firstPrisonerNumber = firstPrisonerNumber,
    firstPrisonerReason = NonAssociationReason.BULLYING,
    secondPrisonerNumber = secondPrisonerNumber,
    secondPrisonerReason = NonAssociationReason.VICTIM,
    comment = "Comment",
    restrictionType = NonAssociationRestrictionType.WING,
    authorisedBy = "TEST",
    whenUpdated = createTime,
    whenCreated = createTime,
  )
}
