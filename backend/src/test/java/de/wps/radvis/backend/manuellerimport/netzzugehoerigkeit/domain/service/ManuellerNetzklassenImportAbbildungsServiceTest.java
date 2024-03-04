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

package de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryFactory;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class ManuellerNetzklassenImportAbbildungsServiceTest {

	@Mock
	private SimpleMatchingService simpleMatchingService;
	@Mock
	private InMemoryKantenRepositoryFactory inMemoryKantenRepositoryFactory;
	@Mock
	private InMemoryKantenRepository inMemoryKantenRepository;

	private ManuellerNetzklassenImportAbbildungsService manuellerNetzklassenImportAbbildungsService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);
		manuellerNetzklassenImportAbbildungsService = new ManuellerNetzklassenImportAbbildungsService(
			simpleMatchingService,
			inMemoryKantenRepositoryFactory);
	}

	@Test
	void testeFindKantenFromLineStrings() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L)
			.build();
		LineString LS0 = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(10, 10));
		LineString LS1 = GeometryTestdataProvider.createLineString(new Coordinate(10, 10),
			new Coordinate(20, 20));
		LineString LS2 = GeometryTestdataProvider.createLineString(new Coordinate(20, 20),
			new Coordinate(30, 30));
		LineString LS3 = GeometryTestdataProvider.createLineString(new Coordinate(30, 30),
			new Coordinate(50, 50));
		Set<LineString> linestrings = Set.of(LS0, LS1, LS2, LS3);

		LineString match0 = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(LS0,
			0.2, 0.2);
		LineString match1 = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(LS1,
			0.2, 0.2);
		LineString match2 = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(LS2,
			0.2, 0.2);

		when(simpleMatchingService.matche(eq(LS0), any())).thenReturn(
			Optional.of(new OsmMatchResult(match0, Collections.emptyList())));
		when(simpleMatchingService.matche(eq(LS1), any())).thenReturn(
			Optional.of(new OsmMatchResult(match1, List.of(OsmWayId.of(0L)))));
		when(simpleMatchingService.matche(eq(LS2), any())).thenReturn(
			Optional.of(new OsmMatchResult(match2, List.of(OsmWayId.of(1L), OsmWayId.of(2L)))));
		when(simpleMatchingService.matche(eq(LS3), any())).thenReturn(Optional.empty());

		when(inMemoryKantenRepository.findKantenById(
			Set.of(0L))).thenReturn(
			List.of(KanteTestDataProvider.withDefaultValues().id(0L).geometry(match1).build()));
		when(inMemoryKantenRepository.findKantenById(Set.of(1L, 2L))).thenReturn(
			List.of(
				KanteTestDataProvider.withDefaultValues().id(1L).geometry(GeometryTestdataProvider.getAbschnitt(match2,
					LinearReferenzierterAbschnitt.of(0, 0.5))).build(),
				KanteTestDataProvider.withDefaultValues().id(2L).geometry(GeometryTestdataProvider.getAbschnitt(match2,
					LinearReferenzierterAbschnitt.of(0.5, 1))).build()
			));

		// act
		Set<Long> result = manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(linestrings,
			organisation).matchedKanten;

		// assert
		assertThat(result).containsExactly(0L, 1L, 2L);

		verify(simpleMatchingService).matche(eq(LS0), any());
		verify(simpleMatchingService).matche(eq(LS1), any());
		verify(simpleMatchingService).matche(eq(LS2), any());
		verify(simpleMatchingService).matche(eq(LS3), any());

		verify(inMemoryKantenRepository, times(3)).findKantenById(any());
		verify(inMemoryKantenRepository).findKantenById(eq(Collections.emptySet()));
		verify(inMemoryKantenRepository).findKantenById(eq(Set.of(0L)));
		verify(inMemoryKantenRepository).findKantenById(eq(Set.of(1L, 2L)));
	}

	@Test
	void testeFindKantenFromLineStrings_eineGematchteKanteLiegtAusserhalbDerOrganisation() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 50, 50))
			.build();
		LineString LS2 = GeometryTestdataProvider.createLineString(new Coordinate(20, 20),
			new Coordinate(30, 30));
		LineString LS3_teilweiseAusserhalb = GeometryTestdataProvider.createLineString(new Coordinate(30, 30),
			new Coordinate(60, 60));
		Set<LineString> linestrings = Set.of(LS2, LS3_teilweiseAusserhalb);

		LineString match2 = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(LS2,
			0.2, 0.2);
		LineString match3 = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(LS3_teilweiseAusserhalb,
			0.2, 0.2);

		when(simpleMatchingService.matche(eq(LS2), any())).thenReturn(
			Optional.of(new OsmMatchResult(match2, List.of(OsmWayId.of(1L), OsmWayId.of(2L)))));
		when(simpleMatchingService.matche(eq(LS3_teilweiseAusserhalb), any())).thenReturn(
			Optional.of(new OsmMatchResult(match3, List.of(OsmWayId.of(3L), OsmWayId.of(4L)))));

		when(inMemoryKantenRepository.findKantenById(Set.of(1L, 2L))).thenReturn(
			List.of(
				KanteTestDataProvider.withDefaultValues().id(1L).geometry(GeometryTestdataProvider.getAbschnitt(match2,
					LinearReferenzierterAbschnitt.of(0, 0.5))).build(),
				KanteTestDataProvider.withDefaultValues().id(2L).geometry(GeometryTestdataProvider.getAbschnitt(match2,
					LinearReferenzierterAbschnitt.of(0.5, 1))).build()
			));

		List<Kante> listMitNullMatch = Stream.of(KanteTestDataProvider.withDefaultValues().id(3L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(30, 30), new Coordinate(52, 52)))
			.build(), null).collect(Collectors.toList());  // kein Match innerhalb von Organisation
		when(inMemoryKantenRepository.findKantenById(
			Set.of(3L, 4L))).thenReturn(
			listMitNullMatch);

		// act
		Set<Long> result = manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(linestrings,
			organisation).matchedKanten;

		// assert
		verify(inMemoryKantenRepository).findKantenById(eq(Set.of(3L, 4L)));

		assertThat(result).containsExactly(1L, 2L, 3L);

		verify(simpleMatchingService).matche(eq(LS2), any());
		verify(simpleMatchingService).matche(eq(LS3_teilweiseAusserhalb), any());

		verify(inMemoryKantenRepository, times(2)).findKantenById(any());
		verify(inMemoryKantenRepository).findKantenById(eq(Set.of(1L, 2L)));
	}

}
