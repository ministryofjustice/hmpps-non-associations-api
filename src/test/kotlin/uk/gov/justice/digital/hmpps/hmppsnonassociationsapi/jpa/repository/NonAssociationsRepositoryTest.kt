package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Reason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.RestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Role
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuthenticationFacade::class, AuditorAwareImpl::class)
@WithMockUser(username = "A_USER")
@Transactional
class NonAssociationsRepositoryTest : TestBase() {

  @Autowired
  lateinit var repository: NonAssociationsRepository

  @BeforeEach
  fun setUp() {
    repository.deleteAll()
  }

  @Test
  fun findAllByPrisonerNumber() {
    repository.saveAll(
      listOf(
        nonAssociation("A1234CB", "X0123BB"), // not returned
        nonAssociation("D5678EF", "A1234BC"),
        nonAssociation("A1234BC", "D5678EG"),
        nonAssociation("X0123AA", "X0123BB"), // not returned
        nonAssociation("A1234BC", "G0011AA"),
        nonAssociation("G0022BB", "A1234BC"),
      ),
    )

    val nonAssociations = repository.findAllByPrisonerNumber("A1234BC")

    assertThat(nonAssociations).hasSize(4)
    assertThat(nonAssociations).allMatch {
      (it.firstPrisonerNumber == "A1234BC").xor(it.secondPrisonerNumber == "A1234BC")
    }
  }

  @Test
  fun findAllByFirstPrisonerNumberInOrSecondPrisonerNumberIn() {
    repository.saveAll(
      listOf(
        nonAssociation("A1234CB", "X0123BB"),
        nonAssociation("D5678EF", "A1234BC", true), // returned
        nonAssociation("A1234BC", "D5678EG"),
        nonAssociation("X0123AA", "X0123BB"),
        nonAssociation("A1234BC", "G0011AA"), // returned
        nonAssociation("G0022BB", "A1234BC"),
      ),
    )

    val prisonerNumbers = listOf("D5678EF", "G0011AA")
    val nonAssociations = repository.findAllByFirstPrisonerNumberInOrSecondPrisonerNumberIn(
      prisonerNumbers,
      prisonerNumbers,
    )

    assertThat(nonAssociations).hasSize(2)
    assertThat(nonAssociations).allMatch { nonna ->
      // All non-associations involve at least one of the required prisoners
      prisonerNumbers.contains(nonna.firstPrisonerNumber) || prisonerNumbers.contains(nonna.secondPrisonerNumber)
    }
  }

