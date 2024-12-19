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

package de.wps.radvis.backend.netz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.netz.domain.LineareReferenzenDefragmentierungJob;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.repository.FahrtrichtungAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.FuehrungsformAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.GeschwindigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.repository.ZustaendigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.NetzklassenStreckenViewService;
import de.wps.radvis.backend.netz.domain.service.SackgassenService;
import de.wps.radvis.backend.netz.domain.service.StreckenViewService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.netz.schnittstelle.NetzGuard;
import de.wps.radvis.backend.netz.schnittstelle.NetzToFeatureDetailsConverter;
import de.wps.radvis.backend.netz.schnittstelle.SaveKanteCommandConverter;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.NonNull;

@Configuration
@EnableEnversRepositories
@EntityScan
public class NetzConfiguration {

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private KnotenRepository knotenRepository;

	@Autowired
	private ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppeRepository;

	@Autowired
	private FahrtrichtungAttributGruppeRepository fahrtrichtungAttributGruppeRepository;

	@Autowired
	private GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository;

	@Autowired
	private FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppeRepository;

	@Autowired
	private KantenAttributGruppeRepository kantenAttributGruppeRepository;

	@Autowired
	private OrganisationConfigurationProperties organisationConfigurationProperties;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@PersistenceContext
	EntityManager entityManager;

	private final VerwaltungseinheitResolver verwaltungseinheitResolver;

	private final BenutzerResolver benutzerResolver;

	@Autowired
	NetzConfigurationProperties netzConfigurationProperties;

	@Autowired
	CommonConfigurationProperties commonConfigurationProperties;

	public NetzConfiguration(@NonNull VerwaltungseinheitResolver verwaltungseinheitResolver,
		@NonNull BenutzerResolver benutzerResolver) {
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
	}

	@Bean
	public NetzService netzService() {
		return new NetzService(kantenRepository, knotenRepository, zustaendigkeitAttributGruppeRepository,
			fahrtrichtungAttributGruppeRepository, geschwindigkeitAttributGruppeRepository,
			fuehrungsformAttributGruppeRepository, kantenAttributGruppeRepository, verwaltungseinheitResolver,
			entityManager, commonConfigurationProperties.getErlaubteAbweichungFuerKnotenNetzbezugRematch());
	}

	@Bean
	public StreckenViewService streckenViewService() {
		return new StreckenViewService();
	}

	@Bean
	public NetzklassenStreckenViewService netzklassenStreckenViewService() {
		return new NetzklassenStreckenViewService();
	}

	@Bean
	public SaveKanteCommandConverter saveKanteCommandConverter() {
		return new SaveKanteCommandConverter(verwaltungseinheitResolver);
	}

	@Bean
	public NetzToFeatureDetailsConverter netzToFeatureDetailsConverter() {
		return new NetzToFeatureDetailsConverter();
	}

	@Bean
	public NetzGuard netzAutorisierungsService() {
		return new NetzGuard(netzService(), zustaendigkeitsService(), benutzerResolver);
	}

	@Bean
	public ZustaendigkeitsService zustaendigkeitsService() {
		return new ZustaendigkeitsService(organisationConfigurationProperties);
	}

	@Bean
	public SackgassenService sackgassenService() {
		return new SackgassenService(netzService(), kantenRepository);
	}

	@Bean
	public LineareReferenzenDefragmentierungJob lineareReferenzenDefragmentierungJob() {
		return new LineareReferenzenDefragmentierungJob(jobExecutionDescriptionRepository,
			zustaendigkeitAttributGruppeRepository, fuehrungsformAttributGruppeRepository,
			geschwindigkeitAttributGruppeRepository, kantenRepository,
			netzConfigurationProperties.getMinimaleSegmentLaenge());
	}
}
