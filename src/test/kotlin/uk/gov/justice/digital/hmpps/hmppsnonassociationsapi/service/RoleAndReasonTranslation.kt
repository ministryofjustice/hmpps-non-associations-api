package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Reason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Role
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.translateFromRolesAndReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.translateToRolesAndReason

class RoleAndReasonTranslation {
  @Test
  fun `translating modern roles and a reason into legacy reasons`() {
    val allButBullyingAndGangRelatedReasons = Reason.entries.filter { it != Reason.BULLYING && it != Reason.GANG_RELATED }

    fun assertThatModernRolesForAllButBullyingAndGangRelatedReasons(firstPrisonerRole: Role, secondPrisonerRole: Role) = object {
      fun translateInto(firstPrisonerReason: LegacyReason, secondPrisonerReason: LegacyReason) {
        allButBullyingAndGangRelatedReasons.forEach { reason ->
          assertThat(translateFromRolesAndReason(firstPrisonerRole, secondPrisonerRole, reason)).isEqualTo(Pair(firstPrisonerReason, secondPrisonerReason))
          assertThat(translateFromRolesAndReason(secondPrisonerRole, firstPrisonerRole, reason)).isEqualTo(Pair(secondPrisonerReason, firstPrisonerReason))
        }
      }
    }

    assertThatModernRolesForAllButBullyingAndGangRelatedReasons(Role.VICTIM, Role.VICTIM).translateInto(LegacyReason.VIC, LegacyReason.VIC)
    assertThatModernRolesForAllButBullyingAndGangRelatedReasons(Role.VICTIM, Role.PERPETRATOR).translateInto(LegacyReason.VIC, LegacyReason.PER)
    assertThatModernRolesForAllButBullyingAndGangRelatedReasons(Role.VICTIM, Role.NOT_RELEVANT).translateInto(LegacyReason.VIC, LegacyReason.NOT_REL)
    assertThatModernRolesForAllButBullyingAndGangRelatedReasons(Role.VICTIM, Role.UNKNOWN).translateInto(LegacyReason.VIC, LegacyReason.UNKNOWN)

    assertThatModernRolesForAllButBullyingAndGangRelatedReasons(Role.PERPETRATOR, Role.PERPETRATOR).translateInto(LegacyReason.PER, LegacyReason.PER)
    assertThatModernRolesForAllButBullyingAndGangRelatedReasons(Role.PERPETRATOR, Role.NOT_RELEVANT).translateInto(LegacyReason.PER, LegacyReason.NOT_REL)
    assertThatModernRolesForAllButBullyingAndGangRelatedReasons(Role.PERPETRATOR, Role.UNKNOWN).translateInto(LegacyReason.PER, LegacyReason.UNKNOWN)

    assertThatModernRolesForAllButBullyingAndGangRelatedReasons(Role.NOT_RELEVANT, Role.NOT_RELEVANT).translateInto(LegacyReason.NOT_REL, LegacyReason.NOT_REL)
    assertThatModernRolesForAllButBullyingAndGangRelatedReasons(Role.NOT_RELEVANT, Role.UNKNOWN).translateInto(LegacyReason.NOT_REL, LegacyReason.UNKNOWN)

    assertThatModernRolesForAllButBullyingAndGangRelatedReasons(Role.UNKNOWN, Role.UNKNOWN).translateInto(LegacyReason.UNKNOWN, LegacyReason.UNKNOWN)

    fun assertThatModernReasonIrrespectiveOfRoles(reason: Reason) = object {
      fun translatesInto(legacyReason: LegacyReason) {
        val expected = Pair(legacyReason, legacyReason)
        Role.entries.forEach { firstPrisonerRole ->
          Role.entries.forEach { secondPrisonerRole ->
            assertThat(translateFromRolesAndReason(firstPrisonerRole, secondPrisonerRole, reason)).isEqualTo(expected)
            assertThat(translateFromRolesAndReason(secondPrisonerRole, firstPrisonerRole, reason)).isEqualTo(expected)
          }
        }
      }
    }

    assertThatModernReasonIrrespectiveOfRoles(Reason.BULLYING).translatesInto(LegacyReason.BUL)
    assertThatModernReasonIrrespectiveOfRoles(Reason.GANG_RELATED).translatesInto(LegacyReason.RIV)
  }

  @Test
  fun `round-trip translations`() {
    fun assertThatLegacyReasons(firstPrisonerReason: LegacyReason, secondPrisonerReason: LegacyReason) = object {
      fun translateBackToThemselves() {
        val (firstPrisonerRole, secondPrisonerRole, reason) = translateToRolesAndReason(firstPrisonerReason, secondPrisonerReason)
        val result = translateFromRolesAndReason(firstPrisonerRole, secondPrisonerRole, reason)
        assertThat(result).isEqualTo(firstPrisonerReason to secondPrisonerReason)
      }
    }

    assertThatLegacyReasons(LegacyReason.RIV, LegacyReason.RIV).translateBackToThemselves()
    assertThatLegacyReasons(LegacyReason.BUL, LegacyReason.BUL).translateBackToThemselves()
    // NB: RIV and BUL will not round-trip translate with any other legacy reason without loss

    assertThatLegacyReasons(LegacyReason.PER, LegacyReason.PER).translateBackToThemselves()
    assertThatLegacyReasons(LegacyReason.PER, LegacyReason.VIC).translateBackToThemselves()
    assertThatLegacyReasons(LegacyReason.PER, LegacyReason.NOT_REL).translateBackToThemselves()
    assertThatLegacyReasons(LegacyReason.PER, LegacyReason.UNKNOWN).translateBackToThemselves()

    assertThatLegacyReasons(LegacyReason.VIC, LegacyReason.VIC).translateBackToThemselves()
    assertThatLegacyReasons(LegacyReason.VIC, LegacyReason.NOT_REL).translateBackToThemselves()
    assertThatLegacyReasons(LegacyReason.VIC, LegacyReason.UNKNOWN).translateBackToThemselves()

    assertThatLegacyReasons(LegacyReason.NOT_REL, LegacyReason.NOT_REL).translateBackToThemselves()
    assertThatLegacyReasons(LegacyReason.NOT_REL, LegacyReason.UNKNOWN).translateBackToThemselves()

    assertThatLegacyReasons(LegacyReason.UNKNOWN, LegacyReason.UNKNOWN).translateBackToThemselves()
  }
}
