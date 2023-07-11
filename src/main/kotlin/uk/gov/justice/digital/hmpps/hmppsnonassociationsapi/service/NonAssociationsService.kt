package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyNonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation as NonAssociationDTO
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

@Service
@Transactional
class NonAssociationsService(
  private val nonAssociationsRepository: NonAssociationsRepository,
  private val authenticationFacade: AuthenticationFacade,
  private val prisonApiService: PrisonApiService,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createNonAssociation(createNonAssociationRequest: CreateNonAssociationRequest): NonAssociationDTO {
    val nonAssociationJpa = createNonAssociationRequest.toNewEntity(
      authorisedBy = authenticationFacade.currentUsername ?: throw Exception("Could not determine current user's username'"),
    )
    return persistNonAssociation(nonAssociationJpa).toDto()
  }

  fun getDetails(prisonerNumber: String): LegacyNonAssociationDetails {
    return prisonApiService.getNonAssociationDetails(prisonerNumber)
  }

  private fun persistNonAssociation(nonAssociation: NonAssociationJPA): NonAssociationJPA {
    return nonAssociationsRepository.save(nonAssociation)
  }

  fun mergePrisonerNumbers(oldPrisonerNumber: String, newPrisonerNumber: String): List<NonAssociationJPA> {
    log.info("Replacing prisoner number $oldPrisonerNumber to $newPrisonerNumber")

    val nonAssociationList1 = nonAssociationsRepository.findAllByFirstPrisonerNumber(oldPrisonerNumber).map {
      it.copy(firstPrisonerNumber = newPrisonerNumber)
    }

    val nonAssociationList2 = nonAssociationsRepository.findAllBySecondPrisonerNumber(oldPrisonerNumber).map {
      it.copy(secondPrisonerNumber = newPrisonerNumber)
    }

    return nonAssociationList1.plus(nonAssociationList2)
  }
}
