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

package de.wps.radvis.backend.barriere.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryCollection;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.entity.BarriereNetzBezug;
import de.wps.radvis.backend.barriere.domain.entity.BarriereNetzBezugAenderung;
import de.wps.radvis.backend.barriere.domain.entity.provider.BarriereTestDataProvider;
import de.wps.radvis.backend.barriere.domain.repository.BarriereNetzBezugAenderungRepository;
import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteDeleteStatistik;
import de.wps.radvis.backend.netz.domain.entity.KanteErsetzenStatistik;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenDeleteStatistik;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.event.KanteErsetztEvent;
import de.wps.radvis.backend.netz.domain.event.KantenDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KnotenDeletedEvent;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;

class BarriereServiceTest {
	@Mock
	private BarriereRepository repository;
	@Mock
	private VerwaltungseinheitService verwaltungseinheitService;
	@Mock
	private NetzService netzService;
	@Mock
	private BarriereNetzBezugAenderungRepository barriereNetzBezugAenderungRepository;
	@Mock
	private BenutzerService benutzerService;

	private BarriereService barriereService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		barriereService = new BarriereService(repository, verwaltungseinheitService, netzService, 1.0,
			barriereNetzBezugAenderungRepository, benutzerService);
	}

	@Test
	@SuppressWarnings("unchecked")
	void onKantenGeloescht_removesKanteFromNetzbezug() {
		// arrange
		ArgumentCaptor<List<Long>> kanteIdCaptor = ArgumentCaptor.forClass(List.class);
		long kanteId = 345l;
		Kante kante1 = KanteTestDataProvider.withDefaultValues().id(kanteId).build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues().id(kanteId + 1).build();
		final BarriereNetzBezug netzbezug = new BarriereNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante1, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS),
				new AbschnittsweiserKantenSeitenBezug(
					kante2, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS)),
			Set.of(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG),
				new PunktuellerKantenSeitenBezug(kante2, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
			Collections.emptySet());
		Barriere barriere = BarriereTestDataProvider.withDefaultValues()
			.netzbezug(netzbezug).build();
		GeometryCollection oldBarriereNetzbezugGeometrie = barriere.getNetzbezug().getGeometrie();
		when(repository.findByKantenInNetzBezug(kanteIdCaptor.capture()))
			.thenReturn(List.of(barriere));

		ArgumentCaptor<BarriereNetzBezugAenderung> captor = ArgumentCaptor.forClass(BarriereNetzBezugAenderung.class);
		when(barriereNetzBezugAenderungRepository.save(captor.capture())).thenReturn(null);
		LocalDateTime currentDate = LocalDateTime.now();

		ArgumentCaptor<List<Barriere>> saveBarriereCaptor = ArgumentCaptor.forClass(List.class);

		// act
		KanteDeleteStatistik statistik = new KanteDeleteStatistik();
		barriereService.onKantenGeloescht(new KantenDeletedEvent(List.of(kanteId), List.of(kante1.getGeometry()),
			NetzAenderungAusloeser.DLM_REIMPORT_JOB, currentDate, statistik));

		// assert
		assertThat(kanteIdCaptor.getValue()).containsExactly(kanteId);

		verify(repository).saveAll(saveBarriereCaptor.capture());
		assertThat(saveBarriereCaptor.getValue()).hasSize(1);
		assertThat(saveBarriereCaptor.getValue().get(0).getNetzbezug().getImmutableKantenAbschnittBezug()).hasSize(1);
		assertThat(saveBarriereCaptor.getValue().get(0).getNetzbezug().getImmutableKantenAbschnittBezug().iterator()
			.next()
			.getKante()).isEqualTo(kante2);
		assertThat(saveBarriereCaptor.getValue().get(0).getNetzbezug().getImmutableKantenPunktBezug()).hasSize(1);
		assertThat(
			saveBarriereCaptor.getValue().get(0).getNetzbezug().getImmutableKantenPunktBezug().iterator().next()
				.getKante())
					.isEqualTo(kante2);
		assertThat(statistik.anzahlAngepassterNetzbezuege).isEqualTo(1);

		verify(barriereNetzBezugAenderungRepository, times(1)).save(any());
		BarriereNetzBezugAenderung netzBezugAenderung = captor.getValue();
		assertThat(netzBezugAenderung.getBarriere().getId()).isEqualTo(saveBarriereCaptor.getValue().get(0).getId());
		assertThat(netzBezugAenderung.getAusloeser()).isEqualTo(NetzAenderungAusloeser.DLM_REIMPORT_JOB);
		assertThat(netzBezugAenderung.getGeometry()).isEqualTo(oldBarriereNetzbezugGeometrie);
		assertThat(netzBezugAenderung.getNetzEntityId()).isEqualTo(kante1.getId());
		assertThat(netzBezugAenderung.getDatum()).isEqualTo(currentDate);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
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
		Barriere barriere1 = BarriereTestDataProvider
			.withDefaultValues().netzbezug(new BarriereNetzBezug(Collections.emptySet(), Collections.emptySet(),
				Set.of(zuLoeschenderKnotenMitErsatz, zuLoeschenderKnotenOhneErsatz, andererKnoten)))
			.id(1l).build();
		Barriere barriere2 = BarriereTestDataProvider
			.withDefaultValues().netzbezug(new BarriereNetzBezug(Collections.emptySet(), Collections.emptySet(),
				Set.of(zuLoeschenderKnotenOhneErsatz, andererKnoten)))
			.id(2l).build();
		when(repository.findByKnotenInNetzBezug(any())).thenReturn(List.of(barriere1, barriere2));
		when(netzService.findErsatzKnoten(eq(zuLoeschenderKnotenMitErsatz.getId()), any()))
			.thenReturn(Optional.of(ersatzKnoten));
		when(netzService.findErsatzKnoten(eq(zuLoeschenderKnotenOhneErsatz.getId()), any()))
			.thenReturn(Optional.empty());

		// act
		KnotenDeleteStatistik statistik = new KnotenDeleteStatistik();
		barriereService.onKnotenGeloescht(
			new KnotenDeletedEvent(List.of(zuLoeschenderKnotenMitErsatz, zuLoeschenderKnotenOhneErsatz),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now(), statistik));

		// assert
		assertThat(barriere1.getNetzbezug().getImmutableKnotenBezug()).containsExactlyInAnyOrder(andererKnoten,
			ersatzKnoten);
		assertThat(barriere2.getNetzbezug().getImmutableKnotenBezug()).containsExactly(andererKnoten);
		assertThat(statistik.anzahlKnotenbezuegeErsetzt).isEqualTo(1);
		assertThat(statistik.anzahlKnotenbezuegeGeloescht).isEqualTo(2);
		ArgumentCaptor<List> excludeIdsCaptor = ArgumentCaptor.forClass(List.class);
		verify(netzService, times(1)).findErsatzKnoten(eq(zuLoeschenderKnotenMitErsatz.getId()),
			excludeIdsCaptor.capture());
		assertThat(excludeIdsCaptor.getValue()).containsExactlyInAnyOrder(zuLoeschenderKnotenMitErsatz.getId(),
			zuLoeschenderKnotenOhneErsatz.getId());
	}

	@Test
	void onKanteErsetzt_ersetztNurWennKeineGeometrischeAenderung() {
		// arrange
		Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM).id(1l)
			.build();
		Kante ersatzKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM).id(2l)
			.build();
		Barriere barriereCanErsetzen = BarriereTestDataProvider.withDefaultValues()
			.netzbezug(
				new BarriereNetzBezug(
					Set.of(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante,
						LinearReferenzierterAbschnitt.of(0, 0.3), Seitenbezug.BEIDSEITIG)),
					Collections.emptySet(), Collections.emptySet()))
			.id(3l).build();
		Barriere barriereCannotErsetzen = BarriereTestDataProvider.withDefaultValues()
			.netzbezug(
				new BarriereNetzBezug(
					Set.of(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante,
						LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG)),
					Collections.emptySet(), Collections.emptySet()))
			.id(4l).build();
		when(repository.findByKantenInNetzBezug(any())).thenReturn(
			List.of(barriereCanErsetzen, barriereCannotErsetzen));

		// act
		KanteErsetzenStatistik statistik = new KanteErsetzenStatistik();
		barriereService.onKanteErsetzt(new KanteErsetztEvent(zuErsetzendeKante, Set.of(ersatzKante), statistik));

		// assert
		assertThat(statistik.anzahlAngepassterNetzbezuege).isEqualTo(1);

		assertThat(barriereCanErsetzen.getNetzbezug().getImmutableKantenAbschnittBezug())
			.containsExactly(new AbschnittsweiserKantenSeitenBezug(ersatzKante,
				LinearReferenzierterAbschnitt.of(0, 0.6), Seitenbezug.BEIDSEITIG));
		assertThat(barriereCannotErsetzen.getNetzbezug().getImmutableKantenAbschnittBezug())
			.containsExactly(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante,
				LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG));
	}

	@Test
	@SuppressWarnings("unchecked")
	void onKnotenGeloescht_removesKnotenFromNetzbezug() {
		ArgumentCaptor<List<Long>> knotenIdCaptor = ArgumentCaptor.forClass(List.class);
		long knotenId = 345l;
		Knoten knoten1 = KnotenTestDataProvider.withDefaultValues().id(knotenId).build();
		Knoten knoten2 = KnotenTestDataProvider.withDefaultValues().id(knotenId + 1).build();
		final BarriereNetzBezug netzbezug = new BarriereNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(knoten1, knoten2));
		Barriere barriere = BarriereTestDataProvider.withDefaultValues()
			.netzbezug(netzbezug).build();
		GeometryCollection oldBarriereNetzbezugGeometrie = barriere.getNetzbezug().getGeometrie();
		when(repository.findByKnotenInNetzBezug(knotenIdCaptor.capture()))
			.thenReturn(List.of(barriere));

		ArgumentCaptor<BarriereNetzBezugAenderung> captor = ArgumentCaptor.forClass(BarriereNetzBezugAenderung.class);
		when(barriereNetzBezugAenderungRepository.save(captor.capture())).thenReturn(null);
		LocalDateTime currentDate = LocalDateTime.now();

		ArgumentCaptor<List<Barriere>> saveBarriereCaptor = ArgumentCaptor.forClass(List.class);

		// act
		barriereService.onKnotenGeloescht(new KnotenDeletedEvent(List.of(knoten1),
			NetzAenderungAusloeser.DLM_REIMPORT_JOB, currentDate, new KnotenDeleteStatistik()));

		// assert
		assertThat(knotenIdCaptor.getValue()).containsExactly(knotenId);
		verify(repository).saveAll(saveBarriereCaptor.capture());
		assertThat(saveBarriereCaptor.getValue()).hasSize(1);
		assertThat(saveBarriereCaptor.getValue().get(0).getNetzbezug().getImmutableKnotenBezug()).hasSize(1);
		assertThat(saveBarriereCaptor.getValue().get(0).getNetzbezug().getImmutableKnotenBezug().iterator().next())
			.isEqualTo(knoten2);

		verify(barriereNetzBezugAenderungRepository, times(1)).save(any());
		BarriereNetzBezugAenderung netzBezugAenderung = captor.getValue();
		assertThat(netzBezugAenderung.getBarriere().getId()).isEqualTo(saveBarriereCaptor.getValue().get(0).getId());
		assertThat(netzBezugAenderung.getAusloeser()).isEqualTo(NetzAenderungAusloeser.DLM_REIMPORT_JOB);
		assertThat(netzBezugAenderung.getGeometry()).isEqualTo(oldBarriereNetzbezugGeometrie);
		assertThat(netzBezugAenderung.getNetzEntityId()).isEqualTo(knoten1.getId());
		assertThat(netzBezugAenderung.getDatum()).isEqualTo(currentDate);
	}
}
