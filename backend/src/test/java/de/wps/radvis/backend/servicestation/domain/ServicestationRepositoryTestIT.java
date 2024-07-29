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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.dokument.domain.entity.provider.DokumentTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportRepository;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.servicestation.ServicestationConfiguration;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import de.wps.radvis.backend.servicestation.domain.entity.provider.ServicestationTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group1")
@ContextConfiguration(classes = {
	ServicestationConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
	DokumentConfiguration.class,
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
})
@MockBeans({
	@MockBean(VerwaltungseinheitImportRepository.class),
	@MockBean(ZustaendigkeitsService.class),
	@MockBean(MailService.class),
})
class ServicestationRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	private ServicestationRepository servicestationRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	private BenutzerRepository benutzerRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void testSaveAndGet() {
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.build());

		Servicestation neueServicestation = ServicestationTestDataProvider.withDefaultValues()
			.organisation(gebietskoerperschaft).build();
		Servicestation servicestationSaved = servicestationRepository.save(neueServicestation);

		entityManager.flush();
		entityManager.clear();

		Optional<Servicestation> servicestationAusRepo = servicestationRepository.findById(servicestationSaved.getId());

		assertThat(servicestationAusRepo).isPresent();
		assertThat(servicestationAusRepo.get())
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringFields("id")
			.isEqualTo(neueServicestation);
	}

	@Test
	void testDokumente() {
		// arrange
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.build());

		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.defaultBenutzer()
			.organisation(gebietskoerperschaft)
			.build());

		Dokument bestehendesDokument = DokumentTestDataProvider.withDefaultValues().benutzer(benutzer)
			.dateiname("bestehendes Dokument")
			.build();

		DokumentListe dokumentListe = new DokumentListe();
		dokumentListe.addDokument(bestehendesDokument);
		Servicestation neueServicestation = ServicestationTestDataProvider.withDefaultValues()
			.organisation(gebietskoerperschaft).dokumentListe(dokumentListe).build();
		Servicestation servicestationSaved = servicestationRepository.save(neueServicestation);

		entityManager.flush();
		entityManager.clear();

		Optional<Servicestation> servicestationAusRepo = servicestationRepository.findById(servicestationSaved.getId());

		// act 1
		Dokument neuerDokument = DokumentTestDataProvider.withDefaultValues().dateiname("neues Dokument")
			.benutzer(benutzer).build();
		servicestationAusRepo.get().addDokument(neuerDokument);

		entityManager.flush();
		entityManager.clear();

		// assert 1
		Optional<Servicestation> servicestationAusRepoMitNeuerDatei = servicestationRepository.findById(
			servicestationSaved.getId());

		assertThat(servicestationAusRepoMitNeuerDatei.get().getDokumentListe().getDokumente()).containsExactly(
			bestehendesDokument, neuerDokument);

		// act 2
		servicestationAusRepoMitNeuerDatei.get().deleteDokument(bestehendesDokument.getId());
		assertThat(servicestationAusRepoMitNeuerDatei.get().getDokumentListe().getDokumente()).containsExactly(
			neuerDokument);
	}
}
