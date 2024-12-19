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

package de.wps.radvis.backend.massnahme.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
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
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.dokument.domain.entity.provider.DokumentTestDataProvider;
import de.wps.radvis.backend.dokument.schnittstelle.AddDokumentCommand;
import de.wps.radvis.backend.dokument.schnittstelle.view.DokumentListView;
import de.wps.radvis.backend.dokument.schnittstelle.view.DokumenteView;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.massnahme.domain.MassnahmeService;
import de.wps.radvis.backend.massnahme.domain.MassnahmenConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandabfrageService;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandsabfrageConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Kostenannahme;
import de.wps.radvis.backend.massnahme.schnittstelle.view.MassnahmeEditView;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.schnittstelle.command.KnotenNetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.NetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.PunktuellerKantenSeitenBezugCommand;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;

@Tag("group2")
@ContextConfiguration(classes = {
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	MassnahmeConfiguration.class,
	NetzConfiguration.class,
	GeoConverterConfiguration.class,
	MassnahmeControllerIntegrationTestIT.TestConfiguration.class,
	CommonConfiguration.class,
	KommentarConfiguration.class,
	MailConfiguration.class,
	DokumentConfiguration.class
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
class MassnahmeControllerIntegrationTestIT extends DBIntegrationTestIT {

	public static class TestConfiguration {

		@MockBean
		private ShapeFileRepository shapeFileRepository;

		@MockBean
		private SimpleMatchingService simpleMatchingService;

		@MockBean
		private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

		@MockBean
		BenutzerResolver benutzerResolver;

		@MockBean
		UmsetzungsstandabfrageService umsetzungsstandabfrageService;

		@MockBean
		VerwaltungseinheitService verwaltungseinheitService;

		@MockBean
		FahrradrouteRepository fahrradrouteRepository;

		@MockBean
		private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;

		@Autowired
		MassnahmeService massnahmeService;
		@Autowired
		CreateMassnahmeCommandConverter createMassnahmeCommandConverter;
		@Autowired
		SaveMassnahmeCommandConverter saveMassnahmeCommandConverter;
		@Autowired
		SaveUmsetzungsstandCommandConverter saveUmsetzungsstandCommandConverter;
		@Autowired
		MassnahmeGuard massnahmeGuard;

		@MockBean
		private CsvRepository csvRepository;

		@Bean
		public MassnahmeController massnahmeController() {
			when(benutzerResolver.fromAuthentication(any()))
				.thenReturn(
					BenutzerTestDataProvider.admin(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
						.build());
			return new MassnahmeController(
				massnahmeService,
				umsetzungsstandabfrageService,
				createMassnahmeCommandConverter,
				saveMassnahmeCommandConverter,
				saveUmsetzungsstandCommandConverter,
				massnahmeGuard,
				benutzerResolver,
				verwaltungseinheitService,
				csvRepository);
		}
	}

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Autowired
	private KnotenRepository knotenRepository;
	@Autowired
	private KantenRepository kantenRepository;
	@Autowired
	private MassnahmeController massnahmeController;
	@Autowired
	private BenutzerResolver benutzerResolver;
	@Autowired
	private MassnahmeRepository massnahmeRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	private BenutzerRepository benutzerRepository;
	@Autowired
	private EntityManager entityManager;

	@Mock
	Authentication authentication;

	@Mock
	MultipartFile mockedMultipartFile;
	String fileContent = "foobar";

	@BeforeEach
	void setup() throws IOException {
		when(mockedMultipartFile.getBytes()).thenReturn(fileContent.getBytes());
	}

	@Test
	void uploadDateiAnLeereDokumentliste() throws SQLException, IOException {
		// Arrange
		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.admin(gebietskoerperschaft).build();

		Massnahme massnahme = createMassnahme(gebietskoerperschaft, benutzer, new DokumentListe());
		Long massnahmeID = massnahme.getId();

		// Act
		massnahmeController.uploadDatei(
			massnahmeID,
			new AddDokumentCommand("datei.jpg"),
			mockedMultipartFile,
			authentication);

		entityManager.flush();
		entityManager.clear();

		// Assert
		DokumentListView resultDokument = massnahmeController.getDokumentListe(massnahmeID, authentication)
			.getDokumente().get(0);
		assertThat(resultDokument.getBenutzerNachname()).isEqualTo(benutzer.getNachname().toString());
		assertThat(resultDokument.getDateiname()).isEqualTo("datei.jpg");

		ResponseEntity<byte[]> responseEntity = massnahmeController.downloadDatei(massnahmeID,
			resultDokument.getDokumentId());
		assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
		assertThat(responseEntity.getBody().length).isEqualTo(fileContent.length());
	}

	@Test
	void uploadDateiAnDokumentlisteMitbestehendemDokument() throws IOException {
		// Arrange
		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.admin(gebietskoerperschaft).build();

		DokumentListe dokumentListe = new DokumentListe();
		dokumentListe.addDokument(
			DokumentTestDataProvider.withDefaultValues().dateiname("ersteDatei.jpg").benutzer(benutzer)
				.datei("DATEINHALTDESERSTENDOKUMENTS".getBytes()).build());

		Massnahme massnahme = createMassnahme(gebietskoerperschaft, benutzer, dokumentListe);
		Long massnahmeID = massnahme.getId();

		// Act
		massnahmeController.uploadDatei(
			massnahmeID,
			new AddDokumentCommand("datei.jpg"),
			mockedMultipartFile,
			authentication);

		entityManager.flush();
		entityManager.clear();

		// Assert
		DokumenteView dokumentListePersistiert = massnahmeController.getDokumentListe(massnahmeID,
			authentication);
		assertThat(dokumentListePersistiert.getDokumente())
			.extracting("dateiname")
			.containsExactlyInAnyOrder("ersteDatei.jpg", "datei.jpg");

		ResponseEntity<byte[]> responseEntityDokument1 = massnahmeController.downloadDatei(massnahmeID,
			dokumentListePersistiert.getDokumente().get(0).getDokumentId());
		ResponseEntity<byte[]> responseEntityDokument2 = massnahmeController.downloadDatei(massnahmeID,
			dokumentListePersistiert.getDokumente().get(1).getDokumentId());

		assertTrue(responseEntityDokument1.getStatusCode().is2xxSuccessful());
		assertThat(responseEntityDokument1.getBody().length).isEqualTo(28);

		assertTrue(responseEntityDokument2.getStatusCode().is2xxSuccessful());
		assertThat(responseEntityDokument2.getBody().length).isEqualTo(fileContent.length());
	}

	@Test
	void deleteFile() {
		// Arrange
		Gebietskoerperschaft build = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build();
		Benutzer benutzer = BenutzerTestDataProvider.admin(build).build();

		DokumentListe dokumentListe = new DokumentListe();

		Dokument dokument1 = DokumentTestDataProvider.withDefaultValues().dateiname("datei-1.jpg").benutzer(benutzer)
			.build();
		Dokument dokument2 = DokumentTestDataProvider.withDefaultValues().dateiname("datei-2.jpg").benutzer(benutzer)
			.build();

		dokumentListe.addDokument(dokument1);
		dokumentListe.addDokument(dokument2);

		Massnahme massnahme = createMassnahme(build, benutzer, dokumentListe);

		// Act
		massnahmeController.deleteDatei(authentication, massnahme.getId(), dokument1.getId());

		entityManager.flush();
		entityManager.clear();

		// Assert
		DokumenteView dokumentViews = massnahmeController.getDokumentListe(massnahme.getId(), authentication);
		assertThat(dokumentViews.getDokumente()).hasSize(1);
		assertThat(dokumentViews.getDokumente().get(0).getDokumentId()).isEqualTo(dokument2.getId());
	}

	@Test
	void uploadDateiAnDokumentOhneBerechtigung() throws AccessDeniedException {
		// Arrange
		Benutzer benutzer = BenutzerTestDataProvider.admin(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.rollen(Collections.singleton(Rolle.RADVIS_BETRACHTER)).build();

		Massnahme massnahme = createMassnahme(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(),
			benutzer, new DokumentListe());
		Long massnahmeID = massnahme.getId();

		AddDokumentCommand uploadcommand = new AddDokumentCommand("datei.jpg");

		// Act & Assert
		assertThrows(AccessDeniedException.class,
			() -> massnahmeController.uploadDatei(massnahmeID, uploadcommand, mockedMultipartFile, authentication));
	}

	@Test
	void uploadDateiAnDokumentNichtImZustaendigkeitsbereich() throws AccessDeniedException {
		// Arrange
		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(2000, 2000, 2100, 2100))
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.admin(gebietskoerperschaft)
			.rollen(Collections.singleton(Rolle.KREISKOORDINATOREN)).build();

		Massnahme massnahme = createMassnahme(gebietskoerperschaft, benutzer, new DokumentListe());

		AddDokumentCommand uploadcommand = new AddDokumentCommand("datei.jpg");

		// Act & Assert
		assertThrows(AccessDeniedException.class,
			() -> massnahmeController.uploadDatei(massnahme.getId(), uploadcommand, mockedMultipartFile,
				authentication));
	}

	@Test
	void updateKostenannahme() throws AccessDeniedException {
		// Arrange
		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Benutzer benutzer = BenutzerTestDataProvider
			.admin(gebietskoerperschaft)
			.rollen(Collections.singleton(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN)).build();

		Massnahme massnahme = createMassnahme(gebietskoerperschaft, benutzer, new DokumentListe());
		Long massnahmeID = massnahme.getId();

		Kostenannahme kostenannahme = Kostenannahme.of(5258L);

		Long kantenId = massnahme.getNetzbezug().getImmutableKantenAbschnittBezug().stream().findFirst().get()
			.getKante()
			.getId();
		Long knotenId = massnahme.getNetzbezug().getImmutableKnotenBezug().stream().findFirst().get().getId();

		SaveMassnahmeCommand updateMassnahmeCommand = new SaveMassnahmeCommand(massnahmeID, massnahme.getVersion(),
			massnahme.getBezeichnung(), massnahme.getMassnahmenkategorien(),
			new NetzbezugCommand(List.of(),
				List.of(new PunktuellerKantenSeitenBezugCommand(kantenId, 0.67D)),
				List.of(new KnotenNetzbezugCommand(knotenId))),
			massnahme.getUmsetzungsstatus(), massnahme.getVeroeffentlicht(), massnahme.getPlanungErforderlich(),
			massnahme.getDurchfuehrungszeitraum().orElse(null),
			gebietskoerperschaft.getId(),
			massnahme.getMaViSID().orElse(null), massnahme.getVerbaID().orElse(null),
			massnahme.getLGVFGID().orElse(null), massnahme.getMassnahmeKonzeptID().orElse(null),
			massnahme.getNetzklassen(), massnahme.getPrioritaet().orElse(null), kostenannahme,
			gebietskoerperschaft.getId(),
			gebietskoerperschaft.getId(),
			massnahme.getSollStandard(),
			massnahme.getHandlungsverantwortlicher().orElse(null), massnahme.getKonzeptionsquelle(),
			massnahme.getSonstigeKonzeptionsquelle().orElse(null),
			null);

		// Act
		MassnahmeEditView result = massnahmeController.saveMassnahme(authentication, updateMassnahmeCommand);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getKostenannahme()).isEqualTo(Kostenannahme.of(5258L));
	}

	private Massnahme createMassnahme(Gebietskoerperschaft organisation, Benutzer benutzer,
		DokumentListe dokumentListe) {
		gebietskoerperschaftRepository.save(organisation);
		benutzerRepository.save(benutzer);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		Knoten knoten = knotenRepository.save(KnotenTestDataProvider.withDefaultValues().build());
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1.),
					Seitenbezug.BEIDSEITIG)),
				Set.of(), Set.of(knoten)))
			.dokumentListe(dokumentListe)
			.baulastZustaendiger(organisation)
			.zustaendiger(organisation)
			.unterhaltsZustaendiger(organisation)
			.benutzerLetzteAenderung(benutzer)
			.build();
		massnahme = massnahmeRepository.save(massnahme);

		return massnahme;
	}
}
