package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.MissingPrisonersInSearchException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.SubjectAccessRequestNoContentException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.SubjectAccessRequestSubjectNotRecognisedException
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
    } catch (e: MissingPrisonersInSearchException) {
      throw SubjectAccessRequestSubjectNotRecognisedException()
    }

    // Filter non-associations by given date range
    val fromDateExclusive = fromDate?.minusDays(1)
    val toDateExclusive = toDate?.plusDays(1)
    val content = nonAssociations.copy(
      nonAssociations = nonAssociations.nonAssociations.filter {
        val updatedDate = it.whenUpdated.toLocalDate()
        (fromDateExclusive == null || updatedDate.isAfter(fromDateExclusive)) &&
          (toDateExclusive == null || updatedDate.isBefore(toDateExclusive))
      },
    )

    if (content.nonAssociations.isEmpty()) {
      // No non-associations in given date range
      throw SubjectAccessRequestNoContentException(prn)
    }

    return HmppsSubjectAccessRequestContent(content)
  }
}
