package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.NonAssociationDetails

@Service
class NonAssociationsService(
  private val prisonApiService: PrisonApiService,
) {

  suspend fun getDetails(prisonerNumber: String): NonAssociationDetails {
    return prisonApiService.getNonAssociationDetails(prisonerNumber)
  }
}
