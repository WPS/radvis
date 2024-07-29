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

package de.wps.radvis.backend.barriere.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.entity.BarriereNetzBezug;
import de.wps.radvis.backend.barriere.domain.entity.provider.BarriereTestDataProvider;
import de.wps.radvis.backend.barriere.domain.valueObject.BarriereBegruendung;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.MailConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;

@Tag("group4")
@ContextConfiguration(classes = {
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	NetzConfiguration.class,
	GeoConverterConfiguration.class,
	BarriereConfiguration.class,
	KommentarConfiguration.class,
	CommonConfiguration.class,
	MailConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	JobConfigurationProperties.class,
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	MailConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
class BarriereRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	private BarriereRepository barriereRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	private KnotenRepository knotenRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private EntityManager entityManager;

	private Verwaltungseinheit testVerwaltungseinheit;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		testVerwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Coole Organisation")
				.organisationsArt(
					OrganisationsArt.BUNDESLAND)
				.build());
	}

	@Test
	void erstelleMassnahme_massnahmeWirdErstellt() {
		// arrange
		Knoten knoten = knotenRepository.save(KnotenTestDataProvider.withDefaultValues().build());
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		final BarriereNetzBezug netzBezug = new BarriereNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS)),
			Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
			Set.of(knoten));

		Barriere barriere = BarriereTestDataProvider.withDefaultValues().begruendung(BarriereBegruendung.of("Test"))
			.verantwortlicheOrganisation(testVerwaltungseinheit).netzbezug(netzBezug).build();

		// act
		Barriere gespeicherteBarriere = barriereRepository.save(barriere);

		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(barriereRepository.findById(gespeicherteBarriere.getId()).get()).usingRecursiveComparison()
			.usingOverriddenEquals().ignoringFields("id")
			.isEqualTo(barriere);
	}
}
