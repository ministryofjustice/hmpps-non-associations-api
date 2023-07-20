package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyNonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.toPrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation as NonAssociationDTO
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

@Service
@Transactional
class NonAssociationsService(
  private val nonAssociationsRepository: NonAssociationsRepository,
  private val offenderSearch: OffenderSearchService,
  private val authenticationFacade: AuthenticationFacade,
  private val prisonApiService: PrisonApiService,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createNonAssociation(createNonAssociationRequest: CreateNonAssociationRequest): NonAssociationDTO {
    offenderSearch.searchByPrisonerNumbers(
      listOf(
        createNonAssociationRequest.firstPrisonerNumber,
        createNonAssociationRequest.secondPrisonerNumber,
      ),
    )

    val nonAssociationJpa = createNonAssociationRequest.toNewEntity(
      authorisedBy = authenticationFacade.currentUsername
        ?: throw Exception("Could not determine current user's username'"),
    )
    return persistNonAssociation(nonAssociationJpa).toDto()
  }

  fun getById(id: Long): NonAssociationDTO? {
    return nonAssociationsRepository.findById(id).getOrNull()?.toDto()
  }

  fun getPrisonerNonAssociations(prisonerNumber: String, options: NonAssociationListOptions): PrisonerNonAssociations {
    val nonAssociations = nonAssociationsRepository.findAllByFirstPrisonerNumber(prisonerNumber) +
      nonAssociationsRepository.findAllBySecondPrisonerNumber(prisonerNumber)

    val prisonerNumbers = (
      listOf(prisonerNumber) +
        nonAssociations.map(NonAssociationJPA::firstPrisonerNumber) +
        nonAssociations.map(NonAssociationJPA::secondPrisonerNumber)
      ).distinct()
    val prisoners = offenderSearch.searchByPrisonerNumbers(prisonerNumbers)

    var nonAssociationsFiltered = nonAssociations
    // filter out non-associations in other prisons
    if (!options.includeOtherPrisons) {
      val prisonId = prisoners[prisonerNumber]!!.prisonId
      nonAssociationsFiltered = nonAssociationsFiltered.filter { nonna ->
        prisoners[nonna.firstPrisonerNumber]!!.prisonId == prisonId &&
          prisoners[nonna.secondPrisonerNumber]!!.prisonId == prisonId
      }
    }
    // filter out closed non-associations
    if (!options.includeClosed) {
      nonAssociationsFiltered = nonAssociationsFiltered.filter { nonna ->
        !nonna.isClosed
      }
    }

    return nonAssociationsFiltered.toPrisonerNonAssociations(
      prisonerNumber,
      prisoners,
      options,
    )
  }

  fun getLegacyDetails(prisonerNumber: String): LegacyNonAssociationDetails {
    return prisonApiService.getNonAssociationDetails(prisonerNumber)
  }

  private fun persistNonAssociation(nonAssociation: NonAssociationJPA): NonAssociationJPA {
    return nonAssociationsRepository.save(nonAssociation)
  }
}

data class NonAssociationListOptions(
  val includeOtherPrisons: Boolean = false,
  val includeClosed: Boolean = false,
  val sortBy: NonAssociationsSort = NonAssociationsSort.WHEN_CREATED,
  val sortDirection: Sort.Direction = Sort.Direction.DESC,
)

enum class NonAssociationsSort {
  WHEN_CREATED,
  LAST_NAME,
  ;

  fun comparator(direction: Sort.Direction): Comparator<NonAssociationDetails> {
    return when (this) {
      WHEN_CREATED -> Comparator.comparing(NonAssociationDetails::whenCreated)
      LAST_NAME -> Comparator.comparing { nonna -> nonna.otherPrisonerDetails.lastName }
    }.run {
      if (direction == Sort.Direction.DESC) this.reversed() else this
    }
  }
}
