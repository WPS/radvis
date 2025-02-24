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

package de.wps.radvis.backend.massnahme.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.MailConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.fahrradroute.FahrradrouteConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.massnahme.domain.MassnahmenConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandsabfrageConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeUmsetzungsstandDBView;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerAbweichungZumMassnahmenblatt;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerNichtUmsetzungDerMassnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.PruefungQualitaetsstandardsErfolgt;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandStatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
	OrganisationConfigurationProperties.class,
	MassnahmenConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@EnableJpaRepositories(basePackageClasses = FahrradrouteConfiguration.class)
@EntityScan(basePackageClasses = FahrradrouteConfiguration.class)
class MassnahmeUmsetzungsstandViewRepositoryIntegrationTestIT extends DBIntegrationTestIT {

	private Gebietskoerperschaft gebietskoerperschaft;

	@Autowired
	private MassnahmeRepository massnahmeRepository;

	@Autowired
	private MassnahmeUmsetzungsstandViewRepository massnahmeUmsetzungsstandViewRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	private BenutzerRepository benutzerRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@MockitoBean
	private ShapeFileRepository shapeFileRepository;
	@MockitoBean
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@MockitoBean
	private BenutzerResolver benutzerResolver;
	@MockitoBean
	private SimpleMatchingService simpleMatchingService;
	@MockitoBean
	private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;
	@MockitoBean
	private FahrradrouteRepository fahrradrouteRepository;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Coole Organisation")
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 200, 200))
				.organisationsArt(
					OrganisationsArt.BUNDESLAND)
				.build());

	}

	@Test
	void findAllById_correctValues() {
		// arrange
		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 30, QuellSystem.DLM).build());
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withKanten(kante)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(benutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.build();
		massnahme1.getUmsetzungsstand().get().fordereAktualisierungAn();
		massnahme1.getUmsetzungsstand().get().update(true, LocalDateTime.of(2024, 12, 17, 14, 20), benutzer,
			GrundFuerAbweichungZumMassnahmenblatt.UMSETZUNG_ALTERNATIVER_MASSNAHME_ERFORDERLICH,
			PruefungQualitaetsstandardsErfolgt.JA_STANDARDS_NICHT_EINGEHALTEN, "abweichende Maßnahme", 100000l,
			GrundFuerNichtUmsetzungDerMassnahme.AUS_SUBJEKTIVER_SICHT_NICHT_ERFORDERLICH,
			"anmerkung", Umsetzungsstatus.UMSETZUNG);

		massnahme1 = massnahmeRepository.save(massnahme1);

		entityManager.flush();

		// act
		List<MassnahmeUmsetzungsstandDBView> result = massnahmeUmsetzungsstandViewRepository
			.findAllById(List.of(massnahme1.getId())).toList();

		// assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0))
			.usingRecursiveComparison()
			.isEqualTo(new MassnahmeUmsetzungsstandDBView(massnahme1.getId(), massnahme1.getBezeichnung().toString(),
				massnahme1.getMassnahmeKonzeptID().get().toString(),
				gebietskoerperschaft.getOrganisationsArt().name(), null, null,
				(int) (kante.getLaengeBerechnet().getValue()),
				massnahme1.getNetzklassen(), massnahme1.getUmsetzungsstatus(), UmsetzungsstandStatus.AKTUALISIERT,
				"Nein", "Ja", "UMSETZUNG_ALTERNATIVER_MASSNAHME_ERFORDERLICH", "JA_STANDARDS_NICHT_EINGEHALTEN",
				"abweichende Maßnahme", "100000", "anmerkung", "adminVorname adminNachname admin@testRadvis.com",
				"17.12.2024 14:20 Uhr"));
	}

	@Test
	void findAllById_alleRadNetzKonzeptionsquellen() {
		// arrange
		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 30, QuellSystem.DLM).build());
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());

		Massnahme massnahme1 = MassnahmeTestDataProvider.withKanten(kante)
			.benutzerLetzteAenderung(benutzer)
			.zustaendiger(gebietskoerperschaft)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		massnahme1.getUmsetzungsstand().get().fordereAktualisierungAn();
		massnahme1 = massnahmeRepository.save(massnahme1);

		Massnahme massnahme2 = MassnahmeTestDataProvider.withKanten(kante)
			.benutzerLetzteAenderung(benutzer)
			.zustaendiger(gebietskoerperschaft)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024)
			.build();
		massnahme2.getUmsetzungsstand().get().fordereAktualisierungAn();
		massnahme2 = massnahmeRepository.save(massnahme2);

		entityManager.flush();

		// act
		List<MassnahmeUmsetzungsstandDBView> result = massnahmeUmsetzungsstandViewRepository
			.findAllById(List.of(massnahme1.getId(), massnahme2.getId())).toList();

		// assert
		assertThat(result).hasSize(2);
		assertThat(result.stream().map(MassnahmeUmsetzungsstandDBView::getId).toList())
			.containsExactlyInAnyOrder(massnahme1.getId(), massnahme2.getId());
	}

	@Test
	void findAllById_laengeSummiert() {
		// arrange
		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 30, QuellSystem.DLM).build());
		Kante kante2 = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 30, 100, 30, QuellSystem.DLM).build());
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(kante2, LinearReferenzierterAbschnitt.of(0.2, 0.6),
					Seitenbezug.BEIDSEITIG),
					new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0.0, 1.0),
						Seitenbezug.BEIDSEITIG)),
				Collections.emptySet(),
				Collections.emptySet()))
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(benutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.build();
		massnahme1.getUmsetzungsstand().get().fordereAktualisierungAn();

		massnahme1 = massnahmeRepository.save(massnahme1);

		entityManager.flush();

		// act
		List<MassnahmeUmsetzungsstandDBView> result = massnahmeUmsetzungsstandViewRepository
			.findAllById(List.of(massnahme1.getId())).toList();

		// assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getLaenge()).isEqualTo(70);
	}

	@Test
	void findAllById_gemeindeUndKreis() {
		// arrange
		Gebietskoerperschaft kreis = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Cooler Kreis")
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 400, 400))
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());
		Gebietskoerperschaft gemeinde = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Coole Gemeinde")
				.uebergeordneteOrganisation(kreis)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 200, 200))
				.organisationsArt(
					OrganisationsArt.GEMEINDE)
				.build());
		Gebietskoerperschaft andereGemeinde = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Andere coole Gemeinde")
				.uebergeordneteOrganisation(kreis)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 200, 200, 400))
				.organisationsArt(
					OrganisationsArt.GEMEINDE)
				.build());
		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 30, QuellSystem.DLM).build());
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withKanten(kante)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(benutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.build();
		massnahme1.getUmsetzungsstand().get().fordereAktualisierungAn();

		massnahme1 = massnahmeRepository.save(massnahme1);

		entityManager.flush();

		// act
		List<MassnahmeUmsetzungsstandDBView> result = massnahmeUmsetzungsstandViewRepository
			.findAllById(List.of(massnahme1.getId())).toList();

		// assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getGemeinde()).isEqualTo(gemeinde.getName());
		assertThat(result.get(0).getGemeinde()).isNotEqualTo(andereGemeinde.getName());
		assertThat(result.get(0).getKreis()).isEqualTo(kreis.getName());
	}

	@Test
	void findAllById_gemeindeUndKreis_punktMassnahme() {
		// arrange
		Gebietskoerperschaft kreis = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Cooler Kreis")
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 400, 400))
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());
		Gebietskoerperschaft gemeinde = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Coole Gemeinde")
				.uebergeordneteOrganisation(kreis)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 200, 200))
				.organisationsArt(
					OrganisationsArt.GEMEINDE)
				.build());
		Gebietskoerperschaft andereGemeinde = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Andere coole Gemeinde")
				.uebergeordneteOrganisation(kreis)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 200, 200, 400))
				.organisationsArt(
					OrganisationsArt.GEMEINDE)
				.build());
		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 30, QuellSystem.DLM).build());
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(new MassnahmeNetzBezug(Collections.emptySet(),
				Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.25), Seitenbezug.LINKS)),
				Collections.emptySet()))
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(benutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.build();
		massnahme1.getUmsetzungsstand().get().fordereAktualisierungAn();

		massnahme1 = massnahmeRepository.save(massnahme1);

		entityManager.flush();

		// act
		List<MassnahmeUmsetzungsstandDBView> result = massnahmeUmsetzungsstandViewRepository
			.findAllById(List.of(massnahme1.getId())).toList();

		// assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getGemeinde()).isEqualTo(gemeinde.getName());
		assertThat(result.get(0).getGemeinde()).isNotEqualTo(andereGemeinde.getName());
		assertThat(result.get(0).getKreis()).isEqualTo(kreis.getName());
	}

	@Test
	void findAllById_gemeindeUndKreis_knotenMassnahme() {
		// arrange
		Gebietskoerperschaft kreis = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Cooler Kreis")
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 400, 400))
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());
		Gebietskoerperschaft gemeinde = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Coole Gemeinde")
				.uebergeordneteOrganisation(kreis)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 200, 200))
				.organisationsArt(
					OrganisationsArt.GEMEINDE)
				.build());
		Gebietskoerperschaft andereGemeinde = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Andere coole Gemeinde")
				.uebergeordneteOrganisation(kreis)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 200, 200, 400))
				.organisationsArt(
					OrganisationsArt.GEMEINDE)
				.build());
		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 30, QuellSystem.DLM).build());
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withKnoten(kante.getVonKnoten())
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(benutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.build();
		massnahme1.getUmsetzungsstand().get().fordereAktualisierungAn();

		massnahme1 = massnahmeRepository.save(massnahme1);

		entityManager.flush();

		// act
		List<MassnahmeUmsetzungsstandDBView> result = massnahmeUmsetzungsstandViewRepository
			.findAllById(List.of(massnahme1.getId())).toList();

		// assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getGemeinde()).isEqualTo(gemeinde.getName());
		assertThat(result.get(0).getGemeinde()).isNotEqualTo(andereGemeinde.getName());
		assertThat(result.get(0).getKreis()).isEqualTo(kreis.getName());
	}

	@Test
	// Wir sind nicht sicher, ob das fachlich wirklich sinnvoll ist, ist aber jetzt schon länger so in Produktion
	void findAllById_gemeindeUndKreis_multipleGemeinden_takesAlphabeticallyFirst() {
		// arrange
		Gebietskoerperschaft kreis = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Cooler Kreis")
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 400, 400))
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());
		Gebietskoerperschaft gemeinde = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Coole Gemeinde")
				.uebergeordneteOrganisation(kreis)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 200, 200))
				.organisationsArt(
					OrganisationsArt.GEMEINDE)
				.build());
		Gebietskoerperschaft gemeinde2 = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Andere coole Gemeinde")
				.uebergeordneteOrganisation(kreis)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 200, 200, 400))
				.organisationsArt(
					OrganisationsArt.GEMEINDE)
				.build());
		Kante kanteGemeinde1 = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 30, QuellSystem.DLM).build());
		Kante kanteGemeinde2 = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 200, 20, 300, QuellSystem.DLM).build());
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withKanten(kanteGemeinde1, kanteGemeinde2)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(benutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.build();
		massnahme1.getUmsetzungsstand().get().fordereAktualisierungAn();

		massnahme1 = massnahmeRepository.save(massnahme1);

		entityManager.flush();

		// act
		List<MassnahmeUmsetzungsstandDBView> result = massnahmeUmsetzungsstandViewRepository
			.findAllById(List.of(massnahme1.getId())).toList();

		// assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getGemeinde()).isEqualTo(gemeinde2.getName());
		assertThat(result.get(0).getGemeinde()).isNotEqualTo(gemeinde.getName());
		assertThat(result.get(0).getKreis()).isEqualTo(kreis.getName());
	}

	@Test
	// Wir sind nicht sicher, ob das fachlich wirklich sinnvoll ist, da ggf. der Kreis nicht der übergeordnete der
	// angegebenen Gemeinde ist. Ist aber jetzt schon länger so in Produktion
	void findAllById_gemeindeUndKreis_multipleKreis_takesAlphabeticallyFirst() {
		// arrange
		Gebietskoerperschaft kreis1 = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Anderer Cooler Kreis")
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 200, 200))
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());
		Gebietskoerperschaft kreis2 = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Cooler Kreis")
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 200, 200, 400))
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());
		Gebietskoerperschaft gemeinde1 = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Coole Gemeinde")
				.uebergeordneteOrganisation(kreis1)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 200, 200))
				.organisationsArt(
					OrganisationsArt.GEMEINDE)
				.build());
		Gebietskoerperschaft gemeinde2 = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Andere coole Gemeinde")
				.uebergeordneteOrganisation(kreis2)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 200, 200, 400))
				.organisationsArt(
					OrganisationsArt.GEMEINDE)
				.build());
		Kante kanteGemeinde1 = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 30, QuellSystem.DLM).build());
		Kante kanteGemeinde2 = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 200, 20, 300, QuellSystem.DLM).build());
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withKanten(kanteGemeinde1, kanteGemeinde2)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(benutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.build();
		massnahme1.getUmsetzungsstand().get().fordereAktualisierungAn();

		massnahme1 = massnahmeRepository.save(massnahme1);

		entityManager.flush();

		// act
		List<MassnahmeUmsetzungsstandDBView> result = massnahmeUmsetzungsstandViewRepository
			.findAllById(List.of(massnahme1.getId())).toList();

		// assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getGemeinde()).isEqualTo(gemeinde2.getName());
		assertThat(result.get(0).getGemeinde()).isNotEqualTo(gemeinde1.getName());
		assertThat(result.get(0).getKreis()).isEqualTo(kreis1.getName());
		assertThat(result.get(0).getKreis()).isNotEqualTo(kreis2.getName());
	}

	@Test
	void findAllById_nichtGeloescht() {
		// arrange
		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 30, QuellSystem.DLM).build());
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withKanten(kante)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(benutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.geloescht(true)
			.build();
		massnahme1.getUmsetzungsstand().get().fordereAktualisierungAn();

		massnahme1 = massnahmeRepository.save(massnahme1);

		entityManager.flush();

		// act
		List<MassnahmeUmsetzungsstandDBView> result = massnahmeUmsetzungsstandViewRepository
			.findAllById(List.of(massnahme1.getId())).toList();

		// assert
		assertThat(result).hasSize(0);
	}

	@Test
	void findAllById_onlyRadnetzMassnahmen() {
		// arrange
		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 30, QuellSystem.DLM).build());
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withKanten(kante)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(benutzer)
			.konzeptionsquelle(Konzeptionsquelle.KOMMUNALES_KONZEPT)
			.build();

		massnahme1 = massnahmeRepository.save(massnahme1);

		entityManager.flush();

		// act
		List<MassnahmeUmsetzungsstandDBView> result = massnahmeUmsetzungsstandViewRepository
			.findAllById(List.of(massnahme1.getId())).toList();

		// assert
		assertThat(result).hasSize(0);
	}

	@Test
	void findAllById_applyFilter() {
		// arrange
		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 30, QuellSystem.DLM).build());
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withKanten(kante)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(benutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		massnahme1.getUmsetzungsstand().get().fordereAktualisierungAn();

		Massnahme massnahme2 = MassnahmeTestDataProvider
			.withKanten(kante)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(benutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		massnahme2.getUmsetzungsstand().get().fordereAktualisierungAn();

		massnahme1 = massnahmeRepository.save(massnahme1);
		massnahme2 = massnahmeRepository.save(massnahme2);

		entityManager.flush();

		// act
		List<MassnahmeUmsetzungsstandDBView> result = massnahmeUmsetzungsstandViewRepository
			.findAllById(List.of(massnahme1.getId())).toList();

		// assert
		assertThat(result).hasSize(1);
		assertThat(result).extracting("id").contains(massnahme1.getId());
		assertThat(result).extracting("id").doesNotContain(massnahme2.getId());
	}

}
