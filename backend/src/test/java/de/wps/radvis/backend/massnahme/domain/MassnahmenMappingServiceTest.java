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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opengis.feature.simple.SimpleFeature;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmenImportProtokoll;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmenPaketId;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class MassnahmenMappingServiceTest {
	@Mock
	private VerwaltungseinheitService verwaltungseinheitService;

	private MassnahmenMappingService massnahmenMappingService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		massnahmenMappingService = new MassnahmenMappingService(verwaltungseinheitService);
	}

	// TODO: Nochmal reflektieren, ob wir das Verhalten tatsächlich wollen
	@Disabled
	@Test
	void startUndZielKategorieBeidesGesetzt_WerteSindIdentisch_erstelltEineMassnahme() {
		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 1.), Seitenbezug.BEIDSEITIG)), Set.of(), Set.of());
		MassnahmenImportProtokoll protokoll = new MassnahmenImportProtokoll();

		SimpleFeature simpleFeatureType = SimpleFeatureTestDataProvider.withMultiLineStringAndAttributes(
			Map.of("Start_M", "3 Furten erneuern",
				"Start_A", "7 Furten erneuern",
				"Ziel_LI", "8 Furten erneuern",
				"Ziel_AB", "9 Furten erneuern",
				"Prio_S", "Sofortmaßnahme",
				"Baulast", "Bund/Land",
				"MASSN_P", "1234",
				"Kategorie", "3.0",
				"Prio_Z", "Sofortmaßnahme",
				"IstZustand", ""
			),
			new Coordinate(5, 5), new Coordinate(10, 10));

		when(verwaltungseinheitService.getOrganisationenByOrganisationsArtFuerGeometrie(any(), any())).thenReturn(
			List.of(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build()));

		// act
		List<Massnahme> result = massnahmenMappingService.createMassnahmen(simpleFeatureType, netzbezug,
			protokoll, BenutzerTestDataProvider.defaultBenutzer().build());

		// assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMassnahmenkategorien()).hasSize(1);
		assertThat(result.get(0).getMassnahmenkategorien()).containsExactly(Massnahmenkategorie.FURTEN_ERNEUERN);
		assertThat(result.get(0).getOriginalRadNETZGeometrie().getCoordinates()).containsExactly(new Coordinate(5, 5),
			new Coordinate(10, 10));
		assertThat(result.get(0).getNetzbezug().getImmutableKantenAbschnittBezug()).containsExactlyElementsOf(
			netzbezug.getImmutableKantenAbschnittBezug());
		assertThat(result.get(0).getNetzbezug().getImmutableKnotenBezug()).containsExactlyElementsOf(
			netzbezug.getImmutableKnotenBezug());
		assertThat(result.get(0).getNetzklassen()).containsExactly(Netzklasse.RADNETZ_ALLTAG);
		assertThat(result.get(0).getPrioritaet()).contains(Prioritaet.of(1));
		assertThat(result.get(0).getMassnahmenPaketId()).isEqualTo(MassnahmenPaketId.of("1234"));
		assertThat(result.get(0).getMassnahmeKonzeptID()).contains(MassnahmeKonzeptID.of("1234"));
		assertThat(result.get(0).getBezeichnung().getValue()).isEqualTo(Massnahmenkategorie.FURTEN_ERNEUERN.toString());
	}

	@Test
	void nurStartKategorieGesetzt_erstelltEineMassnahme() {
		final var kante = KanteTestDataProvider.withDefaultValues().id(1L).build();
		kante.getKantenAttributGruppe().getKantenAttribute().setStrassenName(StrassenName.of("Radvis-Allee"));

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0.2, 1.),
				Seitenbezug.BEIDSEITIG)), Set.of(), Set.of());
		MassnahmenImportProtokoll protokoll = new MassnahmenImportProtokoll();

		SimpleFeature simpleFeatureType = SimpleFeatureTestDataProvider.withMultiLineStringAndAttributes(
			Map.of("Start_M", "3 Furten erneuern",
				"Start_A", "7 Furten erneuern",
				"Prio_S", "Sofortmaßnahme",
				"Baulast", "Bund/Land",
				"MASSN_P", "1234",
				"Kategorie", "3.0",
				"Prio_Z", "Sofortmaßnahme",
				"IstZustand", ""
			),
			new Coordinate(5, 5), new Coordinate(10, 10));

		when(verwaltungseinheitService.getOrganisationenByOrganisationsArtFuerGeometrie(any(), any())).thenReturn(
			List.of(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build()));

		// act
		List<Massnahme> result = massnahmenMappingService.createMassnahmen(simpleFeatureType, netzbezug,
			protokoll, BenutzerTestDataProvider.defaultBenutzer().build());

		// assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMassnahmenkategorien()).hasSize(1);
		assertThat(result.get(0).getMassnahmenkategorien()).containsExactly(Massnahmenkategorie.FURTEN_ERNEUERN);
		assertThat(result.get(0).getOriginalRadNETZGeometrie().getCoordinates()).containsExactly(new Coordinate(5, 5),
			new Coordinate(10, 10));
		assertThat(result.get(0).getNetzbezug().getImmutableKantenAbschnittBezug()).containsExactlyElementsOf(
			netzbezug.getImmutableKantenAbschnittBezug());
		assertThat(result.get(0).getNetzbezug().getImmutableKnotenBezug()).containsExactlyElementsOf(
			netzbezug.getImmutableKnotenBezug());
		assertThat(result.get(0).getNetzklassen()).containsExactly(Netzklasse.RADNETZ_ALLTAG);
		assertThat(result.get(0).getPrioritaet()).contains(Prioritaet.of(1));
		assertThat(result.get(0).getMassnahmenPaketId()).isEqualTo(MassnahmenPaketId.of("1234"));
		assertThat(result.get(0).getMassnahmeKonzeptID()).contains(MassnahmeKonzeptID.of("1234"));
		assertThat(result.get(0).getBezeichnung().getValue()).isEqualTo(
			Massnahmenkategorie.FURTEN_ERNEUERN + " (" + kante.getKantenAttributGruppe().getKantenAttribute()
				.getStrassenName().get() + ")");
	}

	@Test
	void nurZielKategorieGesetzt_erstelltEineMassnahme() {
		final var kante1 = KanteTestDataProvider.withDefaultValues().id(1L).build();
		kante1.getKantenAttributGruppe().getKantenAttribute().setStrassenName(StrassenName.of("Radvis-Allee"));
		final var kante2 = KanteTestDataProvider.withDefaultValues().id(2L).build();
		kante2.getKantenAttributGruppe().getKantenAttribute().setStrassenName(StrassenName.of("Lilienthalstraße"));

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(
				new AbschnittsweiserKantenSeitenBezug(kante1, LinearReferenzierterAbschnitt.of(0.2, 1.),
					Seitenbezug.BEIDSEITIG),
				new AbschnittsweiserKantenSeitenBezug(kante2, LinearReferenzierterAbschnitt.of(0.2, 1.),
					Seitenbezug.BEIDSEITIG)
			), Set.of(), Set.of());
		MassnahmenImportProtokoll protokoll = new MassnahmenImportProtokoll();

		SimpleFeature simpleFeatureType = SimpleFeatureTestDataProvider.withMultiLineStringAndAttributes(
			Map.of("Ziel_LI", "8 Furten erneuern",
				"Ziel_AB", "9 Furten erneuern",
				"Prio_S", "Sofortmaßnahme",
				"Baulast", "Bund/Land",
				"MASSN_P", "1234",
				"Kategorie", "3.0",
				"Prio_Z", "Sofortmaßnahme",
				"IstZustand", ""
			),
			new Coordinate(5, 5), new Coordinate(10, 10));

		when(verwaltungseinheitService.getOrganisationenByOrganisationsArtFuerGeometrie(any(), any())).thenReturn(
			List.of(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build()));

		// act
		List<Massnahme> result = massnahmenMappingService.createMassnahmen(simpleFeatureType, netzbezug,
			protokoll, BenutzerTestDataProvider.defaultBenutzer().build());

		// assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMassnahmenkategorien()).hasSize(1);
		assertThat(result.get(0).getMassnahmenkategorien()).containsExactly(Massnahmenkategorie.FURTEN_ERNEUERN);
		assertThat(result.get(0).getOriginalRadNETZGeometrie().getCoordinates()).containsExactly(new Coordinate(5, 5),
			new Coordinate(10, 10));
		assertThat(result.get(0).getNetzbezug().getImmutableKantenAbschnittBezug()).containsExactlyElementsOf(
			netzbezug.getImmutableKantenAbschnittBezug());
		assertThat(result.get(0).getNetzbezug().getImmutableKnotenBezug()).containsExactlyElementsOf(
			netzbezug.getImmutableKnotenBezug());
		assertThat(result.get(0).getNetzklassen()).containsExactly(Netzklasse.RADNETZ_ALLTAG);
		assertThat(result.get(0).getPrioritaet()).contains(Prioritaet.of(5));
		assertThat(result.get(0).getMassnahmenPaketId()).isEqualTo(MassnahmenPaketId.of("1234"));
		assertThat(result.get(0).getMassnahmeKonzeptID()).contains(MassnahmeKonzeptID.of("1234"));
		assertThat(result.get(0).getBezeichnung().getValue()).isEqualTo(
			Massnahmenkategorie.FURTEN_ERNEUERN + " (" + kante2.getKantenAttributGruppe().getKantenAttribute()
				.getStrassenName().get() + ", " + kante1.getKantenAttributGruppe().getKantenAttribute()
				.getStrassenName().get() + ")");
	}

	@Test
	void startUndZielKategorieBeidesGesetzt_WerteSindUnterschiedlich_erstelltZweiMassnahmen() {
		final var kante1 = KanteTestDataProvider.withDefaultValues().id(1L).build();
		kante1.getKantenAttributGruppe().getKantenAttribute().setStrassenName(StrassenName.of("Radvis-Allee"));
		final var kante2 = KanteTestDataProvider.withDefaultValues().id(2L).build();
		kante2.getKantenAttributGruppe().getKantenAttribute().setStrassenName(StrassenName.of("Radvis-Allee"));

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(
				new AbschnittsweiserKantenSeitenBezug(kante1, LinearReferenzierterAbschnitt.of(0.2, 1.),
					Seitenbezug.BEIDSEITIG),
				new AbschnittsweiserKantenSeitenBezug(kante2, LinearReferenzierterAbschnitt.of(0.2, 1.),
					Seitenbezug.BEIDSEITIG)
			), Set.of(), Set.of());
		MassnahmenImportProtokoll protokoll = new MassnahmenImportProtokoll();

		SimpleFeature simpleFeatureType = SimpleFeatureTestDataProvider.withMultiLineStringAndAttributes(
			Map.of("Start_M", "3 Furten erneuern",
				"Start_A", "7 Furten erneuern",
				"Ziel_LI", "Bordabsenkungen herstellen (außerorts)",
				"Ziel_AB", "Bordabsenkungen herstellen (außerorts)",
				"Prio_S", "Sofortmaßnahme",
				"Baulast", "Bund/Land",
				"MASSN_P", "1234",
				"Kategorie", "3.0",
				"Prio_Z", "Sofortmaßnahme",
				"IstZustand", ""
			),
			new Coordinate(5, 5), new Coordinate(10, 10));

		when(verwaltungseinheitService.getOrganisationenByOrganisationsArtFuerGeometrie(any(), any())).thenReturn(
			List.of(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build()));

		// act
		List<Massnahme> result = massnahmenMappingService.createMassnahmen(simpleFeatureType, netzbezug,
			protokoll, BenutzerTestDataProvider.defaultBenutzer().build());

		// assert
		assertThat(result).hasSize(2);
		result.forEach(r -> {
			assertThat(r.getMassnahmenkategorien()).hasSize(1);
			assertThat(r.getOriginalRadNETZGeometrie().getCoordinates()).containsExactly(new Coordinate(5, 5),
				new Coordinate(10, 10));
			assertThat(r.getNetzbezug().getImmutableKantenAbschnittBezug()).containsExactlyElementsOf(
				netzbezug.getImmutableKantenAbschnittBezug());
			assertThat(r.getNetzbezug().getImmutableKnotenBezug()).containsExactlyElementsOf(
				netzbezug.getImmutableKnotenBezug());
			assertThat(r.getNetzklassen()).containsExactly(Netzklasse.RADNETZ_ALLTAG);
			assertThat(r.getMassnahmenPaketId()).isEqualTo(MassnahmenPaketId.of("1234"));
			assertThat(r.getMassnahmeKonzeptID()).contains(MassnahmeKonzeptID.of("1234"));
		});
		assertThat(result.get(0).getMassnahmenkategorien()).containsExactly(Massnahmenkategorie.FURTEN_ERNEUERN);
		assertThat(result.get(0).getPrioritaet()).contains(Prioritaet.of(1));

		assertThat(result.get(1).getMassnahmenkategorien()).containsExactly(
			Massnahmenkategorie.BORDABSENKUNGEN_HERSTELLEN_AUSSERORTS);
		assertThat(result.get(1).getPrioritaet()).contains(Prioritaet.of(5));
		assertThat(result.get(0).getBezeichnung().getValue()).isEqualTo(
			Massnahmenkategorie.FURTEN_ERNEUERN + " (" + kante1.getKantenAttributGruppe().getKantenAttribute()
				.getStrassenName().get() + ")");
	}
}
