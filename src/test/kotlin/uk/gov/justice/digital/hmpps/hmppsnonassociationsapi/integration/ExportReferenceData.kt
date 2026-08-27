package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Reason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.RestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Role
import java.io.File

/**
 * Writes reference-data.csv, the companion to the SchemaSpy report and data-dictionary.csv.
 *
 * Every code in this schema is a JPA string enum resolved in Kotlin - there are no reference tables in
 * the database - so the schema report alone leaves an analyst looking at a varchar(20) with no idea
 * which values are legal.
 *
 * Only codes actually stored in the schema are exported. LegacyReason and LegacyRestrictionType are
 * deliberately absent: they are NOMIS codes translated at the API boundary and never persisted, so
 * including them would imply values that cannot appear in the data.
 *
 * Needs no database: the values come from the enums themselves, so the list cannot drift from the code.
 * A new enum value with no description fails the test rather than exporting a blank row.
 *
 * Excluded from normal test runs; run with `./gradlew -Pinit-db=true test` (see build.gradle.kts).
 */
class ExportReferenceData {

  @Test
  fun `exports reference data`() {
    val rows = mutableListOf<Row>()

    rows += enumRows(
      "non_association.first_prisoner_role / non_association.second_prisoner_role",
      Role.entries,
      mapOf(
        Role.VICTIM to "The prisoner was the subject of the behaviour the non-association was recorded for.",
        Role.PERPETRATOR to
          "The prisoner is recorded as responsible for the behaviour the non-association was recorded for. " +
          "An allegation recorded by staff, not a finding or a conviction.",
        Role.NOT_RELEVANT to
          "Neither victim nor perpetrator - the two must be kept apart without either being held " +
          "responsible, for example a police request.",
        Role.UNKNOWN to "Not recorded. Common on records migrated from NOMIS, which did not always capture a role.",
      ),
    )

    rows += enumRows(
      "non_association.reason",
      Reason.entries,
      mapOf(
        Reason.BULLYING to "One prisoner has bullied the other.",
        Reason.GANG_RELATED to "The two prisoners are associated with rival or related gangs.",
        Reason.ORGANISED_CRIME to "The two prisoners are linked through organised crime.",
        Reason.LEGAL_REQUEST to
          "The police or a legal body has asked for the two to be kept apart, for example " +
          "co-defendants awaiting trial.",
        Reason.THREAT to "One prisoner has threatened the other.",
        Reason.VIOLENCE to "There has been violence between the two prisoners.",
        Reason.OTHER to "None of the above - the explanation is in the free-text comment.",
      ),
    )

    rows += enumRows(
      "non_association.restriction_type",
      RestrictionType.entries,
      mapOf(
        RestrictionType.CELL to "Cell only - the two must not share a cell.",
        RestrictionType.LANDING to "Cell and landing - the two must not share a cell or a landing.",
        RestrictionType.WING to
          "Cell, landing and wing - the two must not share a cell, a landing or a wing. " +
          "The most restrictive.",
      ),
      notes = { "increasing severity: CELL < LANDING < WING" },
    )

    val output = File(System.getProperty("referenceDataOutput") ?: "reference-data.csv")
    output.bufferedWriter().use { writer ->
      writer.write("column_ref,code,description,notes\n")
      rows.forEach { writer.write("${it.toCsv()}\n") }
    }
    println("Wrote ${rows.size} reference data rows to ${output.absolutePath}")
  }

  /**
   * Every value of the enum, with its description. Fails rather than exporting a blank row when a value
   * has no description - a new enum value is exactly the thing a consumer would otherwise not be able to
   * decode.
   */
  private fun <T : Enum<T>> enumRows(
    columnRef: String,
    values: List<T>,
    descriptions: Map<T, String>,
    notes: (T) -> String = { "" },
  ): List<Row> {
    assertThat(values.filterNot(descriptions::containsKey))
      .describedAs("$columnRef values with no description - add one in ExportReferenceData")
      .isEmpty()

    return values.map { Row(columnRef, it.name, descriptions.getValue(it), notes(it)) }
  }

  private data class Row(
    val columnRef: String,
    val code: String,
    val description: String,
    val notes: String = "",
  ) {
    fun toCsv() = listOf(columnRef, code, description, notes).joinToString(",") { escape(it) }

    private fun escape(value: String) = "\"${value.replace("\"", "\"\"")}\""
  }
}
