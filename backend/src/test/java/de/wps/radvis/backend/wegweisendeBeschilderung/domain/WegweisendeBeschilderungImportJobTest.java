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

package de.wps.radvis.backend.wegweisendeBeschilderung.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.util.Lazy;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.SimpleFeatureTypeFactory;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.entity.WegweisendeBeschilderung;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.entity.WegweisendeBeschilderungImportJobStatistik;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.repository.WegweisendeBeschilderungRepository;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Defizit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Gemeinde;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Kreis;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Land;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.PfostenNr;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.PfostenTyp;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Pfostendefizit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Pfostenzustand;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.WegweiserTyp;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Zustandsbewertung;

class WegweisendeBeschilderungImportJobTest {
	private final SimpleFeatureType simpleFeatureType = SimpleFeatureTypeFactory.createSimpleFeatureType(
		Set.of("PfostenNr", "WWTyp_Tx", "PfTyp_Tx", "GesZus", "GesMangel", "PfZus", "PfMangel", "GE_Gem", "GE_Kreis",
			"GE_Land"
		),
		Point.class,
		SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_GEOMETRY);

	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Mock
	GeoJsonImportRepository geoJsonImportRepository;

	@Mock
	WegweisendeBeschilderungRepository wegweisendeBeschilderungRepository;

	@Mock
	GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Captor
	private ArgumentCaptor<List<WegweisendeBeschilderung>> wegweisendeBeschilderungSaveAllCaptor;

	@Captor
	private ArgumentCaptor<List<WegweisendeBeschilderung>> wegweisendeBeschilderungDeleteAllCaptor;

	WegweisendeBeschilderungImportJob job;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		when(gebietskoerperschaftRepository.findByNameAndOrganisationsArt("Baden-Württemberg",
			OrganisationsArt.BUNDESLAND)).thenReturn(Optional.of(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Baden-Württemberg")
					.organisationsArt(OrganisationsArt.BUNDESLAND).build()));

