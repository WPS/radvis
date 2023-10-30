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

package de.wps.radvis.backend.weitereKartenebenen.domain.valueobject;

import static org.valid4j.Assertive.require;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.usertype.NameUserType;
import jakarta.persistence.Convert;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode
@Convert(converter = NameUserType.class)
public class GeoserverStyleName {
	public static final int NAME_MAX_LENGTH = 255;

	@Getter
	@JsonValue
	@NonNull
	String value;

	private GeoserverStyleName(@NonNull String value) {
		require(isValid(value),
			"Geoserver Style-Name muss mindestens eins und darf maximal 255 Zeichen haben. Nur die Zeichen a-z, 0-9 '-' und '_' sind erlaubt.");
		this.value = value;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static GeoserverStyleName of(String value) {
		return new GeoserverStyleName(value);
	}

	public static GeoserverStyleName withTimestamp(String value) {
		// Damit "isValid()" erfüllt ist.
		// Z.B. "Abc 123#4" -> "abc_123_4"
		value = GeoserverDatastoreName.normalizeGeoserverName(value);

		// Timestamp anhängen, sodass der Name eindeutig ist
		value = value + "_" + System.currentTimeMillis();

		return new GeoserverStyleName(value);
	}

	public static boolean isValid(String value) {
		int length = value.trim().length();
		// Nur simple ASCII-Zeichen erlauben. Es sind mehr erlaubt (siehe RFC-7230 und RFC-3986), aber diese Auswahl
		// reicht uns hier. Der tatsächlich erstelle Name enthält zusätzlich noch einen generierten Suffix, sodass der
		// name auch mit einer reduzierten Zeichenauswahl eindeutig bleibt.
		return length <= NAME_MAX_LENGTH && length >= 1 && value.matches("^[a-z0-9-_]+$");
	}

	@Override
	public String toString() {
		return value;
	}
}
