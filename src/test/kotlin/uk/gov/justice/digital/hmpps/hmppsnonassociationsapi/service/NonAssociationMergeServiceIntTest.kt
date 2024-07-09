package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.transaction.TestTransaction
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ApplicationInsightsConfiguration
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ClockConfiguration
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAnyBetweenPrisonerNumbers
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.genNonAssociation
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuthenticationFacade::class, NonAssociationsMergeService::class, ClockConfiguration::class, ApplicationInsightsConfiguration::class)
@WithMockAuthUser(username = "A_DPS_USER")
@DataJpaTest
class NonAssociationMergeServiceIntTest : TestBase() {

  @Autowired
  lateinit var service: NonAssociationsMergeService

  @Autowired
  lateinit var repository: NonAssociationsRepository

  @Test
  fun testMerge() {
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "A1234AA",
        secondPrisonerNumber = "X1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "X1234AB",
        secondPrisonerNumber = "A1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "B1234AA",
        secondPrisonerNumber = "X1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "X1234AB",
        secondPrisonerNumber = "B1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "A1234AA",
        secondPrisonerNumber = "B1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "A1234AA",
        secondPrisonerNumber = "C1234FF",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "D1234RR",
        secondPrisonerNumber = "A1234AA",
      ),
    )
    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "B1234AA",
        secondPrisonerNumber = "A1234AA",
      ),
    )
    assertThat(repository.findAllByPrisonerNumber("A1234AA")).hasSize(6)
    assertThat(repository.findAllByPrisonerNumber("B1234AA")).hasSize(4)

    assertThat(repository.findAnyBetweenPrisonerNumbers(listOf("X1234AA", "A1234AA"), NonAssociationListInclusion.CLOSED_ONLY)).hasSize(0)
    assertThat(repository.findAnyBetweenPrisonerNumbers(listOf("X1234AA", "B1234AA"), NonAssociationListInclusion.CLOSED_ONLY)).hasSize(0)

    val mergedAssociations = service.mergePrisonerNumbers("A1234AA", "B1234AA")

    assertThat(mergedAssociations[MergeResult.CLOSED]).hasSize(2)
    assertThat(mergedAssociations[MergeResult.MERGED]).hasSize(2)
    assertThat(mergedAssociations[MergeResult.DELETED]).hasSize(2)

    assertThat(mergedAssociations[MergeResult.MERGED]).isEqualTo(
      listOf(
        genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "C1234FF"),
        genNonAssociation(firstPrisonerNumber = "D1234RR", secondPrisonerNumber = "B1234AA"),
      ),
    )

    assertThat(mergedAssociations[MergeResult.CLOSED]).isEqualTo(
      listOf(
        genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "X1234AA"),
        genNonAssociation(firstPrisonerNumber = "X1234AB", secondPrisonerNumber = "B1234AA"),
      ),
    )

    assertThat(mergedAssociations[MergeResult.DELETED]).isEqualTo(
      listOf(
        genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "B1234AA"),
        genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "B1234AA"),
      ),
    )

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    assertThat(repository.findAllByPrisonerNumber("B1234AA")).hasSize(6)
    assertThat(repository.findAllByPrisonerNumber("A1234AA")).hasSize(0)

    assertThat(repository.findAnyBetweenPrisonerNumbers(listOf("X1234AA", "B1234AA"), NonAssociationListInclusion.CLOSED_ONLY)).hasSize(1)

    repository.deleteAll(mergedAssociations.values.flatten())
  }
}
