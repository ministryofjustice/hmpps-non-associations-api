package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.sar

/**
 * The subject access request response: one fully sanitised entry per non-association in the date range.
 *
 * Every field except [SarRedactedNonAssociation.number] holds [SarRedactedNonAssociation.REDACTED], as agreed with
 * the Offender SAR team (IR-2043). The field names follow the headings in the report so that the template stays a
 * plain list of values and needs no helpers, which could not format a redacted value anyway.
 */
data class SarNonAssociations(
  val nonAssociations: List<SarRedactedNonAssociation>,
)

data class SarRedactedNonAssociation(
  /** Position in the report (1, 2, 3...), so the entries can be told apart without showing an internal id */
  val number: Int,
  val role: String = REDACTED,
  val reason: String = REDACTED,
  val restrictionType: String = REDACTED,
  val comment: String = REDACTED,
  val authorisedBy: String = REDACTED,
  val whenCreated: String = REDACTED,
  val updatedBy: String = REDACTED,
  val whenUpdated: String = REDACTED,
  val isClosed: String = REDACTED,
  val closedBy: String = REDACTED,
  val closedReason: String = REDACTED,
  val closedAt: String = REDACTED,
  val otherPrisonerNumber: String = REDACTED,
  val otherPrisonerName: String = REDACTED,
  val otherPrisonerRole: String = REDACTED,
) {
  companion object {
    const val REDACTED = "XXXXXX"
  }
}
