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

package de.wps.radvis.backend.manuellerimport.common.domain.repository;

import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public interface ManuellerImportFehlerRepository extends CrudRepository<ManuellerImportFehler, Long> {

	@Query("FROM ManuellerImportFehler f "
		+ "WHERE f.organisation=?1 "
		+ "AND f.importTyp=?2 "
		+ "AND f.importZeitpunkt=(SELECT max(importZeitpunkt) FROM ManuellerImportFehler f2 WHERE f2.organisation=?1 AND f2.importTyp=?2) "
		+ "AND intersects(CAST(f.iconPosition AS org.locationtech.jts.geom.Geometry), CAST(?3 AS org.locationtech.jts.geom.Geometry)) = true")
	List<ManuellerImportFehler> getAllLatestByOrganisationAndTypeInBereich(Verwaltungseinheit organisation,
		ImportTyp importTyp, Polygon bereich);

	@Query("SELECT mif FROM ManuellerImportFehler mif WHERE mif.kante.id IN :kantenIds")
	List<ManuellerImportFehler> findAllByKanteId(Collection<Long> kantenIds);

}
