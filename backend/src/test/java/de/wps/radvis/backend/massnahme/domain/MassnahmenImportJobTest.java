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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmenImportProtokoll;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;

@SuppressWarnings("deprecation")
class MassnahmenImportJobTest {

	MassnahmenImportJob massnahmenImportJob;

	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Mock
	ShapeFileRepository shapeFileRepository;

	@Mock
	SimpleMatchingService simpleMatchingService;

	@Mock
	NetzService netzService;

	@Mock
	MassnahmenMappingService massnahmenMappingService;

	@Mock
	MassnahmeService massnahmeService;

	@Mock
	BenutzerService benutzerService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		massnahmenImportJob = new MassnahmenImportJob(jobExecutionDescriptionRepository, Path.of("coolerWanderPfad"),
			shapeFileRepository,
			simpleMatchingService,
			netzService, massnahmenMappingService, massnahmeService, benutzerService);
	}

	@Test
	void testeCreatePunktNetzbezug_prefersKnoten() {
		// arrange
		Point point = GeometryTestdataProvider.createPoint(new Coordinate(11.4, 10));

		MassnahmenImportProtokoll massnahmenImportProtokoll = new MassnahmenImportProtokoll();

		Envelope expectedEnvelope = point.getEnvelopeInternal();
		expectedEnvelope.expandBy(30);

		Kante naechsteKante = KanteTestDataProvider.withDefaultValues().id(1L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(8, 8), new Coordinate(16, 8)))
			.build();
		Kante weitereKante = KanteTestDataProvider.withDefaultValues().id(2L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(3, 3), new Coordinate(8, 8)))
			.build();
		when(netzService.getRadVisNetzKantenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(weitereKante, naechsteKante)));

		Knoten closestKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(8, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(16, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 3), QuellSystem.DLM)
			.build();
		when(netzService.getRadVisNetzKnotenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(otherKnoten1, closestKnoten, otherKnoten2)));

		// act
		Optional<MassnahmeNetzBezug> result = massnahmenImportJob.bestimmePunktNetzbezugForPoint(point, "id",
			massnahmenImportProtokoll);

		assertThat(result).isPresent();
		assertThat(result.get().getImmutableKantenAbschnittBezug()).isEmpty();
		assertThat(result.get().getImmutableKantenPunktBezug()).isEmpty();
		assertThat(result.get().getImmutableKnotenBezug()).containsExactly(closestKnoten);
	}

	@Test
	void testeCreatePunktNetzbezug_prefersKnotenButNotOverlySo() {
		// arrange
		Point point = GeometryTestdataProvider.createPoint(new Coordinate(11.5, 10));

		MassnahmenImportProtokoll massnahmenImportProtokoll = new MassnahmenImportProtokoll();

		Envelope expectedEnvelope = point.getEnvelopeInternal();
		expectedEnvelope.expandBy(30);

		Kante naechsteKante = KanteTestDataProvider.withDefaultValues().id(1L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(8, 8), new Coordinate(16, 8)))
			.build();
		Kante weitereKante = KanteTestDataProvider.withDefaultValues().id(2L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(3, 3), new Coordinate(8, 8)))
			.build();
		when(netzService.getRadVisNetzKantenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(weitereKante, naechsteKante)));

		Knoten closestKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(8, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(16, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 3), QuellSystem.DLM)
			.build();
		when(netzService.getRadVisNetzKnotenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(otherKnoten1, closestKnoten, otherKnoten2)));

		// act
		Optional<MassnahmeNetzBezug> result = massnahmenImportJob.bestimmePunktNetzbezugForPoint(point, "id",
			massnahmenImportProtokoll);

		assertThat(result).isPresent();
		assertThat(result.get().getImmutableKantenAbschnittBezug()).isEmpty();
		assertThat(result.get().getImmutableKnotenBezug()).isEmpty();
		assertThat(result.get().getImmutableKantenPunktBezug()).hasSize(1);
		PunktuellerKantenSeitenBezug punktbezug = result.get().getImmutableKantenPunktBezug().stream().findFirst()
			.get();
		assertThat(punktbezug.getKante()).isEqualTo(naechsteKante);
		assertThat(LineareReferenz.fractionEqual(punktbezug.getLineareReferenz(), LineareReferenz.of(0.438))).isTrue();
		assertThat(punktbezug.getSeitenbezug()).isEqualTo(Seitenbezug.BEIDSEITIG);
	}

	@Test
	void testeCreatePunktNetzbezug_nurKanten_NimmtNaechsteKante() {
		// arrange
		Point point = GeometryTestdataProvider.createPoint(new Coordinate(10, 10));

		MassnahmenImportProtokoll massnahmenImportProtokoll = new MassnahmenImportProtokoll();

		Envelope expectedEnvelope = point.getEnvelopeInternal();
		expectedEnvelope.expandBy(30);

		Kante naechsteKante = KanteTestDataProvider.withDefaultValues().id(1L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(8, 8), new Coordinate(16, 8)))
			.build();
		Kante weitereKante = KanteTestDataProvider.withDefaultValues().id(2L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(3, 3), new Coordinate(8, 8)))
			.build();
		when(netzService.getRadVisNetzKantenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(weitereKante, naechsteKante)));

		when(netzService.getRadVisNetzKnotenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of()));

		// act
		Optional<MassnahmeNetzBezug> result = massnahmenImportJob.bestimmePunktNetzbezugForPoint(point, "id",
			massnahmenImportProtokoll);

		assertThat(result).isPresent();
		assertThat(result.get().getImmutableKantenAbschnittBezug()).isEmpty();
		assertThat(result.get().getImmutableKnotenBezug()).isEmpty();
		assertThat(result.get().getImmutableKantenPunktBezug()).hasSize(1);
		PunktuellerKantenSeitenBezug punktbezug = result.get().getImmutableKantenPunktBezug().stream().findFirst()
			.get();
		assertThat(punktbezug.getKante()).isEqualTo(naechsteKante);
		assertThat(LineareReferenz.fractionEqual(punktbezug.getLineareReferenz(), LineareReferenz.of(0.25))).isTrue();
		assertThat(punktbezug.getSeitenbezug()).isEqualTo(Seitenbezug.BEIDSEITIG);
	}

	@Test
	void testeCreatePunktNetzbezug_nurKnoten_NimmtNaechstenKnoten() {
		// arrange
		Point point = GeometryTestdataProvider.createPoint(new Coordinate(11.5, 10));

		MassnahmenImportProtokoll massnahmenImportProtokoll = new MassnahmenImportProtokoll();

		Envelope expectedEnvelope = point.getEnvelopeInternal();
		expectedEnvelope.expandBy(30);

		when(netzService.getRadVisNetzKantenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of()));

		Knoten closestKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(8, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(16, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 3), QuellSystem.DLM)
			.build();
		when(netzService.getRadVisNetzKnotenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(otherKnoten1, closestKnoten, otherKnoten2)));

		// act
		Optional<MassnahmeNetzBezug> result = massnahmenImportJob.bestimmePunktNetzbezugForPoint(point, "id",
			massnahmenImportProtokoll);

		assertThat(result).isPresent();
		assertThat(result.get().getImmutableKantenAbschnittBezug()).isEmpty();
		assertThat(result.get().getImmutableKantenPunktBezug()).isEmpty();
		assertThat(result.get().getImmutableKnotenBezug()).containsExactly(closestKnoten);
	}

	@Nested
	class FiltereUeberlappungen {
		@Test
		void happyPath() {
			AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.6), Seitenbezug.BEIDSEITIG);
			AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.4, 0.8), Seitenbezug.BEIDSEITIG);
			AbschnittsweiserKantenSeitenBezug three = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0.4, 0.8), Seitenbezug.BEIDSEITIG);
			assertThat(massnahmenImportJob.filtereUeberlappungen(Set.of(one, two, three)))
				.containsExactlyInAnyOrder(one.copyWithLR(LinearReferenzierterAbschnitt.of(0.2, 0.8)), three);
		}

		@Test
		void verschiedeneID_filtertNicht() {
			AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.6), Seitenbezug.BEIDSEITIG);
			AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0.4, 0.8), Seitenbezug.BEIDSEITIG);
			assertThat(massnahmenImportJob.filtereUeberlappungen(Set.of(one, two)))
				.containsExactlyInAnyOrder(one, two);
		}

		@Test
		void nichtBeidseitig_failt() {
			AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.6), Seitenbezug.LINKS);
			AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0.4, 0.8), Seitenbezug.BEIDSEITIG);
			assertThatThrownBy(() -> massnahmenImportJob.filtereUeberlappungen(Set.of(one, two))).isInstanceOf(
				RequireViolation.class);
		}
	}
}
