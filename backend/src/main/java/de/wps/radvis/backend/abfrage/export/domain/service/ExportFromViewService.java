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

package de.wps.radvis.backend.abfrage.export.domain.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.query.NativeQuery;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.RamUsageUtility;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

public class ExportFromViewService {

	private final EntityManager entityManager;

	public ExportFromViewService(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Map<String, List<ExportData>> exportBalm() {
		RamUsageUtility.logCurrentRamUsage("Vor dem Bauen der ExportData-Listen");
		List<ExportData> exportDataFahrradrouten = this.exportFromView("geoserver_balm_fahrradrouten_view", "geometry");
		RamUsageUtility.logCurrentRamUsage("Nach Fahrradrouten");
		List<ExportData> exportDataKanten = this.exportFromView("geoserver_balm_kanten_view", "GeometrieAbschnitt");
		RamUsageUtility.logCurrentRamUsage("Nach Kanten");
		List<ExportData> exportDataKnoten = this.exportFromView("geoserver_balm_knoten_view", "GeometrieKnoten");
		RamUsageUtility.logCurrentRamUsage("Nach Knoten");
		List<ExportData> exportDataBeschilderung = this.exportFromView("geoserver_balm_wegweisende_beschilderung_view",
			"GeometrieKnoten");
		RamUsageUtility.logCurrentRamUsage("Nach Beschilderung");

		return Map.of("Kanten", exportDataKanten,
			"Knoten", exportDataKnoten,
			"Fahrradrouten", exportDataFahrradrouten,
			"WegweisendeBeschilderung", exportDataBeschilderung);
	}

	private List<ExportData> exportFromView(String view, String geomColumn) {
		@SuppressWarnings("unchecked")
		List<String> columns = ((List<String>) entityManager.createNativeQuery("""
				SELECT column_name
				FROM information_schema.columns
				WHERE table_name  ='""" + view + "'", String.class)
			.getResultList());

		@SuppressWarnings("unchecked")
		NativeQuery<Tuple> query = entityManager.createNativeQuery(
				"SELECT * FROM " + view,
				Tuple.class)
			// Das müssen wir machen, damit wir die Geometrie nicht händisch zusammenbauen müssen.
			.unwrap(NativeQuery.class)
			.addScalar(geomColumn, Geometry.class);

		columns.forEach(query::addScalar);

		return query.getResultStream()
			.map(tuple -> {
				Map<String, String> props = new HashMap<>();
				tuple.getElements().stream()
					.filter(tupleElement -> !tupleElement.getAlias().equals(geomColumn))
					.forEach(tupleElement ->
						props.put(tupleElement.getAlias(),
							Objects.nonNull(tuple.get(tupleElement)) ? tuple.get(tupleElement).toString() : null)
					);
				return new ExportData(tuple.get(geomColumn, Geometry.class), props);
			}).toList();
	}
}
