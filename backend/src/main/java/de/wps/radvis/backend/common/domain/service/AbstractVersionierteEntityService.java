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

package de.wps.radvis.backend.common.domain.service;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;

public abstract class AbstractVersionierteEntityService<T extends VersionierteEntity> {
	private CrudRepository<T, Long> repository;

	protected AbstractVersionierteEntityService(CrudRepository<T, Long> repository) {
		this.repository = repository;
	}

	public T loadForModification(Long id, Long version) {
		require(version, notNullValue());
		require(id, notNullValue());

		T entity = get(id);

		if (!version.equals(entity.getVersion())) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}

		return entity;
	}

	protected T get(Long id) {
		return repository.findById(id).orElseThrow(EntityNotFoundException::new);
	}
}
