package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.health

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase

class TopicHealthCheckTest : SqsIntegrationTestBase() {

  @Test
  fun `Outbound topic health ok`() {
    hmppsAuthMockServer.stubHealthPing(200)
    prisonerSearchMockServer.stubHealthPing(200)
    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
      .jsonPath("components.domainevents-health.status").isEqualTo("UP")
      .jsonPath(
        "components.domainevents-health.details.topicArn",
      ).isEqualTo(hmppsSqsPropertiesSpy.domaineventsTopicConfig().arn)
      .jsonPath("components.domainevents-health.details.subscriptionsConfirmed").isEqualTo(0)
      .jsonPath("components.domainevents-health.details.subscriptionsPending").isEqualTo(0)
  }
}
