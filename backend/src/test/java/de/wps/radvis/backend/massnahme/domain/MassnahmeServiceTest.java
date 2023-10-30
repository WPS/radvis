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
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.Mock;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.massnahme.domain.bezug.NetzBezugTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeUmsetzungsstandViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.UmsetzungsstandRepository;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
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
	private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;

	@BeforeEach
	void setUp() {
		openMocks(this);
		massnahmeService = new MassnahmeService(massnahmeRepository, massnahmeViewRepository,
			massnahmeUmsetzungsstandViewRepository, umsetzungsstandRepository, kantenRepository,
			massnahmeNetzbezugAenderungProtokollierungsService);
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

}