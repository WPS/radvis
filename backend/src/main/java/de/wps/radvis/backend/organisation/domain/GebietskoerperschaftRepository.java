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

package de.wps.radvis.backend.organisation.domain;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;

public interface GebietskoerperschaftRepository extends Repository<Gebietskoerperschaft, Long> {
	Optional<Gebietskoerperschaft> findById(Long id);

	Iterable<Gebietskoerperschaft> saveAll(Iterable<Gebietskoerperschaft> gebietskoerperschaften);

	Gebietskoerperschaft save(Gebietskoerperschaft gebietskoerperschaft);

	Optional<Gebietskoerperschaft> findByName(String name);

	Optional<Gebietskoerperschaft> findByNameAndOrganisationsArt(String s, OrganisationsArt bundesland);
}
