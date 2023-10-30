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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;

public class ImportSessionRepositoryImpl implements ImportSessionRepository {

	private final Map<Benutzer, AbstractImportSession> map;

	public ImportSessionRepositoryImpl() {
		this.map = new ConcurrentHashMap<>();
	}

	@Override
	public void save(AbstractImportSession importSession) {
		require(importSession, notNullValue());
		require(importSession.getBenutzer(), notNullValue());

		this.map.put(importSession.getBenutzer(), importSession);

		ensure(exists(importSession.getBenutzer()));
	}

	@Override
	public void delete(Benutzer benutzer) {
		require(benutzer, notNullValue());
		require(exists(benutzer));

		this.map.remove(benutzer);

		ensure(!exists(benutzer));
	}

	@Override
	public boolean exists(Benutzer benutzer) {
		require(benutzer, notNullValue());

		return this.map.containsKey(benutzer);
	}

	@Override
	public Optional<AbstractImportSession> find(Benutzer benutzer) {
		require(benutzer, notNullValue());

		if (exists(benutzer)) {
			return Optional.of(this.map.get(benutzer));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public void clear() {
		this.map.clear();
	}
}
