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

package de.wps.radvis.backend.quellimport.common.domain;

import java.util.stream.Stream;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import jakarta.transaction.Transactional;

@Transactional
public interface ImportedFeaturePersistentRepository
	extends CrudRepository<ImportedFeature, Long>, CustomImportedFeaturePersistentRepository {

	String POINT = "ST_Point";
	String LINESTRING = "ST_LineString";
	String MULTILINESTRING = "ST_MultiLineString";

	Stream<ImportedFeature> getAllByQuelleAndArt(QuellSystem quelle, Art art);

	@Query("SELECT importedFeature "
		+ "FROM ImportedFeature importedFeature "
		+ "WHERE importedFeature.quelle = :quelle "
		+ "AND importedFeature.art = :art "
		+ "AND geometryType(CAST(importedFeature.geometrie AS org.locationtech.jts.geom.Geometry)) = :geometryType")
	Stream<ImportedFeature> getAllByQuelleAndArtAndGeometryType(QuellSystem quelle, Art art, String geometryType);
}
