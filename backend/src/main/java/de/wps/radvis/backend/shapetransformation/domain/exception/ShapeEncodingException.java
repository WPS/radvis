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

package de.wps.radvis.backend.shapetransformation.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ShapeEncodingException extends Exception {

	private static final long serialVersionUID = 241158646366626748L;

	public ShapeEncodingException(String charset) {
		super(
			String.format("Die ShapeFile muss in UTF-8 vorliegen. Das angebene Encoding '%s' ist ungültig.", charset));
	}

	public ShapeEncodingException() {
		super("Die ShapeFile muss in UTF-8 vorliegen. Das angebene Encoding konnte nicht ermittelt werden.");
	}
}
