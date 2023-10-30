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

import static de.wps.radvis.backend.konsistenz.regeln.domain.RegelGruppe.MINDESTBREITEN;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.jdbc.core.JdbcTemplate;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MindestbreiteKonsistenzregel implements Konsistenzregel {
	public static String VERLETZUNGS_TYP = "MINDESTBREITE_NICHT_EINGEHALTEN";
	public static String TITEL = "Mindestbreite nicht eingehalten";

	private final JdbcTemplate jdbcTemplate;

	public MindestbreiteKonsistenzregel(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<KonsistenzregelVerletzungsDetails> pruefen() {
		List<Map<String, Object>> konsistenzVerletzungenLinks = jdbcTemplate.queryForList(
			" SELECT k.id, kagis.standard, fa.fahrtrichtung_links, fu.radverkehrsfuehrung, fu.von, fu.bis, fu.breite, mk.breite as mindestbreite, ST_AsText(k.geometry) AS geometry"
				+ " FROM kante k"
				+ " JOIN kanten_attribut_gruppe_ist_standards kagis ON kanten_attribut_gruppe_id=kanten_attributgruppe_id"
				+ " JOIN fahrtrichtung_attribut_gruppe fa ON fa.id=k.fahrtrichtung_attributgruppe_id"
				+ " JOIN fuehrungsform_attribut_gruppe_attribute_links AS fu ON fu.fuehrungsform_attribut_gruppe_id=k.fuehrungsform_attribut_gruppe_id"
				+ " JOIN mindestbreite_konsistenzregel AS mk ON"
				+ " mk.ist_standard = kagis.standard"
				+ " AND mk.radverkehrsfuehrung = fu.radverkehrsfuehrung"
				+ " AND mk.fahrtrichtung = fa.fahrtrichtung_links"
				+ " WHERE (k.quelle='DLM' OR k.quelle='RadVis')"
				+ " AND fu.breite IS NOT NULL"
				+ " AND fu.breite < mk.breite");

		List<Map<String, Object>> konsistenzVerletzungenRechts = jdbcTemplate.queryForList(
			" SELECT k.id, kagis.standard, fa.fahrtrichtung_rechts, fu.radverkehrsfuehrung, fu.von, fu.bis, fu.breite, mk.breite as mindestbreite, ST_AsText(k.geometry) AS geometry"
				+ " FROM kante k"
				+ " JOIN kanten_attribut_gruppe_ist_standards kagis ON kanten_attribut_gruppe_id=kanten_attributgruppe_id"
				+ " JOIN fahrtrichtung_attribut_gruppe fa ON fa.id=k.fahrtrichtung_attributgruppe_id"
				+ " JOIN fuehrungsform_attribut_gruppe_attribute_rechts AS fu ON fu.fuehrungsform_attribut_gruppe_id=k.fuehrungsform_attribut_gruppe_id"
				+ " JOIN mindestbreite_konsistenzregel AS mk ON"
				+ " mk.ist_standard = kagis.standard"
				+ " AND mk.radverkehrsfuehrung = fu.radverkehrsfuehrung"
				+ " AND mk.fahrtrichtung = fa.fahrtrichtung_rechts"
				+ " WHERE (k.quelle='DLM' OR k.quelle='RadVis')"
				+ " AND k.is_zweiseitig IS TRUE"
				+ " AND fu.breite IS NOT NULL"
				+ " AND fu.breite < mk.breite");

		return Stream.concat(konsistenzVerletzungenLinks.stream(), konsistenzVerletzungenRechts.stream())
			.map(this::toKonsistenzregelVerletzungsDetails)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private KonsistenzregelVerletzungsDetails toKonsistenzregelVerletzungsDetails(Map<String, Object> result) {
		Long id = (Long) result.get("id");
		BigDecimal breite = (BigDecimal) result.get("breite");
		Double von = (Double) result.get("von");
		Double bis = (Double) result.get("bis");
		String geometryAsWkt = (String) result.get("geometry");

		String fahrtrichtungLinks = (String) Optional.ofNullable(result.get("fahrtrichtung_links")).orElse("");
		String fahrtrichtungRechts = (String) Optional.ofNullable(result.get("fahrtrichtung_rechts")).orElse("");

		String radverkehrsfuehrung = (String) result.get("radverkehrsfuehrung");

		String standard = (String) result.get("standard");

		BigDecimal mindestbreite = (BigDecimal) result.get("mindestbreite");

		WKTReader wktReader = new WKTReader(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory());
		Geometry geometry;
		try {
			geometry = wktReader.read(geometryAsWkt);
		} catch (ParseException e) {
			log.error(
				String.format("Parsen von WKT-String für Konsistenzregel %s fehlgeschlagen: %s\nWKT-String: %s",
					this.getVerletzungsTyp(), e.getMessage(), geometryAsWkt));
			return null;
		}

		return new KonsistenzregelVerletzungsDetails(geometry.getCentroid(), geometry,
			String.format(
				Locale.GERMANY,
				"Die Breite ist %.2f m und somit wird die Mindestbreite von %.2f m nicht eingehalten.",
				breite, mindestbreite),
			String.format(
				Locale.GERMANY,
				"{id:\"%d\",standard:\"%s\",fahrtrichtung_links:\"%s\",fahrtrichtung_rechts:\"%s\",radverkehrsfuehrung:\"%s\",von:\"%f\",bis:\"%f\"}",
				id, standard, fahrtrichtungLinks, fahrtrichtungRechts, radverkehrsfuehrung, von, bis));
	}

	@Override
	public String getVerletzungsTyp() {
		return VERLETZUNGS_TYP;
	}

	@Override
	public String getTitel() {
		return TITEL;
	}

	@Override
	public RegelGruppe getGruppe() {
		return MINDESTBREITEN;
	}
}
