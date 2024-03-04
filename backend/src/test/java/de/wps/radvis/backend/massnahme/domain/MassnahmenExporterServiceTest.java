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
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeListenDbViewTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
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
		// Arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(kante)
			.id(1L)
			.letzteAenderung(LocalDateTime.of(2020, 10, 1, 10, 12)).planungErforderlich(true)
			.sollStandard(SollStandard.KEIN_STANDARD_ERFUELLT)
			.baulastZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Baulast-Zuständiger")
					.organisationsArt(OrganisationsArt.KREIS)
					.build())
			.unterhaltsZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Unterhalts-Zuständiger")
					.organisationsArt(OrganisationsArt.GEMEINDE)
					.build())
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstand(new Umsetzungsstand())
			.build();
		List<Long> ids = List.of(1L);
		when(massnahmeRepository.findAllByIdIn(ids))
			.thenReturn(List.of(MassnahmeListenDbViewTestDataProvider.withMassnahme(massnahme).build()));

		// Act
		List<ExportData> result = exporterService.export(ids);

		// Assert
		Geometry exportDataGeometry = result.get(0).getGeometry();
		Map<String, String> exportDataProperties = result.get(0).getProperties();

		assertThat(exportDataGeometry.getNumGeometries())
			.isEqualTo(1);
		assertThat(exportDataGeometry.getGeometryN(0).getGeometryType())
			.isEqualTo("LineString");
		assertThat(exportDataGeometry.getGeometryN(0).getCoordinates())
			.isEqualTo(massnahme.getNetzbezug().getImmutableKantenAbschnittBezug().iterator().next().getKante()
				.getGeometry().getCoordinates());

		assertThat(exportDataProperties).hasSize(23);
		assertThat(exportDataProperties.get("RADVIS_ID"))
			.isEqualTo("1");
		assertThat(exportDataProperties.get("Bezeichnung"))
			.isEqualTo("Bezeichnung");
		assertThat(exportDataProperties.get("Kategorien")).satisfiesAnyOf(
			k -> assertThat(k).contains(
				Massnahmenkategorie.FURTEN_ERNEUERN.name() + ";" + Massnahmenkategorie.ANPASSUNG_AN_BAUWERK.name()),
			k -> assertThat(k).contains(
				Massnahmenkategorie.ANPASSUNG_AN_BAUWERK.name() + ";" + Massnahmenkategorie.FURTEN_ERNEUERN.name()));
		assertThat(exportDataProperties.get("Durchführungszeitraum"))
			.isEqualTo("2022");
		assertThat(exportDataProperties.get("Umsetzungsstatus"))
			.isEqualTo("Idee");
		assertThat(exportDataProperties.get("Umsetzungsstand-Status"))
			.isEqualTo("NEU_ANGELEGT");
		assertThat(exportDataProperties.get("Veröffentlicht"))
			.isEqualTo("Ja");
		assertThat(exportDataProperties.get("Planung erforderlich"))
			.isEqualTo("Ja");
		assertThat(exportDataProperties.get("Priorität"))
			.isEqualTo("1");
		assertThat(exportDataProperties.get("Netzklassen")).satisfiesAnyOf(
			nk -> assertThat(nk).contains(
				Netzklasse.KOMMUNALNETZ_ALLTAG.name() + ";" + Netzklasse.KREISNETZ_FREIZEIT.name()),
			nk -> assertThat(nk).contains(
				Netzklasse.KREISNETZ_FREIZEIT.name() + ";" + Netzklasse.KOMMUNALNETZ_ALLTAG.name()));
		assertThat(exportDataProperties.get("Baulastträger"))
			.isEqualTo("Beispiel-Baulast-Zuständiger (Landkreis)");
		assertThat(exportDataProperties.get("Zuständige/r"))
			.isEqualTo("Mega coole zuständige Organisation (Regierungsbezirk)");
		assertThat(exportDataProperties.get("Unterhaltszuständige/r"))
			.isEqualTo("Beispiel-Unterhalts-Zuständiger (Gemeinde)");
		assertThat(exportDataProperties.get("Letzte Änderung"))
			.isEqualTo("01.10.2020 10:12");
		assertThat(exportDataProperties.get("BenutzerIn der letzten Änderung"))
			.isEqualTo("adminVorname adminNachname");
		assertThat(exportDataProperties.get("Soll-Standard"))
			.isEqualTo("Kein Standard erfüllt");
		assertThat(exportDataProperties.get("Wer soll tätig werden?"))
			.isEqualTo("Baulastträger");
		assertThat(exportDataProperties.get("Massnahme-ID"))
			.isEqualTo("ABC123");
		assertThat(exportDataProperties.get("Realisierungshilfe"))
			.isEqualTo("2.2-1 Sichtfelder an Knotenpunkten und Querungsstellen");
		assertThat(exportDataProperties.get("Kostenannahme"))
			.isEqualTo("1234");
		assertThat(exportDataProperties.get("MaViS-ID"))
			.isEqualTo("maViSID");
		assertThat(exportDataProperties.get("Verba-ID"))
			.isEqualTo("verbaID");
		assertThat(exportDataProperties.get("LGVFG-ID"))
			.isEqualTo("lgvfgid");
	}
}
