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
class NonAssociationsService(
  private val nonAssociationsRepository: NonAssociationsRepository,
  private val authenticationFacade: AuthenticationFacade,
  private val prisonApiService: PrisonApiService,
) {

  fun createNonAssociation(createNonAssociationRequest: CreateNonAssociationRequest): NonAssociationDTO {
    val nonAssociationJpa = createNonAssociationRequest.toNewEntity(
      authorisedBy = authenticationFacade.currentUsername ?: throw Exception("Could not determine current user's username'"),
    )
    return persistNonAssociation(nonAssociationJpa).also {
      // TODO: Publish domain event (outside transaction)
    }.toDto()
  }

  fun getDetails(prisonerNumber: String): LegacyNonAssociationDetails {
    return prisonApiService.getNonAssociationDetails(prisonerNumber)
  }

  @Transactional
  private fun persistNonAssociation(nonAssociation: NonAssociationJPA): NonAssociationJPA {
    return nonAssociationsRepository.save(nonAssociation)
  }
}

private fun CreateNonAssociationRequest.toNewEntity(authorisedBy: String): NonAssociationJPA {
  return NonAssociationJPA(
    id = null,
    firstPrisonerNumber = firstPrisonerNumber,
    firstPrisonerReason = firstPrisonerReason,
    secondPrisonerNumber = secondPrisonerNumber,
    secondPrisonerReason = secondPrisonerReason,
    restrictionType = restrictionType,
    comment = comment,
    authorisedBy = authorisedBy,
  )
}

private fun NonAssociationJPA.toDto(): NonAssociationDTO {
  return NonAssociationDTO(
    id = id!!,
    firstPrisonerNumber = firstPrisonerNumber,
    firstPrisonerReason = firstPrisonerReason,
    secondPrisonerNumber = secondPrisonerNumber,
    secondPrisonerReason = secondPrisonerReason,
    restrictionType = restrictionType,
    comment = comment,
    // TODO: Do we need to do anything special with this?
    //       This field being optional in NOMIS/Prison API
    //       It may be one of the things we make mandatory after migration?
    authorisedBy = authorisedBy ?: "",
  )
}
