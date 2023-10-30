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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode
public class MassnahmenPaketId {
	private static final String VALID_MASSNAHMEN_PAKET_ID_REGEX = "^[A-ZÜÖÄ]{1,4} [0-9]{1,4}\\.[0-9]{1,2}[A-Z]?$";

	@Getter
	@JsonValue
	@NonNull
	String value;

	private MassnahmenPaketId(String value) {
		this.value = value;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static MassnahmenPaketId of(String value) {
		return new MassnahmenPaketId(value);
	}

	@Override
	public String toString() {
		return value;
	}

	public static boolean isValid(String string) {
		return string.matches(VALID_MASSNAHMEN_PAKET_ID_REGEX);
	}
}
