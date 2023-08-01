package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.MigrateRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.SyncAndMigrateService

@RestController
@Validated
@RequestMapping("/migrate", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS_MIGRATE')")
@Tag(
  name = "Migrate non-associations from NOMIS",
  description = "Receive initial data being copied over from NOMIS",
)
class MigrateResource(
  private val syncAndMigrateService: SyncAndMigrateService,
) {

  @PostMapping()
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Migrate a non-association",
    description = "Requires ROLE_NON_ASSOCIATIONS_MIGRATE role.",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Returns the new created non-association",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the ROLE_NON_ASSOCIATIONS_MIGRATE role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun migrate(
    @Valid @RequestBody
    migrateRequest: MigrateRequest,
  ): NonAssociation {
    return syncAndMigrateService.migrate(migrateRequest)
  }
}
