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

package de.wps.radvis.backend.massnahme.schnittstelle;

import java.util.List;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.schnittstelle.command.KnotenNetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.PunktuellerKantenSeitenBezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SeitenabschnittsKantenBezugCommand;

public class NetzbezugCommandTestDataProvider {
	public static NetzbezugCommand.NetzbezugCommandBuilder defaultNetzbezug() {
		return NetzbezugCommand.builder()
			.kantenBezug(List.of(new SeitenabschnittsKantenBezugCommand(200L, LinearReferenzierterAbschnitt.of(0, 1),
				Seitenbezug.BEIDSEITIG)))
			.punktuellerKantenBezug(
				List.of(new PunktuellerKantenSeitenBezugCommand(200L, 0.67D)))
			.knotenBezug(
				List.of(new KnotenNetzbezugCommand(32L)));
	}

	public static NetzbezugCommand.NetzbezugCommandBuilder withKante(Long id) {
		return NetzbezugCommand.builder()
			.kantenBezug(List.of(new SeitenabschnittsKantenBezugCommand(id, LinearReferenzierterAbschnitt.of(0, 1),
				Seitenbezug.BEIDSEITIG)))
			.punktuellerKantenBezug(
				List.of())
			.knotenBezug(
				List.of());
	}

	public static NetzbezugCommand.NetzbezugCommandBuilder withNetzbezuege(List<Long> kanteIds, List<Long> knotenIds,
		List<Long> punktuelleNetzbezugIds) {
		return NetzbezugCommand.builder()
			.kantenBezug(kanteIds.stream()
				.map(kanteId -> new SeitenabschnittsKantenBezugCommand(kanteId, LinearReferenzierterAbschnitt.of(0, 1),
					Seitenbezug.BEIDSEITIG))
				.collect(Collectors.toList()))
			.knotenBezug(knotenIds.stream().map(KnotenNetzbezugCommand::new).collect(Collectors.toList()))
			.punktuellerKantenBezug(
				punktuelleNetzbezugIds.stream().map(id -> new PunktuellerKantenSeitenBezugCommand(id, 123.0))
					.collect(Collectors.toList()));
	}
}
