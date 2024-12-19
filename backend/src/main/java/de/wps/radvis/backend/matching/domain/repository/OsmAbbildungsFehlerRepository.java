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

package de.wps.radvis.backend.matching.domain.repository;

import java.util.List;

import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.matching.domain.entity.OsmAbbildungsFehler;

public interface OsmAbbildungsFehlerRepository extends CrudRepository<OsmAbbildungsFehler, Long> {
	List<OsmAbbildungsFehler> findOsmAbbildungsFehlerByRadnetzIsTrue();

	List<OsmAbbildungsFehler> findOsmAbbildungsFehlerByKreisnetzIsTrue();

	List<OsmAbbildungsFehler> findOsmAbbildungsFehlerByKommunalnetzIsTrue();

	// Unklassifiertes Netz + Radvorrangrouten + Radschnellverbindungen
	List<OsmAbbildungsFehler> findOsmAbbildungsFehlerByRadnetzIsFalseAndKreisnetzIsFalseAndKommunalnetzIsFalse();

	@Query("FROM OsmAbbildungsFehler o "
		+ "WHERE intersects(CAST(o.originalGeometry AS org.locationtech.jts.geom.Geometry), CAST(?1 AS org.locationtech.jts.geom.Geometry)) = true "
		+ "AND o.radnetz = true")
	List<OsmAbbildungsFehler> findOsmAbbildungsFehlerInBereichRadNETZ(Polygon bereich);

	@Query("FROM OsmAbbildungsFehler o "
		+ "WHERE intersects(CAST(o.originalGeometry AS org.locationtech.jts.geom.Geometry), CAST(?1 AS org.locationtech.jts.geom.Geometry)) = true "
		+ "AND o.kreisnetz = true")
	List<OsmAbbildungsFehler> findOsmAbbildungsFehlerInBereichKreisnetz(Polygon bereich);

	@Query("FROM OsmAbbildungsFehler o "
		+ "WHERE intersects(CAST(o.originalGeometry AS org.locationtech.jts.geom.Geometry), CAST(?1 AS org.locationtech.jts.geom.Geometry)) = true "
		+ "AND o.kommunalnetz = true")
	List<OsmAbbildungsFehler> findOsmAbbildungsFehlerInBereichKommunalnetz(Polygon bereich);

	@Query("FROM OsmAbbildungsFehler o "
		+ "WHERE intersects(CAST(o.originalGeometry AS org.locationtech.jts.geom.Geometry), CAST(?1 AS org.locationtech.jts.geom.Geometry)) = true "
		+ "AND o.radnetz = false AND o.kreisnetz = false AND o.kommunalnetz = false")
	List<OsmAbbildungsFehler> findOsmAbbildungsFehlerInBereichSonstige(Polygon bereich);
}
