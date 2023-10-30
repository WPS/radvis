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

package de.wps.radvis.backend.netz.domain.valueObject;

import static org.valid4j.Assertive.require;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode
public class Zustandsbeschreibung {
	private static final int MAX_TOTAL_BESCHREIBUNG_LENGTH = 2000;

	@Getter
	@JsonValue
	@NonNull
	String value;

	protected Zustandsbeschreibung(@NotNull String value) {
		require(value.length() <= MAX_TOTAL_BESCHREIBUNG_LENGTH,
			"Die Zustandsbeschreibung überschreitet die maximal erlaubte Länge von 2000 Zeichen");
		this.value = value;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Zustandsbeschreibung of(@NotNull String value) {
		return new Zustandsbeschreibung(value);
	}

	@Override
	public String toString() {
		return this.value;
	}
}
