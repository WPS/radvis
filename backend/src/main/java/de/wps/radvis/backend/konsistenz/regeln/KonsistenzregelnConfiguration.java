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

package de.wps.radvis.backend.konsistenz.regeln;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import de.wps.radvis.backend.konsistenz.regeln.domain.BeschilderungRadNETZKonsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.FahrtrichtungKonsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.FehlendeVernetzungKonsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.KommunalNetzLueckeKonsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.konsistenz.regeln.domain.KreisNetzLueckeKonsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.KurzeDlmKantenKonsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.MindestbreiteKonsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.RadNETZLueckeKonsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.RadNetzMassnahmenNetzklasseKonsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.StartstandardRadNETZKonsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.ZielstandardRadNETZKonsistenzregel;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzklassenSackgassenService;

@Configuration
@EnableJpaRepositories
@EntityScan
public class KonsistenzregelnConfiguration {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private KonsistenzregelnConfigurationProperties konsistenzregelnConfigurationProperties;

	@Autowired
	private NetzklassenSackgassenService netzklassenSackgassenService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private KantenRepository kantenRepository;

	@Bean
	public RadNETZLueckeKonsistenzregel radNETZLueckeKonsistenzregel() {
		return new RadNETZLueckeKonsistenzregel(netzklassenSackgassenService);
	}

	@Bean
	public KommunalNetzLueckeKonsistenzregel kommunalNetzLueckeKonsistenzregel() {
		return new KommunalNetzLueckeKonsistenzregel(netzklassenSackgassenService);
	}

	@Bean
	public KreisNetzLueckeKonsistenzregel kreisNetzLueckeKonsistenzregel() {
		return new KreisNetzLueckeKonsistenzregel(netzklassenSackgassenService);
	}

	@Bean
	public KurzeDlmKantenKonsistenzregel kurzeDlmKantenKonsistenzregel() {
		return new KurzeDlmKantenKonsistenzregel(kantenRepository);
	}

	@Bean
	public FehlendeVernetzungKonsistenzregel fehlendeVernetzungKonsistenzregel() {
		return new FehlendeVernetzungKonsistenzregel(jdbcTemplate);
	}

	@Bean
	public FahrtrichtungKonsistenzregel fahrtrichtungKonsistenzregel() {
		return new FahrtrichtungKonsistenzregel(kantenRepository);
	}

	@Bean
	public RadNetzMassnahmenNetzklasseKonsistenzregel netzMassnahmenNetzklasseKonsistenzregel() {
		return new RadNetzMassnahmenNetzklasseKonsistenzregel(jdbcTemplate);
	}

	@Bean
	public MindestbreiteKonsistenzregel mindestbreiteKonsistenzregel() {
		return new MindestbreiteKonsistenzregel(jdbcTemplate);
	}

	@Bean
	public BeschilderungRadNETZKonsistenzregel beschilderungRoutenverlaufKonsistenzregel() {
		return new BeschilderungRadNETZKonsistenzregel(entityManager,
			konsistenzregelnConfigurationProperties.getBeschilderungMaxEntfernungVonRoute());
	}

	@Bean
	public StartstandardRadNETZKonsistenzregel startstandardRadNETZKonsistenzregel() {
		return new StartstandardRadNETZKonsistenzregel(jdbcTemplate);
	}

	@Bean
	public ZielstandardRadNETZKonsistenzregel zielstandardRadNETZKonsistenzregel() {
		return new ZielstandardRadNETZKonsistenzregel(jdbcTemplate);
	}
}
