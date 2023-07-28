package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant

@Service
class AuditService(
  @Value("\${spring.application.name}")
  private val serviceName: String,
  private val hmppsQueueService: HmppsQueueService,
  private val telemetryClient: TelemetryClient,
  private val objectMapper: ObjectMapper,
  private val authenticationFacade: AuthenticationFacade,
) {
  private val auditQueue by lazy { hmppsQueueService.findByQueueId("audit") as HmppsQueue }
  private val auditSqsClient by lazy { auditQueue.sqsClient }
  private val auditQueueUrl by lazy { auditQueue.queueUrl }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendMessage(auditType: AuditType, id: String, details: Any, username: String? = null) {
    val auditEvent = AuditEvent(
      what = auditType.name,
      who = username ?: authenticationFacade.currentUsername ?: SYSTEM_USERNAME,
      service = serviceName,
      details = details.toJson(),
    )
    log.debug("Audit {} ", auditEvent)

    val result =
      auditSqsClient.sendMessage(
        SendMessageRequest.builder()
          .queueUrl(auditQueueUrl)
          .messageBody(auditEvent.toJson())
          .build(),
      ).get()

    telemetryClient.trackEvent(
      auditEvent.what,
      mapOf("messageId" to result.messageId(), "id" to id),
      null,
    )
  }

  private fun Any.toJson() = objectMapper.writeValueAsString(this)
}

data class AuditEvent(
  val what: String,
  val `when`: Instant = Instant.now(),
  val who: String,
  val service: String,
  val details: String? = null,
)

enum class AuditType {
  NON_ASSOCIATION_CREATED,
  NON_ASSOCIATION_UPDATED,
  NON_ASSOCIATION_CLOSED,
}
