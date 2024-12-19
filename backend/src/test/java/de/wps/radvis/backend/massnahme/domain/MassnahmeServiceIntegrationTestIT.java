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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.MailConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.dokument.domain.entity.provider.DokumentTestDataProvider;
import de.wps.radvis.backend.fahrradroute.FahrradrouteConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.bezug.NetzBezugTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.dbView.MassnahmeListenDbView;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerAbweichungZumMassnahmenblatt;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerNichtUmsetzungDerMassnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.PruefungQualitaetsstandardsErfolgt;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
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
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
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
	OrganisationConfigurationProperties.class,
	MassnahmenConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@EnableJpaRepositories(basePackageClasses = FahrradrouteConfiguration.class)
@EntityScan(basePackageClasses = FahrradrouteConfiguration.class)
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

	@Autowired
	private FahrradrouteRepository fahrradrouteRepository;

	@MockBean
	private SimpleMatchingService simpleMatchingService;

	@MockBean
	private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;

	@Autowired
	EntityManager entityManager;

	@BeforeEach
	void setUp() {
		testVerwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Coole Organisation")
				.organisationsArt(
					OrganisationsArt.BUNDESLAND)
				.build());
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
			.geloescht(true)
			.version(0L)
			.build();

		Massnahme savedMassnahme = massnahmeRepository.save(massnahme);

		// act & assert
		assertThat(massnahmeService.getNetzbezugByUmsetzungsstandId(
			savedMassnahme.getUmsetzungsstand().get().getId())).isEmpty();
	}

	@Test
	void getUmsetzungsstandAuswertung() {
		// arrange
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Massnahme massnahme = MassnahmeTestDataProvider
			.withKanten(kante)
			.baulastZustaendiger(testVerwaltungseinheit)
			.unterhaltsZustaendiger(testVerwaltungseinheit)
			.zustaendiger(testVerwaltungseinheit)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(testBenutzer)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.build();
		massnahme.getUmsetzungsstand().get().fordereAktualisierungAn();
		massnahme.getUmsetzungsstand().get().update(true, LocalDateTime.of(2024, 12, 17, 14, 20), testBenutzer,
			GrundFuerAbweichungZumMassnahmenblatt.UMSETZUNG_ALTERNATIVER_MASSNAHME_ERFORDERLICH,
			PruefungQualitaetsstandardsErfolgt.JA_STANDARDS_NICHT_EINGEHALTEN, "abweichende Maßnahme", 100000l,
			GrundFuerNichtUmsetzungDerMassnahme.AUS_SUBJEKTIVER_SICHT_NICHT_ERFORDERLICH,
			"anmerkung", Umsetzungsstatus.UMSETZUNG);
		massnahmeRepository.save(massnahme);

		entityManager.flush();

		// act
		CsvData result = massnahmeService.getUmsetzungsstandAuswertung(List.of(massnahme.getId()));

		// assert
		assertThat(result.getRows()).hasSize(1);

		String[] resultRow = new String[] { "ABC123",
			"Bezeichnung",
			null,
			null,
			"BUNDESLAND",
			"Planung",
			"141",
			"Nein",
			"Nein",
			"Nein",
			"Nein",
			"Ja",
			"UMSETZUNG_ALTERNATIVER_MASSNAHME_ERFORDERLICH",
			"JA_STANDARDS_NICHT_EINGEHALTEN",
			"abweichende Maßnahme",
			"100000",
			"anmerkung",
			"adminVorname adminNachname admin@testRadvis.com",
			"17.12.2024 14:20 Uhr",
			"AKTUALISIERT" };
		String[] headers = new String[] {
			"Maßnahmennummer",
			"Bezeichnung",
			"Stadt-/Landkreis",
			"Gemeinde",
			"Baulastträger laut Maßnahmenblatt",
			"Umsetzungsstatus",
			"Länge der Kantensegmente in Meter",
			"Zugehörigkeit RadNETZ Alltag",
			"Zugehörigkeit RadNETZ Freizeit",
			"Zugehörigkeit RadNETZ Zielnetz",
			"1. Ist die Umsetzung erfolgt",
			"2. Umsetzung gemäß RadNETZ-Maßnahmenblatt",
			"3. Grund für Abweichung zum RadNETZ-Maßnahmenblatt",
			"4. Prüfung auf Einhaltung der RadNETZ-Qualitätsstandards",
			"5. Beschreibung der abweichenden RadNETZ-Maßnahme",
			"6. Kosten der RadNETZ-Maßnahme",
			"7. Anmerkung zu RadNETZ-Maßnahmen",
			"Vor-, Nachname und E-Mail-Adresse des Nutzers, der die Umfrage zuletzt bestätigt hat",
			"Zeitpunkt der letzten Bestätigung der Umfrage",
			"Umsetzungsstand-Status"
		};

		Map<String, String> expected = new HashMap<>();
		for (int i = 0; i < headers.length; i++) {
			expected.put(headers[i], resultRow[i]);
		}

		assertThat(result.getRows().get(0)).isEqualTo(expected);
	}

	@Nested
	class GetAlleMassnahmenListenViews {
		private Kante kanteVollstaendigInnerhalb;
		private Kante kanteTeilweiseInnerhalb;
		private Kante kanteAusserhalb;
		private Gebietskoerperschaft gebietskoerperschaft;
		private Benutzer benutzer;
		private Gebietskoerperschaft filterGebietskoerperschaft;

		@BeforeEach
		void setup() {
			gebietskoerperschaft = gebietskoerperschaftRepository
				.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Coole Organisation")
					.organisationsArt(
						OrganisationsArt.BUNDESLAND)
					.build());

			filterGebietskoerperschaft = gebietskoerperschaftRepository
				.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Coole Organisation")
					.organisationsArt(
						OrganisationsArt.BUNDESLAND)
					.bereich(GeometryTestdataProvider.createQuadratischerBereich(50, 50, 300, 300))
					.build());

			kanteVollstaendigInnerhalb = KanteTestDataProvider.withCoordinatesAndQuelle(100, 100, 200, 200,
				QuellSystem.DLM)
				.build();

			kanteTeilweiseInnerhalb = KanteTestDataProvider.withCoordinatesAndQuelle(250, 250, 400, 400,
				QuellSystem.DLM)
				.build();

			kanteAusserhalb = KanteTestDataProvider.withCoordinatesAndQuelle(450, 450, 600, 600, QuellSystem.DLM)
				.build();

			kantenRepository.saveAll(List.of(kanteVollstaendigInnerhalb, kanteTeilweiseInnerhalb, kanteAusserhalb));

			benutzer = benutzerRepository.save(
				BenutzerTestDataProvider.admin(gebietskoerperschaft).serviceBwId(ServiceBwId.of("sbwid4"))
					.build());

			entityManager.flush();
			entityManager.clear();
		}

		@Test
		void getAlleMassnahmenListenViews_historischeMassnahmen_filter() {
			// arrange
			Massnahme massnahmeUngefiltert = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kanteVollstaendigInnerhalb))
				.build());

			Massnahme massnahmeUmgesetzt = massnahmeRepository.save(
				MassnahmeTestDataProvider
					.withDefaultValuesAndOrganisation(gebietskoerperschaft)
					.benutzerLetzteAenderung(benutzer)
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kanteVollstaendigInnerhalb))
					.umsetzungsstatus(Umsetzungsstatus.UMGESETZT)
					.build());

			Massnahme massnahmeStorniert = massnahmeRepository.save(
				MassnahmeTestDataProvider
					.withDefaultValuesAndOrganisation(gebietskoerperschaft)
					.benutzerLetzteAenderung(benutzer)
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kanteVollstaendigInnerhalb))
					.umsetzungsstatus(Umsetzungsstatus.STORNIERT)
					.build());

			Massnahme massnahmeArchiviert = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kanteVollstaendigInnerhalb))
				.build());
			massnahmeArchiviert.archivieren();

			entityManager.flush();
			entityManager.clear();

			// act
			List<MassnahmeListenDbView> result = massnahmeService.getAlleMassnahmenListenViews(
				false,
				Optional.empty(),
				Collections.emptyList());

			// assert
			assertThat(result).extracting(MassnahmeListenDbView::getId).containsExactly(massnahmeUngefiltert.getId());
		}

		@Test
		void getAlleMassnahmenListenViews_verwaltungseinheit_filter() {
			// arrange
			Massnahme massnahmeNurInnerhalb = createMassnahmeOnKanten(kanteVollstaendigInnerhalb);

			Massnahme massnahmeTeilweiseInnerhalb = createMassnahmeOnKanten(kanteTeilweiseInnerhalb);

			Massnahme massnahmeInnerhalbUndAusserhalb = createMassnahmeOnKanten(kanteVollstaendigInnerhalb,
				kanteAusserhalb);

			Massnahme massnahmeAusserhalb = createMassnahmeOnKanten(kanteAusserhalb);

			entityManager.flush();
			entityManager.clear();

			// act
			List<MassnahmeListenDbView> result = massnahmeService.getAlleMassnahmenListenViews(
				true,
				Optional.of(filterGebietskoerperschaft),
				Collections.emptyList());

			// assert
			assertThat(result).extracting(MassnahmeListenDbView::getId)
				.contains(
					massnahmeNurInnerhalb.getId(),
					massnahmeInnerhalbUndAusserhalb.getId(),
					massnahmeTeilweiseInnerhalb.getId());
			assertThat(result).extracting(MassnahmeListenDbView::getId)
				.doesNotContain(massnahmeAusserhalb.getId());
		}

		@Test
		void getAlleMassnahmenListenViews_verwaltungseinheit_containsPunktMassnahmen() {
			// arrange
			Massnahme knotenMassnahmeInnerhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(NetzBezugTestDataProvider.forKnoten(kanteVollstaendigInnerhalb.getVonKnoten()))
				.build());
			Massnahme punktMassnahmeInnerhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kanteVollstaendigInnerhalb))
				.build());
			Massnahme knotenMassnahmeAußerhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(NetzBezugTestDataProvider.forKnoten(kanteAusserhalb.getVonKnoten()))
				.build());
			Massnahme punktMassnahmeAußerhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kanteAusserhalb))
				.build());

			entityManager.flush();
			entityManager.clear();

			// act
			List<MassnahmeListenDbView> result = massnahmeService.getAlleMassnahmenListenViews(
				true,
				Optional.of(filterGebietskoerperschaft),
				Collections.emptyList());

			// assert
			assertThat(result).extracting(m -> m.getId()).contains(knotenMassnahmeInnerhalb.getId(),
				punktMassnahmeInnerhalb.getId());
			assertThat(result).extracting(m -> m.getId()).doesNotContain(knotenMassnahmeAußerhalb.getId(),
				punktMassnahmeAußerhalb.getId());
		}

		@Test
		void getAlleMassnahmenListenViews_keineVerwaltungseinheit_keineFahrradroute_returnsAll() {
			// arrange
			Massnahme massnahmeInnerhalb = createMassnahmeOnKanten(kanteVollstaendigInnerhalb);

			Massnahme massnahmeAusserhalb = createMassnahmeOnKanten(kanteAusserhalb);

			entityManager.flush();
			entityManager.clear();

			// act
			List<MassnahmeListenDbView> result = massnahmeService.getAlleMassnahmenListenViews(
				true,
				Optional.empty(),
				Collections.emptyList());

			// assert
			assertThat(result).extracting(MassnahmeListenDbView::getId)
				.containsExactlyInAnyOrder(massnahmeInnerhalb.getId(), massnahmeAusserhalb.getId());
		}

		private Massnahme createMassnahmeOnKanten(Kante... kanten) {
			return massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kanten))
				.build());
		}

		@Test
		void getAlleMassnahmenListenViews_fahrradroute_withBuffer() {
			// arrange
			Kante kanteWithFahrradroute = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100,
				QuellSystem.DLM)
				.build();

			Kante kanteTeilweiseInnerhalbBuffer = KanteTestDataProvider.withCoordinatesAndQuelle(10, 0, 25, 100,
				QuellSystem.DLM)
				.build();

			Kante kanteAusserhalbBuffer = KanteTestDataProvider
				.withCoordinatesAndQuelle(25, 0, 25, 100, QuellSystem.DLM)
				.build();

			kantenRepository
				.saveAll(List.of(kanteWithFahrradroute, kanteTeilweiseInnerhalbBuffer, kanteAusserhalbBuffer));
			Fahrradroute fahrradroute = createFahrradrouteOnKante(kanteWithFahrradroute);
			Massnahme massnahmeOnFahrradroute = createMassnahmeOnKanten(kanteWithFahrradroute);
			Massnahme massnahmeInBuffer = createMassnahmeOnKanten(kanteTeilweiseInnerhalbBuffer);
			Massnahme massnahmeAußerhalbBuffer = createMassnahmeOnKanten(kanteAusserhalbBuffer);

			entityManager.flush();
			entityManager.clear();

			// act
			List<MassnahmeListenDbView> result = massnahmeService.getAlleMassnahmenListenViews(
				true,
				Optional.empty(),
				List.of(fahrradroute.getId()));

			// assert
			assertThat(result).extracting(m -> m.getId()).contains(massnahmeInBuffer.getId(),
				massnahmeOnFahrradroute.getId());
			assertThat(result).extracting(m -> m.getId()).doesNotContain(massnahmeAußerhalbBuffer.getId());
		}

		@Test
		void getAlleMassnahmenListenViews_fahrradroute_containsPunktMassnahmen() {
			// arrange
			Fahrradroute fahrradroute = createFahrradrouteOnKante(kanteVollstaendigInnerhalb);
			Massnahme knotenMassnahmeInnerhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(NetzBezugTestDataProvider.forKnoten(kanteVollstaendigInnerhalb.getVonKnoten()))
				.build());
			Massnahme punktMassnahmeInnerhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kanteVollstaendigInnerhalb))
				.build());
			Massnahme knotenMassnahmeAußerhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(NetzBezugTestDataProvider.forKnoten(kanteAusserhalb.getVonKnoten()))
				.build());
			Massnahme punktMassnahmeAußerhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kanteAusserhalb))
				.build());

			entityManager.flush();
			entityManager.clear();

			// act
			List<MassnahmeListenDbView> result = massnahmeService.getAlleMassnahmenListenViews(
				true,
				Optional.empty(),
				List.of(fahrradroute.getId()));

			// assert
			assertThat(result).extracting(m -> m.getId()).contains(knotenMassnahmeInnerhalb.getId(),
				punktMassnahmeInnerhalb.getId());
			assertThat(result).extracting(m -> m.getId()).doesNotContain(knotenMassnahmeAußerhalb.getId(),
				punktMassnahmeAußerhalb.getId());
		}

		@Test
		void getAlleMassnahmenListenViews_fahrradroute_punktUndStreckenMassnahmeKombiniert() {
			// arrange
			Fahrradroute fahrradroute = createFahrradrouteOnKante(kanteVollstaendigInnerhalb);
			Massnahme massnahmeAusserhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(new MassnahmeNetzBezug(
					Set.of(
						new AbschnittsweiserKantenSeitenBezug(kanteAusserhalb, LinearReferenzierterAbschnitt.of(0, 1),
							Seitenbezug.LINKS)),
					Set.of(new PunktuellerKantenSeitenBezug(kanteAusserhalb, LineareReferenz.of(0.5),
						Seitenbezug.LINKS)),
					Set.of(kanteAusserhalb.getVonKnoten())))
				.build());
			Massnahme massnahmePunktInnerhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(new MassnahmeNetzBezug(
					Set.of(
						new AbschnittsweiserKantenSeitenBezug(kanteAusserhalb, LinearReferenzierterAbschnitt.of(0, 1),
							Seitenbezug.LINKS)),
					Set.of(new PunktuellerKantenSeitenBezug(kanteVollstaendigInnerhalb, LineareReferenz.of(0.5),
						Seitenbezug.LINKS)),
					Set.of(kanteAusserhalb.getVonKnoten())))
				.build());
			Massnahme massnahmeKnotenInnerhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(new MassnahmeNetzBezug(
					Set.of(
						new AbschnittsweiserKantenSeitenBezug(kanteAusserhalb, LinearReferenzierterAbschnitt.of(0, 1),
							Seitenbezug.LINKS)),
					Set.of(new PunktuellerKantenSeitenBezug(kanteAusserhalb, LineareReferenz.of(0.5),
						Seitenbezug.LINKS)),
					Set.of(kanteVollstaendigInnerhalb.getVonKnoten())))
				.build());
			Massnahme massnahmeKanteInnerhalb = massnahmeRepository.save(MassnahmeTestDataProvider
				.withDefaultValuesAndOrganisation(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzer)
				.netzbezug(new MassnahmeNetzBezug(
					Set.of(
						new AbschnittsweiserKantenSeitenBezug(kanteVollstaendigInnerhalb,
							LinearReferenzierterAbschnitt.of(0, 1),
							Seitenbezug.LINKS)),
					Set.of(new PunktuellerKantenSeitenBezug(kanteAusserhalb, LineareReferenz.of(0.5),
						Seitenbezug.LINKS)),
					Set.of(kanteAusserhalb.getVonKnoten())))
				.build());

			entityManager.flush();
			entityManager.clear();

			// act
			List<MassnahmeListenDbView> result = massnahmeService.getAlleMassnahmenListenViews(
				true,
				Optional.empty(),
				List.of(fahrradroute.getId()));

			// assert
			assertThat(result).extracting(m -> m.getId()).contains(massnahmeKanteInnerhalb.getId(),
				massnahmeKnotenInnerhalb.getId(), massnahmePunktInnerhalb.getId());
			assertThat(result).extracting(m -> m.getId()).doesNotContain(massnahmeAusserhalb.getId());
		}

		@Test
		void getAlleMassnahmenListenViews_zweiFahrradrouten_vereinigung() {
			// arrange
			Kante kanteOhneFahrradroute = kantenRepository.save(KanteTestDataProvider
				.withCoordinatesAndQuelle(1000, 1000, 0, 100,
					QuellSystem.DLM)
				.build());

			Massnahme massnahmeOhneFahrradroute = createMassnahmeOnKanten(kanteOhneFahrradroute);

			Fahrradroute fahrradrouteAußerhalb = createFahrradrouteOnKante(kanteAusserhalb);
			Massnahme massnahmeAußerhalb = createMassnahmeOnKanten(
				fahrradrouteAußerhalb.getAbschnittsweiserKantenBezug().get(0).getKante());

			Fahrradroute fahrradrouteInnerhalb1 = createFahrradrouteOnKante(kanteVollstaendigInnerhalb);
			Massnahme massnahmeInnerhalb1 = createMassnahmeOnKanten(
				fahrradrouteInnerhalb1.getAbschnittsweiserKantenBezug().get(0).getKante());

			Fahrradroute fahrradrouteInnerhalb2 = createFahrradrouteOnKante(kanteTeilweiseInnerhalb);
			Massnahme massnahmeInnerhalb2 = createMassnahmeOnKanten(
				fahrradrouteInnerhalb2.getAbschnittsweiserKantenBezug().get(0).getKante());

			entityManager.flush();
			entityManager.clear();

			// act
			List<MassnahmeListenDbView> result = massnahmeService.getAlleMassnahmenListenViews(
				true,
				Optional.empty(),
				List.of(fahrradrouteInnerhalb1.getId(), fahrradrouteInnerhalb2.getId()));

			// assert
			assertThat(result).extracting(m -> m.getId()).contains(massnahmeInnerhalb1.getId(),
				massnahmeInnerhalb2.getId());
			assertThat(result).extracting(m -> m.getId()).doesNotContain(massnahmeOhneFahrradroute.getId(),
				massnahmeAußerhalb.getId());
		}

		private Fahrradroute createFahrradrouteOnKante(Kante kante) {
			return fahrradrouteRepository
				.save(FahrradrouteTestDataProvider.onKante(kante).netzbezugLineString(kante.getGeometry())
					.verantwortlich(gebietskoerperschaft).build());
		}

		@Test
		void getAlleMassnahmenListenViews_verwaltungseinheitUndFahrradroute_schnittmenge() {
			// arrange
			Fahrradroute fahrradrouteInnerhalb = createFahrradrouteOnKante(kanteVollstaendigInnerhalb);
			Massnahme massnahmeInnerhalbMitRoute = createMassnahmeOnKanten(kanteVollstaendigInnerhalb);

			Massnahme massnahmeInnerhalbOhneRoute = createMassnahmeOnKanten(kanteTeilweiseInnerhalb);

			Fahrradroute fahrradrouteAusserhalb = createFahrradrouteOnKante(kanteVollstaendigInnerhalb);
			Massnahme massnahmeAusserhalbMitRoute = createMassnahmeOnKanten(kanteAusserhalb);

			entityManager.flush();
			entityManager.clear();

			// act
			List<MassnahmeListenDbView> result = massnahmeService.getAlleMassnahmenListenViews(
				true,
				Optional.of(filterGebietskoerperschaft),
				List.of(fahrradrouteInnerhalb.getId(), fahrradrouteAusserhalb.getId()));

			// assert
			assertThat(result).extracting(MassnahmeListenDbView::getId).contains(massnahmeInnerhalbMitRoute.getId());
			assertThat(result).extracting(MassnahmeListenDbView::getId)
				.doesNotContain(massnahmeInnerhalbOhneRoute.getId(), massnahmeAusserhalbMitRoute.getId());
		}
	}
}
