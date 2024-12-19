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

package de.wps.radvis.backend.common.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;

public interface JobExecutionDescriptionRepository extends CrudRepository<JobExecutionDescription, Long> {

	Optional<JobExecutionDescription> findFirstByNameEqualsOrderByExecutionStartDesc(String name);

	@Query("FROM JobExecutionDescription WHERE name IN ?2 and executionStart > ?1 ORDER BY executionStart DESC")
	List<JobExecutionDescription> findAllByNameInAfterOrderByExecutionStartDesc(LocalDateTime after,
		List<String> names);

	List<JobExecutionDescription> findByExecutionStartGreaterThanEqualAndExecutionStartLessThan(
		LocalDateTime startDateTime, LocalDateTime endDateTime);
}
