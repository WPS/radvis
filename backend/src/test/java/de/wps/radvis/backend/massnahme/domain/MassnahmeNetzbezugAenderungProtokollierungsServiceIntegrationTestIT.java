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

package de.wps.radvis.backend.massnahme.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.MailConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.bezug.NetzBezugTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezugAenderung;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeNetzBezugAenderungRepository;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzBezugAenderungsArt;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;

@Tag("group4")
@ContextConfiguration(classes = {
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	NetzConfiguration.class,
	GeoConverterConfiguration.class,
	MatchingConfiguration.class,
	MassnahmeConfiguration.class,
	DokumentConfiguration.class,
	KommentarConfiguration.class,
	CommonConfiguration.class,
	MailConfiguration.class,
	BarriereConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	JobConfigurationProperties.class,
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	MailConfigurationProperties.class,
	UmsetzungsstandsabfrageConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	GraphhopperOsmConfigurationProperties.class,
	GraphhopperDlmConfigurationProperties.class,
	DLMConfigurationProperties.class,
	OsmPbfConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
class MassnahmeNetzbezugAenderungProtokollierungsServiceIntegrationTestIT extends DBIntegrationTestIT {

	private Verwaltungseinheit testVerwaltungseinheit;
	private Benutzer testBenutzer;
	private Kante kante;
	private Massnahme massnahme;

	@Autowired
	private MassnahmeService massnahmeService;

	@Autowired
	private MassnahmeNetzBezugAenderungRepository massnahmeNetzBezugAenderungRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private BenutzerService benutzerService;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@MockBean
	NetzfehlerRepository netzfehlerRepository;

	private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;

	@BeforeEach
	void setUp() {
		massnahmeNetzbezugAenderungProtokollierungsService = new MassnahmeNetzbezugAenderungProtokollierungsService(
			benutzerService,
			massnahmeNetzBezugAenderungRepository);

		testVerwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Coole Organisation")
				.organisationsArt(
					OrganisationsArt.BUNDESLAND).build());
		testBenutzer = benutzerService.save(BenutzerTestDataProvider.admin(testVerwaltungseinheit).serviceBwId(
			ServiceBwId.of("testBenutzer")).build());
		kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.zustaendiger(testVerwaltungseinheit)
			.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();
		gebietskoerperschaftRepository
			.save((Gebietskoerperschaft) massnahme.getBenutzerLetzteAenderung().getOrganisation());
		benutzerService.save(massnahme.getBenutzerLetzteAenderung());
		massnahme = massnahmeService.saveMassnahme(massnahme);
	}

	@Test
	void testGetAllMassnahmeNetzBezugAenderungenAfter() {
		// arrange
		LocalDateTime aktuellerZeitpunkt = LocalDateTime.of(2022, 8, 5, 12, 0);

		MassnahmeNetzBezugAenderung massnahmeNetzBezugAenderung1 = new MassnahmeNetzBezugAenderung(
			NetzBezugAenderungsArt.KANTE_GELOESCHT, 1L, massnahme, testBenutzer, aktuellerZeitpunkt.minusMinutes(60),
			NetzAenderungAusloeser.DLM_REIMPORT_JOB, GeometryTestdataProvider.createLineString());
		MassnahmeNetzBezugAenderung massnahmeNetzBezugAenderung2 = new MassnahmeNetzBezugAenderung(
			NetzBezugAenderungsArt.KNOTEN_GELOESCHT, 2L, massnahme,
			testBenutzer, aktuellerZeitpunkt.minusDays(7),
			NetzAenderungAusloeser.DLM_REIMPORT_JOB, GeometryTestdataProvider.createLineString());
		MassnahmeNetzBezugAenderung massnahmeNetzBezugAenderung3 = new MassnahmeNetzBezugAenderung(
			NetzBezugAenderungsArt.KANTE_VERAENDERT, 3L, massnahme,
			testBenutzer, aktuellerZeitpunkt,
			NetzAenderungAusloeser.DLM_REIMPORT_JOB, GeometryTestdataProvider.createLineString());

		massnahmeNetzBezugAenderungRepository.save(massnahmeNetzBezugAenderung1);
		massnahmeNetzBezugAenderungRepository.save(massnahmeNetzBezugAenderung2);
		massnahmeNetzBezugAenderungRepository.save(massnahmeNetzBezugAenderung3);

		// act
		List<MassnahmeNetzBezugAenderung> result = massnahmeNetzbezugAenderungProtokollierungsService
			.getAllMassnahmeNetzBezugAenderungenAfter(
				aktuellerZeitpunkt.minusDays(1));

		// assert
		assertThat(result).containsExactlyInAnyOrder(massnahmeNetzBezugAenderung1, massnahmeNetzBezugAenderung3);
	}

}
