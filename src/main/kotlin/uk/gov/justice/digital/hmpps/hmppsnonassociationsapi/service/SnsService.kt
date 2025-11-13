package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.publish
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SnsService(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domaineventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw RuntimeException("Topic with name domainevents doesn't exist")
  }

  @WithSpan(value = "hmpps-domain-events-topic", kind = SpanKind.PRODUCER)
  fun publishDomainEvent(
    eventType: NonAssociationDomainEventType,
    description: String,
    occurredAt: LocalDateTime,
    additionalInformation: AdditionalInformation? = null,
  ) {
    publishToDomainEventsTopic(
      HMPPSDomainEvent(
        eventType.value,
        additionalInformation,
        occurredAt.atZone(ZoneId.systemDefault()).toInstant(),
        description,
      ),
    )
  }

  private fun publishToDomainEventsTopic(payload: HMPPSDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation)
    domaineventsTopic.publish(
      eventType = payload.eventType.toString(),
      event = objectMapper.writeValueAsString(payload),
    ).also { log.info("Published event $payload to outbound topic") }
  }
}

data class AdditionalInformation(
  val id: Long? = null,
  val nsPrisonerNumber1: String? = null,
  val nsPrisonerNumber2: String? = null,
  val source: InformationSource? = null,
)

data class HMPPSDomainEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformation?,
  val version: String,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: AdditionalInformation?,
    occurredAt: Instant,
    description: String,
  ) : this(
    eventType,
    additionalInformation,
    "1.0",
    occurredAt.toOffsetDateFormat(),
    description,
  )
}

enum class NonAssociationDomainEventType(
  val value: String,
  val description: String,
  val auditType: AuditType,
) {
  NON_ASSOCIATION_CREATED(
    "non-associations.created",
    "A non-association has been created: ",
    AuditType.NON_ASSOCIATION_CREATED,
  ),
  NON_ASSOCIATION_UPSERT(
    "non-associations.amended",
    "A non-association has been amended: ",
    AuditType.NON_ASSOCIATION_UPDATED,
  ),
  NON_ASSOCIATION_REOPENED(
    "non-associations.amended",
    "A non-association has been re-opened: ",
    AuditType.NON_ASSOCIATION_REOPENED,
  ),
  NON_ASSOCIATION_CLOSED(
    "non-associations.closed",
    "A non-association has been closed: ",
    AuditType.NON_ASSOCIATION_CLOSED,
  ),
  NON_ASSOCIATION_DELETED(
    "non-associations.deleted",
    "A non-association has been deleted: ",
    AuditType.NON_ASSOCIATION_DELETED,
  ),
}

fun Instant.toOffsetDateFormat(): String =
  atZone(ZoneId.of("Europe/London")).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
