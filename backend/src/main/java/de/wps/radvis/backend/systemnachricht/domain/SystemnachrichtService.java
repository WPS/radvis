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

package de.wps.radvis.backend.systemnachricht.domain;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Optional;

import de.wps.radvis.backend.systemnachricht.domain.entity.Systemnachricht;
import jakarta.transaction.Transactional;

@Transactional
public class SystemnachrichtService {
	private final SystemnachrichtRepository systemnachrichtRepository;

	public SystemnachrichtService(SystemnachrichtRepository systemnachrichtRepository) {
		this.systemnachrichtRepository = systemnachrichtRepository;
	}

	public void delete() {
		systemnachrichtRepository.deleteAll();
	}

	public void create(String text) {
		Systemnachricht newSystemnachricht = new Systemnachricht(LocalDate.now(), text);
		delete();
		systemnachrichtRepository.save(newSystemnachricht);
	}

	public Optional<Systemnachricht> find() {
		Iterator<Systemnachricht> iterator = systemnachrichtRepository.findAll().iterator();
		if (iterator.hasNext()) {
			return Optional.of(iterator.next());
		}

		return Optional.empty();
	}
}
