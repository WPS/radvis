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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import de.wps.radvis.backend.netz.domain.service.SackgassenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
@EnableJpaRepositories
@EntityScan
public class KonsistenzregelnConfiguration {
	public static final String CONFIG_PREFIX = "radvis.konsistenzregeln.regeln-mit-explizitem-status";

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private KonsistenzregelnConfigurationProperties konsistenzregelnConfigurationProperties;

	@Autowired
	private SackgassenService sackgassenService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private KantenRepository kantenRepository;

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "radnetz-luecke", havingValue = "true", matchIfMissing = true)
	public RadNETZLueckeKonsistenzregel radNETZLueckeKonsistenzregel() {
		return new RadNETZLueckeKonsistenzregel(sackgassenService);
	}

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "kommunal-netz-luecke", havingValue = "true", matchIfMissing = true)
	public KommunalNetzLueckeKonsistenzregel kommunalNetzLueckeKonsistenzregel() {
		return new KommunalNetzLueckeKonsistenzregel(sackgassenService);
	}

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "kreis-netz-luecke", havingValue = "true", matchIfMissing = true)
	public KreisNetzLueckeKonsistenzregel kreisNetzLueckeKonsistenzregel() {
		return new KreisNetzLueckeKonsistenzregel(sackgassenService);
	}

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "kurze-dlm-kanten", havingValue = "true", matchIfMissing = true)
	public KurzeDlmKantenKonsistenzregel kurzeDlmKantenKonsistenzregel() {
		return new KurzeDlmKantenKonsistenzregel(kantenRepository);
	}

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "fehlende-vernetzung", havingValue = "true", matchIfMissing = true)
	public FehlendeVernetzungKonsistenzregel fehlendeVernetzungKonsistenzregel() {
		return new FehlendeVernetzungKonsistenzregel(jdbcTemplate);
	}

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "fahrtrichtung", havingValue = "true", matchIfMissing = true)
	public FahrtrichtungKonsistenzregel fahrtrichtungKonsistenzregel() {
		return new FahrtrichtungKonsistenzregel(kantenRepository);
	}

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "netz-massnahmen-netzklasse", havingValue = "true", matchIfMissing = true)
	public RadNetzMassnahmenNetzklasseKonsistenzregel netzMassnahmenNetzklasseKonsistenzregel() {
		return new RadNetzMassnahmenNetzklasseKonsistenzregel(jdbcTemplate);
	}

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "mindestbreite", havingValue = "true", matchIfMissing = true)
	public MindestbreiteKonsistenzregel mindestbreiteKonsistenzregel() {
		return new MindestbreiteKonsistenzregel(jdbcTemplate);
	}

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "beschilderung-routenverlauf", havingValue = "true", matchIfMissing = true)
	public BeschilderungRadNETZKonsistenzregel beschilderungRoutenverlaufKonsistenzregel() {
		return new BeschilderungRadNETZKonsistenzregel(entityManager,
			konsistenzregelnConfigurationProperties.getBeschilderungMaxEntfernungVonRoute());
	}

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "startstandard-radnetz", havingValue = "true", matchIfMissing = true)
	public StartstandardRadNETZKonsistenzregel startstandardRadNETZKonsistenzregel() {
		return new StartstandardRadNETZKonsistenzregel(jdbcTemplate);
	}

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "zielstandard-radnetz", havingValue = "true", matchIfMissing = true)
	public ZielstandardRadNETZKonsistenzregel zielstandardRadNETZKonsistenzregel() {
		return new ZielstandardRadNETZKonsistenzregel(jdbcTemplate);
	}
}
