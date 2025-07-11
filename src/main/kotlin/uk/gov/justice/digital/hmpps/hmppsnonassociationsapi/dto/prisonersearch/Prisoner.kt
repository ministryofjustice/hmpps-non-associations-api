package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonersearch

data class Prisoner(
  val prisonerNumber: String,
  val firstName: String,
  val lastName: String,

  val prisonId: String?,
  val prisonName: String?,
  val cellLocation: String?,
)
