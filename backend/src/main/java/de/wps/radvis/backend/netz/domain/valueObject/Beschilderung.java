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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Beschilderung {
	//@formatter:off
    UNBEKANNT("Unbekannt", false),
    GEHWEG_OHNE_VZ_239("StVO-Beschilderung: Gehweg ohne VZ 239", false),
    GEHWEG_MIT_VZ_239("StVO-Beschilderung: Gehweg mit VZ 239", false),
    VZ_254_ANGEORDNET("StVO-Beschilderung: VZ 254 (Radfahren verboten) angeordnet", false),
    VZ_1012_32_ANGEORDNET("StVO-Beschilderung: VZ 1012-32 (Radfahrer absteigen) angeordnet", false),
    KEIN_WEG_VORHANDEN("Kein Weg vorhanden (physische Netzlücke)", false),
    PRIVATWEG("Privatweg", false),
    ABGESPERRT("Abgesperrter Weg", false),
    ZUSATZZEICHEN_VORHANDEN("Zusatzzeichen vorhanden", true),
    ZUSATZZEICHEN_NICHT_VORHANDEN("Zusatzzeichen nicht vorhanden", true),
    HAUPTZEICHEN_KFZ_VERBOTEN("Hauptzeichen nur Kraftfahrzeuge verboten (kein Zusatzzeichen nötig)", true),
    NICHT_VORHANDEN("keinerlei StVO-Beschilderung vorhanden", true),
    ;
	//@formatter:on

	@NonNull
	private final String displayText;

	private final boolean onlyForBetriebsweg;

	@Override
	public String toString() {
		return this.displayText;
	}

	public boolean isValidForRadverkehrsfuehrung(Radverkehrsfuehrung radverkehrsfuehrung) {
		require(radverkehrsfuehrung, notNullValue());

		if (onlyForBetriebsweg) {
			return radverkehrsfuehrung.equals(Radverkehrsfuehrung.BETRIEBSWEG_FORST)
				|| radverkehrsfuehrung.equals(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
				|| radverkehrsfuehrung.equals(Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT)
				|| radverkehrsfuehrung.equals(Radverkehrsfuehrung.SONSTIGER_BETRIEBSWEG)
				|| radverkehrsfuehrung.equals(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND);
		}

		return true;
	}

	public boolean widerspruchZu(Beschilderung other) {
		return !(this == Beschilderung.UNBEKANNT || other == Beschilderung.UNBEKANNT || this.equals(other));
	}
}