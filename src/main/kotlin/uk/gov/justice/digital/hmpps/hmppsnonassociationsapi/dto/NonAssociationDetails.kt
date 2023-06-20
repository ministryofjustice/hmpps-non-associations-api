package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Non-association details
 *
 * TODO: This is WIP at the moment. It may share some similarities with the
 * format currently returned by NOMIS/Prison API but it's a distinct type
 * and will likely differ.
 */
data class NonAssociationDetails(
  @Schema(description = "Prisoner number", required = true, example = "A1234BC")
  val prisonerNumber: String,
)
