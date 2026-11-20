package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.SubjectAccessRequestNoContentException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.sar.SarNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.sar.SarRedactedNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPrisonerNumber
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

/**
 * Subject access request (SAR) content for non-associations.
 *
 * The Offender SAR team's senior management decided (12 November 2026, IR-2043) that non-associations must not
 * be disclosed, and that a SAR report should instead show each non-association in a fully sanitised form, every
 * detail replaced by [SarRedactedNonAssociation.REDACTED]. Doing that here rather than in the template means the
 * real values never leave this service, so they cannot be disclosed by mistake while a report is being vetted.
 *
 * The response tells the reader only how many non-associations fall in the requested date range. It reads this
 * service's own table directly, so it no longer depends on prisoner-search, and a prisoner with nothing in range
 * gets 204 No Content as before.
 */
@Service
class SubjectAccessRequestService(
  private val nonAssociationsRepository: NonAssociationsRepository,
) : HmppsPrisonSubjectAccessRequestService {

  @Transactional(readOnly = true)
  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    // Same date rule as before the redaction: a non-association is in range if it was last updated in the range
    val inRange = nonAssociationsRepository.findAllByPrisonerNumber(prn)
      .filter {
        val updatedDate = it.whenUpdated.toLocalDate()
        (fromDate == null || !updatedDate.isBefore(fromDate)) &&
          (toDate == null || !updatedDate.isAfter(toDate))
      }

    if (inRange.isEmpty()) {
      throw SubjectAccessRequestNoContentException(prn)
    }

    val redacted = inRange.indices.map { SarRedactedNonAssociation(number = it + 1) }
    return HmppsSubjectAccessRequestContent(SarNonAssociations(redacted))
  }
}
