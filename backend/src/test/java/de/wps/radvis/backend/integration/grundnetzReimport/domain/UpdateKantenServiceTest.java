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

package de.wps.radvis.backend.integration.grundnetzReimport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.grundnetz.domain.DLMAttributMapper;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.DLMReimportJobStatistik;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.SplitUpdate;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.TopologischesUpdate;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.StartUndEndpunktGleichException;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;

class UpdateKantenServiceTest {

	private UpdateKantenService updateKantenService;
	@Mock
	private DLMAttributMapper dlmAttributMapper;
	@Mock
	private KantenRepository kantenRepository;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		updateKantenService = new UpdateKantenService(dlmAttributMapper, kantenRepository);
		when(dlmAttributMapper.mapKantenAttributGruppe(any())).thenReturn(
			KantenAttributGruppe.builder()
				.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute().build())
				.build());
	}

	@Nested
	class UpdateBestehendeDLMKanteTest {

		@Test
		void testeUpdateBestehendeDLMKante_Aenderung() {
			// arrange
			Kante kante = KanteTestDataProvider.withDefaultValues().build();

			LineString original = kante.getGeometry();

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			ImportedFeature mock = mock(ImportedFeature.class);
			when(mock.getGeometrie())
				.thenReturn(kante.getGeometry());

			// act
			Optional<TopologischesUpdate> topologischesUpdate = updateKantenService
				.updateBestehendeDLMKante(mock, kante, statistik);

			// assert
			assertThat(kante.getGeometry().equals(original)).isTrue();
			assertThat(kante.getKantenAttributGruppe().getKantenAttribute())
				.isEqualTo(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute().build());

			assertThat(statistik.geometrieAenderungen).isEqualTo(0);
			assertThat(statistik.geometrieAenderungenMitTopologischerKonsequenz).isEqualTo(0);
			assertThat(statistik.strassenNummerAenderung).isEqualTo(0);
			assertThat(statistik.strassenNamenAenderung).isEqualTo(0);
			assertThat(topologischesUpdate).isEmpty();
		}

		@Test
		void testeUpdateBestehendeDLMKante_TopologischeAenderungAnStartUndEnde_generiertTopologischesUpdate()
			throws StartUndEndpunktGleichException {
			// arrange
			Knoten altVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(1L).build();
			Knoten altNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
				.id(2L).build();
			Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 10, QuellSystem.DLM)
				.vonKnoten(altVonKnoten)
				.nachKnoten(altNachKnoten)
				.build();

			LineString original = kante.getGeometry();

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			ImportedFeature mock = mock(ImportedFeature.class);
			LineString neu = GeometryTestdataProvider
				.getLinestringVerschobenUmCoordinate(kante.getGeometry(), 0, 3);
			when(mock.getGeometrie()).thenReturn(neu);

			Knoten neuVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 13), QuellSystem.DLM)
				.id(10L)
				.build();
			Knoten neuNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(20, 13), QuellSystem.DLM)
				.id(20L)
				.build();

			// act
			Optional<TopologischesUpdate> topologischesUpdate = updateKantenService
				.updateBestehendeDLMKante(mock, kante, statistik);

			// assert
			assertThat(kante.getGeometry().equals(original)).isTrue();
			assertThat(kante.getZugehoerigeDlmGeometrie().equals(original)).isTrue();
			assertThat(kante.getVonKnoten()).isEqualTo(altVonKnoten);
			assertThat(kante.getNachKnoten()).isEqualTo(altNachKnoten);
			assertThat(statistik.geometrieAenderungen).isEqualTo(1);
			assertThat(statistik.geometrieAenderungenMitTopologischerKonsequenz).isEqualTo(1);

			assertThat(topologischesUpdate).isPresent();
			TopologischesUpdate update = topologischesUpdate.get();
			assertThat(update.getKante()).isEqualTo(kante);
			assertThat(update.getNeueGeometry().equals(neu)).isTrue();
			assertThat(update.getNeuVon().equals(neuVonKnoten.getPoint())).isTrue();
			assertThat(update.getNeuNach().equals(neuNachKnoten.getPoint())).isTrue();
		}

		@Test
		void testeUpdateBestehendeDLMKante_TopologischeAenderungAnVonKnoten_generiertTopologischesUpdate()
			throws StartUndEndpunktGleichException {
			// arrange
			Knoten altVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(1L).build();
			Knoten altNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
				.id(2L).build();
			Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 10, QuellSystem.DLM)
				.vonKnoten(altVonKnoten)
				.nachKnoten(altNachKnoten)
				.build();

			LineString original = kante.getGeometry();

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			ImportedFeature mock = mock(ImportedFeature.class);
			LineString neu = GeometryTestdataProvider.createLineString(new Coordinate[] {
				new Coordinate(10, 13),
				new Coordinate(20, 10)
			});
			when(mock.getGeometrie()).thenReturn(neu);

			Knoten neuVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 13), QuellSystem.DLM)
				.id(10L)
				.build();
			Knoten neuNachKnoten = kante.getNachKnoten();

			// act
			Optional<TopologischesUpdate> topologischesUpdate = updateKantenService
				.updateBestehendeDLMKante(mock, kante, statistik);

			// assert
			assertThat(kante.getGeometry().equals(original)).isTrue();
			assertThat(kante.getZugehoerigeDlmGeometrie().equals(original)).isTrue();
			assertThat(kante.getVonKnoten()).isEqualTo(altVonKnoten);
			assertThat(kante.getNachKnoten()).isEqualTo(altNachKnoten);
			assertThat(statistik.geometrieAenderungen).isEqualTo(1);
			assertThat(statistik.geometrieAenderungenMitTopologischerKonsequenz).isEqualTo(1);

			assertThat(topologischesUpdate).isPresent();
			TopologischesUpdate update = topologischesUpdate.get();
			assertThat(update.getKante()).isEqualTo(kante);
			assertThat(update.getNeueGeometry().equals(neu)).isTrue();
			assertThat(update.getNeuVon().equals(neuVonKnoten.getPoint())).isTrue();
			assertThat(update.getNeuNach().equals(neuNachKnoten.getPoint())).isTrue();
		}

		@Test
		void testeUpdateBestehendeDLMKante_TopologischeAenderungAnNachKnoten_generiertTopologischesUpdate()
			throws StartUndEndpunktGleichException {
			// arrange
			Knoten altVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(1L).build();
			Knoten altNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
				.id(2L).build();
			Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 10, QuellSystem.DLM)
				.vonKnoten(altVonKnoten)
				.nachKnoten(altNachKnoten)
				.build();

			LineString original = kante.getGeometry();

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			ImportedFeature mock = mock(ImportedFeature.class);
			LineString neu = GeometryTestdataProvider.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(20, 13)
			});
			when(mock.getGeometrie()).thenReturn(neu);

			Knoten neuVonKnoten = kante.getVonKnoten();
			Knoten neuNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(20, 13), QuellSystem.DLM)
				.id(20L)
				.build();

			// act
			Optional<TopologischesUpdate> topologischesUpdate = updateKantenService
				.updateBestehendeDLMKante(mock, kante, statistik);

			// assert
			assertThat(kante.getGeometry().equals(original)).isTrue();
			assertThat(kante.getZugehoerigeDlmGeometrie().equals(original)).isTrue();
			assertThat(kante.getVonKnoten()).isEqualTo(altVonKnoten);
			assertThat(kante.getNachKnoten()).isEqualTo(altNachKnoten);
			assertThat(statistik.geometrieAenderungen).isEqualTo(1);
			assertThat(statistik.geometrieAenderungenMitTopologischerKonsequenz).isEqualTo(1);

			assertThat(topologischesUpdate).isPresent();
			TopologischesUpdate update = topologischesUpdate.get();
			assertThat(update.getKante()).isEqualTo(kante);
			assertThat(update.getNeueGeometry().equals(neu)).isTrue();
			assertThat(update.getNeuVon().equals(neuVonKnoten.getPoint())).isTrue();
			assertThat(update.getNeuNach().equals(neuNachKnoten.getPoint())).isTrue();
		}

		@Test
		void testeUpdateBestehendeDLMKante_GeringeGeometrischeAenderung() {
			// arrange
			Kante kante = KanteTestDataProvider.withDefaultValues().build();

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			ImportedFeature mock = mock(ImportedFeature.class);
			LineString verschoben = GeometryTestdataProvider
				.getLinestringVerschobenUmCoordinate(kante.getGeometry(), 0, 1);
			when(mock.getGeometrie()).thenReturn(verschoben);

			// act
			Optional<TopologischesUpdate> topologischesUpdate = updateKantenService
				.updateBestehendeDLMKante(mock, kante, statistik);

			// assert
			assertThat(kante.getGeometry().equals(verschoben)).isTrue();
			assertThat(statistik.geometrieAenderungen).isEqualTo(1);
			assertThat(statistik.geometrieAenderungenMitTopologischerKonsequenz).isEqualTo(0);
			assertThat(topologischesUpdate).isEmpty();
		}

		@Test
		void testeUpdateBestehendeDLMKante_NurAttributeAendernSich_AttributeUpdaten() {
			// arrange
			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			Kante kante = KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(KantenAttributGruppe.builder()
					.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
						.beleuchtung(Beleuchtung.VORHANDEN)
						.status(Status.FIKTIV)
						.strassenName(StrassenName.of("lol"))
						.build())
					.build())
				.build();
			LineString original = kante.getGeometry();
			Knoten vonKnoten = kante.getVonKnoten();
			Knoten nachKnoten = kante.getVonKnoten();

			ImportedFeature mock = mock(ImportedFeature.class);
			when(mock.getGeometrie()).thenReturn(kante.getGeometry());
			when(dlmAttributMapper.mapKantenAttributGruppe(mock)).thenReturn(
				KantenAttributGruppe.builder()
					.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
						.strassenName(StrassenName.of("Watch 'The French Dispatch' you cowards!"))
						.strassenNummer(StrassenNummer.of("B42"))
						.build())
					.build());

			// act
			Optional<TopologischesUpdate> topologischesUpdate = updateKantenService
				.updateBestehendeDLMKante(mock, kante, statistik);

			assertThat(kante.getGeometry().equals(original)).isTrue();
			assertThat(kante.getVonKnoten()).isEqualTo(vonKnoten);
			assertThat(kante.getNachKnoten()).isEqualTo(nachKnoten);

			assertThat(kante.getKantenAttributGruppe().getKantenAttribute().getStrassenName())
				.contains(StrassenName.of("Watch 'The French Dispatch' you cowards!"));
			assertThat(kante.getKantenAttributGruppe().getKantenAttribute().getStrassenNummer())
				.contains(StrassenNummer.of("B42"));
			assertThat(kante.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung())
				.isEqualTo(Beleuchtung.VORHANDEN);
			assertThat(kante.getKantenAttributGruppe().getKantenAttribute().getStatus())
				.isEqualTo(Status.FIKTIV);

			assertThat(statistik.strassenNamenAenderung).isEqualTo(1);
			assertThat(statistik.strassenNummerAenderung).isEqualTo(1);
			assertThat(topologischesUpdate).isEmpty();
		}

		@Test
		void testeUpdateBestehendeDLMKante_SplitKandidat_generiertTopologischesUpdate()
			throws StartUndEndpunktGleichException {
			// arrange
			Knoten altVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(1L).build();
			Knoten altNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
				.id(2L).build();
			Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 10, QuellSystem.DLM)
				.vonKnoten(altVonKnoten)
				.nachKnoten(altNachKnoten)
				.build();

			LineString original = kante.getGeometry();

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			ImportedFeature mock = mock(ImportedFeature.class);
			LineString neu = GeometryTestdataProvider
				.createLineString(new Coordinate(10, 10), new Coordinate(15, 10));
			when(mock.getGeometrie()).thenReturn(neu);

			Knoten neuVonKnoten = kante.getVonKnoten();
			Knoten neuNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(15, 10), QuellSystem.DLM)
				.id(20L)
				.build();

			Kante dummy1 = KanteTestDataProvider.withDefaultValues().id(3L).build();
			Kante dummy2 = KanteTestDataProvider.withDefaultValues().id(4L).build();
			Kante dummy3 = KanteTestDataProvider.withDefaultValues().id(5L).build();
			when(kantenRepository.getAlleKantenEinesKnotens(altVonKnoten)).thenReturn(Set.of(dummy1, dummy2));
			when(kantenRepository.getAlleKantenEinesKnotens(altNachKnoten)).thenReturn(Set.of(dummy3));

			// act
			Optional<TopologischesUpdate> topologischesUpdate = updateKantenService
				.updateBestehendeDLMKante(mock, kante, statistik);

			// assert
			assertThat(kante.getGeometry().equals(original)).isTrue();
			assertThat(kante.getZugehoerigeDlmGeometrie().equals(original)).isTrue();
			assertThat(kante.getVonKnoten()).isEqualTo(altVonKnoten);
			assertThat(kante.getNachKnoten()).isEqualTo(altNachKnoten);
			assertThat(statistik.geometrieAenderungen).isEqualTo(1);
			assertThat(statistik.geometrieAenderungenMitTopologischerKonsequenz).isEqualTo(1);

			assertThat(topologischesUpdate).isPresent();
			TopologischesUpdate update = topologischesUpdate.get();
			assertThat(update.getKante()).isEqualTo(kante);
			assertThat(update.getNeueGeometry().equals(neu)).isTrue();
			assertThat(update.getNeuVon().equals(neuVonKnoten.getPoint())).isTrue();
			assertThat(update.getNeuNach().equals(neuNachKnoten.getPoint())).isTrue();
		}
	}

	@Nested
	class FindSplitIfExistsTest {
		@Test
		void teste_einfacherSplitMitBranchingPaths_ErkenntRichtigenSplitweg() {
			// arrange
			List<Kante> bestehendesNetzMitNeuenDLMKanten = new ArrayList<>();

			Knoten altVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(1L).build();
			Knoten altNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
				.id(2L).build();
			Kante original = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 10, QuellSystem.DLM)
				.vonKnoten(altVonKnoten)
				.nachKnoten(altNachKnoten)
				.id(100L)
				.build();

			bestehendesNetzMitNeuenDLMKanten.add(original);

			Kante vorgaenger = KanteTestDataProvider
				.fromKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 10), QuellSystem.DLM).id(3L)
						.build(),
					altVonKnoten)
				.id(200L)
				.build();
			bestehendesNetzMitNeuenDLMKanten.add(vorgaenger);

			Kante nachfolger = KanteTestDataProvider
				.fromKnoten(
					altNachKnoten,
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).id(4L)
						.build())
				.id(300L)
				.build();
			bestehendesNetzMitNeuenDLMKanten.add(nachfolger);

			Knoten splitKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(15, 10), QuellSystem.DLM)
				.id(5L)
				.build();
			Kante neueSplitKante = KanteTestDataProvider
				.fromKnoten(altVonKnoten,
					splitKnoten)
				.id(400L).build();
			bestehendesNetzMitNeuenDLMKanten.add(neueSplitKante);

			Kante bestehendeSchlechtereAlternativeZurSplitKante = KanteTestDataProvider
				.fromKnoten(altVonKnoten,
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(15, 11), QuellSystem.DLM).id(6L)
						.build())
				.id(500L).build();
			bestehendesNetzMitNeuenDLMKanten.add(bestehendeSchlechtereAlternativeZurSplitKante);

			Kante neuHinzugefuegteAlternative = KanteTestDataProvider
				.fromKnoten(splitKnoten,
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 9), QuellSystem.DLM).id(7L)
						.build())
				.id(600L).build();
			bestehendesNetzMitNeuenDLMKanten.add(neuHinzugefuegteAlternative);

			when(kantenRepository.getKantenInBereichNachQuelle(any(), any()))
				.thenReturn(bestehendesNetzMitNeuenDLMKanten.stream());

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			LineString updatedGeometry = GeometryTestdataProvider
				.createLineString(new Coordinate[] { new Coordinate(15, 10), new Coordinate(20, 10) });
			TopologischesUpdate topologischesUpdate = new TopologischesUpdate(original, updatedGeometry.getStartPoint(),
				updatedGeometry.getEndPoint(),
				updatedGeometry);

			// act
			Optional<SplitUpdate> splitIfExists = updateKantenService.findSplitIfExists(topologischesUpdate,
				Collections.emptySet(), statistik);

			// assert
			assertThat(splitIfExists).isPresent();
			SplitUpdate split = splitIfExists.get();
			assertThat(split.getKante()).isEqualTo(original);
			assertThat(split.getUpdatedGeometry().equals(updatedGeometry)).isTrue();
			assertThat(split.getUpdatedVon()).isEqualTo(splitKnoten);
			assertThat(split.getUpdatedNach()).isEqualTo(original.getNachKnoten());
			assertThat(split.getNeuerWegDerKanteErsetzt()).containsExactly(neueSplitKante, original);
		}

		@Test
		void teste_einfacherSplitMitUnterschiedlicherStationierungsrichtungDerKanten_ErkenntRichtigenSplitweg() {
			// arrange
			List<Kante> bestehendesNetzMitNeuenDLMKanten = new ArrayList<>();

			// original is reversed
			Knoten altVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
				.id(2L).build();
			Knoten altNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(1L).build();
			Kante original = KanteTestDataProvider.withCoordinatesAndQuelle(20, 10, 10, 10, QuellSystem.DLM)
				.vonKnoten(altVonKnoten)
				.nachKnoten(altNachKnoten)
				.id(100L)
				.build();

			bestehendesNetzMitNeuenDLMKanten.add(original);

			Kante vorgaenger = KanteTestDataProvider
				.fromKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 10), QuellSystem.DLM).id(3L)
						.build(),
					altNachKnoten)
				.id(200L)
				.build();
			bestehendesNetzMitNeuenDLMKanten.add(vorgaenger);

			Kante nachfolger = KanteTestDataProvider
				.fromKnoten(
					altVonKnoten,
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).id(4L)
						.build())
				.id(300L)
				.build();
			bestehendesNetzMitNeuenDLMKanten.add(nachfolger);

			Knoten splitKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(15, 10), QuellSystem.DLM)
				.id(5L)
				.build();
			Kante neueSplitKante = KanteTestDataProvider
				.fromKnoten(altNachKnoten,
					splitKnoten)
				.id(400L).build();
			bestehendesNetzMitNeuenDLMKanten.add(neueSplitKante);

			Kante neuHinzugefuegteAlternative = KanteTestDataProvider
				.fromKnoten(splitKnoten,
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 9), QuellSystem.DLM).id(7L)
						.build())
				.id(600L).build();
			bestehendesNetzMitNeuenDLMKanten.add(neuHinzugefuegteAlternative);

			when(kantenRepository.getKantenInBereichNachQuelle(any(), any()))
				.thenReturn(bestehendesNetzMitNeuenDLMKanten.stream());

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			LineString updatedGeometry = GeometryTestdataProvider
				.createLineString(new Coordinate[] { new Coordinate(20, 10), new Coordinate(15, 10) });
			TopologischesUpdate topologischesUpdate = new TopologischesUpdate(original, updatedGeometry.getStartPoint(),
				updatedGeometry.getEndPoint(),
				updatedGeometry);

			// act
			Optional<SplitUpdate> splitIfExists = updateKantenService.findSplitIfExists(topologischesUpdate,
				Collections.emptySet(), statistik);

			// assert
			assertThat(splitIfExists).isPresent();
			SplitUpdate split = splitIfExists.get();
			assertThat(split.getKante()).isEqualTo(original);
			assertThat(split.getUpdatedGeometry().equals(updatedGeometry)).isTrue();
			assertThat(split.getUpdatedVon()).isEqualTo(original.getVonKnoten());
			assertThat(split.getUpdatedNach()).isEqualTo(splitKnoten);
			assertThat(split.getNeuerWegDerKanteErsetzt()).containsExactly(original, neueSplitKante);
		}

		@Test
		void teste_ersteKanteErfuelltBereitsTopologie_AberZweiteKanteFuerSplitIstDa_ErstelltKeinSplitWeilNurEineKanteLang() {
			// arrange
			List<Kante> bestehendesNetzMitNeuenDLMKanten = new ArrayList<>();

			Knoten knoten_45001690 = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(556993.59, 5404266.25), QuellSystem.DLM)
				.id(45001690L).build();
			Knoten knoten_45002921 = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(557047.99, 5404221.15), QuellSystem.DLM)
				.id(45002921L).build();
			Knoten knoten_45044084 = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(557048.21, 5404221.8), QuellSystem.DLM)
				.id(45044084L).build();

			Kante kante_0_originalKante = KanteTestDataProvider.withDefaultValues().id(0L)
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(556993.59, 5404266.25),
					new Coordinate(557048.21, 5404221.8)))
				.vonKnoten(knoten_45001690)
				.nachKnoten(knoten_45002921)
				.build();

			Kante kante_1 = KanteTestDataProvider.withDefaultValues().id(1L)
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(557047.99, 5404221.15),
					new Coordinate(557048.21, 5404221.8)))
				.vonKnoten(knoten_45002921)
				.nachKnoten(knoten_45044084)
				.build();

			Kante existiertNurDamit_knoten_45001690_imSuchindexIst = KanteTestDataProvider.withDefaultValues().id(100L)
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(556922.9, 5404197.57),
					new Coordinate(556969.57, 5404239.16), new Coordinate(556993.59, 5404266.25)))
				.vonKnoten(KnotenTestDataProvider
					.withCoordinateAndQuelle(new Coordinate(556923.83, 5404198.55), QuellSystem.DLM)
					.id(45001668L).build())
				.nachKnoten(knoten_45001690)
				.build();

			bestehendesNetzMitNeuenDLMKanten.add(kante_0_originalKante);
			bestehendesNetzMitNeuenDLMKanten.add(kante_1);
			bestehendesNetzMitNeuenDLMKanten.add(existiertNurDamit_knoten_45001690_imSuchindexIst);

			when(kantenRepository.getKantenInBereichNachQuelle(any(), any()))
				.thenReturn(bestehendesNetzMitNeuenDLMKanten.stream());

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			LineString updatedGeometry = GeometryTestdataProvider
				.createLineString(new Coordinate(557047.99, 5404221.15), new Coordinate(557048.21, 5404221.8),
					new Coordinate(556993.59, 5404266.25));
			TopologischesUpdate topologischesUpdate = new TopologischesUpdate(kante_0_originalKante,
				updatedGeometry.getStartPoint(),
				updatedGeometry.getEndPoint(),
				updatedGeometry);

			// act
			Optional<SplitUpdate> splitIfExists = updateKantenService.findSplitIfExists(topologischesUpdate,
				Collections.emptySet(), statistik);

			// assert
			assertThat(splitIfExists).isEmpty();
			assertThat(statistik.anzahlSplitsMitEinerKanteGefunden).isEqualTo(1);
		}

		@Test
		void teste_einfacherSplitMitStationierungsrichtungsaenderungInTopologischemUpdate_ErkenntRichtigenSplitweg() {
			// arrange
			List<Kante> bestehendesNetzMitNeuenDLMKanten = new ArrayList<>();

			// original is reversed
			Knoten altVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(1L).build();
			Knoten altNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
				.id(2L).build();
			Kante original = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 10, QuellSystem.DLM)
				.vonKnoten(altVonKnoten)
				.nachKnoten(altNachKnoten)
				.id(100L)
				.build();

			bestehendesNetzMitNeuenDLMKanten.add(original);

			Kante vorgaenger = KanteTestDataProvider
				.fromKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 10), QuellSystem.DLM).id(3L)
						.build(),
					altVonKnoten)
				.id(200L)
				.build();
			bestehendesNetzMitNeuenDLMKanten.add(vorgaenger);

			Kante nachfolger = KanteTestDataProvider
				.fromKnoten(
					altNachKnoten,
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).id(4L)
						.build())
				.id(300L)
				.build();
			bestehendesNetzMitNeuenDLMKanten.add(nachfolger);

			Knoten splitKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(15, 10), QuellSystem.DLM)
				.id(5L)
				.build();
			Kante neueSplitKante = KanteTestDataProvider
				.fromKnoten(altVonKnoten,
					splitKnoten)
				.id(400L).build();
			bestehendesNetzMitNeuenDLMKanten.add(neueSplitKante);

			Kante neuHinzugefuegteAlternative = KanteTestDataProvider
				.fromKnoten(splitKnoten,
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 9), QuellSystem.DLM).id(7L)
						.build())
				.id(600L).build();
			bestehendesNetzMitNeuenDLMKanten.add(neuHinzugefuegteAlternative);

			when(kantenRepository.getKantenInBereichNachQuelle(any(), any()))
				.thenReturn(bestehendesNetzMitNeuenDLMKanten.stream());

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			LineString updatedGeometry = GeometryTestdataProvider
				.createLineString(new Coordinate[] { new Coordinate(20, 10), new Coordinate(15, 10) });
			TopologischesUpdate topologischesUpdate = new TopologischesUpdate(original, updatedGeometry.getStartPoint(),
				updatedGeometry.getEndPoint(),
				updatedGeometry);

			// act
			Optional<SplitUpdate> splitIfExists = updateKantenService.findSplitIfExists(topologischesUpdate,
				Collections.emptySet(), statistik);

			// assert
			assertThat(splitIfExists).isPresent();
			SplitUpdate split = splitIfExists.get();
			assertThat(split.getKante()).isEqualTo(original);
			assertThat(split.getUpdatedGeometry().equals(updatedGeometry)).isTrue();
			assertThat(split.getUpdatedVon()).isEqualTo(original.getNachKnoten());
			assertThat(split.getUpdatedNach()).isEqualTo(splitKnoten);
			assertThat(split.getNeuerWegDerKanteErsetzt()).containsExactly(neueSplitKante, original);
		}

		@Test
		void teste_keinSplit_Empty() {
			// arrange
			List<Kante> bestehendesNetzMitNeuenDLMKanten = new ArrayList<>();

			Knoten altVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(1L).build();
			Knoten altNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
				.id(2L).build();
			Kante original = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 10, QuellSystem.DLM)
				.vonKnoten(altVonKnoten)
				.nachKnoten(altNachKnoten)
				.id(100L)
				.build();

			bestehendesNetzMitNeuenDLMKanten.add(original);

			Kante vorgaenger = KanteTestDataProvider
				.fromKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 10), QuellSystem.DLM).id(3L)
						.build(),
					altVonKnoten)
				.id(200L)
				.build();
			bestehendesNetzMitNeuenDLMKanten.add(vorgaenger);

			Kante nachfolger = KanteTestDataProvider
				.fromKnoten(
					altNachKnoten,
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).id(4L)
						.build())
				.id(300L)
				.build();

			bestehendesNetzMitNeuenDLMKanten.add(nachfolger);

			Kante kanteFuerSplitDieZuweitEntferntIst = KanteTestDataProvider
				.fromKnoten(altVonKnoten,
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(15, 12.1), QuellSystem.DLM).id(6L)
						.build())
				.id(500L).build();
			bestehendesNetzMitNeuenDLMKanten.add(kanteFuerSplitDieZuweitEntferntIst);

			when(kantenRepository.getKantenInBereichNachQuelle(any(), any()))
				.thenReturn(bestehendesNetzMitNeuenDLMKanten.stream());

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			LineString updatedGeometry = GeometryTestdataProvider
				.createLineString(new Coordinate[] { new Coordinate(15, 10), new Coordinate(20, 10) });
			TopologischesUpdate topologischesUpdate = new TopologischesUpdate(original, updatedGeometry.getStartPoint(),
				updatedGeometry.getEndPoint(),
				updatedGeometry);

			// act
			Optional<SplitUpdate> splitIfExists = updateKantenService.findSplitIfExists(topologischesUpdate,
				Collections.emptySet(), statistik);

			// assert
			assertThat(splitIfExists).isEmpty();
		}

		@Test
		void teste_loopAufWegUeberAnfangsknoten_terminiert() {
			// arrange
			List<Kante> bestehendesNetzMitNeuenDLMKanten = new ArrayList<>();

			Knoten altVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(1L).build();
			Knoten altNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM)
				.id(2L).build();
			Kante original = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 30, 10, QuellSystem.DLM)
				.vonKnoten(altVonKnoten)
				.nachKnoten(altNachKnoten)
				.id(100L)
				.build();

			bestehendesNetzMitNeuenDLMKanten.add(original);

			Kante vorgaenger = KanteTestDataProvider
				.fromKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 10), QuellSystem.DLM).id(3L)
						.build(),
					altVonKnoten)
				.id(200L)
				.build();
			bestehendesNetzMitNeuenDLMKanten.add(vorgaenger);

			Kante nachfolger = KanteTestDataProvider
				.fromKnoten(
					altNachKnoten,
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(40, 10), QuellSystem.DLM).id(4L)
						.build())
				.id(300L)
				.build();

			bestehendesNetzMitNeuenDLMKanten.add(nachfolger);

			Knoten loopNode1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(13, 10), QuellSystem.DLM)
				.id(6L)
				.build();
			Kante loopKante1 = KanteTestDataProvider
				.fromKnoten(altVonKnoten,
					loopNode1)
				.id(500L).build();
			bestehendesNetzMitNeuenDLMKanten.add(loopKante1);
			Knoten loopNode2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(16, 12), QuellSystem.DLM)
				.id(8L)
				.build();
			Kante loopKante2 = KanteTestDataProvider
				.fromKnoten(loopNode1,
					loopNode2)
				.id(600L).build();
			bestehendesNetzMitNeuenDLMKanten.add(loopKante2);
			Kante loopKante3 = KanteTestDataProvider
				.fromKnoten(loopNode2,
					altVonKnoten)
				.id(700L).build();
			bestehendesNetzMitNeuenDLMKanten.add(loopKante3);

			// wird benötigt damit der Algorithmus nicht frühzeitig abbricht
			Kante neuerVorgaenger = KanteTestDataProvider
				.fromKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
						.id(9L)
						.build(),
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(25, 10), QuellSystem.DLM)
						.id(10L)
						.build())
				.id(800L).build();

			bestehendesNetzMitNeuenDLMKanten.add(neuerVorgaenger);

			when(kantenRepository.getKantenInBereichNachQuelle(any(), any()))
				.thenReturn(bestehendesNetzMitNeuenDLMKanten.stream());

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			LineString updatedGeometry = GeometryTestdataProvider
				.createLineString(new Coordinate[] { new Coordinate(25, 10), new Coordinate(30, 10) });
			TopologischesUpdate topologischesUpdate = new TopologischesUpdate(original, updatedGeometry.getStartPoint(),
				updatedGeometry.getEndPoint(),
				updatedGeometry);

			// act
			Optional<SplitUpdate> splitIfExists = updateKantenService.findSplitIfExists(topologischesUpdate,
				Collections.emptySet(), statistik);

			// assert
			assertThat(splitIfExists).isEmpty();
		}

		@Test
		void teste_loopAufWegUeberNichtAnfangsknoten_terminiert() {
			// arrange
			List<Kante> bestehendesNetzMitNeuenDLMKanten = new ArrayList<>();

			Knoten altVonKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(1L).build();
			Knoten altNachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM)
				.id(2L).build();
			Kante original = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 30, 10, QuellSystem.DLM)
				.vonKnoten(altVonKnoten)
				.nachKnoten(altNachKnoten)
				.id(100L)
				.build();

			bestehendesNetzMitNeuenDLMKanten.add(original);

			Kante vorgaenger = KanteTestDataProvider
				.fromKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 10), QuellSystem.DLM).id(3L)
						.build(),
					altVonKnoten)
				.id(200L)
				.build();
			bestehendesNetzMitNeuenDLMKanten.add(vorgaenger);

			Kante nachfolger = KanteTestDataProvider
				.fromKnoten(
					altNachKnoten,
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(40, 10), QuellSystem.DLM).id(4L)
						.build())
				.id(300L)
				.build();

			bestehendesNetzMitNeuenDLMKanten.add(nachfolger);

			Knoten loopNode1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(13, 10), QuellSystem.DLM)
				.id(6L)
				.build();
			Kante nichtLoopKante1 = KanteTestDataProvider
				.fromKnoten(altVonKnoten,
					loopNode1)
				.id(500L).build();
			bestehendesNetzMitNeuenDLMKanten.add(nichtLoopKante1);

			Knoten loopNode2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(16, 11), QuellSystem.DLM)
				.id(8L)
				.build();
			Kante loopKante1 = KanteTestDataProvider
				.fromKnoten(loopNode1,
					loopNode2)
				.id(600L).build();
			bestehendesNetzMitNeuenDLMKanten.add(loopKante1);

			Knoten loopNode3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(13, 12), QuellSystem.DLM)
				.id(9L)
				.build();
			Kante loopKante2 = KanteTestDataProvider
				.fromKnoten(loopNode2,
					loopNode3)
				.id(700L).build();
			bestehendesNetzMitNeuenDLMKanten.add(loopKante2);

			Kante loopKante3 = KanteTestDataProvider
				.fromKnoten(loopNode3,
					loopNode1)
				.id(800L).build();
			bestehendesNetzMitNeuenDLMKanten.add(loopKante3);

			// wird benötigt damit der Algorithmus nicht frühzeitig abbricht
			Kante neuerVorgaenger = KanteTestDataProvider
				.fromKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
						.id(10L)
						.build(),
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(25, 10), QuellSystem.DLM)
						.id(11L)
						.build())
				.id(900L).build();

			bestehendesNetzMitNeuenDLMKanten.add(neuerVorgaenger);

			when(kantenRepository.getKantenInBereichNachQuelle(any(), any()))
				.thenReturn(bestehendesNetzMitNeuenDLMKanten.stream());

			DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();
			statistik.reset();

			LineString updatedGeometry = GeometryTestdataProvider
				.createLineString(new Coordinate[] { new Coordinate(25, 10), new Coordinate(30, 10) });
			TopologischesUpdate topologischesUpdate = new TopologischesUpdate(original, updatedGeometry.getStartPoint(),
				updatedGeometry.getEndPoint(),
				updatedGeometry);

			// act
			Optional<SplitUpdate> splitIfExists = updateKantenService.findSplitIfExists(topologischesUpdate,
				Collections.emptySet(), statistik);

			// assert
			assertThat(splitIfExists).isEmpty();
		}

		@Test
		void findSpliIfExists_potentiellerSplitPartnerGeloescht_keinSplit() {
			// arrange
			// Knoten der ursprünglichen, gesplitteten Kante
			Knoten altVon = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0),
					QuellSystem.DLM)
				.id(10L)
				.build();
			Knoten altNach = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 20),
					QuellSystem.DLM)
				.id(20L)
				.build();
			Knoten neuVon = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 0),
					QuellSystem.DLM)
				.id(30L)
				.build();
			Knoten neuNach = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(15, 15),
					QuellSystem.DLM)
				.id(30L)
				.build();

			// gesplitteteKante
			Kante gesplitteteKante = KanteTestDataProvider.fromKnoten(altVon, altNach)
				.geometry(GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(5, 0),
					new Coordinate(10, 10),
					new Coordinate(20, 20)
				))
				.quelle(QuellSystem.DLM)
				.id(1L)
				.build();

			Kante splitPartner1 = KanteTestDataProvider.fromKnoten(
					neuNach,
					altNach)
				.id(2L)
				.quelle(QuellSystem.DLM)
				.build();

			Kante splitPartner2 = KanteTestDataProvider.fromKnoten(
					neuVon,
					altVon)
				.id(3L)
				.quelle(QuellSystem.DLM)
				.build();

			TopologischesUpdate topologischesUpdate = new TopologischesUpdate(gesplitteteKante, neuVon.getPoint(),
				neuNach.getPoint(),
				GeometryTestdataProvider
					.createLineString(
						new Coordinate(5, 0),
						new Coordinate(10, 10),
						new Coordinate(15, 15)
					));

			when(kantenRepository.getKantenInBereichNachQuelle(any(), any()))
				.thenReturn(Stream.of(gesplitteteKante, splitPartner1, splitPartner2));

			// act
			Optional<SplitUpdate> splitUpdate = updateKantenService.findSplitIfExists(topologischesUpdate,
				Set.of(splitPartner1),
				new DLMReimportJobStatistik());

			// assert

			assertThat(splitUpdate).isEmpty();

		}
	}
}