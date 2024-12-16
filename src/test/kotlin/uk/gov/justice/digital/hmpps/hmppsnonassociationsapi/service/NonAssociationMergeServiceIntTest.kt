package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.transaction.TestTransaction
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ApplicationInsightsConfiguration
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ClockConfiguration
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAnyBetweenPrisonerNumbers
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.genNonAssociation
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.Clock
import java.time.ZoneId

@DisplayName("Non-associations merge service, integration tests")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JacksonAutoConfiguration::class, HmppsAuthenticationHolder::class, NonAssociationsMergeService::class, ClockConfiguration::class, ApplicationInsightsConfiguration::class)
@WithMockAuthUser(username = "A_DPS_USER")
@DataJpaTest
class NonAssociationMergeServiceIntTest : TestBase() {
  @TestConfiguration
  class FixedClockConfig {
    @Primary
    @Bean
    fun timeZoneId(): ZoneId = zoneId

    @Primary
    @Bean
    fun fixedClock(): Clock = clock
  }

  @Autowired
  lateinit var mapper: ObjectMapper

  @Autowired
  lateinit var service: NonAssociationsMergeService

  @Autowired
  lateinit var repository: NonAssociationsRepository

  @BeforeEach
  fun setUp() {
    repository.deleteAll()
  }

  @Test
  fun `can parse prisoner merge domain event`() {
    val eventListener = PrisonOffenderEventListener(
      mapper = mapper,
      nonAssociationsMergeService = service,
      zoneId = zoneId,
    )

    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "A7777BB",
        secondPrisonerNumber = "B8888CC",
      ),
    )

    // merge A7777BB into B8888CC
    eventListener.onPrisonOffenderEvent("/messages/prison-offender-events.prisoner.merged.json".readResourceAsText())

    // expect deletion
    assertThat(repository.findAllByPrisonerNumber("A7777BB")).isEmpty()
    assertThat(repository.findAllByPrisonerNumber("B8888CC")).isEmpty()
  }

  @Test
  fun `can parse booking moved domain event`() {
    val eventListener = PrisonOffenderEventListener(
      mapper = mapper,
      nonAssociationsMergeService = service,
      zoneId = zoneId,
    )

    repository.save(
      genNonAssociation(
        firstPrisonerNumber = "A7777BB",
        secondPrisonerNumber = "B8888CC",
      ),
    )

    // moved booking (with start date of 2 days ago) from A7777BB to B8888CC
    eventListener.onPrisonOffenderEvent("/messages/prison-offender-events.prisoner.booking.moved.json".readResourceAsText())

    // expect deletion
    assertThat(repository.findAllByPrisonerNumber("A7777BB")).isEmpty()
    assertThat(repository.findAllByPrisonerNumber("B8888CC")).isEmpty()
  }

  private fun setupManyNonAssociations() {
    repository.saveAll(
      listOf(
        // closed
        genNonAssociation(
          firstPrisonerNumber = "A1234AA",
          secondPrisonerNumber = "X1234AA",
          createTime = now.minusDays(5),
        ),
        // closed
        genNonAssociation(
          firstPrisonerNumber = "X1234AB",
          secondPrisonerNumber = "A1234AA",
          createTime = now.minusDays(4),
        ),
        // ignored
        genNonAssociation(
          firstPrisonerNumber = "B1234AA",
          secondPrisonerNumber = "X1234AA",
          createTime = now.minusDays(3),
        ),
        // ignored
        genNonAssociation(
          firstPrisonerNumber = "X1234AB",
          secondPrisonerNumber = "B1234AA",
          createTime = now.minusDays(2),
        ),
        // deleted
        genNonAssociation(
          firstPrisonerNumber = "A1234AA",
          secondPrisonerNumber = "B1234AA",
          createTime = now.minusDays(1),
        ),
        // merged
        genNonAssociation(
          firstPrisonerNumber = "A1234AA",
          secondPrisonerNumber = "C1234FF",
          createTime = now,
        ),
        // merged
        genNonAssociation(
          firstPrisonerNumber = "D1234RR",
          secondPrisonerNumber = "A1234AA",
          createTime = now.plusDays(1),
        ),
        // deleted
        genNonAssociation(
          firstPrisonerNumber = "B1234AA",
          secondPrisonerNumber = "A1234AA",
          createTime = now.plusDays(2),
        ),
      ),
    )
  }

  @Test
  fun `can merge prisoner numbers`() {
    setupManyNonAssociations()

    assertThat(repository.findAllByPrisonerNumber("A1234AA")).hasSize(6)
    assertThat(repository.findAllByPrisonerNumber("B1234AA")).hasSize(4)

    assertThat(repository.findAnyBetweenPrisonerNumbers(listOf("X1234AA", "A1234AA"), NonAssociationListInclusion.CLOSED_ONLY)).isEmpty()
    assertThat(repository.findAnyBetweenPrisonerNumbers(listOf("X1234AA", "B1234AA"), NonAssociationListInclusion.CLOSED_ONLY)).isEmpty()

    val nonAssociationMap = service.replacePrisonerNumber("A1234AA", "B1234AA")

    assertThat(nonAssociationMap).isEqualTo(
      mapOf(
        MergeResult.MERGED to listOf(
          genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "C1234FF"),
          genNonAssociation(firstPrisonerNumber = "D1234RR", secondPrisonerNumber = "B1234AA"),
        ),
        MergeResult.CLOSED to listOf(
          genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "X1234AA"),
          genNonAssociation(firstPrisonerNumber = "X1234AB", secondPrisonerNumber = "B1234AA"),
        ),
        MergeResult.DELETED to listOf(
          genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "B1234AA"),
          genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "B1234AA"),
        ),
      ),
    )

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    assertThat(repository.findAllByPrisonerNumber("B1234AA")).hasSize(6)
    assertThat(repository.findAllByPrisonerNumber("A1234AA")).isEmpty()

    assertThat(repository.findAnyBetweenPrisonerNumbers(listOf("X1234AA", "B1234AA"), NonAssociationListInclusion.CLOSED_ONLY)).hasSize(1)
  }

  @Test
  fun `can move a booking between prisoner numbers`() {
    setupManyNonAssociations()

    val nonAssociationMap = service.replacePrisonerNumberInDateRange(
      oldPrisonerNumber = "A1234AA",
      newPrisonerNumber = "B1234AA",
      since = now.minusDays(10),
      until = now.plusDays(2),
    )

    assertThat(nonAssociationMap).isEqualTo(
      mapOf(
        MergeResult.MERGED to listOf(
          genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "C1234FF"),
          genNonAssociation(firstPrisonerNumber = "D1234RR", secondPrisonerNumber = "B1234AA"),
        ),
        MergeResult.CLOSED to listOf(
          genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "X1234AA"),
          genNonAssociation(firstPrisonerNumber = "X1234AB", secondPrisonerNumber = "B1234AA"),
        ),
        MergeResult.DELETED to listOf(
          genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "B1234AA"),
          genNonAssociation(firstPrisonerNumber = "B1234AA", secondPrisonerNumber = "B1234AA"),
        ),
      ),
    )
  }

  @ParameterizedTest(name = "can move a booking between prisoner numbers in date range (since {0} days, until {1} days)")
  @CsvSource(
    value = [
      "   |    | 2 | 2 | 2",
      "-5 | 0  | 1 | 2 | 1",
      "-1 | 3  | 2 | 0 | 2",
      "1  |    | 1 | 0 | 1",
      "   | -1 | 0 | 2 | 1",
    ],
    delimiter = '|',
  )
  fun `can move a booking between prisoner numbers in date range`(sinceDays: Long?, untilDays: Long?, mergeCount: Int, closedCount: Int, deletedCount: Int) {
    setupManyNonAssociations()

    val nonAssociationMap = service.replacePrisonerNumberInDateRange(
      oldPrisonerNumber = "A1234AA",
      newPrisonerNumber = "B1234AA",
      since = sinceDays?.let { now.plusDays(it) },
      until = untilDays?.let { now.plusDays(it) },
    )

    val resultCounts = nonAssociationMap
      .mapValues { (_, nonAssociations) -> nonAssociations.size }
    assertThat(resultCounts.getOrDefault(MergeResult.MERGED, 0)).isEqualTo(mergeCount)
    assertThat(resultCounts.getOrDefault(MergeResult.CLOSED, 0)).isEqualTo(closedCount)
    assertThat(resultCounts.getOrDefault(MergeResult.DELETED, 0)).isEqualTo(deletedCount)
  }
}
