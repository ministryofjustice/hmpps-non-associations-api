package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.sar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase

/**
 * The template endpoint serves the mustache report template to the SAR tool, which renders it against the data
 * endpoint's response. The endpoint is the library's; what is worth asserting here is that it is switched on,
 * that it serves exactly the file in the repository, and that it is not readable without the SAR role.
 *
 * Exactly matters: the SAR tool recognises a registered template by its hash, so any difference between what is
 * served and what was registered suspends the product.
 */
class SubjectAccessRequestTemplateTest : SqsIntegrationTestBase() {

  @Test
  fun `requires a token`() {
    webTestClient.get().uri(TEMPLATE_URL)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `requires the SAR_DATA_ACCESS role`() {
    webTestClient.get().uri(TEMPLATE_URL)
      .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `serves the report template byte for byte as plain text`() {
    val served = webTestClient.get().uri(TEMPLATE_URL)
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
      .expectBody(ByteArray::class.java)
      .returnResult().responseBody!!

    val file = javaClass.getResourceAsStream(TEMPLATE_FILE)!!.use { it.readBytes() }

    assertThat(served).isEqualTo(file)
  }

  private companion object {
    const val TEMPLATE_URL = "/subject-access-request/template"
    const val TEMPLATE_FILE = "/sar/templates/V1__sar_template.mustache"
  }
}
