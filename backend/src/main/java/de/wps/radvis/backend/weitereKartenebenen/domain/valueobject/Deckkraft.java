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

import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.usertype.DeckkraftType;
import jakarta.persistence.Convert;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Convert(converter = DeckkraftType.class)
public class Deckkraft {

	@Getter
	@JsonValue
	private final Double value;

	private Deckkraft(Double value) {
		require(isValid(value), "Deckkraft nicht valide (Muss zwischen 0 und 1 liegen)");
		this.value = value;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Deckkraft of(Double value) {
		return new Deckkraft(value);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Deckkraft of(int value) {
		return new Deckkraft((double) value);
	}

	private static boolean isValid(Double value) {
		return value != null && value >= 0 && value <= 1.0;
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}
}
