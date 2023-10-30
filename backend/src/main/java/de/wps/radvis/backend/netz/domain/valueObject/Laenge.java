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

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * Beschreibt die Länge in Metern
 */
@EqualsAndHashCode
public class Laenge {

	@Getter
	@JsonValue
	private final double value;

	private Laenge(double value) {
		require(value > 0, "Vorbedingung nicht erfüllt: value > 0, value: " + value);
		this.value = value;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Laenge of(double value) {
		return new Laenge(value);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Laenge of(int value) {
		return new Laenge(value);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Laenge of(long value) {
		return new Laenge(value);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Laenge of(@NonNull String value) {
		if (value.isEmpty()) {
			return null;
		}

		String fixedValue = value.replace(',', '.');
		return new Laenge(Double.parseDouble(fixedValue));
	}

	public static boolean isValid(String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		String fixedValue = value.replace(',', '.');
		try {
			double doubleValue = Double.parseDouble(fixedValue);
			if (doubleValue <= 0) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format(Locale.GERMANY, "%.2f m", this.value);
	}
}
