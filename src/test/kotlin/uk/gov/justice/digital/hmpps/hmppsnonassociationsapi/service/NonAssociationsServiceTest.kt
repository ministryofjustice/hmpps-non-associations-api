package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.createNonAssociationRequest
import java.time.Clock
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation as NonAssociationDTO
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

class NonAssociationsServiceTest {

  private val nonAssociationsRepository: NonAssociationsRepository = mock()
  private val offenderSearchService: OffenderSearchService = mock()
  private val prisonApiService: PrisonApiService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val clock: Clock = mock()
  private val featureFlagsConfig: FeatureFlagsConfig = mock()
  private val service = NonAssociationsService(
    nonAssociationsRepository,
    offenderSearchService,
    authenticationFacade,
    prisonApiService,
    telemetryClient,
    featureFlagsConfig,
    clock,
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
      firstPrisonerRole = createNonAssociationRequest.firstPrisonerRole,
      secondPrisonerNumber = createNonAssociationRequest.secondPrisonerNumber,
      secondPrisonerRole = createNonAssociationRequest.secondPrisonerRole,
      comment = createNonAssociationRequest.comment,
      reason = createNonAssociationRequest.reason,
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
