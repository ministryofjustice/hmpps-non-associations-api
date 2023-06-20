package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.ReasonType

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuthenticationFacade::class, AuditorAwareImpl::class)
@WithMockUser
class ReasonTypeRepositoryTest : TestBase() {

  @Autowired
  lateinit var repository: ReasonTypeRepository

  @Test
  fun getListOfReasons() {
    val reasons = repository.findAll()
    val expectedReasonCodes = listOf("BUL", "PER", "RIV", "VIC")

    assertThat(reasons.map(ReasonType::code)).isEqualTo(expectedReasonCodes)
  }

  @Test
  fun getReasonTypeByCode() {
    var reason = repository.findById("VIC").orElseThrow { Exception("ReasonType not found") }
    assertThat(reason).isNotNull
    assertThat(reason.description).isEqualTo("Victim")
  }

  @Test
  fun updateRecord() {
    var reason = repository.findById("VIC").orElseThrow { Exception("ReasonType not found") }
    var whenCreated = reason.whenCreated

    repository.save(reason.copy(description = "UPDATED"))

    var reasonUpdated = repository.findById("VIC").orElseThrow { Exception("ReasonType not found") }
    assertThat(reasonUpdated.description).isEqualTo("UPDATED")
    assertThat(reasonUpdated.whenCreated).isEqualTo(whenCreated)
  }
}
