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

import java.util.Map;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStandardRadNETZKonsistenzregel implements Konsistenzregel {

	private final WKTReader wktReader;

	public AbstractStandardRadNETZKonsistenzregel() {
		this.wktReader = new WKTReader(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory());
	}

	protected String mischverkehr = "'" + Radverkehrsfuehrung.mischverkehr().stream().map(Enum::name)
		.collect(Collectors.joining("','")) + "'";

	protected String hoechsgeschwindigkeitGroesser(int tempo) {
		return "(regexp_match(ka.hoechstgeschwindigkeit, '_(\\d+)_'))[1]::INTEGER > " + tempo + " ";
	}

	protected String hoechsgeschwindigkeitKleinerGleich(int tempo) {
		return "(regexp_match(ka.hoechstgeschwindigkeit, '_(\\d+)_'))[1]::INTEGER <= " + tempo + " ";
	}

	protected String hoechsgeschwindigkeitZwischen(int minTempoExclusive, int maxTempoInclusive) {
		return hoechsgeschwindigkeitGroesser(minTempoExclusive) + " AND " + hoechsgeschwindigkeitKleinerGleich(
			maxTempoInclusive);
	}

	protected KonsistenzregelVerletzungsDetails toKonsistenzregelVerletzungsDetails(Map<String, Object> result) {
		String geometryAsWkt = (String) result.get("geometry");
		Geometry originalGeometry;
		try {
			originalGeometry = wktReader.read(geometryAsWkt);
		} catch (ParseException e) {
			log.error("Parsen von WKT-String für Konsistenzregel {} fehlgeschlagen: {}\nWKT-String: {}",
				this.getVerletzungsTyp(), e.getMessage(), geometryAsWkt);
			return null;
		}

		Point point = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createPoint(LineStrings.getMidPoint((LineString) originalGeometry));
		String beschreibung = (String) result.get("text");

		String kanteIdMitLR = (String) result.get("id");

		return new KonsistenzregelVerletzungsDetails(
			point, originalGeometry, beschreibung, kanteIdMitLR);
	}
}
