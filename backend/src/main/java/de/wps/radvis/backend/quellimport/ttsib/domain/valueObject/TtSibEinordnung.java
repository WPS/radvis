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

package de.wps.radvis.backend.quellimport.ttsib.domain.valueObject;

import static org.valid4j.Assertive.require;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum TtSibEinordnung {
	LINKS("Links"),
	MITTE("Mitte"),
	RECHTS("Rechts");

	@NonNull
	private final String displayText;

	/**
	 * Accepts "L", "M" or "R"
	 */
	public static TtSibEinordnung fromLMRChar(String lMR) {
		require(lMR.equals("M") || lMR.equals("R") || lMR.equals("L"),
			"Ungültiger Streifeneinordnung. Muss L, R oder M sein.");

		if (lMR.equals("M")) {
			return TtSibEinordnung.MITTE;
		}

		if (lMR.equals("R")) {
			return TtSibEinordnung.RECHTS;
		}

		return TtSibEinordnung.LINKS;
	}

	@Override
	public String toString() {
		return this.displayText;
	}
}
