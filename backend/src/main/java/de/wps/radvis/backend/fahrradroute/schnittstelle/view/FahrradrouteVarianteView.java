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

package de.wps.radvis.backend.fahrradroute.schnittstelle.view;

import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteVariante;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.VarianteKategorie;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import lombok.Getter;

@Getter
public class FahrradrouteVarianteView {
	private final Long id;
	private final VarianteKategorie kategorie;
	private final Geometry stuetzpunkte;
	private final List<Long> kantenIDs;
	private final Geometry geometrie;
	private final List<LinearReferenzierteProfilEigenschaftenView> profilEigenschaften;
	private final List<AbschnittsweiserKantenBezugView> kantenBezug;
	private final Long customProfileId;

	public FahrradrouteVarianteView(FahrradrouteVariante fahrradrouteVariante) {
		id = fahrradrouteVariante.getId();
		kategorie = fahrradrouteVariante.getKategorie();
		stuetzpunkte = fahrradrouteVariante.getStuetzpunkte().orElse(null);
		kantenIDs = fahrradrouteVariante.getAbschnittsweiserKantenBezug().stream()
			.map(bezug -> bezug.getKante().getId()).collect(Collectors.toList());
		this.kantenBezug = fahrradrouteVariante.getAbschnittsweiserKantenBezug().stream()
			.map(AKB -> new AbschnittsweiserKantenBezugView(AKB.getKante()
				.getId(), AKB.getKante().getGeometry(), AKB.getLinearReferenzierterAbschnitt()))
			.collect(Collectors.toList());
		geometrie = fahrradrouteVariante.getGeometrie().orElse(null);
		profilEigenschaften = fahrradrouteVariante.getLinearReferenzierteProfilEigenschaften().stream()
			.map(LinearReferenzierteProfilEigenschaftenView::new)
			.collect(Collectors.toList());
		customProfileId = fahrradrouteVariante.getCustomProfileId().orElse(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID);
	}
}
