/*
 * Copyright (c) 2023 WPS - Workplace Solutions GmbH
 *
 * Licensed under the EUPL, Version 1.2 or as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.wps.radvis.backend.application.schnittstelle;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import de.wps.radvis.backend.common.schnittstelle.ErrorDetails;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import lombok.NonNull;

@ControllerAdvice
public class RadvisRestExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(OptimisticLockException.class)
	protected ResponseEntity<Object> handleOptimisticLockingException(OptimisticLockException ex) {
		return new ResponseEntity<>(ex.getMessage(), new HttpHeaders(), HttpStatus.CONFLICT);
	}

	@ExceptionHandler(AccessDeniedException.class)
	protected ResponseEntity<ErrorDetails> handleAccessDeniedException(
		AccessDeniedException ex) {
		ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), ex.getMessage(),
			ex.getClass().getSimpleName(), ex.getMessage());
		return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	protected ResponseEntity<Object> handleEntityNotFoundException(
		EntityNotFoundException ex) {
		ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(),
			"Die angefragte Ressource ist nicht mehr verfügbar.",
			ex.getClass().getSimpleName(), ex.getMessage());
		return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
		@NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
		Optional<ObjectError> firstError = ex.getBindingResult().getAllErrors().stream().findFirst();
		ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(),
			firstError.map(DefaultMessageSourceResolvable::getDefaultMessage)
				.orElse("Unbekannter Fehler. Validierung fehlgeschlagen"),
			ex.getClass().getSimpleName(), ex.getMessage());
		return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	protected @NonNull ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex) {
		ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), ex.getMessage(),
			ex.getClass().getSimpleName(), ex.getMessage());
		return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
	}

	@Override
	protected @NonNull ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
		@NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
		ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(),
			"Ein unerwarteter Fehler ist aufgetreten.",
			ex.getClass().getSimpleName(), ex.getMessage());
		return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
	}
}
