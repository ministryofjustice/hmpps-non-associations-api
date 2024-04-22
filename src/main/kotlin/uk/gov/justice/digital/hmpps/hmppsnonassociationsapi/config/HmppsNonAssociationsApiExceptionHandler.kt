package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class HmppsNonAssociationsApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
  fun handleInvalidRequestFormatException(e: HttpMediaTypeNotSupportedException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: Request format not supported: {}", e.message)
    return ResponseEntity
      .status(UNSUPPORTED_MEDIA_TYPE)
      .body(
        ErrorResponse(
          status = UNSUPPORTED_MEDIA_TYPE,
          userMessage = "Validation failure: Request format not supported: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleNoBodyValidationException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: Couldn't read request body: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: Couldn't read request body: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
    val type = e.requiredType
    val message = if (type.isEnum) {
      "Parameter ${e.name} must be one of the following ${StringUtils.join(type.enumConstants, ", ")}"
    } else {
      "Parameter ${e.name} must be of type ${type.typeName}"
    }

    log.info("Validation exception: {}", message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: $message",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.debug("Forbidden (403) returned with message {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "Forbidden: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NotFound::class)
  fun handleSpringNotFound(e: NotFound): ResponseEntity<ErrorResponse?>? {
    log.debug("Not found exception caught: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Not Found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(SubjectAccessRequestNoContentException::class)
  fun handleSubjectAccessRequestNoContentException(e: SubjectAccessRequestNoContentException): ResponseEntity<ErrorResponse?>? {
    log.debug("SAR No Content exception caught: {}", e.message)
    return ResponseEntity
      .status(NO_CONTENT)
      .body(
        ErrorResponse(
          status = NO_CONTENT,
          userMessage = "No Content: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse?>? {
    log.debug("No resource found exception caught: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "No resource found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NonAssociationNotFoundException::class)
  fun handleNonAssociationNotFound(e: NonAssociationNotFoundException): ResponseEntity<ErrorResponse?>? {
    log.debug("Non-association not found exception caught: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          errorCode = ErrorCode.NonAssociationNotFound,
          userMessage = "Non-association not found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(MissingPrisonersInSearchException::class)
  fun handleMissingPrisonersInSearchException(e: MissingPrisonersInSearchException): ResponseEntity<ErrorResponse?>? {
    log.debug("Missing prisoners in Offender Search API: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Missing prisoners: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ResponseStatusException::class)
  fun handleResponseStatusException(e: ResponseStatusException): ResponseEntity<ErrorResponse?>? {
    log.debug("Response status exception caught: {}", e.message)
    val reason = e.reason ?: "Unknown error"
    return ResponseEntity
      .status(e.statusCode)
      .body(
        ErrorResponse(
          status = e.statusCode.value(),
          userMessage = reason,
          developerMessage = reason,
        ),
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NonAssociationAlreadyClosedException::class)
  fun handleNonAssociationAlreadyClosedException(e: NonAssociationAlreadyClosedException): ResponseEntity<ErrorResponse?>? {
    log.debug("Already Closed Non-Association caught: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          errorCode = ErrorCode.NonAssociationAlreadyClosed,
          userMessage = "Already Closed Non-Association: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NonAssociationAlreadyOpenException::class)
  fun handleNonAssociationAlreadyOpenException(e: NonAssociationAlreadyOpenException): ResponseEntity<ErrorResponse?>? {
    log.debug("Already Open Non-Association caught: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          errorCode = ErrorCode.NonAssociationAlreadyOpen,
          userMessage = "Already Open Non-Association: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(UserInContextMissingException::class)
  fun handleUserInContextMissingException(e: UserInContextMissingException): ResponseEntity<ErrorResponse?>? {
    log.debug("User in context missing: {}", e.message)
    return ResponseEntity
      .status(UNAUTHORIZED)
      .body(
        ErrorResponse(
          status = UNAUTHORIZED,
          errorCode = ErrorCode.UserInContextMissing,
          userMessage = "User in context missing: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleInvalidMethodArgumentException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse>? {
    log.debug("MethodArgumentNotValidException exception caught: {}", e.message)

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          errorCode = ErrorCode.ValidationFailure,
          userMessage = "Validation Failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(OpenNonAssociationAlreadyExistsException::class)
  fun handleOpenNonAssociationAlreadyExistsException(e: OpenNonAssociationAlreadyExistsException): ResponseEntity<ErrorResponse?>? {
    log.debug("Non-association already exists for these prisoners that is open: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          errorCode = ErrorCode.OpenNonAssociationAlreadyExist,
          userMessage = "Non-association already exists for these prisoners that is open: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NullPrisonerLocationsException::class)
  fun handleNullPrisonerLocationsException(e: NullPrisonerLocationsException): ResponseEntity<ErrorResponse?>? {
    log.debug("Non-association cannot be created when prisoner locations are null: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          errorCode = ErrorCode.NullPrisonerLocations,
          userMessage = "Non-association cannot be created when prisoner locations are null: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

/**
 * Codes that can be used by api clients to uniquely discriminate between error types,
 * instead of relying on non-constant text descriptions of HTTP status codes.
 *
 * NB: Once defined, the values must not be changed
 */
enum class ErrorCode(val errorCode: Int) {
  NonAssociationAlreadyClosed(100),
  UserInContextMissing(401),
  OpenNonAssociationAlreadyExist(101),
  ValidationFailure(102),
  NullPrisonerLocations(103),
  NonAssociationAlreadyOpen(104),
  NonAssociationNotFound(404),
}

@Schema(description = "Error response")
data class ErrorResponse(
  @Schema(description = "HTTP status code", example = "500", required = true)
  val status: Int,
  @Schema(description = "User message for the error", example = "No non-association found for ID `324234`", required = true)
  val userMessage: String,
  @Schema(description = "More detailed error message", example = "[Details, sometimes a stack trace]", required = true)
  val developerMessage: String,
  @Schema(description = "When present, uniquely identifies the type of error making it easier for clients to discriminate without relying on error description or HTTP status code; see `uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorCode` enumeration in hmpps-non-associations-api", example = "101", required = false)
  val errorCode: Int? = null,
  @Schema(description = "More information about the error", example = "[Rarely used, error-specific]", required = false)
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    userMessage: String,
    developerMessage: String? = null,
    errorCode: ErrorCode? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), userMessage, developerMessage ?: userMessage, errorCode?.errorCode, moreInfo)
}

class NonAssociationAlreadyOpenException(id: Long) : Exception("Non-association [ID=$id] already open")

class NonAssociationAlreadyClosedException(id: Long) : Exception("Non-association [ID=$id] already closed")

class UserInContextMissingException : Exception("There is no user in context for this request")

class OpenNonAssociationAlreadyExistsException(prisoners: List<String>) : Exception("Prisoners $prisoners already have open non-associations")

class NonAssociationNotFoundException(id: Long) : Exception("There is no non-association found for ID = $id")

class NullPrisonerLocationsException(prisoners: List<String>) : Exception("Prisoners $prisoners have null locations")

class SubjectAccessRequestNoContentException(prisoner: String) : Exception("No information on prisoner $prisoner")

class MissingPrisonersInSearchException(prisoners: Iterable<String>) : Exception("Could not find the following prisoners: $prisoners")
