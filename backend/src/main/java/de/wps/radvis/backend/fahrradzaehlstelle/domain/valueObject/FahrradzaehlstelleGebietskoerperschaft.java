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

package de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class FahrradzaehlstelleGebietskoerperschaft {

	public static final int MAX_LENGTH = 255;

	@Getter
	@JsonValue
	@NonNull
	private final String value;

	private FahrradzaehlstelleGebietskoerperschaft(String value) {
		require(value, notNullValue());
		require(value.length() > 0, "Value darf nicht leer sein");
		require(value.length() <= MAX_LENGTH, "value.length() <= " + MAX_LENGTH);
		this.value = value;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static FahrradzaehlstelleGebietskoerperschaft of(String value) {
		return new FahrradzaehlstelleGebietskoerperschaft(value);
	}
}
