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

package de.wps.radvis.backend.netz.schnittstelle.view;

import java.util.List;
import java.util.stream.Collectors;

import de.wps.radvis.backend.netz.domain.entity.AbstractNetzBezug;
import lombok.Getter;

@Getter
public class NetzbezugView {

	private final List<AbschnittsweiserKantenSeitenBezugView> kantenBezug;
	private final List<PunktuellerKantenSeitenBezugView> punktuellerKantenBezug;
	private final List<KnotenNetzbezugView> knotenBezug;

	public NetzbezugView(AbstractNetzBezug netzbezug) {
		this.kantenBezug = netzbezug.getImmutableKantenAbschnittBezug().stream()
			.map(AKSB -> new AbschnittsweiserKantenSeitenBezugView(AKSB.getKante().getId(), AKSB.getKante()
				.getGeometry(), AKSB.getSeitenbezug(), AKSB.getLinearReferenzierterAbschnitt()))
			.collect(Collectors.toList());
		this.punktuellerKantenBezug = netzbezug.getImmutableKantenPunktBezug().stream()
			.map(PKSB -> new PunktuellerKantenSeitenBezugView(PKSB.getKante().getId(), PKSB.getKante()
				.getGeometry(), PKSB.getSeitenbezug(), PKSB.getLineareReferenz()))
			.collect(Collectors.toList());
		this.knotenBezug = netzbezug.getImmutableKnotenBezug().stream()
			.map(KnotenNetzbezugView::new).collect(Collectors.toList());
	}
}
