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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.massnahme.domain.bezug.NetzBezugTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezugAenderung;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeUmsetzungsstandViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.UmsetzungsstandRepository;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteDeleteStatistik;
import de.wps.radvis.backend.netz.domain.entity.KanteErsetzenStatistik;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenDeleteStatistik;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.event.KanteErsetztEvent;
import de.wps.radvis.backend.netz.domain.event.KantenDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KnotenDeletedEvent;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;

class MassnahmeServiceTest {

	private MassnahmeService massnahmeService;

	@Mock
	private MassnahmeRepository massnahmeRepository;

	@Mock
	private MassnahmeViewRepository massnahmeViewRepository;

	@Mock
	private MassnahmeUmsetzungsstandViewRepository massnahmeUmsetzungsstandViewRepository;

	@Mock
	private UmsetzungsstandRepository umsetzungsstandRepository;

	@Mock
	private KantenRepository kantenRepository;

	@Mock
	private BenutzerService benutzerService;

	@Mock
	private FahrradrouteRepository fahrradrouteRepository;

	@Mock
	private NetzService netzService;

	@Mock
	private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;
	private AutoCloseable openMocks;

	@BeforeEach
	void setUp() {
		openMocks = openMocks(this);
		massnahmeService = new MassnahmeService(massnahmeRepository, massnahmeViewRepository,
			massnahmeUmsetzungsstandViewRepository, umsetzungsstandRepository, kantenRepository,
			massnahmeNetzbezugAenderungProtokollierungsService, benutzerService, fahrradrouteRepository, netzService,
			20, 1.0);
	}

	@AfterEach
	void cleanUp() throws Exception {
		openMocks.close();
	}

