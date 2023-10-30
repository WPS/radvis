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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum TrennungZu {
	//@formatter:off
	SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN("Sicherheitstrennstreifen zur Fahrbahn"),
	SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN("Sicherheitstrennstreifen zum Parken"),
	SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR("Sicherheitstrennstreifen zum Fußverkehr");
	//@formatter:on

	@NonNull
	private final String displayText;

	public static boolean nullableWiderspruchZu(TrennungZu a, TrennungZu b) {
		// null-Faelle abfangen
		if (a == null && b == null) {
			return false;
		} else if (a == null && b != null) {
			return true;
		} else if (a != null && b == null) {
			return true;
		}
		return !a.equals(b);
	}

	@Override
	public String toString() {
		return this.displayText;
	}
}
