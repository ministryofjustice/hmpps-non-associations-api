package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.LocalStackTestcontainer
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SqsIntegrationTestBase : IntegrationTestBase() {

  companion object {
    private val localstackInstance = LocalStackTestcontainer.instance

    @Suppress("unused")
    @JvmStatic
    @DynamicPropertySource
    fun localstackProperties(registry: DynamicPropertyRegistry) {
      localstackInstance?.also { LocalStackTestcontainer.setupProperties(localstackInstance, registry) }
    }
  }

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  @MockitoSpyBean
  protected lateinit var hmppsSqsPropertiesSpy: HmppsSqsProperties

  protected val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingQueueException("HmppsTopic domainevents not found")
  }
  protected val domainEventsTopicSnsClient by lazy { domainEventsTopic.snsClient }
  protected val domainEventsTopicArn by lazy { domainEventsTopic.arn }

  protected val auditQueue by lazy { hmppsQueueService.findByQueueId("audit") as HmppsQueue }
  protected val nonAssociationsQueue by lazy { hmppsQueueService.findByQueueId("nonassociations") as HmppsQueue }

  fun HmppsSqsProperties.domaineventsTopicConfig() = topics["domainevents"]
    ?: throw MissingTopicException("domainevents has not been loaded from configuration properties")

  @BeforeEach
  fun cleanQueue() {
    auditQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(auditQueue.queueUrl).build())
    nonAssociationsQueue.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(nonAssociationsQueue.queueUrl).build(),
    )
    auditQueue.sqsClient.countMessagesOnQueue(auditQueue.queueUrl).get()
    nonAssociationsQueue.sqsClient.countMessagesOnQueue(nonAssociationsQueue.queueUrl).get()
  }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? =
    nonAssociationsQueue.sqsClient.countMessagesOnQueue(nonAssociationsQueue.queueUrl).get()
}
