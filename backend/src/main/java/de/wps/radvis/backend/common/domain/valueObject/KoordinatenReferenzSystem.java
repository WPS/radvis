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

package de.wps.radvis.backend.common.domain.valueObject;

import static org.valid4j.Assertive.require;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.geojson.Crs;
import org.geojson.jackson.CrsType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KoordinatenReferenzSystem {

	private int srid;

	private GeometryFactory geometryFactory;

	private KoordinatenReferenzSystem(int srid) {
		this.geometryFactory = new GeometryFactory(new PrecisionModel(), srid);
		this.srid = srid;
	}

	private static Map<Integer, KoordinatenReferenzSystem> sridToReferenzSystem = new ConcurrentHashMap<>();

	public static KoordinatenReferenzSystem ETRS89_UTM32_N = new KoordinatenReferenzSystem(25832);
	public static KoordinatenReferenzSystem DHDN_3_Degree_Gauss_Zone_3 = new KoordinatenReferenzSystem(31463);
	public static KoordinatenReferenzSystem WGS84 = new KoordinatenReferenzSystem(4326);

	private static Map<String, KoordinatenReferenzSystem> knownCodeToReferenzSystem = new HashMap<>();

	static {
		knownCodeToReferenzSystem.put("dhdn_3_degree_gauss_zone_3", DHDN_3_Degree_Gauss_Zone_3);
		knownCodeToReferenzSystem.put("etrs_1989_utm_zone_n32", ETRS89_UTM32_N);
		knownCodeToReferenzSystem.put("etrs_1989_utm_zone_32n", ETRS89_UTM32_N);
		knownCodeToReferenzSystem.put("gcs_wgs_1984", WGS84);
	}

	public static boolean isValid(int srid) {
		try {
			convertToGeotoolsCrs(srid);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static KoordinatenReferenzSystem ofSrid(int srid) {
		require(isValid(srid));

		return sridToReferenzSystem.computeIfAbsent(srid, KoordinatenReferenzSystem::new);
	}

	public static KoordinatenReferenzSystem of(CoordinateReferenceSystem coordinateReferenceSystem)
		throws FactoryException {
		if (coordinateReferenceSystem.getName() == null)
			throw new FactoryException("coordinateReferenceSystem.getName() == null");
		String code = coordinateReferenceSystem.getName().getCode().toLowerCase();
		if (knownCodeToReferenzSystem.containsKey(code)) {
			return knownCodeToReferenzSystem.get(code);
		} else {
			log.info("SRID für CRS mit Code {} konnte lokal nicht gefunden werden. Suche über geotools gestartet.",
				code);
			Integer srid = CRS.lookupEpsgCode(coordinateReferenceSystem, false);
			if (srid == null) {
				throw new FactoryException(
					"Suche nach CRS " + coordinateReferenceSystem + " über geotools fehlgeschlagen");
			}

			KoordinatenReferenzSystem koordinatenReferenzSystem = KoordinatenReferenzSystem.ofSrid(srid);
			log.info("KoordinatenReferenzSystem für CRS mit Code {} wird temporär hinzugefügt.",
				code);
			knownCodeToReferenzSystem.put(code.toLowerCase(), koordinatenReferenzSystem);
			return koordinatenReferenzSystem;
		}
	}

	public static KoordinatenReferenzSystem of(Crs geoJsonCrs) throws FactoryException {
		CrsType type = geoJsonCrs.getType();
		if (!type.equals(CrsType.name)) {
			throw new FactoryException(
				String.format("Wir koennen nur mit CrsType `name` CRS umgehen. Nicht mit CrsType: `%s`", type));
		}

		String crsProperty = geoJsonCrs.getProperties().get("name").toString();
		CoordinateReferenceSystem coordinateReferenceSystem = CRS.decode(crsProperty);
		return KoordinatenReferenzSystem.of(coordinateReferenceSystem);
	}

	public GeometryFactory getGeometryFactory() {
		return this.geometryFactory;
	}

	public int getSrid() {
		return srid;
	}

	public CoordinateReferenceSystem getGeotoolsCRS() {
		try {
			return convertToGeotoolsCrs(this.srid);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
	}

	private static CoordinateReferenceSystem convertToGeotoolsCrs(int srid)
		throws NoSuchAuthorityCodeException, FactoryException {
		return CRS.decode("EPSG:" + srid);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + srid;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KoordinatenReferenzSystem other = (KoordinatenReferenzSystem) obj;
		if (srid != other.srid)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EPSG:" + this.srid;
	}
}
