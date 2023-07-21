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
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.SyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.SyncAndMigrateService

@RestController
@Validated
@RequestMapping("/sync", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Sync Non Association Data", description = "Receive non-association record from NOMIS and store in database")
@PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS_SYNC')")
class SyncResource(
  private val syncAndMigrateService: SyncAndMigrateService,
) {

  @PostMapping()
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Sync a non-association",
    description = "Requires ROLE_NON_ASSOCIATIONS_SYNC role.",
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
        description = "Missing required role. Requires the ROLE_NON_ASSOCIATIONS_SYNC role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun sync(
    @Valid @RequestBody
    syncRequest: SyncRequest,
  ): NonAssociation {
    return syncAndMigrateService.sync(syncRequest)
  }
}
