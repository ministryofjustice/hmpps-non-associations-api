package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.NonAssociationDetails

@Service
class NonAssociationsService {

  fun getDetails(bookingId: Long): NonAssociationDetails {
    return prisonApiService.getNonAssociationDetails(bookingId)
  }
}
