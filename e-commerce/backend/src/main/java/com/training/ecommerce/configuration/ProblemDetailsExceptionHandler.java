package com.training.ecommerce.configuration;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders every API failure as RFC 7807 Problem Details: request-parameter failures carry field
 * {@code violations}, every problem carries the request path as {@code instance}, and unexpected
 * failures never expose exception or SQL text.
 */
@RestControllerAdvice
class ProblemDetailsExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProblemDetailsExceptionHandler.class);
	private static final URI ABOUT_BLANK = URI.create("about:blank");
	private static final String INVALID_REQUEST_DETAIL = "The request contains invalid parameters.";

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> violations = exception.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new FieldViolation(result.getMethodParameter().getParameterName(),
								error.getDefaultMessage())))
				.toList();
		return invalidRequest(exception, violations, headers, request);
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String field = exception instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName()
				: exception.getPropertyName();
		return invalidRequest(exception, List.of(new FieldViolation(field, "must be a whole number")), headers, request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {
		LOGGER.error("Unhandled API failure", exception);
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
		return handleExceptionInternal(exception, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		var response = super.handleExceptionInternal(exception, body, headers, statusCode, request);
		if (response != null && response.getBody() instanceof ProblemDetail problem) {
			// Spring 7 leaves type null (implicitly "about:blank"); the published contract always includes it.
			if (problem.getType() == null) {
				problem.setType(ABOUT_BLANK);
			}
			if (problem.getInstance() == null && request instanceof NativeWebRequest nativeRequest
					&& nativeRequest.getNativeRequest() instanceof HttpServletRequest servletRequest) {
				problem.setInstance(URI.create(servletRequest.getRequestURI()));
			}
		}
		return response;
	}

	private ResponseEntity<Object> invalidRequest(Exception exception, List<FieldViolation> violations,
			HttpHeaders headers, WebRequest request) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_REQUEST_DETAIL);
		problem.setProperty("violations", violations);
		return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
	}
}
