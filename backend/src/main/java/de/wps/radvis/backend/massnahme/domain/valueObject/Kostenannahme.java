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

package de.wps.radvis.backend.massnahme.domain.valueObject;

import static org.valid4j.Assertive.require;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Embeddable
@NoArgsConstructor
public class Kostenannahme {

	@Getter
	@JsonValue
	private Long kostenannahme;

	private Kostenannahme(long value) {
		require(isValid(value), "Vorbedingung nicht erfüllt: value >= 0, value: " + value);
		this.kostenannahme = value;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Kostenannahme of(Long value) {
		if (value == null) {
			return null;
		}

		return new Kostenannahme(value);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Kostenannahme of(@NonNull String value) {
		if (value.isEmpty()) {
			return null;
		}

		return new Kostenannahme(Long.parseLong(value));
	}

	public static boolean isValid(String value) {
		long l;

		try {
			l = Long.parseLong(value);
		} catch (NumberFormatException e) {
			return false;
		}

		return isValid(l);
	}

	public static boolean isValid(long value) {
		return value >= 0;
	}
}
