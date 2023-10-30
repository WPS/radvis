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

package de.wps.radvis.backend.organisation.domain.valueObject;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum OrganisationsArt {
	// Achtung, die Zuordnung von gebietskoerperschaft zu den einzelnen Felder
	// findet auch nochmal im FE in organisations-art.ts statt
	BUNDESLAND(true, "Bundesland"),
	GEMEINDE(true, "Gemeinde"),
	KREIS(true, "Landkreis"),
	REGIERUNGSBEZIRK(true, "Regierungsbezirk"),

	TOURISMUSVERBAND(false, "Tourismusverband"),
	EXTERNER_DIENSTLEISTER(false, "Externer Dienstleister"),
	REGIONALVERBAND(false, "Regionalverband"),
	SONSTIGES(false, "Sonstiges");

	private final boolean gebietskoerperschaft;

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return this.displayText;
	}

	public boolean istGebietskoerperschaft() {
		return this.gebietskoerperschaft;
	}

	public static OrganisationsArt fromString(String displayText) {
		switch (displayText) {
		case "Bundesland":
			return OrganisationsArt.BUNDESLAND;
		case "Gemeinde":
			return OrganisationsArt.GEMEINDE;
		case "Landkreis":
			return OrganisationsArt.KREIS;
		case "Regierungsbezirk":
			return OrganisationsArt.REGIERUNGSBEZIRK;
		case "Tourismusverband":
			return OrganisationsArt.TOURISMUSVERBAND;
		case "Externer Dienstleister":
			return OrganisationsArt.EXTERNER_DIENSTLEISTER;
		case "Regionalverband":
			return OrganisationsArt.REGIONALVERBAND;
		case "Sonstiges":
			return OrganisationsArt.SONSTIGES;
		}

		throw new RuntimeException("OrganisationsArt '" + displayText + "' kann nicht gelesen werden");
	}
}
