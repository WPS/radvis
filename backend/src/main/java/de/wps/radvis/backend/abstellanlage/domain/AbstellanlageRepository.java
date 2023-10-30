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

package de.wps.radvis.backend.abstellanlage.domain;

import java.util.Optional;
import java.util.Set;

import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.ExterneAbstellanlagenId;

public interface AbstellanlageRepository extends CrudRepository<Abstellanlage, Long> {

	@Query(value = "Select * FROM abstellanlage WHERE quell_system = 'RADVIS' AND ST_DWithin(?1, geometrie, 1) LIMIT 1", nativeQuery = true)
	Optional<Abstellanlage> findByPositionAndQuellSystemRadVis(Point position);

	Optional<Abstellanlage> findByIdAndQuellSystem(Long id, AbstellanlagenQuellSystem quellsystem);

	Optional<Abstellanlage> findByExterneIdAndQuellSystem(ExterneAbstellanlagenId externeAbstellanlagenId,
		AbstellanlagenQuellSystem quellsystem);

	int deleteAllByQuellSystemAndExterneIdNotIn(AbstellanlagenQuellSystem quellSystem,
		Set<ExterneAbstellanlagenId> externeAbstellanlagenIds);
}
