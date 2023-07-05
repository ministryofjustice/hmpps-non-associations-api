package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationRestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuthenticationFacade::class, AuditorAwareImpl::class)
@WithMockUser
@Transactional
class NonAssociationRepositoryTest : TestBase() {

  @Autowired
  lateinit var repository: NonAssociationsRepository

  @Test
  fun createNonAssociation() {
    val nonna = nonAssociation(firstPrisonerNumber = "A1234BC", secondPrisonerNumber = "D5678EF")
    var savedNonna = repository.save(nonna)

    savedNonna = repository.findById(savedNonna.id).orElseThrow {
      Exception("NonAssociation with id=${savedNonna.id} couldn't be found")
    }

    assertThat(savedNonna.firstPrisonerNumber).isEqualTo(nonna.firstPrisonerNumber)
    assertThat(savedNonna.firstPrisonerReason).isEqualTo(nonna.firstPrisonerReason)
    assertThat(savedNonna.firstPrisonerReason.description).isEqualTo("Victim")
    assertThat(savedNonna.secondPrisonerNumber).isEqualTo(nonna.secondPrisonerNumber)
    assertThat(savedNonna.secondPrisonerReason).isEqualTo(nonna.secondPrisonerReason)
    assertThat(savedNonna.secondPrisonerReason.description).isEqualTo("Perpetrator")
    assertThat(savedNonna.restrictionType).isEqualTo(nonna.restrictionType)
    assertThat(savedNonna.restrictionType.description).isEqualTo("Do Not Locate in Same Cell")
    assertThat(savedNonna.comment).isEqualTo(nonna.comment)
    assertThat(savedNonna.incidentReportNumber).isNull()

    // By default non-associations are open
    assertThat(savedNonna.isClosed).isFalse
    assertThat(savedNonna.closedBy).isNull()
    assertThat(savedNonna.closedAt).isNull()
    assertThat(savedNonna.closedReason).isNull()

    assertThat(savedNonna.whenCreated).isEqualTo(savedNonna.whenUpdated)
  }

  @Test
  fun closeNonAssociationWithDetailsSucceeds() {
    var createdNonna = repository.save(
      nonAssociation(firstPrisonerNumber = "A1234BC", secondPrisonerNumber = "D5678EF"),
    )

    // Update the non-association to close it

    val closedBy = "Aldo"
    val closedReason = "They're friends now"
    val closedAt = LocalDateTime.now()
    createdNonna.close(closedBy, closedReason, closedAt)
    repository.save(createdNonna)

    // Check non-association is now closed with correct details
    val updatedNonna = repository.findById(createdNonna.id).get()
    assertThat(updatedNonna.isClosed).isTrue
    assertThat(updatedNonna.closedBy).isEqualTo(closedBy)
    assertThat(updatedNonna.closedAt).isEqualTo(closedAt)
    assertThat(updatedNonna.closedReason).isEqualTo(closedReason)
  }

  @Test
  fun closeNonAssociationWithoutDetailsFails() {
    var createdNonna = repository.save(
      nonAssociation(firstPrisonerNumber = "A1234BC", secondPrisonerNumber = "D5678EF"),
    )
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    createdNonna = repository.findById(createdNonna.id).orElseThrow {
      Exception("NonAssociation with id=${createdNonna.id} couldn't be found")
    }

    // update fails because details are missing
    createdNonna.isClosed = true
    assertThatThrownBy {
      repository.save(createdNonna)
      TestTransaction.flagForCommit()
      TestTransaction.end()
    }.isInstanceOf(DataIntegrityViolationException::class.java)

    // Check non-association is still open
    val freshNonna = repository.findById(createdNonna.id).get()
    assertThat(freshNonna.isClosed).isFalse
  }

  @Test
  fun whenUpdatedIsUpdated() {
    var created = repository.save(
      nonAssociation(firstPrisonerNumber = "A1234BC", secondPrisonerNumber = "D5678EF"),
    )
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    created.incidentReportNumber = "test-report-id"
    val updated = repository.save(created)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    assertThat(updated.incidentReportNumber).isEqualTo("test-report-id")
    assertThat(updated.whenCreated).isEqualTo(created.whenCreated)
    assertThat(updated.whenUpdated).isAfter(updated.whenCreated)
  }

  fun nonAssociation(firstPrisonerNumber: String, secondPrisonerNumber: String): NonAssociation {
    return NonAssociation(
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerReason = NonAssociationReason.VICTIM,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerReason = NonAssociationReason.PERPETRATOR,
      restrictionType = NonAssociationRestrictionType.CELL,
      comment = "John attacked Bob",
    )
  }
}
