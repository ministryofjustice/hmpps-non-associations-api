package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuthenticationFacade::class, AuditorAwareImpl::class)
@WithMockUser
@Transactional
class NonAssociationRepositoryTest : TestBase() {

  @Autowired
  lateinit var repository: NonAssociationRepository

  @Test
  fun createNonAssociation() {
    val nonna = nonAssociation(firstPrisonerNumber = "A1234BC", secondPrisonerNumber = "D5678EF")
    var savedNonna = repository.save(nonna)

    savedNonna = repository.findById(savedNonna.id).orElseThrow {
      Exception("NonAssociation with id=${savedNonna.id} couldn't be found")
    }

    assertThat(savedNonna.firstPrisonerNumber).isEqualTo(nonna.firstPrisonerNumber)
    assertThat(savedNonna.firstPrisonerReasonCode).isEqualTo(nonna.firstPrisonerReasonCode)
    assertThat(savedNonna.secondPrisonerNumber).isEqualTo(nonna.secondPrisonerNumber)
    assertThat(savedNonna.secondPrisonerReasonCode).isEqualTo(nonna.secondPrisonerReasonCode)
    assertThat(savedNonna.restrictionType).isEqualTo(nonna.restrictionType)
    assertThat(savedNonna.comment).isEqualTo(nonna.comment)
    assertThat(savedNonna.incidentReportNumber).isNull()

    // By default non-associations are open
    assertThat(savedNonna.isClosed).isFalse
    assertThat(savedNonna.closedBy).isNull()
    assertThat(savedNonna.closedAt).isNull()
    assertThat(savedNonna.closedReason).isNull()

    assertThat(savedNonna.whenCreated).isEqualTo(savedNonna.whenUpdated)
  }

  fun nonAssociation(firstPrisonerNumber: String, secondPrisonerNumber: String): NonAssociation {
    return NonAssociation(
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerReasonCode = "VIC",
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerReasonCode = "PER",
      restrictionType = "WING",
      comment = "John attacked Bob",
    )
  }
}
