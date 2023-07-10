package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import jakarta.transaction.Transactional
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
}
