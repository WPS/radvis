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

package de.wps.radvis.backend.massnahme.domain.bezug;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;

public class NetzBezugTestDataProvider {
	public static MassnahmeNetzBezug forKanteAbschnittsweise(Kante... kanten) {
		return new MassnahmeNetzBezug(
			Arrays.stream(kanten)
				.map(kante -> new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1),
					Seitenbezug.LINKS)).collect(Collectors.toSet()),
			Set.of(),
			Set.of());
	}

	public static MassnahmeNetzBezug forKantePunktuell(Kante... kanten) {
		return new MassnahmeNetzBezug(
			Set.of(),
			Arrays.stream(kanten).map(kante -> new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.5),
				Seitenbezug.LINKS)).collect(Collectors.toSet()),
			Set.of());
	}

	public static MassnahmeNetzBezug forKnoten(Knoten... knoten) {
		return new MassnahmeNetzBezug(
			Set.of(),
			Set.of(),
			Arrays.stream(knoten).collect(Collectors.toSet()));
	}
}
