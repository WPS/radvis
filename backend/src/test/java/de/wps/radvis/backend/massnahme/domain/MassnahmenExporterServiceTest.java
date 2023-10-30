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
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeListenDbViewTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;

public class MassnahmenExporterServiceTest {
	private MassnahmenExporterService exporterService;
	@Mock
	private MassnahmeViewRepository massnahmeRepository;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		exporterService = new MassnahmenExporterService(massnahmeRepository);
	}

	@Test
	public void export() {
		// arange
		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(kante)
			.id(1L)
			.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT))
			.letzteAenderung(LocalDateTime.of(2020, 10, 1, 10, 12)).planungErforderlich(true)
			.unterhaltsZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Meine Organisation")
					.organisationsArt(OrganisationsArt.GEMEINDE)
					.build())
			.build();
		List<Long> ids = List.of(1l);
		when(massnahmeRepository.findAllByIdIn(ids))
			.thenReturn(List.of(MassnahmeListenDbViewTestDataProvider.withMassnahme(massnahme).build()));

		// act
		List<ExportData> result = exporterService.export(ids);

		// assert
		assertThat(result.get(0).getGeometry().getNumGeometries()).isEqualTo(1);
		assertThat(result.get(0).getGeometry().getGeometryN(0).getGeometryType()).isEqualTo("LineString");
		assertThat(((LineString) result.get(0).getGeometry().getGeometryN(0)).getCoordinates())
			.isEqualTo(massnahme.getNetzbezug().getImmutableKantenAbschnittBezug().iterator().next().getKante()
				.getGeometry().getCoordinates());
		// wir verzichten darauf, das Mapping vollständig zu testen, außer in speziellen Fällen (Array etc.)
		// es müssen genauso viele Spalten sein, wie die Tabelle im Frontend hat
		assertThat(result.get(0).getProperties()).hasSize(17);
		assertThat(result.get(0).getProperties().get("Netzklassen")).satisfiesAnyOf(
			nk -> assertThat(nk).contains(Netzklasse.KOMMUNALNETZ_ALLTAG + ";" + Netzklasse.KREISNETZ_FREIZEIT),
			nk -> assertThat(nk).contains(Netzklasse.KREISNETZ_FREIZEIT + ";" + Netzklasse.KOMMUNALNETZ_ALLTAG));
		assertThat(result.get(0).getProperties().get("Letzte Änderung")).isEqualTo("01.10.2020 10:12");
		assertThat(result.get(0).getProperties().get("Planung erforderlich")).isEqualTo("Ja");
		assertThat(result.get(0).getProperties().get("Unterhaltszuständige/r"))
			.isEqualTo("Meine Organisation (Gemeinde)");
		assertThat(result.get(0).getProperties().get("RADVIS_ID")).isEqualTo("1");
	}
}
