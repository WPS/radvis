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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Set;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum TrennungZu {
	SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN("Sicherheitstrennstreifen zur Fahrbahn",
		Set.of(Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.RADFAHRSTREIFEN,
			Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR,
			Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR,
			Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER)),
	SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN("Sicherheitstrennstreifen zum Parken",
		Set.of(Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.SCHUTZSTREIFEN,
			Radverkehrsfuehrung.RADFAHRSTREIFEN,
			Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR,
			Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR,
			Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE,
			Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADZONE,
			Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG,
			Radverkehrsfuehrung.MEHRZWECKSTREIFEN_EINSEITIG,
			Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER)),
	SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR("Sicherheitstrennstreifen zum Fußverkehr",
		Set.of(Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND,
			Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER)),
			;

	@NonNull
	private final String displayText;

	private final Set<Radverkehrsfuehrung> validRadverkehrsfuehrungen;

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

	public boolean isValidForRadverkehrsfuehrung(Radverkehrsfuehrung radverkehrsfuehrung) {
		require(radverkehrsfuehrung, notNullValue());
		return validRadverkehrsfuehrungen.contains(radverkehrsfuehrung);
	}
}
