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
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.SackgassenService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class SackgassenServiceTest {

	@Mock
	private NetzService netzService;
	@Mock
	private KantenRepository kantenRepository;

	private SackgassenService sackgassenService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		this.sackgassenService = new SackgassenService(netzService, kantenRepository);
	}

	@Test
	public void teste_bestimmeSackgassenVonRadnetzKanten_endenEinerStreckeSindSackgassen() {
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 50), QuellSystem.DLM)
			.id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 10), QuellSystem.DLM)
			.id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.RadVis)
			.id(3L).build();

		Kante kante1 = KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten2, QuellSystem.DLM).id(1l).build();
		Kante kante2 = KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten3, QuellSystem.RadVis).id(2l).build();

		when(kantenRepository.getKantenForNetzklassenEagerFetchKnoten(Netzklasse.RADNETZ_NETZKLASSEN)).thenReturn(
			List.of(kante1, kante2));

		Set<Knoten> sackgassen = this.sackgassenService.bestimmeSackgassenknotenVonKantenFuerNetzklasse(
			Netzklasse.RADNETZ_NETZKLASSEN);

		assertThat(sackgassen).containsExactly(knoten1, knoten3);
	}

	@Test
	public void teste_bestimmeSackgassenVonRadnetzKanten_kreisHatKeineSackgassen() {
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 50), QuellSystem.DLM)
			.id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 10), QuellSystem.DLM)
			.id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.RadVis)
			.id(3L).build();

		Kante kante1 = KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten2, QuellSystem.DLM).id(1l).build();
		Kante kante2 = KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten3, QuellSystem.RadVis).id(2l).build();
		Kante kante3 = KanteTestDataProvider.fromKnotenUndQuelle(knoten3, knoten1, QuellSystem.RadVis).id(3l).build();

		when(kantenRepository.getKantenForNetzklassenEagerFetchKnoten(Netzklasse.RADNETZ_NETZKLASSEN)).thenReturn(
			List.of(kante1, kante2, kante3));

		Set<Knoten> sackgassen = this.sackgassenService.bestimmeSackgassenknotenVonKantenFuerNetzklasse(
			Netzklasse.RADNETZ_NETZKLASSEN);

		assertThat(sackgassen).isEmpty();
	}

	@Test
	public void teste_bestimmeSackgassenVonRadnetzKanten_verzweigtesBeispiel() {
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 50), QuellSystem.DLM)
			.id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 10), QuellSystem.DLM)
			.id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.RadVis)
			.id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 100), QuellSystem.RadVis)
			.id(4L).build();
		Knoten knoten5 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 30), QuellSystem.RadVis)
			.id(5L).build();
		Knoten knoten6 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 90), QuellSystem.DLM)
			.id(6L).build();

		Kante kante1 = KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten2, QuellSystem.DLM).id(1l).build();
		Kante kante2 = KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten3, QuellSystem.RadVis).id(2l).build();
		Kante kante3 = KanteTestDataProvider.fromKnotenUndQuelle(knoten3, knoten1, QuellSystem.RadVis).id(3l).build();
		Kante kante4 = KanteTestDataProvider.fromKnotenUndQuelle(knoten3, knoten4, QuellSystem.RadVis).id(4l).build();
		Kante kante5 = KanteTestDataProvider.fromKnotenUndQuelle(knoten4, knoten2, QuellSystem.RadVis).id(5l).build();
		Kante kante6 = KanteTestDataProvider.fromKnotenUndQuelle(knoten4, knoten1, QuellSystem.DLM).id(6l).build();
		Kante kante7 = KanteTestDataProvider.fromKnotenUndQuelle(knoten5, knoten6, QuellSystem.RadVis).id(7l).build();

		when(kantenRepository.getKantenForNetzklassenEagerFetchKnoten(Netzklasse.RADNETZ_NETZKLASSEN)).thenReturn(
			List.of(kante1, kante2, kante3, kante4, kante5, kante6, kante7));

		Set<Knoten> sackgassen = this.sackgassenService.bestimmeSackgassenknotenVonKantenFuerNetzklasse(
			Netzklasse.RADNETZ_NETZKLASSEN);

		assertThat(sackgassen).containsExactlyInAnyOrder(knoten5, knoten6);
	}

	@Test
	public void teste_bestimmeSackgassenVonRadnetzKanten_geradeLinieMitLuecke() {
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 50), QuellSystem.DLM)
			.id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 50), QuellSystem.DLM)
			.id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 50), QuellSystem.DLM)
			.id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(40, 50), QuellSystem.DLM)
			.id(4L).build();
		Knoten knoten5 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 50), QuellSystem.DLM)
			.id(5L).build();
		Knoten knoten6 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(60, 50), QuellSystem.DLM)
			.id(6L).build();

		Kante kante1 = KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten2, QuellSystem.DLM).id(1l).build();
		Kante kante2 = KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten3, QuellSystem.RadVis).id(2l).build();
		Kante kante4 = KanteTestDataProvider.fromKnotenUndQuelle(knoten4, knoten5, QuellSystem.RadVis).id(3l).build();
		Kante kante5 = KanteTestDataProvider.fromKnotenUndQuelle(knoten5, knoten6, QuellSystem.RadVis).id(4l).build();

		when(kantenRepository.getKantenForNetzklassenEagerFetchKnoten(Netzklasse.RADNETZ_NETZKLASSEN)).thenReturn(
			List.of(kante1, kante2, kante4, kante5));

		Set<Knoten> sackgassen = this.sackgassenService.bestimmeSackgassenknotenVonKantenFuerNetzklasse(
			Netzklasse.RADNETZ_NETZKLASSEN);

		assertThat(sackgassen).containsExactlyInAnyOrder(knoten1, knoten3, knoten4, knoten6);
	}

	@Test
	public void teste_bestimmeSackgassenknotenVonKanteIdsInOrganisation_EineKanteZweiKnotenImBereich_ZweiSackgassenKnoten() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().bereich(
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 50, 50))
			.build();

		// Alle Knoten liegen innerhalb des Organisationsbereiches
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 20), QuellSystem.DLM)
			.id(10L).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 20), QuellSystem.DLM)
			.id(20L).build();

		List<Kante> kantenInOrganisationsBereich = List.of(
			createRadnetzKanteFromKnotenMitId(knoten1, knoten2, 1L));

		when(netzService.getKantenInOrganisationsbereichEagerFetchKnoten(any())).thenReturn(
			kantenInOrganisationsBereich.stream());

		Set<Long> kanteIdsNeueNetzklasse = Set.of(1L);

		// act
		Set<Knoten> sackgassenKnoten = sackgassenService.bestimmeSackgassenknotenVonKanteIdsInOrganisation(
			kanteIdsNeueNetzklasse, organisation);

		// assert
		// Da es nur eine Kante gibt, sind beide ihrer Knoten Sackgassen und liegen innerhalb des Orga-Bereiches
		assertThat(sackgassenKnoten).hasSize(2);
		assertThat(sackgassenKnoten).extracting(Knoten::getId).containsExactlyInAnyOrder(10L, 20L);
	}

	@Test
	public void teste_bestimmeSackgassenknotenVonKanteIdsInOrganisation_ausserHalbSackgassenknotenWerdenRausgefiltert() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().bereich(
			GeometryTestdataProvider.createQuadratischerBereich(5, 5, 35, 35))
			.build();

		// Alle Knoten liegen ausserhalb des Organisationsbereiches
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 20), QuellSystem.DLM)
			.id(10L).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 20), QuellSystem.DLM)
			.id(20L).build();

		List<Kante> kantenInOrganisationsBereich = List.of(
			createRadnetzKanteFromKnotenMitId(knoten1, knoten2, 1L));

		when(netzService.getKantenInOrganisationsbereichEagerFetchKnoten(any())).thenReturn(
			kantenInOrganisationsBereich.stream());

		Set<Long> kanteIdsNeueNetzklasse = Set.of(1L);

		// act
		Set<Knoten> sackgassenKnoten = sackgassenService.bestimmeSackgassenknotenVonKanteIdsInOrganisation(
			kanteIdsNeueNetzklasse, organisation);

		// assert
		assertThat(sackgassenKnoten).hasSize(0);
	}

	@Test
	public void teste_bestimmeSackgassenknotenVonKanteIdsInOrganisation_knotenInnerhalbUndAusserhalbMitLuecke_zweiSackgassenKnotenErkannt() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().bereich(
			GeometryTestdataProvider.createQuadratischerBereich(5, 5, 35, 35))
			.build();

		// Anfangs- und Endknoten liegen ausserhalb des Organisationsbereiches
		Knoten knoten1Ausserhalb = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 20),
			QuellSystem.DLM)
			.id(10L).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 20), QuellSystem.DLM)
			.id(20L).build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 20), QuellSystem.DLM)
			.id(30L).build();
		Knoten knoten4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(24, 20), QuellSystem.DLM)
			.id(40L).build();
		Knoten knoten5 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 20), QuellSystem.DLM)
			.id(50L).build();
		Knoten knoten6Ausserhalb = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(40, 20),
			QuellSystem.DLM)
			.id(60L).build();

		List<Kante> kantenInOrganisationsBereich = List.of(
			createRadnetzKanteFromKnotenMitId(knoten1Ausserhalb, knoten2, 1L),
			createRadnetzKanteFromKnotenMitId(knoten2, knoten3, 2L),
			createRadnetzKanteFromKnotenMitId(knoten3, knoten4, 3L),
			createRadnetzKanteFromKnotenMitId(knoten4, knoten5, 4L),
			createRadnetzKanteFromKnotenMitId(knoten5, knoten6Ausserhalb, 5L));

		when(netzService.getKantenInOrganisationsbereichEagerFetchKnoten(any())).thenReturn(
			kantenInOrganisationsBereich.stream());

		Set<Long> kanteIdsNeueNetzklasse = Set.of(1L, 2L, 4L, 5L);

		// act
		Set<Knoten> sackgassenKnoten = sackgassenService.bestimmeSackgassenknotenVonKanteIdsInOrganisation(
			kanteIdsNeueNetzklasse, organisation);

		// assert
		// Nur die zwei Sackgassenknoten im Orga-Bereich sollen ermittelt worden sein
		assertThat(sackgassenKnoten).hasSize(2);
		assertThat(sackgassenKnoten).extracting(Knoten::getId).containsExactlyInAnyOrder(30L, 40L);
	}

	private Kante createRadnetzKanteFromKnotenMitId(Knoten vonKnoten, Knoten nachKnoten, long id) {
		return KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.id(id)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
				.build())
			.build();
	}
}
