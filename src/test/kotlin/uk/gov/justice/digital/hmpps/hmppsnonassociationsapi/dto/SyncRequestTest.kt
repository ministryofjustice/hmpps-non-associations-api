package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.helper.TestBase
import java.time.LocalDate

class SyncRequestTest {

  @Test
  fun testOpenNonAssociations() {
    val na = UpsertSyncRequest(
      firstPrisonerNumber = "C7777XX",
      firstPrisonerReason = LegacyReason.VIC,
      secondPrisonerNumber = "D7777XX",
      secondPrisonerReason = LegacyReason.PER,
      restrictionType = LegacyRestrictionType.CELL,
      comment = "They keep fighting",
      authorisedBy = "Me",
      effectiveFromDate = LocalDate.now(TestBase.clock).minusDays(1),
    )

    assertThat(na.isOpen(TestBase.clock)).isTrue()
  }

  @Test
  fun testClosedNonAssociations() {
    val na = UpsertSyncRequest(
      firstPrisonerNumber = "C7777XX",
      firstPrisonerReason = LegacyReason.VIC,
      secondPrisonerNumber = "D7777XX",
      secondPrisonerReason = LegacyReason.PER,
      restrictionType = LegacyRestrictionType.CELL,
      comment = "They keep fighting",
      authorisedBy = "Me",
      effectiveFromDate = LocalDate.now(TestBase.clock).minusDays(1),
      expiryDate = LocalDate.now(TestBase.clock),
    )

    assertThat(na.isClosed(TestBase.clock)).isTrue()
  }

  @Test
  fun testClosedNonAssociationsInFuture() {
    val na = UpsertSyncRequest(
      firstPrisonerNumber = "C7777XX",
      firstPrisonerReason = LegacyReason.VIC,
      secondPrisonerNumber = "D7777XX",
      secondPrisonerReason = LegacyReason.PER,
      restrictionType = LegacyRestrictionType.CELL,
      comment = "They keep fighting",
      authorisedBy = "Me",
      effectiveFromDate = LocalDate.now(TestBase.clock).minusDays(1),
      expiryDate = LocalDate.now(TestBase.clock).plusDays(2),
    )

    assertThat(na.isOpen(TestBase.clock)).isTrue()
  }

  @Test
  fun testClosedNonAssociationsInPast() {
    val na = UpsertSyncRequest(
      firstPrisonerNumber = "C7777XX",
      firstPrisonerReason = LegacyReason.VIC,
      secondPrisonerNumber = "D7777XX",
      secondPrisonerReason = LegacyReason.PER,
      restrictionType = LegacyRestrictionType.CELL,
      comment = "They keep fighting",
      authorisedBy = "Me",
      effectiveFromDate = LocalDate.now(TestBase.clock).plusDays(1),
      expiryDate = LocalDate.now(TestBase.clock).plusDays(2),
    )

    assertThat(na.isClosed(TestBase.clock)).isTrue()
  }

  @Test
  fun testOpenNonAssociationsEffectiveToday() {
    val na = UpsertSyncRequest(
      firstPrisonerNumber = "C7777XX",
      firstPrisonerReason = LegacyReason.VIC,
      secondPrisonerNumber = "D7777XX",
      secondPrisonerReason = LegacyReason.PER,
      restrictionType = LegacyRestrictionType.CELL,
      comment = "They keep fighting",
      authorisedBy = "Me",
      effectiveFromDate = LocalDate.now(TestBase.clock),
    )

    assertThat(na.isOpen(TestBase.clock)).isTrue()
  }

  @Test
  fun testClosedNonAssociationsExpiryInPast() {
    val na = UpsertSyncRequest(
      firstPrisonerNumber = "C7777XX",
      firstPrisonerReason = LegacyReason.VIC,
      secondPrisonerNumber = "D7777XX",
      secondPrisonerReason = LegacyReason.PER,
      restrictionType = LegacyRestrictionType.CELL,
      comment = "They keep fighting",
      authorisedBy = "Me",
      effectiveFromDate = LocalDate.now(TestBase.clock).minusDays(2),
      expiryDate = LocalDate.now(TestBase.clock).minusDays(1),
    )

    assertThat(na.isClosed(TestBase.clock)).isTrue()
  }
}
