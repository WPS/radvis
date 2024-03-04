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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezugAenderung;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeNetzBezugAenderungRepository;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.event.KanteDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KanteTopologieChangedEvent;
import de.wps.radvis.backend.netz.domain.event.KnotenDeletedEvent;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzBezugAenderungsArt;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;

class MassnahmeNetzbezugAenderungProtokollierungsServiceIntegrationTest {

	private Verwaltungseinheit testOrganisation;
	private Benutzer testBenutzer;

	@Mock
	private MassnahmeNetzBezugAenderungRepository massnahmeNetzBezugAenderungRepository;

	@Mock
	private BenutzerService benutzerService;

	private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;
	private LocalDateTime testAenderungsDatum;

	@Captor
	private ArgumentCaptor<Iterable<MassnahmeNetzBezugAenderung>> captor;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		testOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Coole Organisation")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();
		testBenutzer = BenutzerTestDataProvider.admin(testOrganisation).build();
		when(benutzerService.getTechnischerBenutzer()).thenReturn(testBenutzer);
		massnahmeNetzbezugAenderungProtokollierungsService = new MassnahmeNetzbezugAenderungProtokollierungsService(
			benutzerService,
			massnahmeNetzBezugAenderungRepository);

		testAenderungsDatum = LocalDateTime.of(2021, 12, 17, 14, 20);
	}

	// TODO sinnvolle Testfälle, bitte. GGf von Integration auf UnitTest umbauen und den massnahmenService mocken.
	// Die Methode findByKanteInNetzBezug(Kante) wird separat gestestet
	@Test
	void testProtokolliereNetzBezugAenderungFuerVeraenderteKanten() {
		// arrange
		Knoten knoten1 = KnotenTestDataProvider.withDefaultValues().id(5L).build();
		Kante kante1 = KanteTestDataProvider.withDefaultValues().id(10L).build();

		Massnahme massnahme1 = createDefaultTestMassnahme()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(kante1, LinearReferenzierterAbschnitt.of(0, 1.),
					Seitenbezug.BEIDSEITIG)),
				Set.of(),
				Set.of(knoten1)))
			.build();

		LineString alteGeometry = GeometryTestdataProvider.createLineString(new Coordinate(99, 99),
			new Coordinate(123, 321));

		// act
		massnahmeNetzbezugAenderungProtokollierungsService.protokolliereNetzBezugAenderungFuerVeraenderteKante(
			List.of(massnahme1),
			new KanteTopologieChangedEvent(kante1.getId(), alteGeometry, NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				testAenderungsDatum));

		// assert
		Mockito.verify(massnahmeNetzBezugAenderungRepository, times(1))
			.saveAll(captor.capture());
		List<MassnahmeNetzBezugAenderung> massnahmeNetzBezugAenderung = StreamSupport.stream(
			captor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat(massnahmeNetzBezugAenderung).usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
			.containsExactly(
				new MassnahmeNetzBezugAenderung(NetzBezugAenderungsArt.KANTE_VERAENDERT, kante1.getId(), massnahme1,
					testBenutzer, testAenderungsDatum, NetzAenderungAusloeser.DLM_REIMPORT_JOB, alteGeometry));
	}

	@Test
	void testProtokolliereNetzBezugAenderungFuerGeloeschteKanten() {
		// arrange
		Knoten knoten1 = KnotenTestDataProvider.withDefaultValues().id(5L).build();
		Kante kante1 = KanteTestDataProvider.withDefaultValues().id(10L).build();

		Massnahme massnahme1 = createDefaultTestMassnahme()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(kante1, LinearReferenzierterAbschnitt.of(0, 1.),
					Seitenbezug.BEIDSEITIG)),
				Set.of(),
				Set.of(knoten1)))
			.build();

		LineString alteGeometry = GeometryTestdataProvider.createLineString(new Coordinate(99, 99),
			new Coordinate(123, 321));

		// act
		massnahmeNetzbezugAenderungProtokollierungsService.protokolliereNetzBezugAenderungFuerGeloeschteKante(
			List.of(massnahme1),
			new KanteDeletedEvent(kante1.getId(), alteGeometry, NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				testAenderungsDatum));

		// assert
		Mockito.verify(massnahmeNetzBezugAenderungRepository, times(1))
			.saveAll(captor.capture());
		List<MassnahmeNetzBezugAenderung> massnahmeNetzBezugAenderung = StreamSupport.stream(
			captor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat(massnahmeNetzBezugAenderung).usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
			.containsExactly(
				new MassnahmeNetzBezugAenderung(NetzBezugAenderungsArt.KANTE_GELOESCHT, kante1.getId(), massnahme1,
					testBenutzer, testAenderungsDatum, NetzAenderungAusloeser.DLM_REIMPORT_JOB, alteGeometry));
	}

	@Test
	void testProtokolliereNetzBezugAenderungFuerGeloeschtenKnoten() {
		// arrange
		Knoten knoten1 = KnotenTestDataProvider.withDefaultValues().id(5L).build();
		Kante kante1 = KanteTestDataProvider.withDefaultValues().id(10L).build();

		Massnahme massnahme1 = createDefaultTestMassnahme()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(kante1, LinearReferenzierterAbschnitt.of(0, 1.),
					Seitenbezug.BEIDSEITIG)),
				Set.of(),
				Set.of(knoten1)))
			.build();

		Point alteGeometry = knoten1.getPoint();

		// act
		massnahmeNetzbezugAenderungProtokollierungsService.protokolliereNetzBezugAenderungFuerGeloeschteKnoten(
			List.of(massnahme1),
			new KnotenDeletedEvent(knoten1.getId(), alteGeometry, NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				testAenderungsDatum));

		// assert
		Mockito.verify(massnahmeNetzBezugAenderungRepository, times(1))
			.saveAll(captor.capture());
		List<MassnahmeNetzBezugAenderung> massnahmeNetzBezugAenderung = StreamSupport.stream(
			captor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat(massnahmeNetzBezugAenderung).usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build())
			.containsExactly(
				new MassnahmeNetzBezugAenderung(NetzBezugAenderungsArt.KNOTEN_GELOESCHT, knoten1.getId(), massnahme1,
					testBenutzer, testAenderungsDatum, NetzAenderungAusloeser.DLM_REIMPORT_JOB, alteGeometry));
	}

	private Massnahme.MassnahmeBuilder createDefaultTestMassnahme() {
		return MassnahmeTestDataProvider.withDefaultValues()
			.baulastZustaendiger(testOrganisation)
			.unterhaltsZustaendiger(testOrganisation)
			.zustaendiger(testOrganisation)
			.letzteAenderung(testAenderungsDatum)
			.benutzerLetzteAenderung(testBenutzer);
	}

}
