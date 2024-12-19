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

package de.wps.radvis.backend.furtKreuzung.domain;

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

import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzung;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzungNetzBezug;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzungNetzBezugAenderung;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzungTestDataProvider;
import de.wps.radvis.backend.furtKreuzung.domain.repository.FurtKreuzungNetzBezugAenderungRepository;
import de.wps.radvis.backend.furtKreuzung.domain.repository.FurtKreuzungRepository;
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

class FurtKreuzungServiceTest {
	@Mock
	private FurtKreuzungRepository repository;
	@Mock
	private VerwaltungseinheitService verwaltungseinheitService;
	@Mock
	private NetzService netzService;
	@Mock
	private FurtKreuzungNetzBezugAenderungRepository netzBezugAenderungRepository;
	@Mock
	private BenutzerService benutzerService;

	private FurtKreuzungService furtKreuzungService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		furtKreuzungService = new FurtKreuzungService(repository, verwaltungseinheitService, netzService, 1.0,
			netzBezugAenderungRepository, benutzerService);
	}

	@Test
	@SuppressWarnings("unchecked")
	void onKantenGeloescht_removesKanteFromNetzbezug() {
		// arrange
		ArgumentCaptor<List<Long>> kanteIdCaptor = ArgumentCaptor.forClass(List.class);
		long kanteId = 345l;
		Kante kante1 = KanteTestDataProvider.withDefaultValues().id(kanteId).build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues().id(kanteId + 1).build();
		final FurtKreuzungNetzBezug netzbezug = new FurtKreuzungNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante1, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS),
				new AbschnittsweiserKantenSeitenBezug(
					kante2, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS)),
			Set.of(new PunktuellerKantenSeitenBezug(kante1, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG),
				new PunktuellerKantenSeitenBezug(kante2, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
			Collections.emptySet());
		FurtKreuzung furtKreuzung = FurtKreuzungTestDataProvider.withDefaultValues()
			.netzbezug(netzbezug).build();
		GeometryCollection oldNetzbezugGeometrie = furtKreuzung.getNetzbezug().getGeometrie();
		when(repository.findByKanteInNetzBezug(kanteIdCaptor.capture()))
			.thenReturn(List.of(furtKreuzung));

		ArgumentCaptor<FurtKreuzungNetzBezugAenderung> captor = ArgumentCaptor.forClass(
			FurtKreuzungNetzBezugAenderung.class);
		when(netzBezugAenderungRepository.save(captor.capture())).thenReturn(null);
		LocalDateTime currentDate = LocalDateTime.now();

		ArgumentCaptor<List<FurtKreuzung>> saveFurtKreuzungCaptor = ArgumentCaptor.forClass(List.class);
		when(repository.saveAll(saveFurtKreuzungCaptor.capture())).thenReturn(List.of(furtKreuzung));

		// act
		KanteDeleteStatistik statistik = new KanteDeleteStatistik();
		furtKreuzungService.onKantenGeloescht(new KantenDeletedEvent(List.of(kanteId), List.of(kante1.getGeometry()),
			NetzAenderungAusloeser.DLM_REIMPORT_JOB, currentDate, statistik));

		// assert
		assertThat(kanteIdCaptor.getValue()).containsExactly(kanteId);
		assertThat(saveFurtKreuzungCaptor.getValue()).hasSize(1);
		FurtKreuzung capturedFurtKreuzung = saveFurtKreuzungCaptor.getValue().get(0);
		assertThat(capturedFurtKreuzung.getNetzbezug().getImmutableKantenAbschnittBezug()).hasSize(1);
		assertThat(capturedFurtKreuzung.getNetzbezug().getImmutableKantenAbschnittBezug().iterator().next()
			.getKante()).isEqualTo(kante2);
		assertThat(capturedFurtKreuzung.getNetzbezug().getImmutableKantenPunktBezug()).hasSize(1);
		assertThat(capturedFurtKreuzung.getNetzbezug().getImmutableKantenPunktBezug().iterator().next().getKante())
			.isEqualTo(kante2);
		assertThat(statistik.anzahlAngepassterNetzbezuege).isEqualTo(1);

		verify(netzBezugAenderungRepository, times(1)).save(any());
		FurtKreuzungNetzBezugAenderung netzBezugAenderung = captor.getValue();
		assertThat(netzBezugAenderung.getFurtKreuzung().getId()).isEqualTo(saveFurtKreuzungCaptor.getValue().get(0)
			.getId());
		assertThat(netzBezugAenderung.getAusloeser()).isEqualTo(NetzAenderungAusloeser.DLM_REIMPORT_JOB);
		assertThat(netzBezugAenderung.getGeometry()).isEqualTo(oldNetzbezugGeometrie);
		assertThat(netzBezugAenderung.getNetzEntityId()).isEqualTo(kante1.getId());
		assertThat(netzBezugAenderung.getDatum()).isEqualTo(currentDate);
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
		FurtKreuzung furtKreuzung1 = FurtKreuzungTestDataProvider
			.withDefaultValues().netzbezug(new FurtKreuzungNetzBezug(Collections.emptySet(), Collections.emptySet(),
				Set.of(zuLoeschenderKnotenMitErsatz, zuLoeschenderKnotenOhneErsatz, andererKnoten)))
			.id(1l).build();
		FurtKreuzung furtKreuzung2 = FurtKreuzungTestDataProvider
			.withDefaultValues().netzbezug(new FurtKreuzungNetzBezug(Collections.emptySet(), Collections.emptySet(),
				Set.of(zuLoeschenderKnotenOhneErsatz, andererKnoten)))
			.id(2l).build();
		when(repository.findByKnotenInNetzBezug(any())).thenReturn(List.of(furtKreuzung1, furtKreuzung2));
		when(netzService.findErsatzKnoten(eq(zuLoeschenderKnotenMitErsatz.getId()), any()))
			.thenReturn(Optional.of(ersatzKnoten));
		when(netzService.findErsatzKnoten(eq(zuLoeschenderKnotenOhneErsatz.getId()), any()))
			.thenReturn(Optional.empty());

		// act
		KnotenDeleteStatistik statistik = new KnotenDeleteStatistik();
		furtKreuzungService.onKnotenGeloescht(
			new KnotenDeletedEvent(List.of(zuLoeschenderKnotenMitErsatz, zuLoeschenderKnotenOhneErsatz),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now(), statistik));

		// assert
		assertThat(furtKreuzung1.getNetzbezug().getImmutableKnotenBezug()).containsExactlyInAnyOrder(andererKnoten,
			ersatzKnoten);
		assertThat(furtKreuzung2.getNetzbezug().getImmutableKnotenBezug()).containsExactly(andererKnoten);
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
		FurtKreuzung furtKreuzungCanErsetzen = FurtKreuzungTestDataProvider.withDefaultValues()
			.netzbezug(
				new FurtKreuzungNetzBezug(
					Set.of(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante,
						LinearReferenzierterAbschnitt.of(0, 0.3), Seitenbezug.BEIDSEITIG)),
					Collections.emptySet(), Collections.emptySet()))
			.id(3l).build();
		FurtKreuzung furtKreuzungCannotErsetzen = FurtKreuzungTestDataProvider.withDefaultValues()
			.netzbezug(
				new FurtKreuzungNetzBezug(
					Set.of(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante,
						LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG)),
					Collections.emptySet(), Collections.emptySet()))
			.id(4l).build();
		when(repository.findByKanteInNetzBezug(any())).thenReturn(
			List.of(furtKreuzungCanErsetzen, furtKreuzungCannotErsetzen));

		// act
		KanteErsetzenStatistik statistik = new KanteErsetzenStatistik();
		furtKreuzungService.onKanteErsetzt(new KanteErsetztEvent(zuErsetzendeKante, Set.of(ersatzKante), statistik));

		// assert
		assertThat(statistik.anzahlAngepassterNetzbezuege).isEqualTo(1);

		assertThat(furtKreuzungCanErsetzen.getNetzbezug().getImmutableKantenAbschnittBezug())
			.containsExactly(new AbschnittsweiserKantenSeitenBezug(ersatzKante,
				LinearReferenzierterAbschnitt.of(0, 0.6), Seitenbezug.BEIDSEITIG));
		assertThat(furtKreuzungCannotErsetzen.getNetzbezug().getImmutableKantenAbschnittBezug())
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
		final FurtKreuzungNetzBezug netzbezug = new FurtKreuzungNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(knoten1, knoten2));
		FurtKreuzung furtKreuzung = FurtKreuzungTestDataProvider.withDefaultValues()
			.netzbezug(netzbezug).build();
		GeometryCollection oldNetzbezugGeometrie = furtKreuzung.getNetzbezug().getGeometrie();
		when(repository.findByKnotenInNetzBezug(knotenIdCaptor.capture()))
			.thenReturn(List.of(furtKreuzung));

		ArgumentCaptor<FurtKreuzungNetzBezugAenderung> captor = ArgumentCaptor.forClass(
			FurtKreuzungNetzBezugAenderung.class);
		when(netzBezugAenderungRepository.save(captor.capture())).thenReturn(null);
		LocalDateTime currentDate = LocalDateTime.now();

		ArgumentCaptor<List<FurtKreuzung>> saveFurtKreuzungCaptor = ArgumentCaptor.forClass(List.class);

		// act
		furtKreuzungService.onKnotenGeloescht(new KnotenDeletedEvent(List.of(knoten1),
			NetzAenderungAusloeser.DLM_REIMPORT_JOB, currentDate, new KnotenDeleteStatistik()));

		// assert
		assertThat(knotenIdCaptor.getValue()).containsExactly(knotenId);
		verify(repository).saveAll(saveFurtKreuzungCaptor.capture());
		assertThat(saveFurtKreuzungCaptor.getValue()).hasSize(1);
		assertThat(saveFurtKreuzungCaptor.getValue().get(0).getNetzbezug().getImmutableKnotenBezug()).hasSize(1);
		assertThat(saveFurtKreuzungCaptor.getValue().get(0).getNetzbezug().getImmutableKnotenBezug().iterator().next())
			.isEqualTo(knoten2);

		verify(netzBezugAenderungRepository, times(1)).save(any());
		FurtKreuzungNetzBezugAenderung netzBezugAenderung = captor.getValue();
		assertThat(netzBezugAenderung.getFurtKreuzung().getId()).isEqualTo(saveFurtKreuzungCaptor.getValue().get(0)
			.getId());
		assertThat(netzBezugAenderung.getAusloeser()).isEqualTo(NetzAenderungAusloeser.DLM_REIMPORT_JOB);
		assertThat(netzBezugAenderung.getGeometry()).isEqualTo(oldNetzbezugGeometrie);
		assertThat(netzBezugAenderung.getNetzEntityId()).isEqualTo(knoten1.getId());
		assertThat(netzBezugAenderung.getDatum()).isEqualTo(currentDate);
	}
}
