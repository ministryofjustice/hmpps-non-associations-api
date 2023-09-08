package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.services.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.services.NonAssociationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.services.SnsService
import java.time.Clock
import java.time.LocalDateTime

@Service
class EventPublishService(
  private val snsService: SnsService,
  private val auditService: AuditService,
  private val clock: Clock,
) {

  fun publishEvent(
    event: NonAssociationDomainEventType,
    nonAssociation: NonAssociation,
    auditData: Any,
    source: String = "non-associations-api",
  ) {
    snsService.publishDomainEvent(
      event,
      "${event.description} ${nonAssociation.id}",
      occurredAt = LocalDateTime.now(clock),
      AdditionalInformation(
        id = nonAssociation.id,
        nsPrisonerNumber1 = nonAssociation.firstPrisonerNumber,
        nsPrisonerNumber2 = nonAssociation.secondPrisonerNumber,
        source = source,
      ),
    )

    auditService.sendMessage(
      event.auditType,
      nonAssociation.id.toString(),
      auditData,
    )
  }
}
