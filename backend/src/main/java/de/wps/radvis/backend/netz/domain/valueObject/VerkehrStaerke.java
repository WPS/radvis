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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode
public class VerkehrStaerke {
	private static final int MAX_VALUE = 1_000_000;

	@Getter
	@JsonValue
	private final int value;

	private VerkehrStaerke(int value) {
		require(value > 0, "Vorbedingung nicht erfüllt: value > 0, value: " + value);
		require(value <= MAX_VALUE,
			"Der Wert für Verkehrsstärker " + value + " muss kleiner als " + MAX_VALUE + " sein.");
		this.value = value;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static VerkehrStaerke of(int value) {
		return new VerkehrStaerke(value);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static VerkehrStaerke of(@NonNull String value) {
		if (!isValid(value)) {
			return null;
		}

		return new VerkehrStaerke(Integer.parseInt(value));
	}

	public static boolean isValid(String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		try {
			int i = Integer.parseInt(value);
			if (i <= 0) {
				return false;
			}
			if (i > MAX_VALUE) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return value + " Fz/Tag";
	}
}
