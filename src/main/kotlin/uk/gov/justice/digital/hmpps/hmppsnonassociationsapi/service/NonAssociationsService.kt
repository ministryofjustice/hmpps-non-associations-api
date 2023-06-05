package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationsDetails

// TODO: Check annotation, type in Incentives API was different
@Service
class NonAssociationsService {

  fun getDetails(bookingId: Long): NonAssociationsDetails {
    return NonAssociationsDetails(
      offenderNo = "TODO: Implement me",
    )
  }
}
