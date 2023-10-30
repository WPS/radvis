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

package de.wps.radvis.backend.konsistenz.regeln.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.postgresql.geometric.PGpoint;
import org.springframework.jdbc.core.JdbcTemplate;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FehlendeVernetzungKonsistenzregel implements Konsistenzregel {
	private final JdbcTemplate jdbcTemplate;

	public FehlendeVernetzungKonsistenzregel(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<KonsistenzregelVerletzungsDetails> pruefen() {
		String dlmKnoten = "SELECT von_knoten_id knoten_id FROM kante kant"
			+ " WHERE kant.quelle = 'DLM'"
			+ " UNION ALL"
			+ " SELECT nach_knoten_id knoten_id FROM kante kant"
			+ " WHERE kant.quelle = 'DLM'";

		String eindeutigeKnotenIDs = String.format(
			"SELECT knoten_id FROM (%s) AS dlmKnoten"
				+ " GROUP BY knoten_id"
				+ " HAVING COUNT(*) =1", dlmKnoten);

		String sql = String.format(
			"SELECT ka.id as kanteId, ka.dlm_id, knot.id as knotenId, ST_AsText(ka.geometry) AS kanteGeometry, point::Point"
				+ " FROM knoten knot, kante ka"
				+ " WHERE knot.ID IN (%s)"
				+ " AND ST_DWithin(knot.point, ka.geometry, 1)"
				+ " AND knot.id != von_knoten_id"
				+ " AND knot.id != nach_knoten_id"
				+ " AND ka.quelle='DLM'"
				+ " GROUP BY ka.id, knot.id, dlm_id, point;", eindeutigeKnotenIDs);

		List<Map<String, Object>> nichtVernetzteKanten = jdbcTemplate.queryForList(sql);

		return nichtVernetzteKanten.stream()
			.map(this::toKonsistenzregelVerletzungsDetails)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private KonsistenzregelVerletzungsDetails toKonsistenzregelVerletzungsDetails(Map<String, Object> result) {
		PGpoint dbpoint = (PGpoint) result.get("point");
		Point position = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createPoint(new Coordinate(dbpoint.x, dbpoint.y));

		String kanteGeometryAsWkt = (String) result.get("kanteGeometry");
		WKTReader wktReader = new WKTReader(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory());
		Geometry geometry;
		try {
			geometry = wktReader.read(kanteGeometryAsWkt);
		} catch (ParseException e) {
			log.error("Parsen von WKT-String für Konsistenzregel {} fehlgeschlagen: {}\nWKT-String: {}",
				this.getVerletzungsTyp(), e.getMessage(), kanteGeometryAsWkt);
			return null;
		}

		String kanteDlmId = (String) result.get("dlm_id");
		Long kanteId = (Long) result.get("kanteId");
		Long knotenId = (Long) result.get("knotenId");
		String beschreibung = createBeschreibung(kanteDlmId, kanteId, knotenId);

		return new KonsistenzregelVerletzungsDetails(position, geometry, beschreibung,
			knotenId + "/" + kanteId);
	}

	static String createBeschreibung(String kanteDlmId, Long kanteId, Long knotenId) {
		return String.format(
			"Der Knoten mit ID %s ist wahrscheinlich ein Kreuzungspunkt auf die Kante mit ID %s (DLM-ID: %s).",
			knotenId, kanteId, kanteDlmId);
	}

	@Override
	public String getVerletzungsTyp() {
		return "FEHLENDE_VERNETZUNG";
	}

	@Override
	public String getTitel() {
		return "Hinweis auf fehlende Vernetzung";
	}

	@Override
	public RegelGruppe getGruppe() {
		return RegelGruppe.DLM;
	}

}
