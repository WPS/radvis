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

package de.wps.radvis.backend.integration.dlm.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeUebertragungService;
import de.wps.radvis.backend.integration.dlm.domain.entity.DlmReimportJobStatistik;
import de.wps.radvis.backend.matching.domain.repository.CustomDlmMatchingRepositoryFactory;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.service.CustomGrundnetzMappingServiceFactory;
import de.wps.radvis.backend.matching.domain.service.DlmPbfErstellungService;
import de.wps.radvis.backend.matching.domain.service.GrundnetzMappingService;
import de.wps.radvis.backend.matching.domain.service.KanteUpdateElevationService;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopper;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DlmRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group3")
@ContextConfiguration(classes = {
	NetzConfiguration.class, CommonConfiguration.class
})
class DlmReimportJobNetzUpdateTestIT extends DBIntegrationTestIT {
	DlmReimportJob dlmReimportJob;
	@Mock
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Mock
	private DlmRepository dlmRepository;
	@Mock
	private DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory;
	@Mock
	private DlmMatchedGraphHopper dlmMatchedGraphHopper;

	@Autowired
	private NetzService netzService;
	@Autowired
	private KantenRepository kantenRepository;
	@Autowired
	private KnotenRepository knotenRepository;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	protected PlatformTransactionManager transactionManager;

	@MockitoBean
	VerwaltungseinheitResolver verwaltungseinheitResolver;
	@MockitoBean
	BenutzerResolver benutzerResolver;
	@MockitoBean
	BenutzerService benutzerService;
	@MockitoBean
	FeatureToggleProperties featureToggleProperties;
	@MockitoBean
	PostgisConfigurationProperties postgisConfigurationProperties;
	@MockitoBean
	OrganisationConfigurationProperties organisationConfigurationProperties;
	@MockitoBean
	CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;
	@MockitoBean
	private DlmPbfErstellungService dlmPbfErstellungService;
	@MockitoBean
	private CustomDlmMatchingRepositoryFactory customDlmMatchingRepositoryFactory;
	@MockitoBean
	private CustomGrundnetzMappingServiceFactory customGrundnetzMappingServiceFactory;
	@MockitoBean
	private SimpleMatchingService simpleMatchingService;
	@MockitoBean
	private KanteUpdateElevationService elevationUpdateService;
	@MockitoBean
	private NetzConfigurationProperties netzConfigurationProperties;
	@MockitoBean
	private CommonConfigurationProperties commonConfigurationProperties;

	@BeforeEach
	void setup() throws IOException {
		MockitoAnnotations.openMocks(this);
		when(featureToggleProperties.isShowDlm()).thenReturn(true);
		when(postgisConfigurationProperties.getArgumentLimit()).thenReturn(2);

		when(customGrundnetzMappingServiceFactory.createGrundnetzMappingService(any()))
			.thenReturn(new GrundnetzMappingService(simpleMatchingService));
		when(customDlmMatchingRepositoryFactory.createCustomMatchingRepository(any()))
			.thenReturn(mock(DlmMatchingRepository.class));

		dlmReimportJob = new DlmReimportJob(
			jobExecutionDescriptionRepository,
			dlmPbfErstellungService,
			new KantenAttributeUebertragungService(Laenge.of(1.0)),
			new VernetzungService(kantenRepository, knotenRepository, netzService),
			netzService,
			new DlmImportService(dlmRepository, netzService),
			customDlmMatchingRepositoryFactory, customGrundnetzMappingServiceFactory);
	}

