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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.MailConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.dokument.domain.entity.provider.DokumentTestDataProvider;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
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
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import jakarta.persistence.EntityNotFoundException;

@Tag("group5")
@ContextConfiguration(classes = {
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	NetzConfiguration.class,
	GeoConverterConfiguration.class,
	MassnahmeConfiguration.class,
	DokumentConfiguration.class,
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
	UmsetzungsstandsabfrageConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
class MassnahmeServiceIntegrationTestIT extends DBIntegrationTestIT {

	private Verwaltungseinheit testVerwaltungseinheit;
	private Benutzer testBenutzer;

	@Autowired
	private MassnahmeService massnahmeService;

	@Autowired
	private MassnahmeRepository massnahmeRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	private BenutzerRepository benutzerRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private KnotenRepository knotenRepository;

	@MockBean
	private SimpleMatchingService simpleMatchingService;

	@MockBean
	private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;

	@BeforeEach
	void setUp() {
		testVerwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Coole Organisation")
				.organisationsArt(
					OrganisationsArt.BUNDESLAND).build());
		testBenutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(testVerwaltungseinheit).build());
	}

	@Test
	void testGetMassnahme_MassnahmeGeloescht_wirftEntityNotFound() {

		// arrange
		Knoten knoten = knotenRepository.save(KnotenTestDataProvider.withDefaultValues().build());
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1.),
					Seitenbezug.BEIDSEITIG)),
				Set.of(),
				Set.of(knoten)))
			.baulastZustaendiger(testVerwaltungseinheit)
			.unterhaltsZustaendiger(testVerwaltungseinheit)
			.zustaendiger(testVerwaltungseinheit)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(testBenutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstand(new Umsetzungsstand())
			.geloescht(true)
			.version(0L)
			.build();

		Massnahme savedMassnahme = massnahmeRepository.save(massnahme);
		long massnahmeId = savedMassnahme.getId();

		// act & assert
		assertThrows(EntityNotFoundException.class, () -> massnahmeService.get(massnahmeId));
		assertThrows(EntityNotFoundException.class,
			() -> massnahmeService.loadForModification(massnahmeId, 0L));
		assertThrows(EntityNotFoundException.class, () -> massnahmeService.haengeDateiAn(massnahmeId,
			DokumentTestDataProvider.withDefaultValues().build()));
		assertThrows(EntityNotFoundException.class, () -> massnahmeService.getDokument(massnahmeId, 42L));
		assertThrows(EntityNotFoundException.class, () -> massnahmeService.deleteDokument(massnahmeId, 42L));
		assertThrows(EntityNotFoundException.class,
			() -> massnahmeService.getMassnahmeByUmsetzungsstand(savedMassnahme.getUmsetzungsstand().get()));
	}

	@Test
	void testGetNetzbezugByUmsetzungsstandId_MassnahmeGeloescht_returnsEmptyOptional() {

		// arrange
		Knoten knoten = knotenRepository.save(KnotenTestDataProvider.withDefaultValues().build());
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1.),
					Seitenbezug.BEIDSEITIG)),
				Set.of(),
				Set.of(knoten)))
			.baulastZustaendiger(testVerwaltungseinheit)
			.unterhaltsZustaendiger(testVerwaltungseinheit)
			.zustaendiger(testVerwaltungseinheit)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(testBenutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstand(new Umsetzungsstand())
			.geloescht(true)
			.version(0L)
			.build();

		Massnahme savedMassnahme = massnahmeRepository.save(massnahme);

		// act & assert
		assertThat(massnahmeService.getNetzbezugByUmsetzungsstandId(
			savedMassnahme.getUmsetzungsstand().get().getId())).isEmpty();
	}
}
