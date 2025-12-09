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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import de.wps.radvis.backend.auditing.AuditingConfiguration;
import de.wps.radvis.backend.auditing.schnittstelle.WithAuditingAspect;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.domain.AuditingTestIT;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeUebertragungService;
import de.wps.radvis.backend.integration.dlm.domain.entity.DlmReimportJobStatistik;
import de.wps.radvis.backend.matching.domain.repository.CustomDlmMatchingRepositoryFactory;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.service.CustomGrundnetzMappingServiceFactory;
import de.wps.radvis.backend.matching.domain.service.DlmPbfErstellungService;
import de.wps.radvis.backend.matching.domain.service.GraphhopperUpdateService;
import de.wps.radvis.backend.matching.domain.service.GrundnetzMappingService;
import de.wps.radvis.backend.matching.domain.service.KanteUpdateElevationService;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopper;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.provider.FahrtrichtungAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DlmRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group3")
@ContextConfiguration(classes = {
	NetzConfiguration.class, CommonConfiguration.class, AuditingConfiguration.class, WithAuditingAspect.class,
	DlmReimportJobAttributMappingTestIT.DlmReimportJobAttributMappingTestITConfiguration.class })
@EntityScan(basePackageClasses = { BenutzerConfiguration.class, OrganisationConfiguration.class })
@Transactional
class DlmReimportJobAttributMappingTestIT extends AuditingTestIT {
	@Configuration
	static class DlmReimportJobAttributMappingTestITConfiguration {
		@Autowired
		private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
		@Autowired
		private DlmRepository dlmRepository;
		@Autowired
		private DlmPbfErstellungService dlmPbfErstellungService;
		@Autowired
		private NetzService netzService;
		@Autowired
		private KantenRepository kantenRepository;
		@Autowired
		private KnotenRepository knotenRepository;
		@Autowired
		private CustomDlmMatchingRepositoryFactory customDlmMatchingRepositoryFactory;
		@Autowired
		private CustomGrundnetzMappingServiceFactory customGrundnetzMappingServiceFactory;

		@MockitoBean
		private KanteUpdateElevationService elevationUpdateService;

		@Bean
		public DlmReimportJob dlmReimportJob() throws IOException {
			return new DlmReimportJob(
				jobExecutionDescriptionRepository,
				dlmPbfErstellungService,
				kantenAttributeUebertragungService(),
				new VernetzungService(kantenRepository, knotenRepository, netzService),
				netzService,
				new DlmImportService(dlmRepository, netzService),
				customDlmMatchingRepositoryFactory, customGrundnetzMappingServiceFactory);
		}

		@Bean
		public KantenAttributeUebertragungService kantenAttributeUebertragungService() {
			return new KantenAttributeUebertragungService(Laenge.of(1.0));
		}
	}

	@Autowired
	DlmReimportJob dlmReimportJob;
	@Autowired
	private NetzService netzService;
	@Autowired
	private KantenRepository kantenRepository;
	@PersistenceContext
	private EntityManager entityManager;

	@MockitoBean
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@MockitoBean
	private DlmRepository dlmRepository;
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
	CommonConfigurationProperties commonConfigurationProperties;
	@MockitoBean
	CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;
	@MockitoBean
	private DlmPbfErstellungService dlmPbfErstellungService;
	@MockitoBean
	private GraphhopperUpdateService graphhopperUpdaterService;
	@MockitoBean
	private SimpleMatchingService simpleMatchingService;
	@MockitoBean
	private CustomDlmMatchingRepositoryFactory customDlmMatchingRepositoryFactory;
	@MockitoBean
	private CustomGrundnetzMappingServiceFactory customGrundnetzMappingServiceFactory;
	@MockitoBean
	private NetzConfigurationProperties netzConfigurationProperties;