  @Nested
  inner class findAnyBetweenPrisonerNumbers() {
    @Test
    fun openNonAssociations() {
      repository.saveAll(
        listOf(
          nonAssociation("B0000BB", "A0000AA"), // not returned
          nonAssociation("A0000AA", "A1111AA"),
          nonAssociation("A1111AA", "B0000BB"), // not returned
          nonAssociation("A0000AA", "A2222AA", closed = true), // not returned
          nonAssociation("A1111AA", "A2222AA"),
          nonAssociation("B0000BB", "B1111BB"), // not returned
        ),
      )

      val prisonerNumbers = listOf("A0000AA", "A1111AA", "A2222AA")
      val nonAssociations = repository.findAnyBetweenPrisonerNumbers(prisonerNumbers)
      assertThat(nonAssociations).hasSize(2)
      assertThat(nonAssociations).allMatch {
        prisonerNumbers.contains(it.firstPrisonerNumber) && prisonerNumbers.contains(it.secondPrisonerNumber)
      }
      val nonAssociationPairs = nonAssociations.map { listOf(it.firstPrisonerNumber, it.secondPrisonerNumber) }
      assertThat(nonAssociationPairs).isEqualTo(
        listOf(
          listOf("A0000AA", "A1111AA"),
          listOf("A1111AA", "A2222AA"),
        ),
      )
    }

    @Test
    fun closedNonAssociations() {
      repository.saveAll(
        listOf(
          nonAssociation("B0000BB", "A0000AA"), // not returned
          nonAssociation("A0000AA", "A1111AA", closed = true),
          nonAssociation("A1111AA", "B0000BB"), // not returned
          nonAssociation("A0000AA", "A2222AA", closed = true),
          nonAssociation("A1111AA", "A2222AA"), // not returned
          nonAssociation("B0000BB", "B1111BB", closed = true), // not returned
          nonAssociation("A0000AA", "A1111AA"), // not returned
        ),
      )

      val prisonerNumbers = listOf("A0000AA", "A1111AA", "A2222AA")
      val nonAssociations = repository.findAnyBetweenPrisonerNumbers(prisonerNumbers, NonAssociationListInclusion.CLOSED_ONLY)
      assertThat(nonAssociations).hasSize(2)
      assertThat(nonAssociations).allMatch {
        prisonerNumbers.contains(it.firstPrisonerNumber) && prisonerNumbers.contains(it.secondPrisonerNumber)
      }
      val nonAssociationPairs = nonAssociations.map { listOf(it.firstPrisonerNumber, it.secondPrisonerNumber) }
      assertThat(nonAssociationPairs).isEqualTo(
        listOf(
          listOf("A0000AA", "A1111AA"),
          listOf("A0000AA", "A2222AA"),
        ),
      )
    }

    @Test
    fun allNonAssociations() {
      repository.saveAll(
        listOf(
          nonAssociation("B0000BB", "A0000AA"), // not returned
          nonAssociation("A0000AA", "A1111AA", closed = true),
          nonAssociation("A1111AA", "B0000BB"), // not returned
          nonAssociation("A0000AA", "A2222AA", closed = true),
          nonAssociation("A1111AA", "A2222AA"),
          nonAssociation("B0000BB", "B1111BB", closed = true), // not returned
          nonAssociation("A0000AA", "A1111AA"),
        ),
      )

      val prisonerNumbers = listOf("A0000AA", "A1111AA", "A2222AA")
      val nonAssociations = repository.findAnyBetweenPrisonerNumbers(prisonerNumbers, NonAssociationListInclusion.ALL)
      assertThat(nonAssociations).hasSize(4)
      assertThat(nonAssociations).allMatch {
        prisonerNumbers.contains(it.firstPrisonerNumber) && prisonerNumbers.contains(it.secondPrisonerNumber)
      }
      val nonAssociationPairs = nonAssociations.map { listOf(it.firstPrisonerNumber, it.secondPrisonerNumber) }
      assertThat(nonAssociationPairs).isEqualTo(
        listOf(
          listOf("A0000AA", "A1111AA"),
          listOf("A0000AA", "A2222AA"),
          listOf("A1111AA", "A2222AA"),
          listOf("A0000AA", "A1111AA"),
        ),
      )
    }
  }

