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
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

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
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.dokument.domain.entity.provider.DokumentTestDataProvider;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.kommentar.domain.entity.Kommentar;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandsabfrageConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmenPaketId;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import jakarta.persistence.EntityManager;

@Tag("group6")
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
class MassnahmeRepositoryTestIT extends DBIntegrationTestIT {

	private Gebietskoerperschaft gebietskoerperschaft;
	private Benutzer testBenutzer;
	private LocalDateTime testAenderungsDatum;

	@MockBean
	private ShapeFileRepository shapeFileRepository;

	@MockBean
	private SimpleMatchingService simpleMatchingService;

	@MockBean
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@MockBean
	private BenutzerResolver benutzerResolver;

	@MockBean
	private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;

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
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Coole Organisation")
				.organisationsArt(
					OrganisationsArt.BUNDESLAND)
				.build());
		testBenutzer = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		testAenderungsDatum = LocalDateTime.of(2021, 12, 17, 14, 20);
	}

	@Test
	void erstelleMassnahme_massnahmeWirdErstellt() {
		// arrange
		Knoten knoten = knotenRepository.save(KnotenTestDataProvider.withDefaultValues().build());
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Massnahme massnahme = createDefaultTestmassnahme()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1.),
					Seitenbezug.BEIDSEITIG)),
				Set.of(),
				Set.of(knoten)))
			.veroeffentlicht(true)
			.planungErforderlich(false)
			.massnahmeKonzeptId(MassnahmeKonzeptID.of("AbC212"))
			.sollStandard(SollStandard.BASISSTANDARD)
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.sonstigeKonzeptionsquelle("WAMBO")
			.build();

		// act
		Massnahme gespeicherteMassnahme = massnahmeRepository.save(massnahme);

		// assert
		assertThat(gespeicherteMassnahme).usingRecursiveComparison().ignoringFields("id").isEqualTo(massnahme);
	}

	@Test
	void findAllMassnahmenPacketIds() {
		// arrange
		gebietskoerperschaftRepository.save(gebietskoerperschaft);

		Massnahme massnahmeOhnePacketID = createDefaultTestmassnahme()
			.veroeffentlicht(true)
			.planungErforderlich(false)
			.massnahmeKonzeptId(MassnahmeKonzeptID.of("afd12"))
			.sollStandard(SollStandard.BASISSTANDARD)
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.sonstigeKonzeptionsquelle("WAMBO")
			.build();

		Massnahme massnahmeMitPacketID = createDefaultTestmassnahme()
			.veroeffentlicht(false)
			.planungErforderlich(false)
			.massnahmeKonzeptId(MassnahmeKonzeptID.of("afd12"))
			.sollStandard(SollStandard.BASISSTANDARD)
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.sonstigeKonzeptionsquelle("WAMBO")
			.massnahmenPaketId(MassnahmenPaketId.of("massnahmenPacketId"))
			.build();

		kantenRepository.save(
			massnahmeOhnePacketID.getNetzbezug().getImmutableKantenAbschnittBezug().stream().findFirst().get()
				.getKante());

		kantenRepository.save(
			massnahmeMitPacketID.getNetzbezug().getImmutableKantenAbschnittBezug().stream().findFirst().get()
				.getKante());

		massnahmeRepository.save(massnahmeOhnePacketID);
		massnahmeRepository.save(massnahmeMitPacketID);

		// act
		Set<MassnahmenPaketId> result = massnahmeRepository.findAllMassnahmenPaketIds();

		// assert
		assertThat(result).containsExactly(MassnahmenPaketId.of("massnahmenPacketId"));
	}

	@Test
	void saveMassnahmeMitDokument() {
		// arrange
		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.defaultBenutzer()
			.organisation(gebietskoerperschaft)
			.build());

		Dokument dokument = DokumentTestDataProvider.withDefaultValues().benutzer(benutzer).build();

		Long mannahmeID = massnahmeRepository.save(
				createDefaultTestmassnahme()
					.netzbezug(new MassnahmeNetzBezug(Set.of(new AbschnittsweiserKantenSeitenBezug(
						kantenRepository.save(KanteTestDataProvider.withDefaultValues().build()),
						LinearReferenzierterAbschnitt.of(0, 1.),
						Seitenbezug.BEIDSEITIG)),
						Set.of(),
						Set.of(
							knotenRepository.save(KnotenTestDataProvider.withDefaultValues().build()))))
					.benutzerLetzteAenderung(benutzer)
					.build())
			.getId();

		entityManager.flush();
		entityManager.clear();
		// act
		Massnahme testMassnahme = massnahmeRepository.findById(mannahmeID).get();
		testMassnahme.addDokument(dokument);
		massnahmeRepository.save(testMassnahme);
		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(massnahmeRepository.findById(mannahmeID).get().getDokumentListe().getDokumente().get(0))
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Dokument.class)
			.ignoringFields("id")
			.isEqualTo(dokument);
	}

	@Test
	void netzbezug_wirdRichtigVerarbeitet() {
		// arrange

		Knoten knoten = knotenRepository.save(KnotenTestDataProvider.withDefaultValues()
			.point(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(10, 100)))
			.build());
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().geometry(
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100))).build());

		final AbschnittsweiserKantenSeitenBezug kantenSeitenAbschnitt = new AbschnittsweiserKantenSeitenBezug(
			kante,
			LinearReferenzierterAbschnitt.of(0.2, 0.6),
			Seitenbezug.BEIDSEITIG);
		PunktuellerKantenSeitenBezug kantenPunkt = new PunktuellerKantenSeitenBezug(
			kante,
			LineareReferenz.of(0.7),
			Seitenbezug.LINKS);
		final MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(kantenSeitenAbschnitt),
			Set.of(kantenPunkt),
			Set.of(knoten));

		Massnahme massnahme = createDefaultTestmassnahme()
			.netzbezug(netzbezug)
			.build();

		// act
		Long massnahmenID = massnahmeRepository.save(massnahme).getId();
		entityManager.flush();
		entityManager.clear();

		// assert
		Geometry geometry = massnahmeRepository.findById(massnahmenID).get().getNetzbezug().getGeometrie();
		assertThat(List.of(geometry.getGeometryN(0), geometry.getGeometryN(1),
			geometry.getGeometryN(2))).containsExactlyInAnyOrder(
			GeometryTestdataProvider.createLineString(new Coordinate(20, 20), new Coordinate(60, 60)),
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(70, 70)),
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(10, 100)));
	}

	@Test
	void netzbezug_maxLaengeKommentarFunktioniert() {
		// arrange

		Knoten knoten = knotenRepository.save(KnotenTestDataProvider.withDefaultValues()
			.point(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(10, 100)))
			.build());
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().geometry(
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100))).build());

		final AbschnittsweiserKantenSeitenBezug kantenSeitenAbschnitt = new AbschnittsweiserKantenSeitenBezug(
			kante,
			LinearReferenzierterAbschnitt.of(0.2, 0.6),
			Seitenbezug.BEIDSEITIG);
		PunktuellerKantenSeitenBezug kantenPunkt = new PunktuellerKantenSeitenBezug(
			kante,
			LineareReferenz.of(0.7),
			Seitenbezug.LINKS);
		final MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(kantenSeitenAbschnitt),
			Set.of(kantenPunkt),
			Set.of(knoten));

		Massnahme massnahme = createDefaultTestmassnahme()
			.kommentarListe(new KommentarListe(List.of(
				new Kommentar(StringUtils.repeat("*", 4000), testBenutzer))))
			.netzbezug(netzbezug)
			.build();

		// act
		Long massnahmenID = massnahmeRepository.save(massnahme).getId();
		entityManager.flush();
		entityManager.clear();

		// assert
		Kommentar kommentar = massnahmeRepository.findById(massnahmenID).get().getKommentarListe().getKommentare()
			.get(0);
		assertThat(kommentar.getKommentarText().length()).isEqualTo(4000);
	}

	@Test
	void testFindByKanteInNetzBezug() {
		// arrange
		Knoten knoten1 = knotenRepository.save(KnotenTestDataProvider.withDefaultValues().build());
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Massnahme massnahmeAbschnittsweiserBezugAufKante1 = createDefaultTestmassnahme()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(kante1, LinearReferenzierterAbschnitt.of(0, 1.),
					Seitenbezug.BEIDSEITIG)),
				Set.of(),
				Set.of(knoten1)))
			.build();

		Massnahme massnahmePunktuellerBezugAufKante1 = createDefaultTestmassnahme()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(),
				Set.of(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.5), Seitenbezug.RECHTS)),
				Set.of()))
			.build();

		Massnahme massnahmeKeinBezugAufKante1 = createDefaultTestmassnahme()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(),
				Set.of(new PunktuellerKantenSeitenBezug(kante2, LineareReferenz.of(0.5), Seitenbezug.RECHTS)),
				Set.of()))
			.build();

		massnahmeRepository.saveAll(List.of(massnahmeAbschnittsweiserBezugAufKante1, massnahmePunktuellerBezugAufKante1,
			massnahmeKeinBezugAufKante1));

		// act
		List<Massnahme> byKanteInNetzBezug = massnahmeRepository.findByKanteInNetzBezug(kante1.getId());

		// assert
		assertThat(byKanteInNetzBezug).containsExactlyInAnyOrder(massnahmeAbschnittsweiserBezugAufKante1,
			massnahmePunktuellerBezugAufKante1);
	}

	@Test
	void testFindByKnotenInNetzBezug() {
		// arrange
		Knoten knoten1 = knotenRepository.save(KnotenTestDataProvider.withDefaultValues().build());
		Knoten knoten2 = knotenRepository.save(KnotenTestDataProvider.withDefaultValues().build());
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Massnahme hatKnoten = createDefaultTestmassnahme()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(kante1, LinearReferenzierterAbschnitt.of(0, 1.),
					Seitenbezug.BEIDSEITIG)),
				Set.of(),
				Set.of(knoten1)))
			.build();

		Massnahme hatKnotenUndAnderenKnoten = createDefaultTestmassnahme()
			.netzbezug(new MassnahmeNetzBezug(Set.of(), Set.of(), Set.of(knoten1, knoten2)))
			.build();

		Massnahme hatKeineKnoten = createDefaultTestmassnahme()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(),
				Set.of(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.5), Seitenbezug.RECHTS)),
				Set.of()))
			.build();

		Massnahme hatAnderenKnoten = createDefaultTestmassnahme()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(),
				Set.of(new PunktuellerKantenSeitenBezug(kante2, LineareReferenz.of(0.5), Seitenbezug.RECHTS)),
				Set.of(knoten2)))
			.build();

		massnahmeRepository.saveAll(List.of(hatKnoten, hatKnotenUndAnderenKnoten, hatKeineKnoten,
			hatAnderenKnoten));

		// act
		List<Massnahme> byKanteInNetzBezug = massnahmeRepository.findByKnotenInNetzBezug(knoten1.getId());

		// assert
		assertThat(byKanteInNetzBezug).containsExactlyInAnyOrder(hatKnoten, hatKnotenUndAnderenKnoten);
	}

	Massnahme.MassnahmeBuilder createDefaultTestmassnahme() {
		return MassnahmeTestDataProvider.withDefaultValues()
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(testAenderungsDatum)
			.benutzerLetzteAenderung(testBenutzer);
	}
}
