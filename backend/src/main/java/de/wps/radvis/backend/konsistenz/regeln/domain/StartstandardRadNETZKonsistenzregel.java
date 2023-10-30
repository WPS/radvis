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

import org.springframework.jdbc.core.JdbcTemplate;

import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StartstandardRadNETZKonsistenzregel extends AbstractStandardRadNETZKonsistenzregel {
	private final JdbcTemplate jdbcTemplate;

	public StartstandardRadNETZKonsistenzregel(JdbcTemplate jdbcTemplate) {
		super();
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<KonsistenzregelVerletzungsDetails> pruefen() {

		String radNETZ_AlltagOderZielnetz = "(ka.netzklassen LIKE '%RADNETZ_ALLTAG%' OR ka.netzklassen LIKE '%RADNETZ_ZIELNETZ%')";

		String alltagZielnetzTempoGroesser50 = radNETZ_AlltagOderZielnetz + " AND " + hoechsgeschwindigkeitGroesser(50);

		String alltagZielnetzTempo30_50_DTV5000_SV500 =
			radNETZ_AlltagOderZielnetz + " AND "
				+ hoechsgeschwindigkeitZwischen(30, 50) + " AND "
				+ "(dtv_pkw > 5000 OR sv > 500) ";

		String alltagZielnetzTempo20_30_DTV10000_SV800 =
			radNETZ_AlltagOderZielnetz + " AND "
				+ hoechsgeschwindigkeitZwischen(20, 30) + " AND "
				+ "(dtv_pkw > 10000 OR sv > 800) ";

		String alltagZielnetzTempoKleiner20_DTV12000_SV1000 =
			radNETZ_AlltagOderZielnetz + " AND "
				+ hoechsgeschwindigkeitKleinerGleich(20) + " AND "
				+ "(dtv_pkw > 12000 OR sv > 1000) ";

		String sql = "SELECT "
			+ "				ka.id, "
			+ "				ST_AsText(ka.geometry) AS geometry, "
			+ " 			CASE "
			//		 RadNETZ-Klasse enthält “Freizeit”
			+ " 				WHEN ka.netzklassen LIKE '%RADNETZ_FREIZEIT%'"
			+ "						THEN 'Verletzung der Konsistenzregel: “Startstandard: Wenn Strecke Landesradfernweg und Mischverkehr innerorts”'"

			//		 RadNETZ-Klasse enthält “Alltag” oder “Zielnetz” AND Höchstgeschwindigkeit > 50
			+ " 				WHEN " + alltagZielnetzTempoGroesser50
			+ "						THEN 'Verletzung der Konsistenzregel: “Startstandard: Wenn Strecke RadNETZ und Mischverkehr innerorts: Tempo <= 50“' "

			//		 RadNETZ-Klasse enthält “Alltag” oder “Zielnetz” AND Höchstgeschwindigkeit > 30 AND Höchstgeschwindigkeit <= 50 AND (DTV (PKW) > 5000 Fz/Tag OR SV > 500 Fz/Tag)
			+ " 				WHEN " + alltagZielnetzTempo30_50_DTV5000_SV500
			+ "						THEN 'Verletzung der Konsistenzregel: “Startstandard: Wenn Strecke RadNETZ und Mischverkehr innerorts und Tempo > 30 und Tempo <= 50: DTV <= 5000 Fz/Tag und SV <= 500 Fz/Tag“' "

			//		 RadNETZ-Klasse enthält “Alltag” oder “Zielnetz” AND Höchstgeschwindigkeit > 20 AND Höchstgeschwindigkeit <=30 AND (DTV > 10.000 Fz/Tag OR SV > 800 Fz/Tag)
			+ " 				WHEN " + alltagZielnetzTempo20_30_DTV10000_SV800
			+ "						THEN 'Verletzung der Konsistenzregel: “Startstandard: Wenn Strecke RadNETZ und Mischverkehr innerorts und Tempo > 20 und Tempo <= 30: DTV <= 10.000 Fz/Tag und SV <= 800 Fz/Tag“' "

			//		 RadNETZ-Klasse enthält “Alltag” oder “Zielnetz” AND Höchstgeschwindigkeit <= 20 AND (DTV > 12.000 Fz/Tag OR SV > 1.000 Fz/Tag)
			+ " 				WHEN " + alltagZielnetzTempoKleiner20_DTV12000_SV1000
			+ "						THEN 'Verletzung der Konsistenzregel: “Startstandard: Wenn Strecke RadNETZ und Mischverkehr innerorts und Tempo <= 20: DTV <= 12.000 Fz/Tag und SV <= 1.000 Fz/Tag“' "

			+ " 			END                AS text"
			+ " FROM " + KantenRepository.GEOSERVER_RADVISNETZ_ABSCHNITTE_MAT_VIEW_NAME + " ka "
			+ " WHERE ka.ortslage = 'INNERORTS' "
			+ "   AND ka.radverkehrsfuehrung IN (" + mischverkehr + ") "
			+ "   AND ka.standards LIKE '%STARTSTANDARD_RADNETZ%'"
			+ "   AND ( ka.netzklassen LIKE '%RADNETZ_FREIZEIT%' OR "
			+ "		" + alltagZielnetzTempoGroesser50 + " OR "
			+ "		" + alltagZielnetzTempo30_50_DTV5000_SV500 + " OR "
			+ "		" + alltagZielnetzTempo20_30_DTV10000_SV800 + " OR "
			+ "		" + alltagZielnetzTempoKleiner20_DTV12000_SV1000 + ")";

		List<Map<String, Object>> startstandartVerletzungen = jdbcTemplate.queryForList(sql);

		return startstandartVerletzungen.stream()
			.map(this::toKonsistenzregelVerletzungsDetails)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	@Override
	public String getVerletzungsTyp() {
		return "RADNETZ_STARTSTANDARD_NICHT_EINGEHALTEN";
	}

	@Override
	public String getTitel() {
		return "Startstandard trotz Kennzeichnung nicht eingehalten";
	}

	@Override
	public RegelGruppe getGruppe() {
		return RegelGruppe.RADNETZ;
	}

}