  @Nested
  inner class findAnyInvolvingPrisonerNumbers() {
    @Test
    fun openNonAssociations() {
      repository.saveAll(
        listOf(
          nonAssociation("B0000BB", "A0000AA"), // returned
          nonAssociation("A0000AA", "A1111AA"), // returned
          nonAssociation("A1111AA", "B0000BB"), // returned
          nonAssociation("A0000AA", "A2222AA", closed = true), // not returned
          nonAssociation("A1111AA", "A2222AA"), // returned
          nonAssociation("B0000BB", "B1111BB"), // not returned
        ),
      )

      val prisonerNumbers = listOf("A0000AA", "A1111AA")
      val nonAssociations = repository.findAnyInvolvingPrisonerNumbers(prisonerNumbers)
      assertThat(nonAssociations).hasSize(4)
      assertThat(nonAssociations).allMatch {
        it.isOpen &&
        (prisonerNumbers.contains(it.firstPrisonerNumber) || prisonerNumbers.contains(it.secondPrisonerNumber))
      }
      val nonAssociationPairs = nonAssociations.map { listOf(it.firstPrisonerNumber, it.secondPrisonerNumber) }
      assertThat(nonAssociationPairs).isEqualTo(
        listOf(
          listOf("B0000BB", "A0000AA"),
          listOf("A0000AA", "A1111AA"),
          listOf("A1111AA", "B0000BB"),
          listOf("A1111AA", "A2222AA"),

        ),
      )
    }

    @Test
    fun closedNonAssociations() {
      repository.saveAll(
        listOf(
          nonAssociation("B0000BB", "A0000AA"), // not returned
          nonAssociation("A0000AA", "A1111AA", closed = true),
          nonAssociation("A1111AA", "B0000BB"), // not returned
          nonAssociation("A0000AA", "A2222AA", closed = true),
          nonAssociation("A1111AA", "A2222AA"), // not returned
          nonAssociation("B0000BB", "B1111BB", closed = true),
          nonAssociation("A0000AA", "A1111AA"), // not returned
          nonAssociation("B0000BB", "C3333CC", closed = true), // not returned
        ),
      )

      val prisonerNumbers = listOf("A0000AA", "B1111BB")
      val nonAssociations = repository.findAnyInvolvingPrisonerNumbers(prisonerNumbers, NonAssociationListInclusion.CLOSED_ONLY)
      assertThat(nonAssociations).hasSize(3)
      assertThat(nonAssociations).allMatch {
        it.isClosed &&
        (prisonerNumbers.contains(it.firstPrisonerNumber) || prisonerNumbers.contains(it.secondPrisonerNumber))
      }
      val nonAssociationPairs = nonAssociations.map { listOf(it.firstPrisonerNumber, it.secondPrisonerNumber) }
      assertThat(nonAssociationPairs).isEqualTo(
        listOf(
          listOf("A0000AA", "A1111AA"),
          listOf("A0000AA", "A2222AA"),
          listOf("B0000BB", "B1111BB"),
        ),
      )
    }

    @Test
    fun allNonAssociations() {
      repository.saveAll(
        listOf(
          nonAssociation("B0000BB", "A0000AA"),
          nonAssociation("A0000AA", "A1111AA", closed = true),
          nonAssociation("A1111AA", "B0000BB"), // not returned
          nonAssociation("A0000AA", "A2222AA", closed = true),
          nonAssociation("A1111AA", "C3333CC"),
          nonAssociation("B0000BB", "B1111BB", closed = true), // not returned
          nonAssociation("D4444DD", "A1111AA"),
        ),
      )

      val prisonerNumbers = listOf("A0000AA", "C3333CC", "D4444DD")
      val nonAssociations = repository.findAnyInvolvingPrisonerNumbers(prisonerNumbers, NonAssociationListInclusion.ALL)
      assertThat(nonAssociations).hasSize(5)
      assertThat(nonAssociations).allMatch {
        prisonerNumbers.contains(it.firstPrisonerNumber) || prisonerNumbers.contains(it.secondPrisonerNumber)
      }
      val nonAssociationPairs = nonAssociations.map { listOf(it.firstPrisonerNumber, it.secondPrisonerNumber) }
      assertThat(nonAssociationPairs).isEqualTo(
        listOf(
          listOf("B0000BB", "A0000AA"),
          listOf("A0000AA", "A1111AA"),
          listOf("A0000AA", "A2222AA"),
          listOf("A1111AA", "C3333CC"),
          listOf("D4444DD", "A1111AA"),
        ),
      )
    }
  }

