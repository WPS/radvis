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

package de.wps.radvis.backend.abfrage.netzausschnitt.domain;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzMapView;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;

public class RadNETZNetzViewCacheRepository extends StreckeViewCacheRepository<NetzMapView, StreckeVonKanten> {

	@Override
	public void reloadCache() {
		final var kantenMapViews = this.streckenVonKanten.stream().map(
			streckeVonKanten -> new KanteMapView(streckeVonKanten.getKanten().get(0).getId(),
				TopologyPreservingSimplifier
					.simplify(streckeVonKanten.getStrecke(), DISTANCE_TOLERANCE),
				false, true))
			.collect(Collectors.toSet());
		this.cache = new NetzMapView(kantenMapViews, new ArrayList<>());
	}

	@Override
	String getCacheName() {
		return "das RadNETZ";
	}

}
