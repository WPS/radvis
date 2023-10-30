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

package de.wps.radvis.backend.netzfehler.schnittstelle;

import org.geojson.Feature;
import org.geojson.FeatureCollection;

import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;

public class AnpassungswunschToGeoJsonConverter {

	public FeatureCollection convertAnpassungswuensche(Iterable<Anpassungswunsch> anpassungswuensche) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		anpassungswuensche.forEach(anpassungswunsch -> {
			Feature feature = GeoJsonConverter.createFeature(anpassungswunsch.getGeometrie());
			feature.setId(anpassungswunsch.getId().toString());
			feature.setProperty("beschreibung", anpassungswunsch.getBeschreibung());
			feature.setProperty("status", anpassungswunsch.getStatus());
			featureCollection.add(feature);
		});
		return featureCollection;
	}
}
