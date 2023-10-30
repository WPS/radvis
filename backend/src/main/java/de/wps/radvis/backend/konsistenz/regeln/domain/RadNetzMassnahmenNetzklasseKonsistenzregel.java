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

import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.jdbc.core.JdbcTemplate;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RadNetzMassnahmenNetzklasseKonsistenzregel implements Konsistenzregel {
	private final JdbcTemplate jdbcTemplate;
	private final WKTReader wktReader;

	public RadNetzMassnahmenNetzklasseKonsistenzregel(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.wktReader = new WKTReader(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory());
	}

	@Override
	public List<KonsistenzregelVerletzungsDetails> pruefen() {
		String fromAlleRadNetzKanten = String.format(
			"FROM kante k"
				+ " LEFT OUTER JOIN kanten_attribut_gruppe_netzklassen nkl"
				+ " ON k.kanten_attributgruppe_id = nkl.kanten_attribut_gruppe_id"
				+ " WHERE nkl.netzklasse IN ('%s', '%s', '%s')",
			Netzklasse.RADNETZ_FREIZEIT.name(), Netzklasse.RADNETZ_ALLTAG.name(), Netzklasse.RADNETZ_ZIELNETZ.name());

		String alleRadNetzKnoten = String.format(
			"(SELECT distinct(von_knoten_id) %s) UNION (SELECT distinct(nach_knoten_id) %s)", fromAlleRadNetzKanten,
			fromAlleRadNetzKanten);

		String whereStatusAndKonzeptionsquelle = String.format(
			"WHERE m.konzeptionsquelle='%s'"
				+ " AND m.umsetzungsstatus!='%s'"
				+ " AND m.umsetzungsstatus!='%s' ",
			Konzeptionsquelle.RADNETZ_MASSNAHME.name(), Umsetzungsstatus.STORNIERT.name(),
			Umsetzungsstatus.UMGESETZT.name());

		String streckenmassnahmen = String.format(
			"SELECT distinct ON (m.id) m.id AS massnahme_id, ST_AsText(k.geometry) AS geometry, k.id AS bezug_id"
				+ " FROM massnahme_kantenseitenabschnitte mk"
				+ " JOIN kante k ON k.id=mk.kante_id"
				+ " JOIN massnahme m ON m.id=mk.massnahme_id %s AND k.id NOT IN (SELECT distinct(id) %s) ",
			whereStatusAndKonzeptionsquelle, fromAlleRadNetzKanten);

		String punktmassnahmen = String.format(
			"SELECT distinct ON (m.id) m.id AS massnahme_id, ST_AsText(kn.point) AS geometry, kn.id as bezug_id"
				+ " FROM massnahme_knoten mkn"
				+ " JOIN massnahme m ON m.id=mkn.massnahme_id"
				+ " JOIN knoten kn ON mkn.knoten_id=kn.id %s AND kn.id NOT IN (%s) ",
			whereStatusAndKonzeptionsquelle, alleRadNetzKnoten);

		String sql = streckenmassnahmen + " UNION " + punktmassnahmen;

		List<Map<String, Object>> massnahmenMitVerletzung = jdbcTemplate.queryForList(sql);

		return massnahmenMitVerletzung.stream()
			.map(this::toKonsistenzregelVerletzungsDetails)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	@Nullable
	private KonsistenzregelVerletzungsDetails toKonsistenzregelVerletzungsDetails(Map<String, Object> result) {
		String geometryAsWkt = (String) result.get("geometry");
		Geometry originalGeometry;
		try {
			originalGeometry = wktReader.read(geometryAsWkt);
		} catch (ParseException e) {
			log.error("Parsen von WKT-String für Konsistenzregel {} fehlgeschlagen: {}\nWKT-String: {}",
				this.getVerletzungsTyp(), e.getMessage(), geometryAsWkt);
			return null;
		}

		Point point;
		String beschreibung;
		Long massnahmeId = (Long) result.get("massnahme_id");
		if (Geometry.TYPENAME_POINT.equals(originalGeometry.getGeometryType())) {
			point = (Point) originalGeometry;
			beschreibung = createBeschreibungFuerKnotenmassnahme(massnahmeId,
				(Long) result.get("bezug_id"));
		} else if (Geometry.TYPENAME_LINESTRING.equals(originalGeometry.getGeometryType())) {
			point = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPoint(LineStrings.getMidPoint((LineString) originalGeometry));
			beschreibung = createBeschreibungFuerStreckenmassnahme(massnahmeId,
				(Long) result.get("bezug_id"));
		} else {
			if (originalGeometry.getCoordinates().length == 0) {
				log.error(
					"Konsistenzregel {}: Verortung der Maßnahme {} konnte nicht ermittelt werden, da Geometrie keine Koordinaten hat",
					getVerletzungsTyp(), massnahmeId);
				return null;
			}

			point = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPoint(originalGeometry.getCoordinates()[0]);
			beschreibung = createBeschreibungFuerInvalideMassnahme(massnahmeId, (Long) result.get("bezug_id"));
		}

		return new KonsistenzregelVerletzungsDetails(
			point, originalGeometry, beschreibung, massnahmeId.toString());
	}

	@Override
	public String getVerletzungsTyp() {
		return "RADNETZ_MASSNAHME_FALSCHE_NETZKLASSE";
	}

	@Override
	public String getTitel() {
		return "RadNETZ-Maßnahme ohne RadNETZ-Netzklasse";
	}

	@Override
	public RegelGruppe getGruppe() {
		return RegelGruppe.RADNETZ;
	}

	static String createBeschreibungFuerKnotenmassnahme(Long massnahmeId, Long knotenId) {
		return String.format(
			"Massnahme mit ID %s aus der Konzeptionsquelle RadNETZ bezieht sich auf mind. einen nicht zum RadNETZ gehörenden Knoten (ID %s)",
			massnahmeId, knotenId);
	}

	static String createBeschreibungFuerStreckenmassnahme(Long massnahmeId, Long kanteId) {
		return String.format(
			"Massnahme mit ID %s aus der Konzeptionsquelle RadNETZ bezieht sich auf mind. eine nicht zum RadNETZ gehörende Kante (ID %s)",
			massnahmeId, kanteId);
	}

	static String createBeschreibungFuerInvalideMassnahme(Long massnahmeId, Long kanteId) {
		return String.format(
			"Massnahme mit ID %s aus der Konzeptionsquelle RadNETZ bezieht sich auf ungültige Netz-Geometrie (ID %s)",
			massnahmeId, kanteId);
	}
}
