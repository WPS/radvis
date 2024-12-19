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

package de.wps.radvis.backend.netz.domain.entity.provider;

import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenkategorieRIN;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;

public class KantenAttributeTestDataProvider {
	public static KantenAttribute.KantenAttributeBuilder withLeereGrundnetzAttribute() {
		return KantenAttribute.builder()
			.beleuchtung(Beleuchtung.UNBEKANNT)
			.umfeld(Umfeld.UNBEKANNT)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
			.status(Status.defaultWert());
	}

	public static KantenAttribute.KantenAttributeBuilder createWithValues(Gebietskoerperschaft gemeinde) {
		return KantenAttribute.builder()
			.umfeld(Umfeld.GEWERBEGEBIET)
			.strassenkategorieRIN(StrassenkategorieRIN.NAHRAEUMIG)
			.wegeNiveau(WegeNiveau.FAHRBAHN)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.HAUPTGESCHAEFTSSTRASSE)
			.sv(VerkehrStaerke.of(12))
			.beleuchtung(Beleuchtung.VORHANDEN)
			.dtvFussverkehr(VerkehrStaerke.of(1))
			.dtvRadverkehr(VerkehrStaerke.of(2))
			.dtvPkw(VerkehrStaerke.of(3))
			.gemeinde(gemeinde)
			.laengeManuellErfasst(Laenge.of(123.45))
			.kommentar(Kommentar.of("Toller Kommentar"))
			.strassenNummer(StrassenNummer.of("B123"))
			.strassenName(StrassenName.of("Dingenskirchener Straße"))
			.status(Status.UNTER_VERKEHR);
	}
}
