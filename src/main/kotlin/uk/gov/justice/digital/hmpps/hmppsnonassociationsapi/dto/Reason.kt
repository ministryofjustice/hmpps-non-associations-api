package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

enum class Reason(val description: String) {
  BULLYING("Bullying"),
  GANG_RELATED("Gang related"),
  ORGANISED_CRIME("Organised crime"),
  LEGAL_REQUEST("Police or legal request"),
  THREAT("Threat"),
  VIOLENCE("Violence"),
  OTHER("Other"),
  ;
}
