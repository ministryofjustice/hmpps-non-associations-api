package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.SubjectAccessRequestNoContentException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListOptions
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SubjectAccessRequestService(
  private val nonAssociationsService: NonAssociationsService,
) : HmppsPrisonSubjectAccessRequestService {

  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val options = NonAssociationListOptions(includeOtherPrisons = true, inclusion = NonAssociationListInclusion.ALL)

    val nonAssociations = try {
      nonAssociationsService.getPrisonerNonAssociations(prn, options)
    } catch (e: NotFound)  {
      throw SubjectAccessRequestNoContentException(prn)
    }

    // Filter non-associations by given date range
    val content = nonAssociations.copy(
      nonAssociations = nonAssociations.nonAssociations.filter {
        (fromDate == null || it.whenCreated.toLocalDate().isAfter(fromDate)) && (toDate == null || it.whenCreated.toLocalDate().isBefore(toDate))
      },
    )

    if (content.nonAssociations.isEmpty()) {
      // No non-associations in given date range
      throw SubjectAccessRequestNoContentException(prn)
    }

    return HmppsSubjectAccessRequestContent(content)
  }
}
