package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.EventPublishService
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.services.NonAssociationDomainEventType

abstract class NonAssociationsBaseResource {

  @Autowired
  private lateinit var eventPublishService: EventPublishService

  protected fun eventPublishWrapper(event: NonAssociationDomainEventType, function: () -> NonAssociation) =
    function().also { nonAssociation -> eventPublishService.publishEvent(event, nonAssociation, nonAssociation) }

  protected fun deleteEventPublishWrapper(function: () -> Pair<NonAssociation, Any>) =
    function().also { (nonAssociation, auditData) ->
      eventPublishService.publishEvent(
        NonAssociationDomainEventType.NON_ASSOCIATION_DELETED,
        nonAssociation,
        auditData,
      )
    }
}
