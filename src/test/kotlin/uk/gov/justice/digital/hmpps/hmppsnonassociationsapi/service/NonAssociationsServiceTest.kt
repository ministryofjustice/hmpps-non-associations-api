package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListOptions
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationsSort
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.createNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.genNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.prisonerSearchPrisoners
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockUserSecurityContextFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation as NonAssociationDTO
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

/**
 * This class contains unit tests for the NonAssociationsService class.
 */
@DisplayName("Non-associations service")
class NonAssociationsServiceTest {
  private val nonAssociationsRepository: NonAssociationsRepository = mock()
  private val prisonerSearchService: PrisonerSearchService = mock()
  private val authenticationHolder = HmppsAuthenticationHolder()
  private val telemetryClient: TelemetryClient = mock()

  private val service = NonAssociationsService(
    nonAssociationsRepository,
    prisonerSearchService,
    authenticationHolder,
    telemetryClient,
    TestBase.clock,
  )

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  private fun setUserInSecurityContext(
    username: String,
    roles: List<String> = listOf("ROLE_READ_NON_ASSOCIATIONS", "ROLE_WRITE_NON_ASSOCIATIONS"),
    scopes: List<String> = listOf("read", "write"),
  ) {
    val authorities = roles.toMutableList()
    authorities.addAll(scopes.map { "SCOPE_$it" })
    SecurityContextHolder.setContext(
      WithMockUserSecurityContextFactory().createSecurityContext(
        WithMockAuthUser(
          clientId = "hmpps-non-associations-api",
          username = username,
          authorities = authorities.toTypedArray(),
        ),
      ),
    )
  }

  @Test
  fun createNonAssociation() {
    val createNonAssociationRequest: CreateNonAssociationRequest = createNonAssociationRequest()

    val createdBy = "TEST_USER_GEN"
    setUserInSecurityContext(createdBy)

    val expectedId = 42L
    val now = LocalDateTime.now(TestBase.clock)
    val createdNonAssociationJPA = NonAssociationJPA(
      id = expectedId,
      firstPrisonerNumber = createNonAssociationRequest.firstPrisonerNumber,
      firstPrisonerRole = createNonAssociationRequest.firstPrisonerRole,
      secondPrisonerNumber = createNonAssociationRequest.secondPrisonerNumber,
      secondPrisonerRole = createNonAssociationRequest.secondPrisonerRole,
      comment = createNonAssociationRequest.comment,
      reason = createNonAssociationRequest.reason,
      restrictionType = createNonAssociationRequest.restrictionType,
      authorisedBy = createdBy,
      isClosed = false,
      closedBy = null,
      closedReason = null,
      closedAt = null,
      updatedBy = createdBy,
      whenCreated = now,
      whenUpdated = now,
    )

    whenever(nonAssociationsRepository.save(any()))
      .thenReturn(createdNonAssociationJPA)

    val createdNonAssociationDTO: NonAssociationDTO = service.createNonAssociation(createNonAssociationRequest)

    val expectedCreatedNonAssociationDTO = createdNonAssociationJPA.toDto()
    assertThat(createdNonAssociationDTO).isEqualTo(expectedCreatedNonAssociationDTO)
  }