	@Mock
	private DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory;
	@Mock
	private DlmMatchedGraphHopper dlmMatchedGraphHopper;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		when(featureToggleProperties.isShowDlm()).thenReturn(true);
		when(postgisConfigurationProperties.getArgumentLimit()).thenReturn(2);
		when(jobExecutionDescriptionRepository
			.findFirstByNameEqualsOrderByExecutionStartDesc(any())).thenReturn(Optional.empty());
		when(jobExecutionDescriptionRepository.save(any()))
			.thenAnswer(invocationMock -> invocationMock.getArguments()[0]);
		when(customGrundnetzMappingServiceFactory.createGrundnetzMappingService(any()))
			.thenReturn(new GrundnetzMappingService(simpleMatchingService));
		when(customDlmMatchingRepositoryFactory.createCustomMatchingRepository(any()))
			.thenReturn(mock(DlmMatchingRepository.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void handlingKantenOhneMatch() throws IOException {
		// arrange
		Kante kante1 = KanteTestDataProvider
			.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("123"))
			.build();
		Kante kante2 = KanteTestDataProvider
			.withCoordinatesAndQuelle(100, 100, 200, 200, QuellSystem.DLM)
			.dlmId(DlmId.of("234"))
			.build();
		netzService.saveKante(kante1);
		netzService.saveKante(kante2);

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		ImportedFeature importedKante1Feature = ImportedFeatureTestDataProvider
			.withLineString(new Coordinate(50, 50), kante1.getNachKnoten().getKoordinate())
			.fachId(kante1.getDlmId().getValue())
			.build();
		when(dlmRepository.getKanten(any())).thenReturn(List.of(importedKante1Feature));

		// Die ID der hinzugefügten Kante wird automatisch vergeben (beim save() am KantenRepo) und hier entsprechend
		// nicht bekannt. Diese ID brauchen wir aber für das Matching-Result, da ja auf neue Kanten gematcht wird. Daher
		// speichern wir hier die Kanten-IDs, die an die PBF-Erstellung übergeben werden.
		HashMap<String, Long> dlmIdToNewKanteId = new HashMap<>();
		doAnswer(invocationOnMock -> {
			((Collection<Kante>) invocationOnMock.getArgument(1)).forEach(kante -> {
				dlmIdToNewKanteId.put(kante.getDlmId().getValue(), kante.getId());
			});
			return null;
		}).when(dlmPbfErstellungService).erstellePbfForKanten(any(), any());

		when(simpleMatchingService.matche(eq(kante1.getGeometry()), any())).thenAnswer(invocationOnMock -> {
			OsmMatchResult importedFeatureMatchingResult = new OsmMatchResult(
				(LineString) importedKante1Feature.getGeometrie(),
				List.of(OsmWayId.of(dlmIdToNewKanteId.get(kante1.getDlmId().getValue()))));
			return Optional.of(importedFeatureMatchingResult);
		});
		when(simpleMatchingService.matche(eq(kante2.getGeometry()), any())).thenReturn(Optional.empty());

		// act
		Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();
		entityManager.flush();

		// assert
		List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
			.toList();
		assertThat(resultingKanten).hasSize(1);

		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).updateDlmNetzStatistik.getAnzahlImDlmGeloeschterKanten()).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) jobStatistik
			.get()).anzahlKantenOhneAttributuebertragung).isEqualTo(1);
	}

	@Nested
	class AllgemeineAttributeTest {
		@Test
		void allgmeineAttribute_uebertragen() {
			// arrange
			Kante kante1 = netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue()
						.istStandards(new HashSet<>(Set.of(IstStandard.BASISSTANDARD)))
						.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
							.beleuchtung(Beleuchtung.RETROREFLEKTIERENDE_RANDMARKIERUNG)
							.dtvRadverkehr(VerkehrStaerke.of(3)).build())
						.netzklassen(new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)))
						.build())
				.dlmId(DlmId.of("123")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider.withLineString(kante1.getGeometry().getCoordinates())
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(resultingKanten.get(0).getKantenAttributGruppe().getNetzklassen())
				.containsExactlyElementsOf(kante1.getKantenAttributGruppe().getNetzklassen());
			assertThat(resultingKanten.get(0).getKantenAttributGruppe().getIstStandards())
				.containsExactlyElementsOf(kante1.getKantenAttributGruppe().getIstStandards());
			assertThat(resultingKanten.get(0).getKantenAttributGruppe().getKantenAttribute())
				.isEqualTo(kante1.getKantenAttributGruppe().getKantenAttribute());

			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).anzahlKantenMitAttributuebertragung).isEqualTo(1);
		}

		@Test
		void allgmeineAttribute_noErrorOnKollision() {
			// arrange
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue()
						.istStandards(new HashSet<>(Set.of(IstStandard.BASISSTANDARD)))
						.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
							.beleuchtung(Beleuchtung.RETROREFLEKTIERENDE_RANDMARKIERUNG)
							.dtvRadverkehr(VerkehrStaerke.of(3)).build())
						.netzklassen(new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)))
						.build())
				.dlmId(DlmId.of("123")).build());
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 100, 100, 100, QuellSystem.DLM)
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue()
						.istStandards(new HashSet<>(Set.of(IstStandard.RADVORRANGROUTEN)))
						.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
							.beleuchtung(Beleuchtung.VORHANDEN)
							.dtvRadverkehr(VerkehrStaerke.of(3)).build())
						.netzklassen(new HashSet<>(Set.of(Netzklasse.KOMMUNALNETZ_FREIZEIT)))
						.build())
				.dlmId(DlmId.of("456")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider
						.withLineString(new Coordinate(0, 0), new Coordinate(0, 100), new Coordinate(100, 100))
						.fachId("789").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("789")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();

			// assert
			assertThat(jobStatistik).isPresent();
			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).anzahlMoeglicherKollisionen).isEqualTo(1);
		}
	}

	@Test
	void linearReferenziert_AufNeueKanteZuschneidenUndUebertragen_zuKleineSegmenteEntfernen() {
		Kante kante = netzService.saveKante(KanteTestDataProvider
			.withCoordinatesAndQuelle(0, 0, 0, 99.5, QuellSystem.DLM)
			.isZweiseitig(true)
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("Erster Abschnitt")).build())))
			.fahrtrichtungAttributGruppe(new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, true))
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte()
					.geschwindigkeitAttribute(
						(List.of(
							GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1)
								.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH).build())))
					.build())
			.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(List.of(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).belagArt(BelagArt.BETON)
					.build()),
				List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).belagArt(BelagArt.ASPHALT)
						.build()),
				true))
			.dlmId(DlmId.of("123")).build());

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(ImportedFeatureTestDataProvider
					.withLineString(new Coordinate(0, 0), new Coordinate(0, 100))
					.fachId("456").build()));
		when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
			return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
					return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
				});
		});

		// act
		dlmReimportJob.run(true);

		// assert
		List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
			.toList();
		assertThat(resultingKanten).hasSize(1);
		assertThat(resultingKanten.get(0).getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.hasSize(1);
		assertThat(resultingKanten.get(0).getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrderElementsOf(
				kante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute());
		assertThat(resultingKanten.get(0).getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrderElementsOf(
				kante.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute());
		assertThat(resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrderElementsOf(
				kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());
		assertThat(resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrderElementsOf(
				kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts());
	}

	@Nested
	class FahrtrichtungAttributGruppeTest {
		@Test
		void kanteZweiseitig_vertauscheSeitenAbhVonStationierungsrichtung() {
			// arrange
			Kante kante1 = netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
				.isZweiseitig(true)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte().build()), true))
				.fahrtrichtungAttributGruppe(
					new FahrtrichtungAttributGruppe(Richtung.IN_RICHTUNG, Richtung.GEGEN_RICHTUNG, true))
				.dlmId(DlmId.of("123")).build());
			Kante kante2 = netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 100, 100, 100, QuellSystem.DLM)
				.isZweiseitig(true)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte().build()), true))
				.fahrtrichtungAttributGruppe(
					new FahrtrichtungAttributGruppe(Richtung.IN_RICHTUNG, Richtung.GEGEN_RICHTUNG, true))
				.dlmId(DlmId.of("456")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(
						ImportedFeatureTestDataProvider.withLineString(kante1.getGeometry().reverse().getCoordinates())
							.fachId("789").build(),
						ImportedFeatureTestDataProvider.withLineString(kante2.getGeometry().getCoordinates())
							.fachId("101").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				Stream<Kante> kantenFromDb = StreamSupport.stream(kantenRepository.findAll().spliterator(), false);

				if (invocationOnMock.getArgument(0).equals(kante1.getGeometry())) {
					return kantenFromDb.filter(k -> k.getDlmId().getValue().equals("789")).findAny()
						.map(k -> new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId()))));
				}

				return kantenFromDb.filter(k -> k.getDlmId().getValue().equals("101")).findAny()
					.map(k -> new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId()))));
			});

			// act

			Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(2);

			Optional<Kante> newKante1 = resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals("789"))
				.findAny();
			Optional<Kante> newKante2 = resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals("101"))
				.findAny();

			assertThat(newKante1).isPresent();
			assertThat(newKante2).isPresent();

			assertThat(newKante1.get().isZweiseitig()).isTrue();
			assertThat(newKante1.get().getFahrtrichtungAttributGruppe().getFahrtrichtungLinks())
				.isEqualTo(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts());
			assertThat(newKante1.get().getFahrtrichtungAttributGruppe().getFahrtrichtungRechts())
				.isEqualTo(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks());

			assertThat(newKante2.get().isZweiseitig()).isTrue();
			assertThat(newKante2.get().getFahrtrichtungAttributGruppe().getFahrtrichtungLinks())
				.isEqualTo(kante2.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks());
			assertThat(newKante2.get().getFahrtrichtungAttributGruppe().getFahrtrichtungRechts())
				.isEqualTo(kante2.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts());

			assertThat(jobStatistik).isPresent();
			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).anzahlMoeglicherKollisionen).isEqualTo(0);
		}

		@Test
		void kanteEinseitig_changeValueAbhVonStationierungsrichtung() {
			// arrange
			Kante kante1 = netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
				.isZweiseitig(false)
				.fahrtrichtungAttributGruppe(
					new FahrtrichtungAttributGruppe(Richtung.GEGEN_RICHTUNG, false))
				.dlmId(DlmId.of("123")).build());
			Kante kante2 = netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 100, 100, 100, QuellSystem.DLM)
				.isZweiseitig(false)
				.fahrtrichtungAttributGruppe(
					new FahrtrichtungAttributGruppe(Richtung.GEGEN_RICHTUNG, false))
				.dlmId(DlmId.of("456")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(
						ImportedFeatureTestDataProvider.withLineString(kante1.getGeometry().reverse().getCoordinates())
							.fachId("789").build(),
						ImportedFeatureTestDataProvider.withLineString(kante2.getGeometry().getCoordinates())
							.fachId("101").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				Stream<Kante> kantenFromDb = StreamSupport.stream(kantenRepository.findAll().spliterator(), false);

				if (invocationOnMock.getArgument(0).equals(kante1.getGeometry())) {
					return kantenFromDb.filter(k -> k.getDlmId().getValue().equals("789")).findAny()
						.map(k -> new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId()))));
				}

				return kantenFromDb.filter(k -> k.getDlmId().getValue().equals("101")).findAny()
					.map(k -> new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId()))));
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(2);

			Optional<Kante> newKante1 = resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals("789"))
				.findAny();
			Optional<Kante> newKante2 = resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals("101"))
				.findAny();

			assertThat(newKante1).isPresent();
			assertThat(newKante2).isPresent();

			assertThat(newKante1.get().isZweiseitig()).isFalse();
			assertThat(newKante1.get().getFahrtrichtungAttributGruppe().getFahrtrichtungLinks())
				.isEqualTo(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks().umgedreht());
			assertThat(newKante1.get().getFahrtrichtungAttributGruppe().getFahrtrichtungRechts())
				.isEqualTo(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts().umgedreht());

			assertThat(newKante2.get().isZweiseitig()).isFalse();
			assertThat(newKante2.get().getFahrtrichtungAttributGruppe().getFahrtrichtungLinks())
				.isEqualTo(kante2.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks());
			assertThat(newKante2.get().getFahrtrichtungAttributGruppe().getFahrtrichtungRechts())
				.isEqualTo(kante2.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts());
		}

		@Test
		void kanteEinseitig_valueBeidseitigOrUnbekannt_valueUnchanged() {
			// arrange
			Kante kante1 = netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
				.isZweiseitig(false)
				.fahrtrichtungAttributGruppe(
					new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, false))
				.dlmId(DlmId.of("123")).build());
			Kante kante2 = netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 100, 100, 100, QuellSystem.DLM)
				.isZweiseitig(false)
				.fahrtrichtungAttributGruppe(
					new FahrtrichtungAttributGruppe(Richtung.UNBEKANNT, false))
				.dlmId(DlmId.of("456")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(
						ImportedFeatureTestDataProvider.withLineString(kante1.getGeometry().reverse().getCoordinates())
							.fachId("789").build(),
						ImportedFeatureTestDataProvider.withLineString(kante2.getGeometry().reverse().getCoordinates())
							.fachId("101").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				Stream<Kante> kantenFromDb = StreamSupport.stream(kantenRepository.findAll().spliterator(), false);

				if (invocationOnMock.getArgument(0).equals(kante1.getGeometry())) {
					return kantenFromDb.filter(k -> k.getDlmId().getValue().equals("789")).findAny()
						.map(k -> new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId()))));
				}

				return kantenFromDb.filter(k -> k.getDlmId().getValue().equals("101")).findAny()
					.map(k -> new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId()))));
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(2);

			Optional<Kante> newKante1 = resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals("789"))
				.findAny();
			Optional<Kante> newKante2 = resultingKanten.stream().filter(k -> k.getDlmId().getValue().equals("101"))
				.findAny();

			assertThat(newKante1).isPresent();
			assertThat(newKante2).isPresent();

			assertThat(newKante1.get().isZweiseitig()).isFalse();
			assertThat(newKante1.get().getFahrtrichtungAttributGruppe().getFahrtrichtungLinks())
				.isEqualTo(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks());
			assertThat(newKante1.get().getFahrtrichtungAttributGruppe().getFahrtrichtungRechts())
				.isEqualTo(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts());

			assertThat(newKante2.get().isZweiseitig()).isFalse();
			assertThat(newKante2.get().getFahrtrichtungAttributGruppe().getFahrtrichtungLinks())
				.isEqualTo(kante2.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks());
			assertThat(newKante2.get().getFahrtrichtungAttributGruppe().getFahrtrichtungRechts())
				.isEqualTo(kante2.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts());
		}
	}

	@Nested
	class ZustaendigkeitsAttributGruppeTest {
		@Test
		void linearReferenziert_AufNeueKanteZuschneidenUndUebertragen() {
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.1)
						.vereinbarungsKennung(VereinbarungsKennung.of("Erster Abschnitt")).build(),
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.1, 0.5)
						.vereinbarungsKennung(VereinbarungsKennung.of("Zweiter Abschnitt")).build(),
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.5, 1)
						.vereinbarungsKennung(VereinbarungsKennung.of("Dritter Abschnitt")).build())))
				.dlmId(DlmId.of("123")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider
						.withLineString(new Coordinate(0, 40), new Coordinate(0, 90))
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(resultingKanten.get(0).getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.hasSize(2);
			assertThat(resultingKanten.get(0).getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrder(
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.2)
						.vereinbarungsKennung(VereinbarungsKennung.of("Zweiter Abschnitt")).build(),
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 1)
						.vereinbarungsKennung(VereinbarungsKennung.of("Dritter Abschnitt")).build());
		}

		@Test
		void linearReferenziert_AufNeueKanteZuschneidenUndUebertragen_stationierungsrichtungBeruecksichtigen() {
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.5)
						.vereinbarungsKennung(VereinbarungsKennung.of("Zweiter Abschnitt")).build(),
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.5, 1)
						.vereinbarungsKennung(VereinbarungsKennung.of("Dritter Abschnitt")).build())))
				.dlmId(DlmId.of("123")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider
						.withLineString(new Coordinate(0, 90), new Coordinate(0, 40))
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(resultingKanten.get(0).getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.hasSize(2);
			assertThat(resultingKanten.get(0).getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrder(
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.8)
						.vereinbarungsKennung(VereinbarungsKennung.of("Dritter Abschnitt")).build(),
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.8, 1)
						.vereinbarungsKennung(VereinbarungsKennung.of("Zweiter Abschnitt")).build());

			assertThat(jobStatistik).isPresent();
			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).anzahlMoeglicherKollisionen).isEqualTo(0);
		}

		@Test
		void linearReferenziert_AufNeueKanteZuschneidenUndUebertragen_defragmentieren() {
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 50, QuellSystem.DLM)
				.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.5)
						.vereinbarungsKennung(VereinbarungsKennung.of("Erster Abschnitt")).build(),
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.5, 1)
						.vereinbarungsKennung(VereinbarungsKennung.of("Zweiter Abschnitt")).build())))
				.dlmId(DlmId.of("123")).build());
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 50, 0, 100, QuellSystem.DLM)
				.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
						.vereinbarungsKennung(VereinbarungsKennung.of("Zweiter Abschnitt")).build())))
				.dlmId(DlmId.of("456")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider
						.withLineString(new Coordinate(0, 0), new Coordinate(0, 100))
						.fachId("789").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("789")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			Optional<JobStatistik> jobStatistik = dlmReimportJob.doRun();

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(resultingKanten.get(0).getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.hasSize(2);
			assertThat(resultingKanten.get(0).getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrder(
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.25)
						.vereinbarungsKennung(VereinbarungsKennung.of("Erster Abschnitt")).build(),
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.25, 1)
						.vereinbarungsKennung(VereinbarungsKennung.of("Zweiter Abschnitt")).build());

			assertThat(jobStatistik).isPresent();
			assertThat(((DlmReimportJobStatistik) jobStatistik
				.get()).anzahlMoeglicherKollisionen).isEqualTo(1);
		}

	}

	@Nested
	class FuehrungsformAttributGruppeTest {
		@Test
		void einseitigeKante_linearReferenziert_aufNeuKanteZuschneidenUndUebertragen() {
			// arrange
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.1).belagArt(BelagArt.ASPHALT)
						.build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.1, 0.5).belagArt(BelagArt.BETON)
						.build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
						.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
						.build()),
					false))
				.dlmId(DlmId.of("123")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 40), new Coordinate(0, 90))
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			List<FuehrungsformAttribute> expectedValues = List.of(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.0, 0.2).belagArt(BelagArt.BETON)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.2, 1)
					.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
					.build());
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
					.containsExactlyInAnyOrderElementsOf(expectedValues);
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
					.containsExactlyInAnyOrderElementsOf(expectedValues);
		}

		@Test
		void einseitigeKante_linearReferenziert_defragmentieren() {
			// arrange
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 50, QuellSystem.DLM)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5).belagArt(BelagArt.BETON)
						.build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
						.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
						.build()),
					false))
				.dlmId(DlmId.of("123")).build());

			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 50, 0, 100, QuellSystem.DLM)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
						.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
						.build()),
					false))
				.dlmId(DlmId.of("789")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 0), new Coordinate(0, 100))
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			List<FuehrungsformAttribute> expectedValues = List.of(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.0, 0.25).belagArt(BelagArt.BETON)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.25, 1)
					.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
					.build());
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
					.containsExactlyInAnyOrderElementsOf(expectedValues);
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
					.containsExactlyInAnyOrderElementsOf(expectedValues);
		}

		@Test
		void einseitigeKante_mitSicherheitstrennstreifen_stationierungsrichtungBeachten() {
			// arrange
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.isZweiseitig(false)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).belagArt(BelagArt.ASPHALT)
						.radverkehrsfuehrung(Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND)
						.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM)
						.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR)
						.trennstreifenBreiteRechts(Laenge.of(0.1))
						.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN)
						.trennstreifenBreiteLinks(Laenge.of(0.2))
						.build()),
					false))
				.dlmId(DlmId.of("123")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 100), new Coordinate(0, 0))
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			FuehrungsformAttribute expectedValue = FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
				.belagArt(BelagArt.ASPHALT)
				.radverkehrsfuehrung(Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND)
				.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM)
				.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR)
				.trennstreifenBreiteLinks(Laenge.of(0.1))
				.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN)
				.trennstreifenBreiteRechts(Laenge.of(0.2))
				.build();
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
					.containsExactly(expectedValue);
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
					.containsExactly(expectedValue);
		}

		@Test
		void zweiseitigeKante_linearReferenziert_aufNeuKanteZuschneidenUndUebertragen() {
			// arrange
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.isZweiseitig(true)
				.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider
					.withGrundnetzDefaultwerte().isZweiseitig(true).build())
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.1).belagArt(BelagArt.ASPHALT)
						.build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.1, 0.5).belagArt(BelagArt.BETON)
						.build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
						.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
						.build()),
					List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.1).belagArt(BelagArt.ASPHALT)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.1, 0.6)
							.belagArt(BelagArt.NATURSTEINPFLASTER)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1)
							.belagArt(BelagArt.SONSTIGER_BELAG)
							.build()),
					true))
				.dlmId(DlmId.of("123")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 40), new Coordinate(0, 90))
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
					.containsExactlyInAnyOrder(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.0, 0.2).belagArt(BelagArt.BETON)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.2, 1)
							.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
							.build());
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
					.containsExactlyInAnyOrder(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.0, 0.4)
							.belagArt(BelagArt.NATURSTEINPFLASTER)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1)
							.belagArt(BelagArt.SONSTIGER_BELAG)
							.build());
		}

		@Test
		void zweiseitigeKante_linearReferenziert_stationierungsrichtungBeachten() {
			// arrange
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.isZweiseitig(true)
				.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider
					.withGrundnetzDefaultwerte().isZweiseitig(true).build())
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.1).belagArt(BelagArt.ASPHALT)
						.build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.1, 0.5).belagArt(BelagArt.BETON)
						.build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
						.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
						.build()),
					List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.1).belagArt(BelagArt.ASPHALT)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.1, 0.8)
							.belagArt(BelagArt.NATURSTEINPFLASTER)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.8, 1)
							.belagArt(BelagArt.SONSTIGER_BELAG)
							.build()),
					true))
				.dlmId(DlmId.of("123")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 90), new Coordinate(0, 40))
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
					.containsExactlyInAnyOrder(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.0, 0.8)
							.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.8, 1)
							.belagArt(BelagArt.BETON)
							.build());
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
					.containsExactlyInAnyOrder(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.2, 1)
							.belagArt(BelagArt.NATURSTEINPFLASTER)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.2)
							.belagArt(BelagArt.SONSTIGER_BELAG)
							.build());
		}

		@Test
		void zweiseitigeKante_linearReferenziert_defragmentieren() {
			// arrange
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 50, QuellSystem.DLM)
				.isZweiseitig(true)
				.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider
					.withGrundnetzDefaultwerte().isZweiseitig(true).build())
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5).belagArt(BelagArt.BETON)
						.build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
						.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
						.build()),
					List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).belagArt(BelagArt.ASPHALT)
							.build()),
					true))
				.dlmId(DlmId.of("123")).build());

			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 50, 0, 100, QuellSystem.DLM)
				.isZweiseitig(true)
				.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider
					.withGrundnetzDefaultwerte().isZweiseitig(true).build())
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
						.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
						.build()),
					List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5).belagArt(BelagArt.ASPHALT)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
							.belagArt(BelagArt.NATURSTEINPFLASTER)
							.build()),
					true))
				.dlmId(DlmId.of("789")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 0), new Coordinate(0, 100))
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
					.containsExactlyInAnyOrder(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.0, 0.25).belagArt(BelagArt.BETON)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.25, 1)
							.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
							.build());
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
					.containsExactlyInAnyOrder(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.0, 0.75)
							.belagArt(BelagArt.ASPHALT)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.75, 1)
							.belagArt(BelagArt.NATURSTEINPFLASTER)
							.build());
		}

		@Test
		void zweiseitigeKante_mitSicherheitstrennstreifen_stationierungsrichtungBeachten() {
			// arrange
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.isZweiseitig(true)
				.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider
					.withGrundnetzDefaultwerte().isZweiseitig(true).build())
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).belagArt(BelagArt.ASPHALT)
						.radverkehrsfuehrung(Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND)
						.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM)
						.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR)
						.trennstreifenBreiteRechts(Laenge.of(0.1))
						.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN)
						.trennstreifenBreiteLinks(Laenge.of(0.2))
						.build()),
					List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
							.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
							.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
							.trennstreifenBreiteRechts(Laenge.of(1))
							.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
							.trennstreifenBreiteLinks(Laenge.of(0.5))
							.belagArt(BelagArt.BETON)
							.build()),
					true))
				.dlmId(DlmId.of("123")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 100), new Coordinate(0, 0))
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
					.containsExactly(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).belagArt(BelagArt.ASPHALT)
							.radverkehrsfuehrung(Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND)
							.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM)
							.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR)
							.trennstreifenBreiteLinks(Laenge.of(0.1))
							.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN)
							.trennstreifenBreiteRechts(Laenge.of(0.2))
							.build());
			assertThat(
				resultingKanten.get(0).getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
					.containsExactly(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
						.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
						.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
						.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
						.trennstreifenBreiteLinks(Laenge.of(1))
						.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
						.trennstreifenBreiteRechts(Laenge.of(0.5))
						.belagArt(BelagArt.BETON)
						.build());
		}
	}

	@Nested
	class GeschwindigkeitAttributGruppeTest {
		@Test
		void linearReferenziert_AufNeueKanteZuschneidenUndUebertragen() {
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							(List.of(
								GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.1)
									.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build(),
								GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.1, 0.5)
									.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_40_KMH).build(),
								GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.5, 1)
									.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH).build())))
						.build())
				.dlmId(DlmId.of("123")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider
						.withLineString(new Coordinate(0, 40), new Coordinate(0, 90))
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(resultingKanten.get(0).getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute())
				.hasSize(2);
			assertThat(resultingKanten.get(0).getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute())
				.containsExactlyInAnyOrder(
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.2)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_40_KMH).build(),
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.2, 1)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH).build());
		}

		@Test
		void linearReferenziert_AufNeueKanteZuschneidenUndUebertragen_stationierungsrichtungBeruecksichtigen() {
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							(List.of(
								GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.5)
									.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_40_KMH)
									.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
										Hoechstgeschwindigkeit.MAX_90_KMH)
									.build(),
								GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.5, 1)
									.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH).build())))
						.build())
				.dlmId(DlmId.of("123")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider
						.withLineString(new Coordinate(0, 90), new Coordinate(0, 40))
						.fachId("456").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("456")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(resultingKanten.get(0).getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute())
				.hasSize(2);
			assertThat(resultingKanten.get(0).getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute())
				.containsExactlyInAnyOrder(
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.8, 1)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_90_KMH)
						.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
							Hoechstgeschwindigkeit.MAX_40_KMH)
						.build(),
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.8)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH).build());
		}

		@Test
		void linearReferenziert_AufNeueKanteZuschneidenUndUebertragen_defragmentieren() {
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 0, 0, 50, QuellSystem.DLM)
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							(List.of(
								GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.5)
									.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_40_KMH)
									.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
										Hoechstgeschwindigkeit.MAX_90_KMH)
									.build(),
								GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.5, 1)
									.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH).build())))
						.build())
				.dlmId(DlmId.of("123")).build());
			netzService.saveKante(KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 50, 0, 100, QuellSystem.DLM)
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							(List.of(
								GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1)
									.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH).build())))
						.build())
				.dlmId(DlmId.of("456")).build());

			when(dlmRepository.getPartitionen())
				.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
			when(dlmRepository.getKanten(any()))
				.thenReturn(
					List.of(ImportedFeatureTestDataProvider
						.withLineString(new Coordinate(0, 0), new Coordinate(0, 100))
						.fachId("789").build()));
			when(simpleMatchingService.matche(any(), any())).thenAnswer(invocationOnMock -> {
				return StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
					.filter(k -> k.getDlmId().getValue().equals("789")).findAny().map(k -> {
						return new OsmMatchResult(invocationOnMock.getArgument(0), List.of(OsmWayId.of(k.getId())));
					});
			});

			// act
			dlmReimportJob.run(true);

			// assert
			List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
				.toList();
			assertThat(resultingKanten).hasSize(1);
			assertThat(resultingKanten.get(0).getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute())
				.hasSize(2);
			assertThat(resultingKanten.get(0).getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute())
				.containsExactlyInAnyOrder(
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.25)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_40_KMH)
						.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
							Hoechstgeschwindigkeit.MAX_90_KMH)
						.build(),
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.25, 1)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH).build());
		}

	}
}
