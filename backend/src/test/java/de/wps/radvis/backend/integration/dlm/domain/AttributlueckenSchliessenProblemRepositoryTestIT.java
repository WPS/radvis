/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.integration.dlm.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.dlm.IntegrationDlmConfiguration;
import de.wps.radvis.backend.integration.dlm.domain.entity.AttributlueckenSchliessenProblem;
import de.wps.radvis.backend.integration.radnetz.IntegrationRadNetzConfiguration;
import de.wps.radvis.backend.integration.radwegedb.IntegrationRadwegeDBConfiguration;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.quellimport.common.ImportsCommonConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.ImportsGrundnetzConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;

@Tag("group6")
@ContextConfiguration(classes = {
	CommonConfiguration.class,
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	GeoConverterConfiguration.class,
	IntegrationDlmConfiguration.class,
	NetzConfiguration.class,
	NetzfehlerConfiguration.class,
	KommentarConfiguration.class,
	KonsistenzregelnConfiguration.class,
	KonsistenzregelPruefungsConfiguration.class,
	MatchingConfiguration.class,
	BarriereConfiguration.class,
	ImportsCommonConfiguration.class,
	ImportsGrundnetzConfiguration.class,
	IntegrationAttributAbbildungConfiguration.class,
	IntegrationRadNetzConfiguration.class,
	IntegrationRadwegeDBConfiguration.class,
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	FeatureToggleProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	GraphhopperOsmConfigurationProperties.class,
	DLMConfigurationProperties.class,
	NetzkorrekturConfigurationProperties.class,
	OsmPbfConfigurationProperties.class,
	GraphhopperDlmConfigurationProperties.class,
	GraphhopperOsmConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
class AttributlueckenSchliessenProblemRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	NetzService netzService;
	@Autowired
	AttributlueckenSchliessenProblemRepository attributlueckenSchliessenProblemRepository;

	@Test
	public void findAttributlueckenSchliessenProblemByDatumAfter() {
		// arrange

		Knoten knoten = netzService.saveKnoten(KnotenTestDataProvider.withDefaultValues().build());

		// alte Aenderung (soll nicht gefunden werden)
		AttributlueckenSchliessenProblem outdatedProblem = attributlueckenSchliessenProblemRepository.save(
			new AttributlueckenSchliessenProblem(
				LocalDateTime.of(2022, 10, 12, 10, 0),
				knoten));

		// aktuelle Aenderung
		AttributlueckenSchliessenProblem expectedProblem = attributlueckenSchliessenProblemRepository.save(
			new AttributlueckenSchliessenProblem(
				LocalDateTime.of(2022, 10, 12, 10, 1),
				knoten));

		// act
		List<AttributlueckenSchliessenProblem> result = attributlueckenSchliessenProblemRepository
			.findAttributlueckenSchliessenProblemByDatumAfter(LocalDateTime.of(2022, 10, 12, 10, 0));

		// assert
		assertThat(attributlueckenSchliessenProblemRepository.findAll()).hasSize(2);
		assertThat(result).hasSize(1);
		assertThat(result).contains(expectedProblem);
		assertThat(result).doesNotContain(outdatedProblem);
	}

	@Test
	public void findAttributlueckenSchliessenProblemByDatumAfterInBereich() {
		// arrange
		Knoten knoten = netzService.saveKnoten(KnotenTestDataProvider.withDefaultValues().build());

		// alte Aenderung (soll nicht gefunden werden)
		AttributlueckenSchliessenProblem outdatedProblem = attributlueckenSchliessenProblemRepository.save(
			new AttributlueckenSchliessenProblem(
				LocalDateTime.of(2022, 10, 12, 10, 0),
				knoten));

		// aktuelle Aenderung
		AttributlueckenSchliessenProblem expectedProblem = attributlueckenSchliessenProblemRepository.save(
			new AttributlueckenSchliessenProblem(
				LocalDateTime.of(2022, 10, 12, 10, 1),
				knoten));

		// act & assert (Netzbezugänderung außerhalb des Bereiches)
		List<AttributlueckenSchliessenProblem> result = attributlueckenSchliessenProblemRepository
			.findAttributlueckenSchliessenProblemByDatumAfterInBereich(
				LocalDateTime.of(2022, 10, 12, 10, 0),
				GeometryTestdataProvider.createQuadratischerBereichAsPolygon(10, 10, 20, 20));

		assertThat(attributlueckenSchliessenProblemRepository.findAll()).hasSize(2);
		assertThat(result).hasSize(0);

		// act & assert (Netzbezugänderung innerhalb des Bereiches)
		result = attributlueckenSchliessenProblemRepository
			.findAttributlueckenSchliessenProblemByDatumAfterInBereich(
				LocalDateTime.of(2022, 10, 12, 10, 0),
				GeometryTestdataProvider.createQuadratischerBereichAsPolygon(0, 0, 20, 20));

		assertThat(attributlueckenSchliessenProblemRepository.findAll()).hasSize(2);
		assertThat(result).hasSize(1);
		assertThat(result).contains(expectedProblem);
		assertThat(result).doesNotContain(outdatedProblem);
	}
}