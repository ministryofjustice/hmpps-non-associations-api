package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.createNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation as NonAssociationDTO
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

class NonAssociationsServiceTest {

  private val nonAssociationsRepository: NonAssociationsRepository = mock()
  private val offenderSearchService: OffenderSearchService = mock()
  private val prisonApiService: PrisonApiService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val service = NonAssociationsService(
    nonAssociationsRepository,
    offenderSearchService,
    authenticationFacade,
    prisonApiService,
    telemetryClient,
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
      updatedBy = "A_USER",
    )

    whenever(nonAssociationsRepository.save(any()))
      .thenReturn(createdNonAssociationJPA)

    val createdNonAssociationDTO: NonAssociationDTO = service.createNonAssociation(createNonAssociationRequest)

    val expectedCreatedNonAssociationDTO = createdNonAssociationJPA.toDto()
    assertThat(createdNonAssociationDTO).isEqualTo(expectedCreatedNonAssociationDTO)
  }
}
