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

package de.wps.radvis.backend.fahrradroute.domain.repository;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomFahrradrouteRepositoryImpl implements CustomFahrradrouteRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public void resetGeoserverFahrradrouteImportDiffMaterializedView(int anzahlTageImportprotokolleVorhalten) {

		entityManager.createNativeQuery(
			"DROP MATERIALIZED VIEW IF EXISTS geoserver_fahrradroute_import_diff_materialized_view;")
			.executeUpdate();

		Instant queryStartZeit = Instant.now();
		log.info("Lege geoserver_fahrradroute_import_diff_materialized_view an...");

		entityManager.createNativeQuery(
			"CREATE MATERIALIZED VIEW geoserver_fahrradroute_import_diff_materialized_view AS "
				+ "SELECT r.job_execution_description_id            as job_id, "
				+ "       f_nachher.id                              as fahrradroute_id, "
				+ "       f_vorher.netzbezug_line_string            as geometrie_vorher, "
				+ "       COALESCE(st_difference(f_nachher.netzbezug_line_string, f_vorher.netzbezug_line_string), "
				+ "                f_nachher.netzbezug_line_string) as geometrie_diff "
				+ "FROM fahrradroute_aud as f_nachher, "
				+ "     fahrradroute_aud as f_vorher, "
				+ "     rev_info as r "
				+ "WHERE f_nachher.revtype = 1 "
				+ "  AND r.timestamp >= " +
				Instant.now().minus(anzahlTageImportprotokolleVorhalten, ChronoUnit.DAYS).toEpochMilli()
				+ "  AND r.id = f_nachher.rev "
				+ "  AND f_nachher.netzbezug_line_string IS NOT NULL "
				+ "  AND r.job_execution_description_id IS NOT NULL "
				+ "  AND f_vorher.id = f_nachher.id "
				+ "  AND f_vorher.rev = (SELECT max(f.rev) FROM fahrradroute_aud as f WHERE f.id = f_nachher.id AND f.rev < f_nachher.rev); ")
			.executeUpdate();

		log.info("Angelegt geoserver_fahrradroute_import_diff_materialized_view in {} Sekunden",
			Duration.between(queryStartZeit, Instant.now()).toSeconds());
	}
}
