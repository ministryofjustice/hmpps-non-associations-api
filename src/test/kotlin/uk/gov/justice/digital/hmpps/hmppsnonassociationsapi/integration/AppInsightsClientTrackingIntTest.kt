package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import uk.gov.justice.hmpps.kotlin.clienttracking.HmppsClientTrackingInterceptor

class AppInsightsClientTrackingIntTest : SqsIntegrationTestBase() {
  @Autowired
  private lateinit var applicationContext: ConfigurableApplicationContext

  @Test
  fun `the client tracking interceptor that records user UUID replaces the library default`() {
    val interceptors = applicationContext.getBeansOfType(HmppsClientTrackingInterceptor::class.java)

    assertThat(interceptors).containsOnlyKeys("hmppsClientTrackingInterceptor")
    assertThat(applicationContext.beanFactory.getBeanDefinition("hmppsClientTrackingInterceptor").factoryBeanName)
      .isEqualTo("appInsightsClientTrackingConfiguration")
  }
}
