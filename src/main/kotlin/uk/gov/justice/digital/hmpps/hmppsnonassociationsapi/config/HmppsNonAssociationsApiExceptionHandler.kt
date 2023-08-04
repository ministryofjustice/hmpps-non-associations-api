package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class HmppsNonAssociationsApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
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
  fun handleInvalidRequestFormatValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: Request format not supported: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: Request format not supported: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleNoBodyValidationException(e: Exception): ResponseEntity<ErrorResponse> {
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
      .status(HttpStatus.FORBIDDEN)
      .body(
        ErrorResponse(
          status = HttpStatus.FORBIDDEN,
          userMessage = "Forbidden: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NotFound::class)
  fun handleSpringNotFound(e: NotFound): ResponseEntity<ErrorResponse?>? {
    log.debug("Not found exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "Not Found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ResponseStatusException::class)
  fun handleResponseStatusException(e: ResponseStatusException): ResponseEntity<ErrorResponse?>? {
    log.debug("Response status exception caught: {}", e.message)
    return ResponseEntity
      .status(e.statusCode)
      .body(
        ErrorResponse(
          status = e.statusCode.value(),
          userMessage = e.reason,
          developerMessage = e.reason,
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
      .status(HttpStatus.CONFLICT)
      .body(
        ErrorResponse(
          status = HttpStatus.CONFLICT,
          errorCode = ErrorCode.NonAssociationAlreadyClosed,
          userMessage = "Already Closed Non-Association: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(UserInContextMissingException::class)
  fun handleUserInContextMissingException(e: UserInContextMissingException): ResponseEntity<ErrorResponse?>? {
    log.debug("User in context missing: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.UNAUTHORIZED)
      .body(
        ErrorResponse(
          status = HttpStatus.UNAUTHORIZED,
          errorCode = ErrorCode.UserInContextMissing,
          userMessage = "User in context missing: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(OpenNonAssociationAlreadyExistsException::class)
  fun handleOpenNonAssociationAlreadyExistsException(e: OpenNonAssociationAlreadyExistsException): ResponseEntity<ErrorResponse?>? {
    log.debug("Non association already exists for these prisoners that is open: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST,
          errorCode = ErrorCode.OpenNonAssociationAlreadyExist,
          userMessage = "Non association already exists for these prisoners that is open: ${e.message}",
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
 * instead of relying on non-constant text descriptions.
 *
 * NB: Once defined, the values must not be changed
 */
enum class ErrorCode(val errorCode: Int) {
  NonAssociationAlreadyClosed(100),
  UserInContextMissing(401),
  OpenNonAssociationAlreadyExist(101),
}

@Schema(description = "Error response")
data class ErrorResponse(
  @Schema(description = "HTTP status code", example = "500", required = true)
  val status: Int,
  @Schema(description = "When present, uniquely identifies the type of error making it easier for clients to discriminate without relying on error description; see `uk.gov.justice.digital.hmpps.incentivesapi.config.ErrorResponse` enumeration in hmpps-incentives-api", example = "123", required = false)
  val errorCode: Int? = null,
  @Schema(description = "User message for the error", example = "No incentive level found for code `ABC`", required = false)
  val userMessage: String? = null,
  @Schema(description = "More detailed error message", example = "[Details, sometimes a stack trace]", required = false)
  val developerMessage: String? = null,
  @Schema(description = "More information about the error", example = "[Rarely used, error-specific]", required = false)
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: ErrorCode? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode?.errorCode, userMessage, developerMessage, moreInfo)
}

class NonAssociationAlreadyClosedException(id: Long) : Exception("Non-association [ID=$id] already closed")

class UserInContextMissingException() : Exception("There is no user in context for this request")

class OpenNonAssociationAlreadyExistsException(prisoners: List<String>) : Exception("Prisoners [$prisoners] already have open non-associations")
