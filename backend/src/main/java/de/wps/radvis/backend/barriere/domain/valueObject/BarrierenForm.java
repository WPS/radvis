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

package de.wps.radvis.backend.barriere.domain.valueObject;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public enum BarrierenForm {

	// BarrierenFormArt.ABSPERRANLAGEN
	SPERRPFOSTEN(BarrierenFormArt.ABSPERRANLAGEN, "Sperrpfosten"),

	VERSENKBARE_SPERRPFOSTEN(BarrierenFormArt.ABSPERRANLAGEN, "Versenkbare Sperrpfosten"),

	MOBILE_SPERRPFOSTEN(BarrierenFormArt.ABSPERRANLAGEN, "Mobile Sperrpfosten"),

	UMLAUFSPERREN(BarrierenFormArt.ABSPERRANLAGEN, "Umlaufsperren"),

	SCHRANKE(BarrierenFormArt.ABSPERRANLAGEN, "Schranke"),

	// BarrierenFormArt.BARRIERE_DURCH_ANORDNUNG

	ANORDNUNG_VZ_220(BarrierenFormArt.BARRIERE_DURCH_ANORDNUNG, "Anordnung VZ 220 ohne 1022-10 (Schiebestrecke)"),

	ANORDNUNG_VZ_239(BarrierenFormArt.BARRIERE_DURCH_ANORDNUNG, "Anordnung VZ 239 (Schiebestrecke)"),

	ANORDNUNG_VZ_242(BarrierenFormArt.BARRIERE_DURCH_ANORDNUNG, "Anordnung VZ 242 (Schiebestrecke)"),

	ANORDNUNG_VZ_242_1040_42(BarrierenFormArt.BARRIERE_DURCH_ANORDNUNG,
		"Anordnung VZ 242 +1040 / 1042 (Schiebestrecke)"),

	ANORDNUNG_VZ_250(BarrierenFormArt.BARRIERE_DURCH_ANORDNUNG, "Anordnung VZ 250 (Schiebestrecke)"),

	ANORDNUNG_VZ_254(BarrierenFormArt.BARRIERE_DURCH_ANORDNUNG, "Anordnung VZ 254 (Schiebestrecke)"),

	ANORDNUNG_VZ_1012_32(BarrierenFormArt.BARRIERE_DURCH_ANORDNUNG, "Anordnung VZ 1012-32 (Schiebestrecke)"),

	// BarrierenFormArt.ANDERE_BARRIEREN

	TREPPE(BarrierenFormArt.ANDERE_BARRIEREN, "Treppe"),

	STEILE_RAMPE(BarrierenFormArt.ANDERE_BARRIEREN, "Steile Rampe (>6%)"),

	LICHTMAST_SIGNALMAST(BarrierenFormArt.ANDERE_BARRIEREN, "Lichtmast / Signalmast im Verkehrsraum"),

	VERKEHRSZEICHEN(BarrierenFormArt.ANDERE_BARRIEREN, "Verkehrszeichen im Verkehrsraum"),

	EINENGUNG_HALTESTELLE(BarrierenFormArt.ANDERE_BARRIEREN, "Einengung durch Haltestelle"),

	EINENGUNG_SONSTIGE(BarrierenFormArt.ANDERE_BARRIEREN, "Einengung durch sonstige bauliche Gegebenheit"),

	BAUM_BAUMSCHEIBE(BarrierenFormArt.ANDERE_BARRIEREN, "Baum / Baumscheibe im Verkehrsraum"),

	SONSTIGE_BARRIERE(BarrierenFormArt.ANDERE_BARRIEREN, "Sonstige Barriere");

	private final BarrierenFormArt barrierenFormArt;

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}
}
