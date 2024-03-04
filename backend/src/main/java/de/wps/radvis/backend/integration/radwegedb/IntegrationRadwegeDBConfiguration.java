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

package de.wps.radvis.backend.integration.radwegedb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBAttributMapper;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungProtokollService;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungService;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.NonNull;

@Configuration
public class IntegrationRadwegeDBConfiguration {
	@Autowired
	private NetzfehlerRepository netzfehlerRepository;

	@PersistenceContext
	private EntityManager entityManager;

	private final NetzService netzService;

	IntegrationRadwegeDBConfiguration(@NonNull NetzService netzService) {
		this.netzService = netzService;
	}

	@Bean
	public RadwegeDBNetzbildungService radwegeDBNetzbildungService() {
		return new RadwegeDBNetzbildungService(netzService, radwegeDBNetzbildungProtokollService(),
			RadwegeDBNetzbildungAttributmapper(),
			entityManager);
	}

	@Bean
	public RadwegeDBNetzbildungProtokollService radwegeDBNetzbildungProtokollService() {
		return new RadwegeDBNetzbildungProtokollService(netzfehlerRepository);
	}

	@Bean
	public RadwegeDBAttributMapper RadwegeDBNetzbildungAttributmapper() {
		return new RadwegeDBAttributMapper(radwegeDBNetzbildungProtokollService());
	}
}
