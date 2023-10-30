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

package de.wps.radvis.backend.massnahme.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezugAenderung;

public interface MassnahmeNetzBezugAenderungRepository extends CrudRepository<MassnahmeNetzBezugAenderung, Long> {

	List<MassnahmeNetzBezugAenderung> findMassnahmeNetzBezugAenderungByDatumAfter(LocalDateTime time);

	@Query("FROM MassnahmeNetzBezugAenderung m "
		+ "WHERE m.datum > ?1 "
		+ "AND intersects(CAST(m.geometry AS org.locationtech.jts.geom.Geometry), CAST(?2 AS org.locationtech.jts.geom.Geometry)) = true")
	List<MassnahmeNetzBezugAenderung> findMassnahmeNetzBezugAenderungByDatumAfterInBereich(LocalDateTime time,
		Polygon bereich);
}
