package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.createNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.genNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.offenderSearchPrisoners
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation as NonAssociationDTO
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

class NonAssociationsServiceTest {

  private val nonAssociationsRepository: NonAssociationsRepository = mock()
  private val offenderSearchService: OffenderSearchService = mock()
  private val prisonApiService: PrisonApiService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val clock: Clock = Clock.fixed(
    Instant.parse("2023-07-15T12:34:56+00:00"),
    ZoneId.of("Europe/London"),
  )
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
      updatedBy = authorisedBy,
    )

    whenever(nonAssociationsRepository.save(any()))
      .thenReturn(createdNonAssociationJPA)

    val createdNonAssociationDTO: NonAssociationDTO = service.createNonAssociation(createNonAssociationRequest)

    val expectedCreatedNonAssociationDTO = createdNonAssociationJPA.toDto()
    assertThat(createdNonAssociationDTO).isEqualTo(expectedCreatedNonAssociationDTO)
  }

  @Nested
  inner class `sorting of prisoner non-associations` {
    private val otherPrisoners = listOf("D5678EF", "G9012HI", "L3456MN")

    @BeforeEach
    fun setUp() {
      whenever(offenderSearchService.searchByPrisonerNumbers(any(), any())).thenReturn(offenderSearchPrisoners)
    }

    private infix fun NonAssociationListOptions.assertSortsNonAssociationsBy(comparator: Comparator<PrisonerNonAssociation>) {
      val prisonerNonAssociations = service.getPrisonerNonAssociations("A1234BC", this)
      assertThat(prisonerNonAssociations.nonAssociations).isSortedAccordingTo(comparator)
    }

    @Test
    fun `by default order`() {
      var createTime = LocalDateTime.now(clock)
      val nonAssociations = otherPrisoners.mapIndexed { index, otherPrisoner ->
        createTime = createTime.plusDays(1)
        genNonAssociation(
          id = index.toLong(),
          firstPrisonerNumber = "A1234BC",
          secondPrisonerNumber = otherPrisoner,
          createTime = createTime,
        )
      }
      whenever(nonAssociationsRepository.findAllByFirstPrisonerNumberOrSecondPrisonerNumber(eq("A1234BC"), eq("A1234BC")))
        .thenReturn(nonAssociations)

      NonAssociationListOptions() assertSortsNonAssociationsBy { n1, n2 ->
        // descending by created date
        n2.whenCreated.compareTo(n1.whenCreated)
      }
    }

    @Test
    fun `by updated date in default direction`() {
      var createTime = LocalDateTime.now(clock)
      val nonAssociations = otherPrisoners.mapIndexed { index, otherPrisoner ->
        createTime = createTime.plusDays(1)
        genNonAssociation(
          id = index.toLong(),
          firstPrisonerNumber = "A1234BC",
          secondPrisonerNumber = otherPrisoner,
          createTime = createTime,
        )
      }
      whenever(nonAssociationsRepository.findAllByFirstPrisonerNumberOrSecondPrisonerNumber(eq("A1234BC"), eq("A1234BC")))
        .thenReturn(nonAssociations)

      NonAssociationListOptions(
        sortBy = NonAssociationsSort.WHEN_UPDATED,
      ) assertSortsNonAssociationsBy { n1, n2 ->
        // descending by updated date
        n2.whenUpdated.compareTo(n1.whenUpdated)
      }
    }

    @Test
    fun `by updated date in specified direction`() {
      var createTime = LocalDateTime.now(clock)
      val nonAssociations = otherPrisoners.mapIndexed { index, otherPrisoner ->
        createTime = createTime.plusDays(1)
        genNonAssociation(
          id = index.toLong(),
          firstPrisonerNumber = "A1234BC",
          secondPrisonerNumber = otherPrisoner,
          createTime = createTime,
        )
      }
      whenever(nonAssociationsRepository.findAllByFirstPrisonerNumberOrSecondPrisonerNumber(eq("A1234BC"), eq("A1234BC")))
        .thenReturn(nonAssociations)

      NonAssociationListOptions(
        sortBy = NonAssociationsSort.WHEN_UPDATED,
        sortDirection = Sort.Direction.ASC,
      ) assertSortsNonAssociationsBy { n1, n2 ->
        // ascending by updated date
        n1.whenCreated.compareTo(n2.whenCreated)
      }
    }

    @Test
    fun `by prisoner number in default direction`() {
      val nonAssociations = otherPrisoners.mapIndexed { index, otherPrisoner ->
        genNonAssociation(
          id = index.toLong(),
          firstPrisonerNumber = "A1234BC",
          secondPrisonerNumber = otherPrisoner,
        )
      }
      whenever(nonAssociationsRepository.findAllByFirstPrisonerNumberOrSecondPrisonerNumber(eq("A1234BC"), eq("A1234BC")))
        .thenReturn(nonAssociations)

      NonAssociationListOptions(
        sortBy = NonAssociationsSort.PRISONER_NUMBER,
      ) assertSortsNonAssociationsBy { n1, n2 ->
        // ascending by updated date
        n1.otherPrisonerDetails.prisonerNumber.compareTo(n2.otherPrisonerDetails.prisonerNumber)
      }
    }

    @Test
    fun `by prisoner number in specified direction`() {
      val nonAssociations = otherPrisoners.mapIndexed { index, otherPrisoner ->
        genNonAssociation(
          id = index.toLong(),
          firstPrisonerNumber = "A1234BC",
          secondPrisonerNumber = otherPrisoner,
        )
      }
      whenever(nonAssociationsRepository.findAllByFirstPrisonerNumberOrSecondPrisonerNumber(eq("A1234BC"), eq("A1234BC")))
        .thenReturn(nonAssociations)

      NonAssociationListOptions(
        sortBy = NonAssociationsSort.PRISONER_NUMBER,
        sortDirection = Sort.Direction.DESC,
      ) assertSortsNonAssociationsBy { n1, n2 ->
        // descending by updated date
        n2.otherPrisonerDetails.prisonerNumber.compareTo(n1.otherPrisonerDetails.prisonerNumber)
      }
    }
  }
}