	@Test
	void strassenNameOderNummerChanged_updateKante() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("123"))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
					.strassenName(StrassenName.of("ABC-Straße")).strassenNummer(StrassenNummer.of("1")).build())
				.build())
			.build();
		netzService.saveKante(kante1);

		Kante kante2 = KanteTestDataProvider.withCoordinatesAndQuelle(100, 100, 200, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("456"))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
					.strassenName(StrassenName.of("ABC-Straße")).strassenNummer(StrassenNummer.of("1")).build())
				.build())
			.build();
		netzService.saveKante(kante2);

		Kante kante3 = KanteTestDataProvider.withCoordinatesAndQuelle(100, 100, 200, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("789")).build();
		netzService.saveKante(kante3);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		String neuerStrassenName = "DEF-Straße";
		String neueStrassenNummer = "2";
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(kante1.getGeometry().getCoordinates())
						.addAttribut("eigenname", neuerStrassenName)
						.addAttribut("bezeichnung", neueStrassenNummer)
						.fachId(kante1.getDlmId().getValue()).build(),
					ImportedFeatureTestDataProvider.withLineString(new Coordinate(100, 100), new Coordinate(200, 99.5))
						.addAttribut("eigenname", neuerStrassenName)
						.addAttribut("bezeichnung", neueStrassenNummer)
						.fachId(kante2.getDlmId().getValue()).build(),
					ImportedFeatureTestDataProvider.withLineString(kante3.getGeometry().getCoordinates())
						.fachId(kante3.getDlmId().getValue()).build()));

		// act
		Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		Kante updatedKante1 = kantenRepository.findById(kante1.getId()).get();
		assertThat(updatedKante1.getKantenAttributGruppe().getKantenAttribute()
			.getStrassenName()).contains(StrassenName.of(neuerStrassenName));
		assertThat(updatedKante1.getKantenAttributGruppe().getKantenAttribute()
			.getStrassenNummer()).contains(StrassenNummer.of(neueStrassenNummer));

		Kante updatedKante2 = kantenRepository.findById(kante2.getId()).get();
		assertThat(updatedKante2.getKantenAttributGruppe().getKantenAttribute()
			.getStrassenName()).contains(StrassenName.of(neuerStrassenName));
		assertThat(updatedKante2.getKantenAttributGruppe().getKantenAttribute()
			.getStrassenNummer()).contains(StrassenNummer.of(neueStrassenNummer));

		assertThat(jobStatistik).isPresent();
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlImportierterKanten).isEqualTo(3);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlAktualisierterKanten).isEqualTo(2);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.getAnzahlImDlmGeloeschterKanten()).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlUnveraenderterKanten).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.hinzugefuegteKanten.size()).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlTopologischStarkVeraenderterKanten).isEqualTo(0);
	}

	@Test
	void nothingChanged_DoNothing() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("123")).build();
		netzService.saveKante(kante1);

		Kante kante2 = KanteTestDataProvider.withCoordinatesAndQuelle(100, 100, 200, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("456")).build();
		netzService.saveKante(kante2);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(kante1.getGeometry().getCoordinates())
						.fachId(kante1.getDlmId().getValue()).build(),
					ImportedFeatureTestDataProvider.withLineString(kante2.getGeometry().getCoordinates())
						.fachId(kante2.getDlmId().getValue()).build()));

		// act
		Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
			.withComparedFields("id", "dlmId", "vonKnoten.id", "nachKnoten.id", "geometry")
			.withIgnoreAllOverriddenEquals(false)
			.build();
		assertThat(StreamSupport.stream(kantenRepository.findAll().spliterator(), false).toList())
			.usingRecursiveFieldByFieldElementComparator(configuration)
			.containsExactlyInAnyOrder(kante1, kante2);

		assertThat(jobStatistik).isPresent();
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlImportierterKanten).isEqualTo(2);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlAktualisierterKanten).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.getAnzahlImDlmGeloeschterKanten()).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlUnveraenderterKanten).isEqualTo(2);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.hinzugefuegteKanten.size()).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlTopologischStarkVeraenderterKanten).isEqualTo(0);
	}

	@Test
	void ignoreFeaturesWithoutLinestring() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("123")).build();
		netzService.saveKante(kante1);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.defaultRadNetzObject()
						.geometry(new MultiLineString(new LineString[] { kante1.getGeometry() },
							kante1.getGeometry().getFactory()))
						.fachId(kante1.getDlmId().getValue()).build()));

		// act
		Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(kantenRepository.count()).isEqualTo(0);

		assertThat(jobStatistik).isPresent();
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlImportierterKanten).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlImportierterKantenOhneLineStringGeometrie).isEqualTo(1);
	}

	@Test
	void ignoreFeaturesWithKreisgeometrie() {
		// arrange
		Coordinate[] coordinatesUpdatedDlmKante = new Coordinate[] {
			new Coordinate(0, 0),
			new Coordinate(100, 0),
			new Coordinate(100, 100),
			new Coordinate(0, 0), // letzte Koordinate = erste Koordinate
		};
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("123")).build();
		netzService.saveKante(kante1);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.defaultRadNetzObject()
						.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
							.createLineString(coordinatesUpdatedDlmKante))
						.fachId(kante1.getDlmId().getValue()).build()));

		// act
		Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(kantenRepository.count()).isEqualTo(0);
		assertThat(jobStatistik).isPresent();
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlImportierterKanten).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlImportierterKantenMitKreisgeometrie).isEqualTo(1);
	}

	@Test
	void ignoreAutobahnFeatures() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("123")).build();
		netzService.saveKante(kante1);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.defaultRadNetzObject()
						.geometry(kante1.getGeometry())
						.addAttribut("bezeichnung", "A7") // Autobahn
						.fachId(kante1.getDlmId().getValue()).build()));

		// act
		Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(kantenRepository.count()).isEqualTo(0);
		assertThat(jobStatistik).isPresent();
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlImportierterKanten).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlImportierterAutobahnKanten).isEqualTo(1);
	}

	@SuppressWarnings("unchecked")
	@Test
	void geometryChanged_updateGeometryOfExistingKante() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("123")).build();
		netzService.saveKante(kante1);

		Kante kante2 = KanteTestDataProvider.withCoordinatesAndQuelle(100, 100, 200, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("456")).build();
		netzService.saveKante(kante2);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		Coordinate[] newCoordinates = new Coordinate[] { new Coordinate(0, 0), new Coordinate(100, 0),
			new Coordinate(100, 100) };
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(newCoordinates).fachId(kante1.getDlmId().getValue())
						.build(),
					ImportedFeatureTestDataProvider.withLineString(kante2.getGeometry().getCoordinates())
						.fachId(kante2.getDlmId().getValue()).build()));

		// act
		Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
			.withComparedFields("id", "dlmId", "vonKnoten.id", "nachKnoten.id")
			.withIgnoreAllOverriddenEquals(false)
			.build();
		assertThat(StreamSupport.stream(kantenRepository.findAll().spliterator(), false).toList())
			.usingRecursiveFieldByFieldElementComparator(configuration)
			.containsExactlyInAnyOrder(kante1, kante2);

		LineString newKanteGeometry = GeometryTestdataProvider
			.createLineString(newCoordinates);
		Kante updatedKante = kantenRepository.findById(kante1.getId()).get();
		assertThat(updatedKante.getGeometry()).isEqualTo(newKanteGeometry);
		assertThat(updatedKante.getZugehoerigeDlmGeometrie()).isEqualTo(newKanteGeometry);
		assertThat(updatedKante.getLaengeBerechnet().getValue()).isEqualTo(200);

		assertThat(kantenRepository.findById(kante2.getId()).get()).usingRecursiveComparison()
			.comparingOnlyFields("geometry", "aufDlmAbgebildeteGeometry", "kantenLaengeInCm").usingOverriddenEquals()
			.isEqualTo(kante2);

		assertThat(jobStatistik).isPresent();
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlImportierterKanten).isEqualTo(2);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlAktualisierterKanten).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlUnveraenderterKanten).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.getAnzahlImDlmGeloeschterKanten()).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.hinzugefuegteKanten.size()).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlTopologischStarkVeraenderterKanten).isEqualTo(0);
	}

	@SuppressWarnings("unchecked")
	@Test
	void geometryChanged_updateKnotenGeometry() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("123")).build();
		netzService.saveKante(kante1);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		Coordinate[] newCoordinates = new Coordinate[] { new Coordinate(KnotenIndex.SNAPPING_DISTANCE, 0),
			new Coordinate(100 + KnotenIndex.SNAPPING_DISTANCE, 100) };
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(newCoordinates).fachId(kante1.getDlmId().getValue())
						.build()));

		// act
		dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
			.withComparedFields("id", "dlmId", "vonKnoten.id", "nachKnoten.id")
			.withIgnoreAllOverriddenEquals(false)
			.build();
		assertThat(StreamSupport.stream(kantenRepository.findAll().spliterator(), false).toList())
			.usingRecursiveFieldByFieldElementComparator(configuration)
			.containsExactlyInAnyOrder(kante1);

		LineString newKanteGeometry = GeometryTestdataProvider
			.createLineString(newCoordinates);
		Kante updatedKante = kantenRepository.findById(kante1.getId()).get();
		assertThat(updatedKante.getGeometry()).isEqualTo(newKanteGeometry);
		assertThat(updatedKante.getVonKnoten().getKoordinate())
			.isEqualTo(newCoordinates[0]);
		assertThat(updatedKante.getNachKnoten().getKoordinate())
			.isEqualTo(newCoordinates[1]);
	}

	@Test
	void vernetzungChanged_createNewKante_deleteOld() {
		// arrange
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM)
			.build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM)
			.build();

		Kante kante1 = KanteTestDataProvider.fromKnoten(knoten1, knoten2).quelle(QuellSystem.DLM).dlmId(DlmId.of("123"))
			.build();
		netzService.saveKante(kante1);

		Kante kante2 = KanteTestDataProvider.fromKnoten(knoten2, knoten3).quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("456")).build();
		netzService.saveKante(kante2);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		Coordinate[] newCoordinates = new Coordinate[] { new Coordinate(0, 0), new Coordinate(100, 100) };
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(newCoordinates).fachId(kante1.getDlmId().getValue())
						.build(),
					ImportedFeatureTestDataProvider.withLineString(kante2.getGeometry().getCoordinates())
						.fachId(kante2.getDlmId().getValue()).build()));

		// act
		Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		Kante newKante = KanteTestDataProvider.fromKnoten(knoten1, knoten3).quelle(QuellSystem.DLM)
			.dlmId(kante1.getDlmId()).build();
		RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
			.withComparedFields("dlmId", "vonKnoten.id", "nachKnoten.id", "geometry")
			.withIgnoreAllOverriddenEquals(false)
			.build();
		assertThat(StreamSupport.stream(kantenRepository.findAll().spliterator(), false).toList())
			.usingRecursiveFieldByFieldElementComparator(configuration)
			.containsExactlyInAnyOrder(newKante, kante2);
		assertThat(kantenRepository.findById(kante1.getId())).isEmpty();

		assertThat(jobStatistik).isPresent();
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlImportierterKanten).isEqualTo(2);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlAktualisierterKanten).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlUnveraenderterKanten).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.getAnzahlImDlmGeloeschterKanten()).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.hinzugefuegteKanten.size()).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlTopologischStarkVeraenderterKanten).isEqualTo(1);
	}

	@Nested
	class VFoermigZuTFoermig {
		private Knoten knoten1;
		private Knoten knoten2;
		private Knoten knoten3;
		private Kante kante1;
		private Kante kante2;
		private String newDlmId;
		private String eigenname;
		private String bezeichnung;

		@BeforeEach
		void setup() {
			knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build();
			knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM)
				.build();
			knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 200), QuellSystem.DLM)
				.build();

			kante1 = KanteTestDataProvider.fromKnoten(knoten1, knoten2).quelle(QuellSystem.DLM).dlmId(DlmId.of("123"))
				.build();
			netzService.saveKante(kante1);

			kante2 = KanteTestDataProvider.fromKnoten(knoten2, knoten3).quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("456")).build();
			netzService.saveKante(kante2);

			entityManager.flush();
			entityManager.clear();

			newDlmId = "789";
			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			ImportedFeature newDlmFeature = ImportedFeatureTestDataProvider
				.withLineString(new Coordinate(100, 100), new Coordinate(50, 100))
				.fachId(newDlmId)
				.build();
			eigenname = "ABC-Straße";
			newDlmFeature.addAttribut("eigenname", eigenname);
			bezeichnung = "1a";
			newDlmFeature.addAttribut("bezeichnung", bezeichnung);
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(
						ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 0), new Coordinate(50, 100))
							.fachId(kante1.getDlmId().getValue())
							.build(),
						newDlmFeature,
						ImportedFeatureTestDataProvider.withLineString(new Coordinate(50, 100), new Coordinate(0, 200))
							.fachId(kante2.getDlmId().getValue()).build()));
		}

		@SuppressWarnings("unchecked")
		@Test
		void vernetzung_createsNewKnoten() {
			// act
			Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();
			entityManager.flush();
			entityManager.clear();

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(3);

			Optional<Kante> newKante1 = resultingKanten.stream().filter(k -> k.getDlmId().equals(kante1.getDlmId()))
				.findFirst();
			assertThat(newKante1).isPresent();
			assertThat(newKante1.get().getVonKnoten()).isEqualTo(knoten1);
			assertThat(newKante1.get().getGeometry())
				.isEqualTo(GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(50, 100)));

			Optional<Kante> newKante2 = resultingKanten.stream().filter(k -> k.getDlmId().equals(kante2.getDlmId()))
				.findFirst();
			assertThat(newKante2).isPresent();
			assertThat(newKante2.get().getNachKnoten()).isEqualTo(knoten3);
			assertThat(newKante2.get().getGeometry())
				.isEqualTo(GeometryTestdataProvider.createLineString(new Coordinate(50, 100), new Coordinate(0, 200)));

			Optional<Kante> kante3 = resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals(newDlmId))
				.findFirst();
			assertThat(kante3).isPresent();
			assertThat(kante3.get().getVonKnoten().getKoordinate()).isEqualTo(new Coordinate(100, 100));
			assertThat(kante3.get().getVonKnoten().getQuelle()).isEqualTo(QuellSystem.DLM);
			assertThat(kante3.get().getGeometry())
				.isEqualTo(
					GeometryTestdataProvider.createLineString(new Coordinate(100, 100), new Coordinate(50, 100)));

			assertThat(newKante1.get().getNachKnoten()).isEqualTo(kante3.get().getNachKnoten());
			assertThat(newKante2.get().getVonKnoten()).isEqualTo(kante3.get().getNachKnoten());

			assertThat(jobStatistik).isPresent();
			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).updateDlmNetzStatistik.anzahlImportierterKanten).isEqualTo(3);
			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).updateDlmNetzStatistik.anzahlAktualisierterKanten).isEqualTo(0);
			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).updateDlmNetzStatistik.getAnzahlImDlmGeloeschterKanten()).isEqualTo(0);
			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).updateDlmNetzStatistik.hinzugefuegteKanten.size()).isEqualTo(3);
			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).updateDlmNetzStatistik.anzahlHinzugefuegterKnoten).isEqualTo(1);
			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).anzahlGeloeschterKnoten).isEqualTo(0);
			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).updateDlmNetzStatistik.anzahlTopologischStarkVeraenderterKanten).isEqualTo(2);
		}

		@Test
		void neueKante_insertWithAttribute() {
			// act
			dlmReimportJob.doRun();
			entityManager.flush();
			entityManager.clear();

			// assert

			Optional<Kante> kante3 = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.filter(k -> k.getDlmId().getValue().equals(newDlmId))
				.findFirst();
			assertThat(kante3).isPresent();
			assertThat(kante3.get().getKantenAttributGruppe().getKantenAttribute().getStrassenName())
				.contains(StrassenName.of(eigenname));
			assertThat(kante3.get().getKantenAttributGruppe().getKantenAttribute().getStrassenNummer())
				.contains(StrassenNummer.of(bezeichnung));
			assertThat(kante3.get().isGrundnetz()).isTrue();
		}
	}

	@Test
	void knotenVerwaist_delete() {
		// arrange
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM)
			.build();

		Kante kante1 = KanteTestDataProvider.fromKnoten(knoten1, knoten2).quelle(QuellSystem.DLM).dlmId(DlmId.of("123"))
			.build();
		netzService.saveKante(kante1);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		Coordinate[] newCoordinates = new Coordinate[] { new Coordinate(0, 0), new Coordinate(100, 100) };
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(newCoordinates).fachId(kante1.getDlmId().getValue())
						.build()));

		// act
		Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(knotenRepository.findById(knoten2.getId())).isEmpty();
		assertThat(knotenRepository.count()).isEqualTo(2);

		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).anzahlGeloeschterKnoten).isEqualTo(1);
	}

	@SuppressWarnings("unchecked")
	@Test
	void kanteDurch2NeueErsetzt_deleteOld() {
		// arrange
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM)
			.build();

		Kante kante1 = KanteTestDataProvider.fromKnoten(knoten1, knoten2).quelle(QuellSystem.DLM).dlmId(DlmId.of("123"))
			.build();

		netzService.saveKante(kante1);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 0), new Coordinate(0, 50))
						.fachId("456")
						.build(),
					ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 50), new Coordinate(0, 100))
						.fachId("789").build()));

		// act
		Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
			.toList();
		assertThat(resultingKanten).hasSize(2);

		Optional<Kante> newKante1 = resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals("456"))
			.findFirst();
		assertThat(newKante1).isPresent();
		assertThat(newKante1.get().getVonKnoten()).isEqualTo(knoten1);
		assertThat(newKante1.get().getGeometry())
			.isEqualTo(GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(0, 50)));

		Optional<Kante> newKante2 = resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals("789"))
			.findFirst();
		assertThat(newKante2).isPresent();
		assertThat(newKante2.get().getNachKnoten()).isEqualTo(knoten2);
		assertThat(newKante2.get().getGeometry())
			.isEqualTo(GeometryTestdataProvider.createLineString(new Coordinate(0, 50), new Coordinate(0, 100)));

		assertThat(newKante2.get().getVonKnoten().getKoordinate()).isEqualTo(new Coordinate(0, 50));
		assertThat(newKante2.get().getVonKnoten()).isEqualTo(newKante1.get().getNachKnoten());

		assertThat(jobStatistik).isPresent();
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlImportierterKanten).isEqualTo(2);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlAktualisierterKanten).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.getAnzahlImDlmGeloeschterKanten()).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).anzahlGeloeschterKnoten).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.hinzugefuegteKanten.size()).isEqualTo(2);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlHinzugefuegterKnoten).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).anzahlGeloeschterKnoten).isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.anzahlTopologischStarkVeraenderterKanten).isEqualTo(0);
	}

	@Nested
	class MultiplePartitions {

		private Envelope envelope1;
		private Envelope envelope2;

		@BeforeEach
		void setup() {
			envelope1 = new Envelope(new Coordinate(0, 0), new Coordinate(200, 200));
			envelope2 = new Envelope(new Coordinate(0, 200), new Coordinate(200, 400));
			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(envelope1, envelope2));
		}

		@SuppressWarnings("unchecked")
		@Test
		void kanteInNextPartitionVerschoben_createNewKanteDeleteOld() {
			// arrange
			Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
				.dlmId(DlmId.of("123")).build();
			netzService.saveKante(kante1);

			entityManager.flush();
			entityManager.clear();

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(envelope1, envelope2));
			when(dlmRepository.getKanten(envelope1)).thenReturn(Collections.emptyList());
			when(dlmRepository.getKanten(envelope2))
				.thenReturn(
					List.of(
						ImportedFeatureTestDataProvider
							.withLineString(new Coordinate(0, 250), new Coordinate(0, 350))
							.fachId(kante1.getDlmId().getValue()).build()));

			// act
			dlmReimportJob.doRun();
			entityManager.flush();
			entityManager.clear();

			// assert

			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(resultingKanten.get(0).getGeometry())
				.isEqualTo(GeometryTestdataProvider.createLineString(new Coordinate(0, 250), new Coordinate(0, 350)));
			assertThat(resultingKanten.get(0).getDlmId()).isEqualTo(kante1.getDlmId());
			assertThat(knotenRepository.count()).isEqualTo(2);
		}

		@Test
		void kanteAcross2Partitions_useNewKnoten() {
			// arrange
			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(envelope1, envelope2));
			when(dlmRepository.getKanten(envelope1)).thenReturn(Collections.emptyList());
			ImportedFeature dlmKanteAcrossPartitions = ImportedFeatureTestDataProvider
				.withLineString(new Coordinate(0, 0), new Coordinate(0, 300))
				.fachId("123").build();
			when(dlmRepository.getKanten(envelope1))
				.thenReturn(List.of(dlmKanteAcrossPartitions));
			when(dlmRepository.getKanten(envelope2))
				.thenReturn(
					List.of(dlmKanteAcrossPartitions,
						ImportedFeatureTestDataProvider
							.withLineString(new Coordinate(0, 300), new Coordinate(300, 250))
							.fachId("456").build()));

			// act
			dlmReimportJob.doRun();
			entityManager.flush();
			entityManager.clear();

			// assert

			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(2);
			assertThat(resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals("123")).findFirst().get()
				.getNachKnoten())
					.isEqualTo(resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals("456")).findFirst()
						.get().getVonKnoten());
			assertThat(knotenRepository.count()).isEqualTo(3);

		}

		@Test
		void newKanteAcross2Partitions_use2ExistingKnoten() {
			// arrange
			Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
				.dlmId(DlmId.of("123")).build();
			netzService.saveKante(kante1);
			Kante kante2 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 300, 300, 300, QuellSystem.DLM)
				.dlmId(DlmId.of("456")).build();
			netzService.saveKante(kante2);

			entityManager.flush();
			entityManager.clear();

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(envelope1, envelope2));
			when(dlmRepository.getKanten(envelope1)).thenReturn(Collections.emptyList());
			ImportedFeature dlmKanteAcrossPartitions = ImportedFeatureTestDataProvider
				.withLineString(new Coordinate(100, 100), new Coordinate(0, 300))
				.fachId("789").build();
			when(dlmRepository.getKanten(envelope1))
				.thenReturn(
					List.of(dlmKanteAcrossPartitions, ImportedFeatureTestDataProvider
						.withLineString(kante1.getGeometry().getCoordinates())
						.fachId(kante1.getDlmId().getValue()).build()));
			when(dlmRepository.getKanten(envelope2))
				.thenReturn(
					List.of(dlmKanteAcrossPartitions, ImportedFeatureTestDataProvider
						.withLineString(kante2.getGeometry().getCoordinates())
						.fachId(kante2.getDlmId().getValue()).build()));

			// act
			dlmReimportJob.doRun();
			entityManager.flush();
			entityManager.clear();

			// assert

			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(3);
			Optional<Kante> newKante = resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals("789"))
				.findFirst();
			assertThat(newKante).isPresent();
			assertThat(newKante.get().getVonKnoten()).isEqualTo(kante1.getNachKnoten());
			assertThat(newKante.get().getNachKnoten()).isEqualTo(kante2.getVonKnoten());
			assertThat(knotenRepository.count()).isEqualTo(4);
		}

		@Test
		void kanteAcross2Partitions_useExistingKnoten() {
			// arrange
			Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
				.dlmId(DlmId.of("123")).build();
			netzService.saveKante(kante1);
			Kante kante2 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 250, 300, 300, QuellSystem.DLM)
				.dlmId(DlmId.of("456")).build();
			netzService.saveKante(kante2);

			entityManager.flush();
			entityManager.clear();

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(envelope1, envelope2));
			when(dlmRepository.getKanten(envelope1)).thenReturn(Collections.emptyList());
			ImportedFeature dlmKanteAcrossPartitions = ImportedFeatureTestDataProvider
				.withLineString(new Coordinate(0, 0), new Coordinate(0, 250))
				.fachId(kante1.getDlmId().getValue()).build();
			when(dlmRepository.getKanten(envelope1))
				.thenReturn(List.of(dlmKanteAcrossPartitions));
			when(dlmRepository.getKanten(envelope2))
				.thenReturn(List.of(dlmKanteAcrossPartitions, ImportedFeatureTestDataProvider
					.withLineString(kante2.getGeometry().getCoordinates())
					.fachId(kante2.getDlmId().getValue()).build()));

			// act
			dlmReimportJob.doRun();
			entityManager.flush();
			entityManager.clear();

			// assert

			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(2);
			assertThat(resultingKanten.stream().filter(k -> k.getDlmId().equals(kante1.getDlmId())).findFirst().get()
				.getNachKnoten())
					.isEqualTo(resultingKanten.stream().filter(k -> k.getDlmId().equals(kante2.getDlmId())).findFirst()
						.get().getVonKnoten());
			assertThat(knotenRepository.count()).isEqualTo(3);
		}
	}

	@Test
	void doesNotTouchRadvisKanten() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.RadVis).build();
		netzService.saveKante(kante1);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(Collections.emptyList());

		// act
		dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(knotenRepository.count()).isEqualTo(2);
		assertThat(kantenRepository.count()).isEqualTo(1);
	}
}
