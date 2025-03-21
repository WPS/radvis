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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
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
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.massnahme.domain.MassnahmenConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandsabfrageConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.dbView.MassnahmeListenDbView;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandStatus;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group1")
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
class MassnahmeViewRepositoryTestIT extends DBIntegrationTestIT {

	private Gebietskoerperschaft gebietskoerperschaft;

	@MockitoBean
	private ShapeFileRepository shapeFileRepository;

	@MockitoBean
	private SimpleMatchingService simpleMatchingService;

	@MockitoBean
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@MockitoBean
	private BenutzerResolver benutzerResolver;

	@MockitoBean
	private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;

	@MockitoBean
	private FahrradrouteRepository fahrradrouteRepository;

	@Autowired
	private MassnahmeRepository massnahmeRepository;

	@Autowired
	private MassnahmeViewRepository massnahmeViewRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	private BenutzerRepository benutzerRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Coole Organisation")
				.organisationsArt(
					OrganisationsArt.BUNDESLAND)
				.build());

	}

	@SuppressWarnings("unchecked")
	@Test
	void findAll_returnsCorrectValues() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();

		massnahme.getUmsetzungsstand().get().fordereAktualisierungAn();

		Kante kante = massnahme.getNetzbezug().getImmutableKantenAbschnittBezug().stream().findFirst().get()
			.getKante();

		kantenRepository.save(kante);

		massnahme = massnahmeRepository.save(massnahme);

		entityManager.flush();

		// act
		List<MassnahmeListenDbView> result = massnahmeViewRepository.findAll();

		// assert
		assertThat(result).hasSize(1);
		MassnahmeListenDbView dbView = result.get(0);
		assertThat(dbView.getBaulastId())
			.isEqualTo(massnahme.getBaulastZustaendiger().map(Verwaltungseinheit::getId).orElse(null));

		assertThat(dbView.getId())
			.isEqualTo(massnahme.getId());
		assertThat(dbView.getBezeichnung())
			.isEqualTo(
				massnahme.getBezeichnung());
		assertThat(dbView.getMassnahmenkategorien())
			.isEqualTo(
				massnahme.getMassnahmenkategorien());
		assertThat(dbView.getUmsetzungsstatus())
			.isEqualTo(
				massnahme.getUmsetzungsstatus());
		assertThat(dbView.getUmsetzungsstandStatus())
			.isEqualTo(UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT);
		assertThat(dbView.getVeroeffentlicht())
			.isEqualTo(
				massnahme.getVeroeffentlicht());
		assertThat(dbView.getPlanungErforderlich())
			.isEqualTo(
				massnahme.getPlanungErforderlich());
		assertThat(dbView.getDurchfuehrungszeitraum())
			.isEqualTo(
				massnahme.getDurchfuehrungszeitraum().orElse(null));
		assertThat(dbView.getPrioritaet())
			.isEqualTo(
				massnahme.getPrioritaet().orElse(null));
		assertThat(dbView.getNetzklassen())
			.isEqualTo(
				massnahme.getNetzklassen());
		assertThat(dbView.getLetzteAenderung())
			.isEqualTo(
				massnahme.getLetzteAenderung());
		assertThat(dbView.getBenutzerLetzteAenderungEmail())
			.isEqualTo(massnahme.getBenutzerLetzteAenderung().getMailadresse());
		assertThat(dbView.getZustaendigId())
			.isEqualTo(massnahme.getZustaendiger().map(Verwaltungseinheit::getId).orElse(null));
		assertThat(dbView.getUnterhaltId())
			.isEqualTo(
				massnahme.getunterhaltsZustaendiger().map(Verwaltungseinheit::getId).orElse(null));
		assertThat(dbView.getSollStandard())
			.isEqualTo(
				massnahme.getSollStandard());
		assertThat(dbView.getHandlungsverantwortlicher())
			.isEqualTo(
				massnahme.getHandlungsverantwortlicher().orElse(null));
		assertThat(dbView.getGeometry().getGeometryN(0))
			.isEqualTo(
				massnahme.berechneMittelpunkt().get());
		assertThat(dbView.getKonzeptionsquelle()).isEqualTo(massnahme.getKonzeptionsquelle());
		assertThat(dbView.isArchiviert())
			.isEqualTo(
				massnahme.isArchiviert());
	}

	@SuppressWarnings("unchecked")
	@Test
	void alleGeometrienJoined_noKnoten() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 1),
				new Coordinate(100, 1)
			}))
			.build();

		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 2),
				new Coordinate(100, 2)
			})).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezugSet = new HashSet<>();

		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante1, LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.LINKS));
		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante1, LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.LINKS));

		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante2, LinearReferenzierterAbschnitt.of(0.3, 0.4), Seitenbezug.LINKS));
		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante2, LinearReferenzierterAbschnitt.of(0.4, 0.7), Seitenbezug.LINKS));

		HashSet<PunktuellerKantenSeitenBezug> punktuellerKantenSeitenBezug = new HashSet<>();
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.3), Seitenbezug.BEIDSEITIG));
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.4), Seitenbezug.BEIDSEITIG));
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante2, LineareReferenz.of(0.5), Seitenbezug.BEIDSEITIG));

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(abschnittsweiserKantenSeitenBezugSet,
			punktuellerKantenSeitenBezug,
			new HashSet<>());

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(netzbezug)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();

		massnahme = massnahmeRepository.save(massnahme);

		entityManager.flush();

		// act and assert

		Geometry geometryCollection = massnahmeViewRepository.findAll().get(0).getGeometry();

		assertThat(geometryCollection.getNumGeometries()).isEqualTo(2);
		assertThat(geometryCollection.getGeometryN(0).getGeometryType()).isEqualTo(Geometry.TYPENAME_MULTILINESTRING);
		assertThat(geometryCollection.getGeometryN(0).getCoordinates()).containsExactlyInAnyOrder(
			// in kante 1
			new CoordinateXY(20.0, 1.0),
			new CoordinateXY(80.0, 1.0),
			// in kante 2
			new CoordinateXY(30.0, 2.0),
			new CoordinateXY(70.0, 2.0));
		assertThat(geometryCollection.getGeometryN(1).getGeometryType()).isEqualTo(Geometry.TYPENAME_MULTIPOINT);
		assertThat(geometryCollection.getGeometryN(1).getCoordinates()).containsExactlyInAnyOrder(
			new CoordinateXY(30, 1),
			new CoordinateXY(40, 1), new CoordinateXY(50, 2));
		assertThat(massnahmeViewRepository.findAll().get(0).getDisplayGeometry())
			.isEqualTo(geometryCollection.getGeometryN(0));
	}

	@Test
	void alleGeometrienJoined() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 1),
				new Coordinate(100, 1)
			}))
			.build();

		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 2),
				new Coordinate(100, 2)
			})).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezugSet = new HashSet<>();

		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante1, LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.LINKS));
		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante1, LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.LINKS));

		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante2, LinearReferenzierterAbschnitt.of(0.3, 0.4), Seitenbezug.LINKS));
		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante2, LinearReferenzierterAbschnitt.of(0.4, 0.7), Seitenbezug.LINKS));

		HashSet<PunktuellerKantenSeitenBezug> punktuellerKantenSeitenBezug = new HashSet<>();
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.3), Seitenbezug.BEIDSEITIG));
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.4), Seitenbezug.BEIDSEITIG));
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante2, LineareReferenz.of(0.5), Seitenbezug.BEIDSEITIG));

		HashSet<Knoten> knotenBezug = new HashSet<>();
		knotenBezug.add(kante1.getVonKnoten());
		knotenBezug.add(kante2.getNachKnoten());

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(abschnittsweiserKantenSeitenBezugSet,
			punktuellerKantenSeitenBezug,
			knotenBezug);

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(netzbezug)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();

		massnahme = massnahmeRepository.save(massnahme);

		entityManager.flush();

		// act and assert

		Geometry geometryCollection = massnahmeViewRepository.findAll().get(0).getGeometry();

		assertThat(geometryCollection.getNumGeometries()).isEqualTo(3);
		assertThat(geometryCollection.getGeometryN(0).getGeometryType()).isEqualTo(Geometry.TYPENAME_MULTILINESTRING);
		assertThat(geometryCollection.getGeometryN(0).getCoordinates()).containsExactlyInAnyOrder(
			// in kante 1
			new CoordinateXY(20.0, 1.0),
			new CoordinateXY(80.0, 1.0),
			// in kante 2
			new CoordinateXY(30.0, 2.0),
			new CoordinateXY(70.0, 2.0));
		assertThat(geometryCollection.getGeometryN(1).getGeometryType()).isEqualTo(Geometry.TYPENAME_MULTIPOINT);
		assertThat(geometryCollection.getGeometryN(1).getCoordinates()).containsExactlyInAnyOrder(
			kante1.getVonKnoten().getKoordinate(),
			kante2.getNachKnoten().getKoordinate());
		assertThat(geometryCollection.getGeometryN(2).getGeometryType()).isEqualTo(Geometry.TYPENAME_MULTIPOINT);
		assertThat(geometryCollection.getGeometryN(2).getCoordinates()).containsExactlyInAnyOrder(
			new CoordinateXY(30, 1),
			new CoordinateXY(40, 1), new CoordinateXY(50, 2));
	}

	@SuppressWarnings("unchecked")
	@Test
	void getDisplayGeometry_noMultiPoint() {
		// arrange
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Massnahme massnahme = MassnahmeTestDataProvider.withKnoten(kante.getVonKnoten(), kante.getNachKnoten())
			.letzteAenderung(LocalDateTime.of(2020, 10, 1, 10, 12)).planungErforderlich(true)
			.sollStandard(SollStandard.KEIN_STANDARD_ERFUELLT)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		massnahmeRepository.save(massnahme);
		entityManager.flush();
		MassnahmeListenDbView massnahmeListenDbView = massnahmeViewRepository.findAll().get(0);

		// act + assert
		assertThat(massnahmeListenDbView.getGeometry().getNumGeometries()).isEqualTo(1);
		assertThat(massnahmeListenDbView.getGeometry().getGeometryN(0).getGeometryType())
			.isEqualTo(Geometry.TYPENAME_MULTIPOINT);
		assertThat(massnahmeListenDbView.getDisplayGeometry().getGeometryType())
			.isEqualTo(Geometry.TYPENAME_POINT);
		assertThat(massnahmeListenDbView.getDisplayGeometry())
			.isEqualTo(massnahmeListenDbView.getGeometry().getGeometryN(0).getGeometryN(0));
	}

	@SuppressWarnings("unchecked")
	@Test
	void noGeometry() {
		// arrange
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(kante)
			.letzteAenderung(LocalDateTime.of(2020, 10, 1, 10, 12)).planungErforderlich(true)
			.sollStandard(SollStandard.KEIN_STANDARD_ERFUELLT)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		massnahme.removeKanteFromNetzbezug(List.of(kante.getId()));
		massnahmeRepository.save(massnahme);
		entityManager.flush();
		MassnahmeListenDbView massnahmeListenDbView = massnahmeViewRepository.findAll().get(0);

		// act + assert
		assertThat(massnahmeListenDbView.getGeometry()).isNull();
		assertThat(massnahmeListenDbView.getDisplayGeometry()).isNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	void noGeometry_archiviert() {
		// arrange
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(kante)
			.letzteAenderung(LocalDateTime.of(2020, 10, 1, 10, 12)).planungErforderlich(true)
			.sollStandard(SollStandard.KEIN_STANDARD_ERFUELLT)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		massnahme.removeKanteFromNetzbezug(List.of(kante.getId()));
		massnahme.archivieren();
		massnahmeRepository.save(massnahme);
		entityManager.flush();
		MassnahmeListenDbView massnahmeListenDbView = massnahmeViewRepository.findAll().get(0);

		// act + assert
		assertThat(massnahmeListenDbView.getGeometry().getNumGeometries()).isEqualTo(0);
		assertThat(massnahmeListenDbView.getDisplayGeometry()).isNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	void getDisplayGeometry_archiviert_noMultiPoint() {
		// arrange
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Massnahme massnahme = MassnahmeTestDataProvider.withKnoten(kante.getVonKnoten(), kante.getNachKnoten())
			.letzteAenderung(LocalDateTime.of(2020, 10, 1, 10, 12)).planungErforderlich(true)
			.sollStandard(SollStandard.KEIN_STANDARD_ERFUELLT)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		massnahme.archivieren();
		massnahme.removeKnotenFromNetzbezug(List.of(kante.getVonKnoten().getId(), kante.getNachKnoten().getId()));
		massnahmeRepository.save(massnahme);
		entityManager.flush();
		MassnahmeListenDbView massnahmeListenDbView = massnahmeViewRepository.findAll().get(0);

		// act + assert
		assertThat(massnahmeListenDbView.getGeometry().getNumGeometries()).isEqualTo(1);
		assertThat(massnahmeListenDbView.getGeometry().getGeometryN(0).getGeometryType())
			.isEqualTo(Geometry.TYPENAME_MULTIPOINT);
		assertThat(massnahmeListenDbView.getDisplayGeometry().getGeometryType())
			.isEqualTo(Geometry.TYPENAME_POINT);
		assertThat(massnahmeListenDbView.getDisplayGeometry())
			.isEqualTo(massnahmeListenDbView.getNetzbezugSnapshotPoints().getGeometryN(0));
	}

	@Test
	void alleGeometrienJoined_noPunkte() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 1),
				new Coordinate(100, 1)
			}))
			.build();

		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 2),
				new Coordinate(100, 2)
			})).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezugSet = new HashSet<>();

		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante1, LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.LINKS));
		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante1, LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.LINKS));

		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante2, LinearReferenzierterAbschnitt.of(0.3, 0.4), Seitenbezug.LINKS));
		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante2, LinearReferenzierterAbschnitt.of(0.4, 0.7), Seitenbezug.LINKS));

		HashSet<Knoten> knotenBezug = new HashSet<>();
		knotenBezug.add(kante1.getVonKnoten());
		knotenBezug.add(kante2.getNachKnoten());

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(abschnittsweiserKantenSeitenBezugSet, new HashSet<>(),
			knotenBezug);

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(netzbezug)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();

		massnahme = massnahmeRepository.save(massnahme);

		entityManager.flush();

		// act and assert

		Geometry geometryCollection = massnahmeViewRepository.findAll().get(0).getGeometry();

		assertThat(geometryCollection.getNumGeometries()).isEqualTo(2);
		assertThat(geometryCollection.getGeometryN(0).getGeometryType()).isEqualTo(Geometry.TYPENAME_MULTILINESTRING);
		assertThat(geometryCollection.getGeometryN(0).getCoordinates()).containsExactlyInAnyOrder(
			// in kante 1
			new CoordinateXY(20.0, 1.0),
			new CoordinateXY(80.0, 1.0),
			// in kante 2
			new CoordinateXY(30.0, 2.0),
			new CoordinateXY(70.0, 2.0));
		assertThat(geometryCollection.getGeometryN(1).getGeometryType()).isEqualTo(Geometry.TYPENAME_MULTIPOINT);
		assertThat(geometryCollection.getGeometryN(1).getCoordinates()).containsExactlyInAnyOrder(
			kante1.getVonKnoten().getKoordinate(),
			kante2.getNachKnoten().getKoordinate());
	}

	@SuppressWarnings("unchecked")
	@Test
	void alleGeometrienJoined_noKanten() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 1),
				new Coordinate(100, 1)
			}))
			.build();

		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 2),
				new Coordinate(100, 2)
			})).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		HashSet<PunktuellerKantenSeitenBezug> punktuellerKantenSeitenBezug = new HashSet<>();
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.3), Seitenbezug.BEIDSEITIG));
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.4), Seitenbezug.BEIDSEITIG));
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante2, LineareReferenz.of(0.5), Seitenbezug.BEIDSEITIG));

		HashSet<Knoten> knotenBezug = new HashSet<>();
		knotenBezug.add(kante1.getVonKnoten());
		knotenBezug.add(kante2.getNachKnoten());

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(new HashSet<>(), punktuellerKantenSeitenBezug,
			knotenBezug);

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(netzbezug)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();

		massnahme = massnahmeRepository.save(massnahme);

		entityManager.flush();

		// act and assert

		Geometry geometryCollection = massnahmeViewRepository.findAll().get(0).getGeometry();

		assertThat(geometryCollection.getNumGeometries()).isEqualTo(2);
		assertThat(geometryCollection.getGeometryN(0).getGeometryType()).isEqualTo(Geometry.TYPENAME_MULTIPOINT);
		assertThat(geometryCollection.getGeometryN(0).getCoordinates()).containsExactlyInAnyOrder(
			kante1.getVonKnoten().getKoordinate(),
			kante2.getNachKnoten().getKoordinate());
		assertThat(geometryCollection.getGeometryN(1).getGeometryType()).isEqualTo(Geometry.TYPENAME_MULTIPOINT);
		assertThat(geometryCollection.getGeometryN(1).getCoordinates()).containsExactlyInAnyOrder(
			new CoordinateXY(30, 1),
			new CoordinateXY(40, 1), new CoordinateXY(50, 2));
		assertThat(massnahmeViewRepository.findAll().get(0).getDisplayGeometry())
			.isEqualTo(geometryCollection.getGeometryN(0).getGeometryN(0));
	}

	@SuppressWarnings("unchecked")
	@Test
	void getDisplayGeometry_archiviert_noKanten_returnsPoint() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 1),
				new Coordinate(100, 1)
			}))
			.build();

		kantenRepository.save(kante1);

		PunktuellerKantenSeitenBezug punktuellerKantenSeitenBezug = new PunktuellerKantenSeitenBezug(kante1,
			LineareReferenz.of(0.3),
			Seitenbezug.BEIDSEITIG);

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(Collections.emptySet(),
			Set.of(punktuellerKantenSeitenBezug),
			Collections.emptySet());

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(netzbezug)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();

		massnahme.archivieren();
		massnahme.removeKanteFromNetzbezug(List.of(kante1.getId()));

		massnahme = massnahmeRepository.save(massnahme);

		entityManager.flush();

		// act and assert

		assertThat(massnahmeViewRepository.findAll().get(0).getDisplayGeometry())
			.isEqualTo(punktuellerKantenSeitenBezug.getPointGeometry());
	}

	@Test
	void netzbezugSnapshotIfArchiviert() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 1),
				new Coordinate(100, 1)
			}))
			.build();

		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 2),
				new Coordinate(100, 2)
			})).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezugSet = new HashSet<>();

		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante1, LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.LINKS));
		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante1, LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.LINKS));

		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante2, LinearReferenzierterAbschnitt.of(0.3, 0.4), Seitenbezug.LINKS));
		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante2, LinearReferenzierterAbschnitt.of(0.4, 0.7), Seitenbezug.LINKS));

		HashSet<PunktuellerKantenSeitenBezug> punktuellerKantenSeitenBezug = new HashSet<>();
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.3), Seitenbezug.BEIDSEITIG));
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.4), Seitenbezug.BEIDSEITIG));
		punktuellerKantenSeitenBezug
			.add(new PunktuellerKantenSeitenBezug(kante2, LineareReferenz.of(0.5), Seitenbezug.BEIDSEITIG));

		HashSet<Knoten> knotenBezug = new HashSet<>();
		knotenBezug.add(kante1.getVonKnoten());
		knotenBezug.add(kante2.getNachKnoten());

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(abschnittsweiserKantenSeitenBezugSet,
			punktuellerKantenSeitenBezug,
			knotenBezug);

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(netzbezug)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();

		massnahme.archivieren();
		massnahme.removeKanteFromNetzbezug(Set.of(kante1.getId(), kante2.getId()));
		massnahme.removeKnotenFromNetzbezug(Set.of(kante1.getVonKnoten().getId()));

		massnahme = massnahmeRepository.save(massnahme);

		entityManager.flush();

		// act and assert

		Geometry geometryCollection = massnahmeViewRepository.findAll().get(0).getGeometry();

		assertThat(geometryCollection.getNumGeometries()).isEqualTo(2);
		assertThat(geometryCollection.getGeometryN(0).getGeometryType()).isEqualTo(Geometry.TYPENAME_MULTILINESTRING);
		assertThat(geometryCollection.getGeometryN(0).getCoordinates()).containsExactlyInAnyOrder(
			// in kante 1
			new CoordinateXY(20.0, 1.0),
			new CoordinateXY(80.0, 1.0),
			// in kante 2
			new CoordinateXY(30.0, 2.0),
			new CoordinateXY(70.0, 2.0));
		assertThat(geometryCollection.getGeometryN(1).getGeometryType()).isEqualTo(Geometry.TYPENAME_MULTIPOINT);
		assertThat(geometryCollection.getGeometryN(1).getCoordinates()).containsExactlyInAnyOrder(
			kante1.getVonKnoten().getKoordinate(), kante2.getNachKnoten().getKoordinate(), new CoordinateXY(30, 1),
			new CoordinateXY(40, 1), new CoordinateXY(50, 2));
	}

	@Test
	void kantenSeiteAbschnitteSindInViewsEnthalten() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 1),
				new Coordinate(100, 1)
			}))
			.build();

		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(0, 2),
				new Coordinate(100, 2)
			})).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezugSet = new HashSet<>();

		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante1, LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.LINKS));
		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante1, LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.LINKS));

		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante2, LinearReferenzierterAbschnitt.of(0.3, 0.4), Seitenbezug.LINKS));
		abschnittsweiserKantenSeitenBezugSet.add(new AbschnittsweiserKantenSeitenBezug(
			kante2, LinearReferenzierterAbschnitt.of(0.4, 0.7), Seitenbezug.LINKS));

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(abschnittsweiserKantenSeitenBezugSet, new HashSet<>(),
			new HashSet<>());

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(netzbezug)
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();

		massnahme = massnahmeRepository.save(massnahme);

		entityManager.flush();

		// act and assert
		assertThat(massnahmeViewRepository.findAll().get(0).getGeometry().getCoordinates()).containsOnly(
			// in kante 1
			new CoordinateXY(20.0, 1.0),
			new CoordinateXY(80.0, 1.0),
			// in kante 2
			new CoordinateXY(30.0, 2.0),
			new CoordinateXY(70.0, 2.0));
	}
}
