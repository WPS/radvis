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

package de.wps.radvis.backend.netz.domain.entity;

import java.util.HashSet;

import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe.KantenAttributGruppeBuilder;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;

public class KantenAttributGruppeTestDataProvider {
	public static KantenAttributGruppeBuilder defaultValue() {
		return KantenAttributGruppe.builder()
			.netzklassen(new HashSet<>())
			.istStandards(new HashSet<>())
			.kantenAttribute(KantenAttribute.builder()
				.beleuchtung(Beleuchtung.UNBEKANNT)
				.umfeld(Umfeld.UNBEKANNT)
				.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
				.status(Status.defaultWert()).build());
	}
}
