package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.EventPublishService
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.services.NonAssociationDomainEventType

abstract class NonAssociationsBaseResource {

  @Autowired
  private lateinit var eventPublishService: EventPublishService

  fun eventPublishWrapper(event: NonAssociationDomainEventType, function: () -> NonAssociation) =
    function.invoke().also { eventPublishService.publishEvent(event, it) }
}
