package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.sar

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Reason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.RestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Role
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.prisonerSearchPrisoners
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import java.time.LocalDateTime
import javax.sql.DataSource

/**
 * The four checks from the HMPPS SAR test library, against a fixed set of non-association fixtures.
 *
 * These are approval tests: each compares against a committed file under `src/test/resources/sar/`, and the
 * point of them is what happens when one fails.
 *
 *  - `SarApiDataTest` / `SarReportTest` fail when the response or the rendered report changes. The rendered
 *    HTML is what the Offender SAR team review and sign off, so a diff here is the trigger for a conversation
 *    with them through the SAR change control process, not something to regenerate and move on from.
 *    `SarReportTest` renders the template served by `GET /subject-access-request/template`, so it also proves
 *    the endpoint is switched on and serving the right file.
 *  - `SarJpaEntitiesTest` fails when a column is added to the entity, and `SarFlywaySchemaTest` when a
 *    migration is added. Both force whoever makes the change to decide whether it belongs in a prisoner's
 *    report, rather than the field quietly never appearing.
 *
 * The approved report was first generated from V1 of the template, copied unchanged from the HAA team's central
 * repository (IR-2038), so it is the report the SAR tool was already producing for this data.
 *
 * Regenerate the files by running with `SAR_GENERATE_ACTUAL=true`, which writes `*.log` files under
 * `src/test/resources/`, then read them before copying them over the approved ones.
 */
@Import(SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestLibraryTest :
  SqsIntegrationTestBase(),
  SarApiDataTest,
  SarFlywaySchemaTest,
  SarJpaEntitiesTest,
  SarReportTest {

  @Autowired
  private lateinit var dataSource: DataSource

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  override fun getDataSourceInstance(): DataSource = dataSource

  override fun getEntityManagerInstance(): EntityManager = entityManager

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  override fun getPrn(): String = PRISONER

  /**
   * Deserialise the response as plain JSON rather than into the response DTO. That is what the SAR tool itself
   * does - it fetches the JSON and renders the template against the parsed result - so the template helpers see
   * the same strings they see in production. The response is a single object rather than a list.
   */
  override fun getContentType(): Class<*> = Map::class.java

  /**
   * Two non-associations for the same prisoner, which between them exercise every field the template reads:
   * one open, with the prisoner recorded first and an authoriser; one closed, with the prisoner recorded second.
   *
   * The report prints each non-association's database id, so the sequence is restarted to keep the approved
   * files stable whatever ran before. Dates are fixed for the same reason. The service looks the prisoners up in
   * prisoner-search, so that is stubbed too.
   *
   * Idempotent, because the library calls this itself and the rows must not stack up.
   */
  override fun setupTestData() {
    repository.deleteAll()
    jdbcTemplate.execute("ALTER SEQUENCE non_association_id_seq RESTART WITH 101")

    repository.save(
      NonAssociation(
        firstPrisonerNumber = PRISONER,
        firstPrisonerRole = Role.VICTIM,
        secondPrisonerNumber = OPEN_OTHER,
        secondPrisonerRole = Role.PERPETRATOR,
        reason = Reason.THREAT,
        restrictionType = RestrictionType.LANDING,
        comment = "Threats made on the landing",
        authorisedBy = "AUTH_USER",
        whenCreated = LocalDateTime.parse("2023-05-01T10:00:00"),
        whenUpdated = LocalDateTime.parse("2023-05-02T11:30:00"),
        updatedBy = "UPDATE_USER",
      ),
    )
    repository.save(
      NonAssociation(
        firstPrisonerNumber = CLOSED_OTHER,
        firstPrisonerRole = Role.NOT_RELEVANT,
        secondPrisonerNumber = PRISONER,
        secondPrisonerRole = Role.UNKNOWN,
        reason = Reason.GANG_RELATED,
        restrictionType = RestrictionType.WING,
        comment = "Rival groups",
        whenCreated = LocalDateTime.parse("2023-01-10T09:15:00"),
        whenUpdated = LocalDateTime.parse("2023-03-20T14:45:00"),
        updatedBy = "CLOSE_USER",
        isClosed = true,
        closedBy = "CLOSE_USER",
        closedReason = "Other prisoner released",
        closedAt = LocalDateTime.parse("2023-03-20T14:45:00"),
      ),
    )

    val prisonerNumbers = listOf(PRISONER, OPEN_OTHER, CLOSED_OTHER)
    prisonerSearchMockServer.stubSearchByPrisonerNumbers(
      prisonerNumbers,
      prisonerNumbers.map { prisonerSearchPrisoners[it]!! },
    )
  }

  private companion object {
    const val PRISONER = "A1234BC"
    const val OPEN_OTHER = "D5678EF"
    const val CLOSED_OTHER = "G9012HI"
  }
}
