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

package de.wps.radvis.backend.fahrradroute.domain.valueObject;

import static org.valid4j.Assertive.require;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Höhenunterschied in Meter. Ein Höhenunterschied wird als die absolute Differenz in m zwischen zwei Höhenwerten
 * bezeichnet und kann somit nur Werte >= 0 annehmen.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode
public class Hoehenunterschied {
	@Getter
	@JsonValue
	@NonNull
	Double value;

	public static Hoehenunterschied of(Double hoehenunterschied) {
		require(hoehenunterschied >= 0.0, "Ein Höhenunterschied muss >= 0 sein, ist aber " + hoehenunterschied);
		return new Hoehenunterschied(hoehenunterschied);
	}

	@Override
	public String toString() {
		return String.format("%.2f", this.value) + " m";
	}
}