		job = new WegweisendeBeschilderungImportJob(
			"https://tolle-url.de", geoJsonImportRepository,
			wegweisendeBeschilderungRepository,
			Lazy.of(() -> gebietskoerperschaftRepository.findByNameAndOrganisationsArt("Baden-Württemberg",
				OrganisationsArt.BUNDESLAND).orElseThrow()),
			jobExecutionDescriptionRepository);
	}

	@Test
	void doRun_erstelltBeschilderungenUndSpeichert() throws ReadGeoJSONException {
		// arrange
		SimpleFeatureBuilder f = new SimpleFeatureBuilder(simpleFeatureType);
		f.add(GeometryTestdataProvider.createPoint(new Coordinate(11, 11)));
		f.set("PfostenNr", "PfostenNr");
		f.set("WWTyp_Tx", "WegweiserTyp");
		f.set("PfTyp_Tx", "PfostenTyp");
		f.set("GesZus", "Zustandsbewertung");
		f.set("GesMangel", "Defizit");
		f.set("PfZus", "Pfostenzustand");
		f.set("PfMangel", "Pfostendefizit");
		f.set("GE_Gem", "Gemeinde");
		f.set("GE_Kreis", "Kreis");
		f.set("GE_Land", "Land");

		when(geoJsonImportRepository.getSimpleFeatures(any())).thenReturn(List.of(f.buildFeature("id")));

		// act
		Optional<JobStatistik> jobStatistik = job.doRun();

		// assert
		WegweisendeBeschilderungImportJobStatistik statistik = (WegweisendeBeschilderungImportJobStatistik) jobStatistik
			.get();
		assertThat(statistik.anzahlFeatures).isEqualTo(1);
		assertThat(statistik.anzahlIgnoriert).isEqualTo(0);
		assertThat(statistik.anzahlNeuErstellt).isEqualTo(1);
		assertThat(statistik.anzahlAktualisiert).isEqualTo(0);
		assertThat(statistik.anzahlEntfernt).isEqualTo(0);

		verify(wegweisendeBeschilderungRepository).deleteAll(List.of());

		verify(wegweisendeBeschilderungRepository).saveAll(wegweisendeBeschilderungSaveAllCaptor.capture());
		WegweisendeBeschilderung saved = wegweisendeBeschilderungSaveAllCaptor.getValue().get(0);
		assertThat(saved.getPfostenNr().getValue()).isEqualTo("PfostenNr");
		assertThat(saved.getGeometrie().getCoordinate()).isEqualTo(new Coordinate(11, 11));
		assertThat(saved.getWegweiserTyp().getValue()).isEqualTo("WegweiserTyp");
		assertThat(saved.getPfostenTyp().getValue()).isEqualTo("PfostenTyp");
		assertThat(saved.getZustandsbewertung().getValue()).isEqualTo("Zustandsbewertung");
		assertThat(saved.getDefizit().getValue()).isEqualTo("Defizit");
		assertThat(saved.getPfostenzustand().getValue()).isEqualTo("Pfostenzustand");
		assertThat(saved.getPfostendefizit().getValue()).isEqualTo("Pfostendefizit");
		assertThat(saved.getGemeinde().getValue()).isEqualTo("Gemeinde");
		assertThat(saved.getKreis().getValue()).isEqualTo("Kreis");
		assertThat(saved.getLand().getValue()).isEqualTo("Land");
	}

	@Test
	void doRun_beiGleichePfostenNummer_ignorierBeide() throws ReadGeoJSONException {
		// arrange
		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(simpleFeatureType);
		f1.add(GeometryTestdataProvider.createPoint(new Coordinate(558945.281669753, 5350346.047167036)));
		f1.set("PfostenNr", "Gleicher PfostenNr");
		f1.set("WWTyp_Tx", "WegweiserTyp");
		f1.set("PfTyp_Tx", "PfostenTyp");
		f1.set("GesZus", "Zustandsbewertung");
		f1.set("GesMangel", "Defizit");
		f1.set("PfZus", "Pfostenzustand");
		f1.set("PfMangel", "Pfostendefizit");
		f1.set("GE_Gem", "Gemeinde");
		f1.set("GE_Kreis", "Kreis");
		f1.set("GE_Land", "Land");
		SimpleFeatureBuilder f2 = new SimpleFeatureBuilder(simpleFeatureType);
		f2.add(GeometryTestdataProvider.createPoint(new Coordinate(558945.281669753, 5350346.047167036)));
		f2.set("PfostenNr", "Gleicher PfostenNr");
		f2.set("WWTyp_Tx", "WegweiserTyp");
		f2.set("PfTyp_Tx", "PfostenTyp");
		f2.set("GesZus", "Zustandsbewertung");
		f2.set("GesMangel", "Defizit");
		f2.set("PfZus", "Pfostenzustand");
		f2.set("PfMangel", "Pfostendefizit");
		f2.set("GE_Gem", "Gemeinde");
		f2.set("GE_Kreis", "Kreis");
		f2.set("GE_Land", "Land");

		when(geoJsonImportRepository.getSimpleFeatures(any())).thenReturn(
			List.of(f1.buildFeature("id"), f2.buildFeature("id")));

		// act
		Optional<JobStatistik> jobStatistik = job.doRun();

		// assert
		WegweisendeBeschilderungImportJobStatistik statistik = (WegweisendeBeschilderungImportJobStatistik) jobStatistik
			.get();
		assertThat(statistik.anzahlFeatures).isEqualTo(2);
		assertThat(statistik.anzahlIgnoriert).isEqualTo(2);
		assertThat(statistik.anzahlNeuErstellt).isEqualTo(0);
		assertThat(statistik.anzahlAktualisiert).isEqualTo(0);
		assertThat(statistik.anzahlEntfernt).isEqualTo(0);
		assertThat(statistik.doppeltePfostenNummern).hasSize(1);

		verify(wegweisendeBeschilderungRepository).saveAll(List.of());
		verify(wegweisendeBeschilderungRepository).deleteAll(List.of());
	}

	@Test
	void doRun_aktualisiertBeschilderung() throws ReadGeoJSONException {
		// arrange
		SimpleFeatureBuilder f = new SimpleFeatureBuilder(simpleFeatureType);
		f.add(GeometryTestdataProvider.createPoint(new Coordinate(558945.281669753, 5350346.047167036)));
		f.set("PfostenNr", "Bekannter PfostenNr");
		f.set("WWTyp_Tx", "Neu WegweiserTyp");
		f.set("PfTyp_Tx", "Neu PfostenTyp");
		f.set("GesZus", "Neu Zustandsbewertung");
		f.set("GesMangel", "Neu Defizit");
		f.set("PfZus", "Neu Pfostenzustand");
		f.set("PfMangel", "Neu Pfostendefizit");
		f.set("GE_Gem", "Neu Gemeinde");
		f.set("GE_Kreis", "Neu Kreis");
		f.set("GE_Land", "Neu Land");

		when(geoJsonImportRepository.getSimpleFeatures(any())).thenReturn(List.of(f.buildFeature("id")));

		WegweisendeBeschilderung bekannteBeschilderung = WegweisendeBeschilderung.builder()
			.id(1L)
			.pfostenNr(PfostenNr.of("Bekannter PfostenNr"))
			.geometrie(GeometryTestdataProvider.createPoint(new Coordinate(11, 11)))
			.wegweiserTyp(WegweiserTyp.of("Alt WegweiserTyp"))
			.pfostenTyp(PfostenTyp.of("Alt PfostenTyp"))
			.zustandsbewertung(Zustandsbewertung.of("Alt Zustandsbewertung"))
			.defizit(Defizit.of("Alt Defizit"))
			.pfostenzustand(Pfostenzustand.of("Alt Pfostenzustand"))
			.pfostendefizit(Pfostendefizit.of("Alt Pfostendefizit"))
			.gemeinde(Gemeinde.of("Alt Gemeinde"))
			.kreis(Kreis.of("Alt Kreis"))
			.land(Land.of("Alt Land"))
			.zustaendigeVerwaltungseinheit(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Baden-Württemberg")
					.organisationsArt(OrganisationsArt.BUNDESLAND).build())
			.build();

		when(wegweisendeBeschilderungRepository.findAll()).thenReturn(List.of(bekannteBeschilderung));

		// act
		Optional<JobStatistik> jobStatistik = job.doRun();

		// assert
		WegweisendeBeschilderungImportJobStatistik statistik = (WegweisendeBeschilderungImportJobStatistik) jobStatistik
			.get();
		assertThat(statistik.anzahlFeatures).isEqualTo(1);
		assertThat(statistik.anzahlIgnoriert).isEqualTo(0);
		assertThat(statistik.anzahlNeuErstellt).isEqualTo(0);
		assertThat(statistik.anzahlAktualisiert).isEqualTo(1);
		assertThat(statistik.anzahlEntfernt).isEqualTo(0);

		verify(wegweisendeBeschilderungRepository).deleteAll(List.of());
		verify(wegweisendeBeschilderungRepository).saveAll(wegweisendeBeschilderungSaveAllCaptor.capture());
		WegweisendeBeschilderung saved = wegweisendeBeschilderungSaveAllCaptor.getValue().get(0);
		assertThat(saved.getId()).isEqualTo(1L);
		assertThat(saved.getLand().getValue()).isEqualTo("Neu Land");
	}

	@Test
	void doRun_entfernteAlteBeschilderung() throws ReadGeoJSONException {
		// arrange
		// In Repo, aber nicht in Features
		WegweisendeBeschilderung toDeleteBeschilderung = WegweisendeBeschilderung.builder()
			.id(1L)
			.pfostenNr(PfostenNr.of("Soll entfernt werden PfostenNr"))
			.geometrie(GeometryTestdataProvider.createPoint(new Coordinate(11, 11)))
			.wegweiserTyp(WegweiserTyp.of("Soll entfernt werden WegweiserTyp"))
			.pfostenTyp(PfostenTyp.of("Soll entfernt werden PfostenTyp"))
			.zustandsbewertung(Zustandsbewertung.of("Soll entfernt werden Zustandsbewertung"))
			.defizit(Defizit.of("Soll entfernt werden Defizit"))
			.pfostenzustand(Pfostenzustand.of("Soll entfernt werden Pfostenzustand"))
			.pfostendefizit(Pfostendefizit.of("Soll entfernt werden Pfostendefizit"))
			.gemeinde(Gemeinde.of("Soll entfernt werden Gemeinde"))
			.kreis(Kreis.of("Soll entfernt werden Kreis"))
			.land(Land.of("Soll entfernt werden Land"))
			.zustaendigeVerwaltungseinheit(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Baden-Württemberg")
					.organisationsArt(OrganisationsArt.BUNDESLAND).build())
			.build();

		// In Repo und auch in Features
		WegweisendeBeschilderung toKeepBeschilderung = WegweisendeBeschilderung.builder()
			.id(2L)
			.pfostenNr(PfostenNr.of("Soll bleiben PfostenNr"))
			.geometrie(GeometryTestdataProvider.createPoint(new Coordinate(12, 12)))
			.wegweiserTyp(WegweiserTyp.of("Soll bleiben WegweiserTyp"))
			.pfostenTyp(PfostenTyp.of("Soll bleiben PfostenTyp"))
			.zustandsbewertung(Zustandsbewertung.of("Soll bleiben Zustandsbewertung"))
			.defizit(Defizit.of("Soll bleiben Defizit"))
			.pfostenzustand(Pfostenzustand.of("Soll bleiben Pfostenzustand"))
			.pfostendefizit(Pfostendefizit.of("Soll bleiben Pfostendefizit"))
			.gemeinde(Gemeinde.of("Soll bleiben Gemeinde"))
			.kreis(Kreis.of("Soll bleiben Kreis"))
			.land(Land.of("Soll bleiben Land"))
			.zustaendigeVerwaltungseinheit(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Baden-Württemberg")
					.organisationsArt(OrganisationsArt.BUNDESLAND).build())
			.build();

		when(wegweisendeBeschilderungRepository.findAll())
			.thenReturn(List.of(toDeleteBeschilderung, toKeepBeschilderung));

		// In GeoJson ist "Alt" nicht vorhanden. Es enthaelt nur ein anderes Feature
		SimpleFeatureBuilder f = new SimpleFeatureBuilder(simpleFeatureType);
		f.add(GeometryTestdataProvider.createPoint(new Coordinate(12, 12)));
		f.set("PfostenNr", "Soll bleiben PfostenNr");
		f.set("WWTyp_Tx", "Soll bleiben WegweiserTyp");
		f.set("PfTyp_Tx", "Soll bleiben PfostenTyp");
		f.set("GesZus", "Soll bleiben Zustandsbewertung");
		f.set("GesMangel", "Soll bleiben Defizit");
		f.set("PfZus", "Soll bleiben Pfostenzustand");
		f.set("PfMangel", "Soll bleiben Pfostendefizit");
		f.set("GE_Gem", "Soll bleiben Gemeinde");
		f.set("GE_Kreis", "Anderer Kreis, damit Feature nicht ignoriert wird");
		f.set("GE_Land", "Soll bleiben Land");
		when(geoJsonImportRepository.getSimpleFeatures(any())).thenReturn(List.of(f.buildFeature("id2")));

		// act
		Optional<JobStatistik> jobStatistik = job.doRun();

		// assert
		WegweisendeBeschilderungImportJobStatistik statistik = (WegweisendeBeschilderungImportJobStatistik) jobStatistik
			.get();
		assertThat(statistik.anzahlFeatures).isEqualTo(1);
		assertThat(statistik.anzahlIgnoriert).isEqualTo(0);
		assertThat(statistik.anzahlNeuErstellt).isEqualTo(0);
		assertThat(statistik.anzahlAktualisiert).isEqualTo(1);
		assertThat(statistik.anzahlEntfernt).isEqualTo(1);
		assertThat(statistik.doppeltePfostenNummern).hasSize(0);

		verify(wegweisendeBeschilderungRepository).deleteAll(wegweisendeBeschilderungDeleteAllCaptor.capture());
		assertThat(wegweisendeBeschilderungDeleteAllCaptor.getValue().size()).isEqualTo(1);
		assertThat(wegweisendeBeschilderungDeleteAllCaptor.getValue().get(0).getPfostenNr().getValue()).isEqualTo(
			"Soll entfernt werden PfostenNr");

		// "Soll bleiben Feature" ist unveraendert
		verify(wegweisendeBeschilderungRepository).saveAll(wegweisendeBeschilderungSaveAllCaptor.capture());
		assertThat(wegweisendeBeschilderungSaveAllCaptor.getValue())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactly(toKeepBeschilderung);
	}

	@Test
	void doRun_ignoriertFeaturesMitNullWerte() throws ReadGeoJSONException {
		// arrange
		SimpleFeatureBuilder f = new SimpleFeatureBuilder(simpleFeatureType);
		f.add(GeometryTestdataProvider.createPoint(new Coordinate(558945.281669753, 5350346.047167036)));
		f.set("PfostenNr", "PfostenNr");
		f.set("WWTyp_Tx", "WegweiserTyp");
		f.set("PfTyp_Tx", "PfostenTyp");
		f.set("GesZus", "Zustandsbewertung");
		f.set("GesMangel", null);
		f.set("PfZus", "Pfostenzustand");
		f.set("PfMangel", "Pfostendefizit");
		f.set("GE_Gem", "Gemeinde");
		f.set("GE_Kreis", "Kreis");
		f.set("GE_Land", "Land");

		when(geoJsonImportRepository.getSimpleFeatures(any())).thenReturn(List.of(f.buildFeature("id")));

		// act
		Optional<JobStatistik> jobStatistik = job.doRun();

		// assert
		WegweisendeBeschilderungImportJobStatistik statistik = (WegweisendeBeschilderungImportJobStatistik) jobStatistik
			.get();
		assertThat(statistik.anzahlFeatures).isEqualTo(1);
		assertThat(statistik.anzahlIgnoriert).isEqualTo(1);
		assertThat(statistik.anzahlNeuErstellt).isEqualTo(0);
		assertThat(statistik.anzahlAktualisiert).isEqualTo(0);
		assertThat(statistik.anzahlEntfernt).isEqualTo(0);

		verify(wegweisendeBeschilderungRepository).deleteAll(List.of());
		verify(wegweisendeBeschilderungRepository).saveAll(List.of());
	}

	@Test
	void doRun_ignoriertFeaturesMitFehlenderGeometry() throws ReadGeoJSONException {
		// arrange
		SimpleFeatureBuilder f = new SimpleFeatureBuilder(simpleFeatureType);
		// Geometry fehlt
		f.set("PfostenNr", "PfostenNr");
		f.set("WWTyp_Tx", "WegweiserTyp");
		f.set("PfTyp_Tx", "PfostenTyp");
		f.set("GesZus", "Zustandsbewertung");
		f.set("GesMangel", "Defizit");
		f.set("PfZus", "Pfostenzustand");
		f.set("PfMangel", "Pfostendefizit");
		f.set("GE_Gem", "Gemeinde");
		f.set("GE_Kreis", "Kreis");
		f.set("GE_Land", "Land");

		when(geoJsonImportRepository.getSimpleFeatures(any())).thenReturn(List.of(f.buildFeature("id")));

		// act
		Optional<JobStatistik> jobStatistik = job.doRun();

		// assert
		WegweisendeBeschilderungImportJobStatistik statistik = (WegweisendeBeschilderungImportJobStatistik) jobStatistik
			.get();
		assertThat(statistik.anzahlFeatures).isEqualTo(1);
		assertThat(statistik.anzahlIgnoriert).isEqualTo(1);
		assertThat(statistik.anzahlNeuErstellt).isEqualTo(0);
		assertThat(statistik.anzahlAktualisiert).isEqualTo(0);
		assertThat(statistik.anzahlEntfernt).isEqualTo(0);

		verify(wegweisendeBeschilderungRepository).deleteAll(List.of());
		verify(wegweisendeBeschilderungRepository).saveAll(List.of());
	}

	@Test
	void doRun_ignoriertFeaturesMitFehlendemWert() throws ReadGeoJSONException {
		// arrange
		SimpleFeatureType simpleFeatureTypeWoEinFeldFehlt = SimpleFeatureTypeFactory.createSimpleFeatureType(Set.of(
			// PfostenNr fehlt
			"WWTyp_Tx",  // --> WegweiserTyp
			"PfTyp_Tx",  // --> PfostenTyp
			"GesZus",    // --> Zustandsbewertung
			"GesMangel", // --> Defizit
			"PfZus",     // --> Pfostenzustand
			"PfMangel",  // --> Pfostendefizit
			"GE_Gem",    // --> Gemeinde
			"GE_Kreis",  // --> Kreis
			"GE_Land"    // --> Land
		),
			Geometry.class,
			SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_GEOMETRY);
		SimpleFeatureBuilder f = new SimpleFeatureBuilder(simpleFeatureTypeWoEinFeldFehlt);
		f.add(GeometryTestdataProvider.createPoint(new Coordinate(558945.281669753, 5350346.047167036)));
		// PfostenNr fehlt hier auch
		f.set("WWTyp_Tx", "WegweiserTyp");
		f.set("PfTyp_Tx", "PfostenTyp");
		f.set("GesZus", "Zustandsbewertung");
		f.set("GesMangel", "Defizit");
		f.set("PfZus", "Pfostenzustand");
		f.set("PfMangel", "Pfostendefizit");
		f.set("GE_Gem", "Gemeinde");
		f.set("GE_Kreis", "Kreis");
		f.set("GE_Land", "Land");

		when(geoJsonImportRepository.getSimpleFeatures(any())).thenReturn(List.of(f.buildFeature("id")));

		// act
		Optional<JobStatistik> jobStatistik = job.doRun();

		// assert
		WegweisendeBeschilderungImportJobStatistik statistik = (WegweisendeBeschilderungImportJobStatistik) jobStatistik
			.get();
		assertThat(statistik.anzahlFeatures).isEqualTo(1);
		assertThat(statistik.anzahlIgnoriert).isEqualTo(1);
		assertThat(statistik.anzahlNeuErstellt).isEqualTo(0);
		assertThat(statistik.anzahlAktualisiert).isEqualTo(0);
		assertThat(statistik.anzahlEntfernt).isEqualTo(0);

		verify(wegweisendeBeschilderungRepository).deleteAll(List.of());
		verify(wegweisendeBeschilderungRepository).saveAll(List.of());
	}

	@Test
	void doRun_ignoriertBeschilderungMitGleichenAttributen() throws ReadGeoJSONException {
		// arrange
		WegweisendeBeschilderung beschilderungUnveraendert = WegweisendeBeschilderungTestDataProvider
			.withDefaultValuesGeometrieAndVerwaltungseinheit(
				new Coordinate(11, 11),
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Baden-Württemberg")
					.organisationsArt(OrganisationsArt.BUNDESLAND).build()
			)
			.id(1L)
			.pfostenNr(PfostenNr.of("111"))
			.build();
		WegweisendeBeschilderung beschilderungVeraendert = WegweisendeBeschilderungTestDataProvider
			.withDefaultValuesGeometrieAndVerwaltungseinheit(
				new Coordinate(22, 22),
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Baden-Württemberg")
					.organisationsArt(OrganisationsArt.BUNDESLAND).build()
			)
			.id(2L)
			.pfostenNr(PfostenNr.of("222"))
			.pfostenTyp(PfostenTyp.of("Komischer Typ"))
			.build();

		when(wegweisendeBeschilderungRepository.findAll()).thenReturn(
			List.of(
				beschilderungUnveraendert,
				beschilderungVeraendert
			)
		);

		// Feature mit gleichen Attributen wie "beschilderungUnveraendert":
		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(simpleFeatureType);
		f1.add(beschilderungUnveraendert.getGeometrie());
		f1.set("PfostenNr", beschilderungUnveraendert.getPfostenNr().getValue());
		f1.set("WWTyp_Tx", beschilderungUnveraendert.getWegweiserTyp().getValue());
		f1.set("PfTyp_Tx", beschilderungUnveraendert.getPfostenTyp().getValue());
		f1.set("GesZus", beschilderungUnveraendert.getZustandsbewertung().getValue());
		f1.set("GesMangel", beschilderungUnveraendert.getDefizit().getValue());
		f1.set("PfZus", beschilderungUnveraendert.getPfostenzustand().getValue());
		f1.set("PfMangel", beschilderungUnveraendert.getPfostendefizit().getValue());
		f1.set("GE_Gem", beschilderungUnveraendert.getGemeinde().getValue());
		f1.set("GE_Kreis", beschilderungUnveraendert.getKreis().getValue());
		f1.set("GE_Land", beschilderungUnveraendert.getLand().getValue());

		// Feature mit unterschiedlichen Attributen wie "beschilderungVeraendert":
		SimpleFeatureBuilder f2 = new SimpleFeatureBuilder(simpleFeatureType);
		f2.add(beschilderungVeraendert.getGeometrie());
		f2.set("PfostenNr", beschilderungVeraendert.getPfostenNr().getValue());
		f2.set("WWTyp_Tx", beschilderungVeraendert.getWegweiserTyp().getValue());
		f2.set("PfTyp_Tx", "Cooler Typ"); // <-- Unterschied zu "beschilderungVeraendert"
		f2.set("GesZus", beschilderungVeraendert.getZustandsbewertung().getValue());
		f2.set("GesMangel", beschilderungVeraendert.getDefizit().getValue());
		f2.set("PfZus", beschilderungVeraendert.getPfostenzustand().getValue());
		f2.set("PfMangel", beschilderungVeraendert.getPfostendefizit().getValue());
		f2.set("GE_Gem", beschilderungVeraendert.getGemeinde().getValue());
		f2.set("GE_Kreis", beschilderungVeraendert.getKreis().getValue());
		f2.set("GE_Land", beschilderungVeraendert.getLand().getValue());

		when(geoJsonImportRepository.getSimpleFeatures(any())).thenReturn(
			List.of(
				f1.buildFeature("id1"),
				f2.buildFeature("id2")
			)
		);

		// act
		Optional<JobStatistik> jobStatistik = job.doRun();

		// assert
		WegweisendeBeschilderungImportJobStatistik statistik = (WegweisendeBeschilderungImportJobStatistik) jobStatistik
			.get();
		assertThat(statistik.anzahlFeatures).isEqualTo(2);
		assertThat(statistik.anzahlIgnoriert).isEqualTo(1);
		assertThat(statistik.anzahlNeuErstellt).isEqualTo(0);
		assertThat(statistik.anzahlAktualisiert).isEqualTo(1);
		assertThat(statistik.anzahlEntfernt).isEqualTo(0);
		assertThat(statistik.doppeltePfostenNummern).hasSize(0);

		verify(wegweisendeBeschilderungRepository).deleteAll(wegweisendeBeschilderungDeleteAllCaptor.capture());
		assertThat(wegweisendeBeschilderungDeleteAllCaptor.getValue()).isEmpty();

		verify(wegweisendeBeschilderungRepository).saveAll(wegweisendeBeschilderungSaveAllCaptor.capture());
		assertThat(wegweisendeBeschilderungSaveAllCaptor.getValue())
			// In den Geometrien sind die Envelopes seltsam rekursiv: geometrie.Envelope == geometrie.Envelope.Envelope
			// Das lässt den comparator hier in eine Endlosschleife laufen.
			.usingRecursiveFieldByFieldElementComparatorIgnoringFields("geometrie", "zustaendigeVerwaltungseinheit")
			// "beschilderungUnveraendert" wird nicht gespeichert, da unverändert.
			.containsExactly(beschilderungVeraendert);
	}
}