  @Test
  fun createNonAssociation() {
    val nonna = nonAssociation(firstPrisonerNumber = "A1234BC", secondPrisonerNumber = "D5678EF")
    var savedNonna = repository.save(nonna)

    savedNonna = savedNonna.id?.let {
      repository.findById(it).orElseThrow {
        Exception("NonAssociation with id=${savedNonna.id} couldn't be found")
      }
    }!!

    assertThat(savedNonna.firstPrisonerNumber).isEqualTo(nonna.firstPrisonerNumber)
    assertThat(savedNonna.firstPrisonerRole).isEqualTo(nonna.firstPrisonerRole)
    assertThat(savedNonna.firstPrisonerRole.description).isEqualTo("Victim")
    assertThat(savedNonna.secondPrisonerNumber).isEqualTo(nonna.secondPrisonerNumber)
    assertThat(savedNonna.secondPrisonerRole).isEqualTo(nonna.secondPrisonerRole)
    assertThat(savedNonna.secondPrisonerRole.description).isEqualTo("Perpetrator")
    assertThat(savedNonna.reason).isEqualTo(nonna.reason)
    assertThat(savedNonna.reason.description).isEqualTo("Bullying")
    assertThat(savedNonna.restrictionType).isEqualTo(nonna.restrictionType)
    assertThat(savedNonna.restrictionType.description).isEqualTo("Cell only")
    assertThat(savedNonna.comment).isEqualTo(nonna.comment)

    // By default non-associations are open
    assertThat(savedNonna.isClosed).isFalse
    assertThat(savedNonna.closedBy).isNull()
    assertThat(savedNonna.closedAt).isNull()
    assertThat(savedNonna.closedReason).isNull()

    assertThat(savedNonna.whenCreated).isEqualTo(savedNonna.whenUpdated)
  }

  @Test
  fun closeNonAssociationWithDetailsSucceeds() {
    val createdNonna = repository.save(
      nonAssociation(firstPrisonerNumber = "A1234BC", secondPrisonerNumber = "D5678EF"),
    )

    // Update the non-association to close it

    val closedBy = "Aldo"
    val closedReason = "They're friends now"
    val closedAt = LocalDateTime.now(clock)
    createdNonna.close(closedBy, closedReason, closedAt)
    repository.save(createdNonna)

    // Check non-association is now closed with correct details
    val updatedNonna = createdNonna.id?.let { repository.findById(it).get() }
    assertThat(updatedNonna?.isClosed).isTrue
    assertThat(updatedNonna?.closedBy).isEqualTo(closedBy)
    assertThat(updatedNonna?.closedAt).isEqualTo(closedAt)
    assertThat(updatedNonna?.closedReason).isEqualTo(closedReason)
  }

  @Test
  fun closeNonAssociationWithoutDetailsFails() {
    var createdNonna = repository.save(
      nonAssociation(firstPrisonerNumber = "A1234BC", secondPrisonerNumber = "D5678EG"),
    )
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    createdNonna = repository.findById(createdNonna.id!!).orElseThrow {
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
    val freshNonna = createdNonna.id?.let { repository.findById(it).get() }
    assertThat(freshNonna?.isClosed).isFalse
  }

  @Test
  fun whenUpdatedIsUpdated() {
    val created = repository.save(
      nonAssociation(firstPrisonerNumber = "A1234BC", secondPrisonerNumber = "D5678EF"),
    )
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    created.comment = "John attacked Bob after being provoked"
    val updated = repository.save(created)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    assertThat(updated.comment).isEqualTo("John attacked Bob after being provoked")
    assertThat(updated.whenCreated).isEqualTo(created.whenCreated)
    assertThat(updated.whenUpdated).isAfter(updated.whenCreated)
  }

  private fun nonAssociation(firstPrisonerNumber: String, secondPrisonerNumber: String, closed: Boolean = false): NonAssociation {
    return NonAssociation(
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerRole = Role.VICTIM,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerRole = Role.PERPETRATOR,
      reason = Reason.BULLYING,
      restrictionType = RestrictionType.CELL,
      comment = "John attacked Bob",
      authorisedBy = "A_USER",
      updatedBy = "A_USER",
      isClosed = closed,
      closedAt = if (closed) { LocalDateTime.now() } else { null },
      closedBy = if (closed) { "A USER" } else { null },
      closedReason = if (closed) { "Problems resolved" } else { null },
    )
  }
}
