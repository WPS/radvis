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

package de.wps.radvis.backend.servicestation.domain;

import java.util.Collection;
import java.util.Optional;

import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationName;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationenQuellSystem;

public interface ServicestationRepository extends CrudRepository<Servicestation, Long> {

	@Query(value = "Select * FROM servicestation WHERE quell_system = 'RADVIS' AND ST_DWithin(?1, geometrie, 1) LIMIT 1", nativeQuery = true)
	Optional<Servicestation> findByPositionAndQuellSystemRadvis(Point position);

	Optional<Servicestation> findByIdAndQuellSystem(Long id, ServicestationenQuellSystem quellsystem);

	@Query(value = """
		Select s FROM Servicestation s
		WHERE s.name = ?1 AND s.quellSystem = ?2
		AND	WITHIN(?3, BUFFER(s.geometrie, ?4)) = TRUE
		ORDER BY distance(?3, s.geometrie)
		""")
	Optional<Servicestation> findNearestByNameAndQuellSystemAndPosition(ServicestationName name,
		ServicestationenQuellSystem quellSystem, Point position, Double distanceWithin);

	int deleteByIdNotInAndQuellSystem(Collection<Long> namen, ServicestationenQuellSystem quellSystem);
}
