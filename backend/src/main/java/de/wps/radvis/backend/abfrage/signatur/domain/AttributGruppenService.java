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

package de.wps.radvis.backend.abfrage.signatur.domain;

import java.util.List;

import de.wps.radvis.backend.netz.domain.valueObject.AttributGruppe;

public class AttributGruppenService {

	public static AttributGruppe getAttributGruppe(List<String> attribute)
		throws AttributGruppeNichtBestimmbarException {
		if (attribute == null || attribute.isEmpty()) {
			throw new AttributGruppeNichtBestimmbarException("Mindestens ein Attribut muss zur Bestimmung der "
				+ "AttributGruppe gesetzt sein");
		}
		AttributGruppe resultAttributGruppe = getAttributGruppeOf(attribute.get(0));
		for (String attribut : attribute) {
			if (!getAttributGruppeOf(attribut).equals(resultAttributGruppe)) {
				throw new AttributGruppeNichtBestimmbarException(
					"Alle Attribute müssen derselben AttributGruppe angehören."
						+ "Gefunden: " + attribute.get(0) + "(" + resultAttributGruppe + ") und " + attribut + "("
						+ getAttributGruppeOf(attribut) + ")");
			}
		}
		return resultAttributGruppe;
	}

	private static AttributGruppe getAttributGruppeOf(String attribut) throws AttributGruppeNichtBestimmbarException {
		if (attribut.equals("wege_niveau") ||
			attribut.equals("beleuchtung") ||
			attribut.equals("laenge_manuell_erfasst") ||
			attribut.equals("dtv_fussverkehr") ||
			attribut.equals("dtv_radverkehr") ||
			attribut.equals("dtv_pkw") ||
			attribut.equals("sv") ||
			attribut.equals("status") ||
			attribut.equals("kommentar") ||
			attribut.equals("strassen_name") ||
			attribut.equals("strassen_nummer") ||
			attribut.equals("gemeinde_name") ||
			attribut.equals("landkreis_name") ||
			attribut.equals("netzklassen") ||
			attribut.equals("standards") ||
			attribut.equals("hoechsteNetzklasse")) {
			return AttributGruppe.KANTENATTRIBUTE;
		}
		if (attribut.equals("ortslage") ||
			attribut.equals("hoechstgeschwindigkeit") ||
			attribut.equals("abweichende_hoechstgeschwindigkeit_gegen_stationierungsrichtung")) {
			return AttributGruppe.GESCHWINDIGKEITATTRIBUTE;
		}
		if (attribut.equals("belag_art") ||
			attribut.equals("bordstein") ||
			attribut.equals("benutzungspflicht") ||
			attribut.equals("oberflaechenbeschaffenheit") ||
			attribut.equals("radverkehrsfuehrung") ||
			attribut.equals("strassenquerschnittrast06") ||
			attribut.equals("umfeld") ||
			attribut.equals("parken_typ") ||
			attribut.equals("parken_form") ||
			attribut.equals("breite")) {
			return AttributGruppe.FUEHRUNGSFORMATTRIBUTE;
		}
		if (attribut.equals("baulast_traeger") ||
			attribut.equals("unterhalts_zustaendiger") ||
			attribut.equals("erhalts_zustaendiger") ||
			attribut.equals("vereinbarungs_kennung")) {
			return AttributGruppe.ZUSTAENDIGKEIT;
		}
		if (attribut.equals("is_zweiseitig") ||
			attribut.equals("fahrtrichtung_links") ||
			attribut.equals("fahrtrichtung_rechts")) {
			return AttributGruppe.FAHRTRICHTUNG;
		}
		throw new AttributGruppeNichtBestimmbarException(
			"Attribut " + attribut + " konnte keiner AttributGruppe zugeordnet werden");
	}

}
