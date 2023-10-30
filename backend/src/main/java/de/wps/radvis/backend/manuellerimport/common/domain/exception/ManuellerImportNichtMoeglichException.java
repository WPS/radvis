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

package de.wps.radvis.backend.manuellerimport.common.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ManuellerImportNichtMoeglichException extends Exception {

	private static final long serialVersionUID = -4144114049486031043L;

	private static final String MESSAGE_PREFIX = "Manueller Import nicht möglich: ";

	public ManuellerImportNichtMoeglichException(Throwable e) {
		this(e.getMessage(), e);
	}

	public ManuellerImportNichtMoeglichException(String message, Throwable e) {
		super(MESSAGE_PREFIX + message, e);
	}
}