	@Test
	void testHatRadNETZNetzBezug() {
		// arrange
		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppeTestDataProvider.defaultValue()
			.id(1000L)
			.build();

		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM)
			.id(100L).build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 200), QuellSystem.DLM)
			.id(200L).build();
		Kante kante = KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.id(10L)
			.kantenAttributGruppe(kantenAttributGruppe)
			.build();

		KantenAttributGruppe kantenAttributGruppeRadNETZ = KantenAttributGruppeTestDataProvider.defaultValue()
			.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
			.id(2000L)
			.build();

		Knoten vonKnotenRadNETZ = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100),
			QuellSystem.DLM)
			.id(300L).build();
		Knoten nachKnotenRadNETZ = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 200),
			QuellSystem.DLM)
			.id(400L).build();
		Kante kanteRadNETZ = KanteTestDataProvider.fromKnoten(vonKnotenRadNETZ, nachKnotenRadNETZ)
			.id(20L)
			.kantenAttributGruppe(kantenAttributGruppeRadNETZ)
			.build();

		when(kantenRepository.getAdjazenteKanten(or(eq(vonKnoten), eq(nachKnoten)))).thenReturn(List.of(kante));
		when(kantenRepository.getAdjazenteKanten(or(eq(vonKnotenRadNETZ), eq(nachKnotenRadNETZ)))).thenReturn(
			List.of(kanteRadNETZ));

		Massnahme abschnittsweiseOhneRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante))
			.build();

		Massnahme knotenOhneRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.netzbezug(NetzBezugTestDataProvider.forKnoten(vonKnoten))
			.build();

		Massnahme punktuellOhneRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
			.id(3L)
			.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante))
			.build();

		Massnahme punktuellTeilweiseRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
			.id(4L)
			.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante, kanteRadNETZ))
			.build();

		Massnahme abschnittTeilweiseRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
			.id(5L)
			.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante, kanteRadNETZ))
			.build();

		Massnahme knotenTeilweiseRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
			.id(6L)
			.netzbezug(NetzBezugTestDataProvider.forKnoten(vonKnoten, nachKnotenRadNETZ))
			.build();

		// act & assert
		assertThat(massnahmeService.hatRadNETZNetzBezug(abschnittsweiseOhneRadNETZ)).isFalse();
		assertThat(massnahmeService.hatRadNETZNetzBezug(punktuellOhneRadNETZ)).isFalse();
		assertThat(massnahmeService.hatRadNETZNetzBezug(knotenOhneRadNETZ)).isFalse();
		assertThat(massnahmeService.hatRadNETZNetzBezug(punktuellTeilweiseRadNETZ)).isTrue();
		assertThat(massnahmeService.hatRadNETZNetzBezug(abschnittTeilweiseRadNETZ)).isTrue();
		assertThat(massnahmeService.hatRadNETZNetzBezug(knotenTeilweiseRadNETZ)).isTrue();

	}

	@Test
	void testonKantenGeloescht_ausloeserRadVisKanteGeloescht_keinProtokollEintrag() {
		// arrange
		Kante kanteRadVis = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis)
			.id(10L)
			.build();

		Kante kanteDLM = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM)
			.id(20L)
			.build();

		Massnahme abschnittsweiseRadVis = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kanteRadVis))
			.build();

		Massnahme punktuellRadVis = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kanteRadVis))
			.build();

		Massnahme abschnittsweiseDlm = MassnahmeTestDataProvider.withDefaultValues()
			.id(3L)
			.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kanteDLM))
			.build();

		when(massnahmeRepository.findByKantenInNetzBezug(List.of(10L))).thenReturn(
			List.of(abschnittsweiseRadVis, punktuellRadVis));
		when(massnahmeRepository.findByKantenInNetzBezug(List.of(20L))).thenReturn(List.of(abschnittsweiseDlm));

		// act
		KanteDeleteStatistik statistik = new KanteDeleteStatistik();
		massnahmeService.onKantenGeloescht(new KantenDeletedEvent(List.of(kanteRadVis.getId()), List.of(kanteRadVis
			.getGeometry()),
			NetzAenderungAusloeser.RADVIS_KANTE_LOESCHEN,
			LocalDateTime.now(), statistik));

		// assert
		assertThat(abschnittsweiseRadVis.getNetzbezug().getImmutableKantenAbschnittBezug()).isEmpty();
		assertThat(punktuellRadVis.getNetzbezug().getImmutableKantenPunktBezug()).isEmpty();
		assertThat(abschnittsweiseDlm.getNetzbezug().getImmutableKantenAbschnittBezug()).hasSize(1);

		verifyNoInteractions(massnahmeNetzbezugAenderungProtokollierungsService);

		assertThat(statistik.anzahlAngepassterNetzbezuege).isEqualTo(2);
	}

	@ParameterizedTest
	@EnumSource(value = NetzAenderungAusloeser.class, names = {
		"RADVIS_KANTE_LOESCHEN" }, mode = EnumSource.Mode.EXCLUDE)
	void testonKantenGeloescht_ausloeserNichtRadVisKanteLoeschen_protokollEintrag(
		NetzAenderungAusloeser netzAenderungAusloeser) {
		// arrange
		Kante kanteRadVis = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis)
			.id(10L)
			.build();

		Kante kanteDLM = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM)
			.id(20L)
			.build();

		Massnahme abschnittsweiseRadVis = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kanteRadVis))
			.build();

		Massnahme punktuellRadVis = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kanteRadVis))
			.build();

		Massnahme abschnittsweiseDlm = MassnahmeTestDataProvider.withDefaultValues()
			.id(3L)
			.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kanteDLM))
			.build();

		when(massnahmeRepository.findByKantenInNetzBezug(List.of(10L))).thenReturn(
			List.of(abschnittsweiseRadVis, punktuellRadVis));
		when(massnahmeRepository.findByKantenInNetzBezug(List.of(20L))).thenReturn(List.of(abschnittsweiseDlm));

		List<MassnahmeNetzBezugAenderung> dummyNetzbezugAenderungen = List.of(mock(MassnahmeNetzBezugAenderung.class));
		when(massnahmeNetzbezugAenderungProtokollierungsService.protokolliereNetzBezugAenderungFuerGeloeschteKante(
			any(), any(), any(), any(), any(), any()))
				.thenReturn(dummyNetzbezugAenderungen);

		Benutzer benutzer = BenutzerTestDataProvider.technischerBenutzer().build();
		when(benutzerService.getTechnischerBenutzer()).thenReturn(benutzer);

		LocalDateTime aenderungsdatum = LocalDateTime.now();

		// act
		KanteDeleteStatistik statistik = new KanteDeleteStatistik();
		massnahmeService.onKantenGeloescht(new KantenDeletedEvent(List.of(kanteRadVis.getId()), List.of(kanteRadVis
			.getGeometry()),
			netzAenderungAusloeser, aenderungsdatum, statistik));

		// assert
		assertThat(abschnittsweiseRadVis.getNetzbezug().getImmutableKantenAbschnittBezug()).isEmpty();
		assertThat(punktuellRadVis.getNetzbezug().getImmutableKantenPunktBezug()).isEmpty();
		assertThat(abschnittsweiseDlm.getNetzbezug().getImmutableKantenAbschnittBezug()).hasSize(1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Massnahme>> massnahmenCaptor = ArgumentCaptor.forClass(List.class);
		verify(massnahmeNetzbezugAenderungProtokollierungsService, times(1))
			.protokolliereNetzBezugAenderungFuerGeloeschteKante(massnahmenCaptor.capture(), eq(kanteRadVis.getId()), eq(
				aenderungsdatum), eq(netzAenderungAusloeser), eq(kanteRadVis.getGeometry()), eq(benutzer));
		assertThat(massnahmenCaptor.getValue()).containsExactlyInAnyOrder(abschnittsweiseRadVis, punktuellRadVis);

		assertThat(statistik.anzahlAngepassterNetzbezuege).isEqualTo(2);
	}

	@Test
	void archivieren() {
		// arrange
		Massnahme massnahme1 = MassnahmeTestDataProvider.withDefaultValues().build();
		Massnahme massnahme2 = MassnahmeTestDataProvider.withDefaultValues().build();
		when(massnahmeRepository.findAllById(any()))
			.thenReturn(List.of(massnahme1, massnahme2));

		// act
		massnahmeService.massnahmenArchivieren(List.of());

		// assert
		assertThat(massnahme1.isArchiviert()).isTrue();
		assertThat(massnahme2.isArchiviert()).isTrue();
	}

	@Test
	void archivieren_ignoresArchivierteMassnahmen() {
		// arrange
		Massnahme archivierteMassnahme = MassnahmeTestDataProvider.withDefaultValues().build();
		archivierteMassnahme.archivieren();
		Massnahme nichtArchivierteMassnahme = MassnahmeTestDataProvider.withDefaultValues().build();
		when(massnahmeRepository.findAllById(any()))
			.thenReturn(List.of(archivierteMassnahme, nichtArchivierteMassnahme));

		// act + assert
		assertDoesNotThrow(() -> massnahmeService.massnahmenArchivieren(List.of()));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void onKnotenGeloescht_ersetzeWennMoeglich_loescheRest() {
		// arrange
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues()
			.id(1l).point(GeometryTestdataProvider.createPoint(new Coordinate(100.5, 10))).build();
		Knoten zuLoeschenderKnotenMitErsatz = KnotenTestDataProvider.withDefaultValues()
			.id(2l).point(GeometryTestdataProvider.createPoint(new Coordinate(100, 10))).build();
		Knoten zuLoeschenderKnotenOhneErsatz = KnotenTestDataProvider.withDefaultValues()
			.id(3l).point(GeometryTestdataProvider.createPoint(new Coordinate(100, 100))).build();
		Knoten andererKnoten = KnotenTestDataProvider.withDefaultValues()
			.id(4l).point(GeometryTestdataProvider.createPoint(new Coordinate(200, 100))).build();
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withKnoten(zuLoeschenderKnotenMitErsatz, zuLoeschenderKnotenOhneErsatz, andererKnoten).id(1l).build();
		Massnahme massnahme2 = MassnahmeTestDataProvider
			.withKnoten(zuLoeschenderKnotenOhneErsatz, andererKnoten).id(2l).build();
		when(massnahmeRepository.findByKnotenInNetzBezug(any())).thenReturn(List.of(massnahme1, massnahme2));
		when(netzService.findErsatzKnoten(eq(zuLoeschenderKnotenMitErsatz.getId()), any()))
			.thenReturn(Optional.of(ersatzKnoten));
		when(netzService.findErsatzKnoten(eq(zuLoeschenderKnotenOhneErsatz.getId()), any()))
			.thenReturn(Optional.empty());

		// act
		KnotenDeleteStatistik statistik = new KnotenDeleteStatistik();
		massnahmeService.onKnotenGeloescht(
			new KnotenDeletedEvent(List.of(zuLoeschenderKnotenMitErsatz, zuLoeschenderKnotenOhneErsatz),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now(), statistik));

		// assert
		assertThat(massnahme1.getNetzbezug().getImmutableKnotenBezug()).containsExactlyInAnyOrder(andererKnoten,
			ersatzKnoten);
		assertThat(massnahme2.getNetzbezug().getImmutableKnotenBezug()).containsExactly(andererKnoten);
		assertThat(statistik.anzahlKnotenbezuegeErsetzt).isEqualTo(1);
		assertThat(statistik.anzahlKnotenbezuegeGeloescht).isEqualTo(2);
		ArgumentCaptor<List> excludeIdsCaptor = ArgumentCaptor.forClass(List.class);
		verify(netzService, times(1)).findErsatzKnoten(eq(zuLoeschenderKnotenMitErsatz.getId()),
			excludeIdsCaptor.capture());
		assertThat(excludeIdsCaptor.getValue()).containsExactlyInAnyOrder(zuLoeschenderKnotenMitErsatz.getId(),
			zuLoeschenderKnotenOhneErsatz.getId());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void onKnotenGeloescht_schreibeProtokollNurFuerLoeschung() {
		// arrange
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues()
			.id(1l).point(GeometryTestdataProvider.createPoint(new Coordinate(100.5, 10))).build();
		Knoten zuLoeschenderKnotenMitErsatz = KnotenTestDataProvider.withDefaultValues()
			.id(2l).point(GeometryTestdataProvider.createPoint(new Coordinate(100, 10))).build();
		Knoten zuLoeschenderKnotenOhneErsatz = KnotenTestDataProvider.withDefaultValues()
			.id(3l).point(GeometryTestdataProvider.createPoint(new Coordinate(100, 100))).build();
		Massnahme massnahme1 = MassnahmeTestDataProvider
			.withKnoten(zuLoeschenderKnotenMitErsatz).id(1l).build();
		Massnahme massnahme2 = MassnahmeTestDataProvider
			.withKnoten(zuLoeschenderKnotenOhneErsatz).id(2l).build();
		when(massnahmeRepository.findByKnotenInNetzBezug(any())).thenReturn(List.of(massnahme1, massnahme2));
		when(netzService.findErsatzKnoten(eq(zuLoeschenderKnotenMitErsatz.getId()), any()))
			.thenReturn(Optional.of(ersatzKnoten));

		// act
		KnotenDeleteStatistik statistik = new KnotenDeleteStatistik();
		KnotenDeletedEvent event = new KnotenDeletedEvent(
			List.of(zuLoeschenderKnotenMitErsatz, zuLoeschenderKnotenOhneErsatz),
			NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now(), statistik);
		massnahmeService.onKnotenGeloescht(event);

		// assert
		ArgumentCaptor<List> massnahmeListCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Geometry> geometryCaptor = ArgumentCaptor.forClass(Geometry.class);
		verify(massnahmeNetzbezugAenderungProtokollierungsService, atMostOnce())
			.protokolliereNetzBezugAenderungFuerGeloeschteKnoten(massnahmeListCaptor.capture(),
				eq(zuLoeschenderKnotenOhneErsatz.getId()), eq(event.getDatum()), eq(event.getAusloeser()),
				geometryCaptor.capture(), any());
		assertThat(massnahmeListCaptor.getValue()).containsExactly(massnahme2);
		assertThat(geometryCaptor.getValue()).isEqualTo(zuLoeschenderKnotenOhneErsatz.getPoint());
		assertThat(statistik.anzahlKnotenbezuegeErsetzt).isEqualTo(1);
		assertThat(statistik.anzahlKnotenbezuegeGeloescht).isEqualTo(1);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void onKnotenGeloescht_multipleKnotenErsetzt_korrektErsatzknoten() {
		// arrange
		Massnahme massnahmeMock = mock(Massnahme.class);
		when(massnahmeRepository.findByKnotenInNetzBezug(any())).thenReturn(List.of(massnahmeMock));
		Knoten ersatzKnoten1 = KnotenTestDataProvider.withDefaultValues()
			.id(2l).point(GeometryTestdataProvider.createPoint(new Coordinate(100.1, 10))).build();
		Knoten zuLoeschenderKnoten1 = KnotenTestDataProvider.withDefaultValues()
			.id(1l).point(GeometryTestdataProvider.createPoint(new Coordinate(100, 10))).build();
		Knoten ersatzKnoten2 = KnotenTestDataProvider.withDefaultValues()
			.id(3l).point(GeometryTestdataProvider.createPoint(new Coordinate(100.1, 100))).build();
		Knoten zuLoeschenderKnoten2 = KnotenTestDataProvider.withDefaultValues()
			.id(4l).point(GeometryTestdataProvider.createPoint(new Coordinate(100, 100))).build();
		when(netzService.findErsatzKnoten(eq(zuLoeschenderKnoten1.getId()), any()))
			.thenReturn(Optional.of(ersatzKnoten1));
		when(netzService.findErsatzKnoten(eq(zuLoeschenderKnoten2.getId()), any()))
			.thenReturn(Optional.of(ersatzKnoten2));
		when(massnahmeMock.getNetzbezug()).thenReturn(
			new MassnahmeNetzBezug(Collections.emptySet(), Collections.emptySet(),
				Set.of(ersatzKnoten1, ersatzKnoten2)));

		// act
		KnotenDeleteStatistik statistik = new KnotenDeleteStatistik();
		massnahmeService.onKnotenGeloescht(
			new KnotenDeletedEvent(List.of(zuLoeschenderKnoten1, zuLoeschenderKnoten2),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now(), statistik));

		// assert
		ArgumentCaptor<Map> ersatzKnotenArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		verify(massnahmeMock).ersetzeKnotenInNetzbezug((Map<Long, Knoten>) ersatzKnotenArgumentCaptor.capture());
		assertThat(ersatzKnotenArgumentCaptor.getValue()).hasSize(2);
		assertThat(ersatzKnotenArgumentCaptor.getValue()).containsKey(zuLoeschenderKnoten1.getId());
		assertThat(ersatzKnotenArgumentCaptor.getValue().get(zuLoeschenderKnoten1.getId())).isEqualTo(ersatzKnoten1);
		assertThat(ersatzKnotenArgumentCaptor.getValue()).containsKey(zuLoeschenderKnoten2.getId());
		assertThat(ersatzKnotenArgumentCaptor.getValue().get(zuLoeschenderKnoten2.getId())).isEqualTo(ersatzKnoten2);
	}

	@Test
	void onKnotenGeloescht_multipleKnotenErsetzt_korrektStatistik() {
		// arrange
		Knoten ersatzKnoten1 = KnotenTestDataProvider.withDefaultValues()
			.id(2l).point(GeometryTestdataProvider.createPoint(new Coordinate(100.1, 10))).build();
		Knoten zuLoeschenderKnoten1 = KnotenTestDataProvider.withDefaultValues()
			.id(1l).point(GeometryTestdataProvider.createPoint(new Coordinate(100, 10))).build();
		Knoten ersatzKnoten2 = KnotenTestDataProvider.withDefaultValues()
			.id(3l).point(GeometryTestdataProvider.createPoint(new Coordinate(100.1, 100))).build();
		Knoten zuLoeschenderKnoten2 = KnotenTestDataProvider.withDefaultValues()
			.id(4l).point(GeometryTestdataProvider.createPoint(new Coordinate(100, 100))).build();

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues().netzbezug(new MassnahmeNetzBezug(Collections.emptySet(), Collections.emptySet(),
				Set.of(zuLoeschenderKnoten1, zuLoeschenderKnoten2)))
			.id(1l).build();
		when(massnahmeRepository.findByKnotenInNetzBezug(any())).thenReturn(List.of(massnahme));
		when(netzService.findErsatzKnoten(eq(zuLoeschenderKnoten1.getId()), any()))
			.thenReturn(Optional.of(ersatzKnoten1));
		when(netzService.findErsatzKnoten(eq(zuLoeschenderKnoten2.getId()), any()))
			.thenReturn(Optional.of(ersatzKnoten2));

		// act
		KnotenDeleteStatistik statistik = new KnotenDeleteStatistik();
		massnahmeService.onKnotenGeloescht(
			new KnotenDeletedEvent(List.of(zuLoeschenderKnoten1, zuLoeschenderKnoten2),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now(), statistik));

		// assert
		assertThat(statistik.anzahlKnotenbezuegeErsetzt).isEqualTo(1);
		assertThat(statistik.anzahlKnotenbezuegeGeloescht).isEqualTo(0);
	}

	@Test
	void onKnotenGeloescht_multipleKnotenGeloescht_korrektStatistik() {
		// arrange
		Knoten zuLoeschenderKnoten1 = KnotenTestDataProvider.withDefaultValues()
			.id(1l).point(GeometryTestdataProvider.createPoint(new Coordinate(100, 10))).build();
		Knoten zuLoeschenderKnoten2 = KnotenTestDataProvider.withDefaultValues()
			.id(4l).point(GeometryTestdataProvider.createPoint(new Coordinate(100, 100))).build();

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues().netzbezug(new MassnahmeNetzBezug(Collections.emptySet(), Collections.emptySet(),
				Set.of(zuLoeschenderKnoten1, zuLoeschenderKnoten2)))
			.id(1l).build();
		when(massnahmeRepository.findByKnotenInNetzBezug(any())).thenReturn(List.of(massnahme));
		when(netzService.findKnotenInBereich(any())).thenReturn(Collections.emptyList());

		// act
		KnotenDeleteStatistik statistik = new KnotenDeleteStatistik();
		massnahmeService.onKnotenGeloescht(
			new KnotenDeletedEvent(List.of(zuLoeschenderKnoten1, zuLoeschenderKnoten2),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now(), statistik));

		// assert
		assertThat(massnahme.getNetzbezug().getImmutableKnotenBezug()).isEmpty();

		assertThat(statistik.anzahlKnotenbezuegeErsetzt).isEqualTo(0);
		assertThat(statistik.anzahlKnotenbezuegeGeloescht).isEqualTo(1);
	}

	@Test
	void onKnotenGeloescht_noMatch_doesNothing() {
		// arrange
		Massnahme massnahmeMock = mock(Massnahme.class);
		when(massnahmeRepository.findByKnotenInNetzBezug(any())).thenReturn(List.of(massnahmeMock));
		Knoten zuLoeschenderKnoten = KnotenTestDataProvider.withDefaultValues()
			.id(1l).point(GeometryTestdataProvider.createPoint(new Coordinate(100, 10))).build();
		when(massnahmeMock.getNetzbezug())
			.thenReturn(
				new MassnahmeNetzBezug(Collections.emptySet(), Collections.emptySet(), Set.of(zuLoeschenderKnoten)));
		when(netzService.findErsatzKnoten(any(), any())).thenReturn(Optional.empty());

		// act
		massnahmeService.onKnotenGeloescht(new KnotenDeletedEvent(List.of(zuLoeschenderKnoten),
			NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now(), new KnotenDeleteStatistik()));

		// assert
		verify(massnahmeMock, never()).ersetzeKnotenInNetzbezug(any());
	}

	@Test
	void onKanteErsetzt_ersetztNurWennKeineGeometrischeAenderung() {
		// arrange
		Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM).id(1l)
			.build();
		Kante ersatzKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM).id(2l)
			.build();
		Massnahme massnahmeCanErsetzen = MassnahmeTestDataProvider.withDefaultValues()
			.netzbezug(
				new MassnahmeNetzBezug(
					Set.of(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante,
						LinearReferenzierterAbschnitt.of(0, 0.3), Seitenbezug.BEIDSEITIG)),
					Collections.emptySet(), Collections.emptySet()))
			.id(3l).build();
		Massnahme massnahmeCannotErsetzen = MassnahmeTestDataProvider.withDefaultValues()
			.netzbezug(
				new MassnahmeNetzBezug(
					Set.of(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante,
						LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG)),
					Collections.emptySet(), Collections.emptySet()))
			.id(4l).build();
		when(massnahmeRepository.findByKantenInNetzBezug(any())).thenReturn(
			List.of(massnahmeCanErsetzen, massnahmeCannotErsetzen));

		// act
		KanteErsetzenStatistik statistik = new KanteErsetzenStatistik();
		massnahmeService.onKanteErsetzt(new KanteErsetztEvent(zuErsetzendeKante, Set.of(ersatzKante), statistik));

		// assert
		assertThat(statistik.anzahlAngepassterNetzbezuege).isEqualTo(1);

		assertThat(massnahmeCanErsetzen.getNetzbezug().getImmutableKantenAbschnittBezug())
			.containsExactly(new AbschnittsweiserKantenSeitenBezug(ersatzKante,
				LinearReferenzierterAbschnitt.of(0, 0.6), Seitenbezug.BEIDSEITIG));
		assertThat(massnahmeCannotErsetzen.getNetzbezug().getImmutableKantenAbschnittBezug())
			.containsExactly(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante,
				LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG));
	}
}