  @DisplayName("sorting of prisoner non-associations")
  @Nested
  inner class Sorting {
    private val keyPrisoner = "A1234BC"
    private val otherPrisoners = prisonerSearchPrisoners.keys.filter { it != keyPrisoner }

    @BeforeEach
    fun setUp() {
      var createTime = LocalDateTime.now(TestBase.clock)
      val nonAssociations = otherPrisoners.mapIndexed { index, otherPrisoner ->
        createTime = createTime.plusDays(1)
        genNonAssociation(
          id = index.toLong(),
          firstPrisonerNumber = keyPrisoner,
          secondPrisonerNumber = otherPrisoner,
          createTime = createTime,
        )
      }

      whenever(prisonerSearchService.searchByPrisonerNumbers(any())).thenReturn(prisonerSearchPrisoners)
      whenever(
        nonAssociationsRepository.findAllByFirstPrisonerNumberOrSecondPrisonerNumber(eq(keyPrisoner), eq(keyPrisoner)),
      )
        .thenReturn(nonAssociations)
    }

    private infix fun NonAssociationListOptions.assertSortsNonAssociationsBy(
      comparator: Comparator<PrisonerNonAssociation>,
    ) {
      val prisonerNonAssociations = service.getPrisonerNonAssociations(keyPrisoner, this)
      assertThat(prisonerNonAssociations.nonAssociations).isSortedAccordingTo(comparator)
    }

    @Test
    fun `by default order`() {
      val defaultListOptions = NonAssociationListOptions()
      defaultListOptions assertSortsNonAssociationsBy { n1, n2 ->
        // descending by created date
        n2.whenCreated.compareTo(n1.whenCreated)
      }
    }

    @Test
    fun `by updated date in default direction`() {
      val listOptions = NonAssociationListOptions(sortBy = NonAssociationsSort.WHEN_UPDATED)
      listOptions assertSortsNonAssociationsBy { n1, n2 ->
        // descending by updated date
        n2.whenUpdated.compareTo(n1.whenUpdated)
      }
    }

    @Test
    fun `by updated date in specified direction`() {
      val listOptions =
        NonAssociationListOptions(sortBy = NonAssociationsSort.WHEN_UPDATED, sortDirection = Sort.Direction.ASC)
      listOptions assertSortsNonAssociationsBy { n1, n2 ->
        // ascending by updated date
        n1.whenCreated.compareTo(n2.whenCreated)
      }
    }

    @Test
    fun `by prisoner number in default direction`() {
      val listOptions = NonAssociationListOptions(sortBy = NonAssociationsSort.PRISONER_NUMBER)
      listOptions assertSortsNonAssociationsBy { n1, n2 ->
        // ascending by updated date
        n1.otherPrisonerDetails.prisonerNumber.compareTo(n2.otherPrisonerDetails.prisonerNumber)
      }
    }

    @Test
    fun `by prisoner number in specified direction`() {
      val listOptions =
        NonAssociationListOptions(sortBy = NonAssociationsSort.PRISONER_NUMBER, sortDirection = Sort.Direction.DESC)
      listOptions assertSortsNonAssociationsBy { n1, n2 ->
        // descending by updated date
        n2.otherPrisonerDetails.prisonerNumber.compareTo(n1.otherPrisonerDetails.prisonerNumber)
      }
    }

    @Test
    fun `by all sort-by options`() {
      val dtFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

      NonAssociationsSort.entries.forEach { sortBy ->
        val getter: (PrisonerNonAssociation) -> String = when (sortBy) {
          NonAssociationsSort.WHEN_CREATED -> { n -> n.whenCreated.format(dtFormat) }
          NonAssociationsSort.WHEN_UPDATED -> { n -> n.whenUpdated.format(dtFormat) }
          NonAssociationsSort.WHEN_CLOSED -> { n -> n.closedAt?.format(dtFormat) ?: "" }
          NonAssociationsSort.LAST_NAME -> { n -> n.otherPrisonerDetails.lastName }
          NonAssociationsSort.FIRST_NAME -> { n -> n.otherPrisonerDetails.firstName }
          NonAssociationsSort.PRISONER_NUMBER -> { n -> n.otherPrisonerDetails.prisonerNumber }
          NonAssociationsSort.PRISON_ID -> { n -> n.otherPrisonerDetails.prisonId ?: "" }
          NonAssociationsSort.PRISON_NAME -> { n -> n.otherPrisonerDetails.prisonName ?: "" }
          NonAssociationsSort.CELL_LOCATION -> { n -> n.otherPrisonerDetails.cellLocation ?: "" }
        }
        Sort.Direction.entries.forEach { sortDirection ->
          val listOptions = NonAssociationListOptions(sortBy = sortBy, sortDirection = sortDirection)
          listOptions assertSortsNonAssociationsBy { n1, n2 ->
            val p1 = getter(n1)
            val p2 = getter(n2)
            when (sortDirection) {
              Sort.Direction.ASC -> p1.compareTo(p2)
              Sort.Direction.DESC -> p2.compareTo(p1)
            }
          }
        }
      }
    }
  }
}
