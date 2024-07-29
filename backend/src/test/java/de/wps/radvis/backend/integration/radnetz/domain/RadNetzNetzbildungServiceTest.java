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

package de.wps.radvis.backend.integration.radnetz.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.GeometrieTypNichtUnterstuetztException;
import de.wps.radvis.backend.integration.radnetz.domain.entity.RadNetzNetzbildungStatistik;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import jakarta.persistence.EntityManager;
import lombok.NonNull;

class RadNetzNetzbildungServiceTest {

	private RadNetzNetzbildungService radNetzNetzbildungService;
	@Mock
	private NetzService netzService;
	@Mock
	private EntityManager entityManager;
	@Captor
	private ArgumentCaptor<Kante> kanteCaptor;
	@Mock
	private @NonNull RadNetzNetzbildungProtokollService protokollService;
	private RadNetzAttributMapper attributMapper;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		this.attributMapper = new RadNetzAttributMapper(protokollService);
		this.radNetzNetzbildungService = new RadNetzNetzbildungService(netzService, protokollService, attributMapper,
			entityManager);
		when(entityManager.merge(any())).thenAnswer(answer -> answer.getArgument(0));
	}

	@Test
	public void testBildeRadNetz_keineFeatures_netzserviceWirdNichtAufgerufen() {

		// act
		this.radNetzNetzbildungService.bildeRadNetz(Stream.empty(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(netzService, never()).saveKante(any());
	}

	@Test
	public void testBildeRadNetz_vieleLineStringFeatures_ModelEnthaeltKanten() {
		// arrange
		List<ImportedFeature> features = new ArrayList<>();

		Coordinate coordinate1 = new Coordinate(10, 10);
		Coordinate coordinate2 = new Coordinate(15, 15);
		ImportedFeature feature1 = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.quelle(QuellSystem.RadNETZ).build();
		LineString lineString1 = (LineString) feature1.getGeometrie();
		features.add(feature1);

		Coordinate coordinate3 = new Coordinate(20, 20);
		Coordinate coordinate4 = new Coordinate(25, 25);
		ImportedFeature feature2 = ImportedFeatureTestDataProvider.withLineString(coordinate3, coordinate4)
			.quelle(QuellSystem.RadNETZ).build();
		LineString lineString2 = (LineString) feature2.getGeometrie();
		features.add(feature2);

		Coordinate coordinate5 = new Coordinate(30, 30);
		Coordinate coordinate6 = new Coordinate(35, 35);
		Coordinate coordinate7 = new Coordinate(40, 40);
		ImportedFeature feature3 = ImportedFeatureTestDataProvider.withLineString(coordinate5, coordinate6, coordinate7)
			.quelle(QuellSystem.RadNETZ).build();
		LineString lineString3 = (LineString) feature3.getGeometrie();
		features.add(feature3);

		Coordinate coordinate8 = new Coordinate(45, 45);
		Coordinate coordinate9 = new Coordinate(50, 50);
		Coordinate coordinate10 = new Coordinate(55, 55);
		ImportedFeature feature4 = ImportedFeatureTestDataProvider
			.withLineString(coordinate8, coordinate9, coordinate10)
			.quelle(QuellSystem.RadNETZ).build();
		LineString lineString4 = (LineString) feature4.getGeometrie();
		features.add(feature4);

		Coordinate coordinate11 = new Coordinate(60, 60);
		Coordinate coordinate12 = new Coordinate(65, 65);
		Coordinate coordinate13 = new Coordinate(70, 70);
		ImportedFeature feature5 = ImportedFeatureTestDataProvider
			.withLineString(coordinate11, coordinate12, coordinate13).quelle(QuellSystem.RadNETZ).build();
		LineString lineString5 = (LineString) feature5.getGeometrie();
		features.add(feature5);

		// act
		this.radNetzNetzbildungService.bildeRadNetz(features.stream(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(netzService, times(5)).saveKante(kanteCaptor.capture());
		List<Kante> alleKanten = kanteCaptor.getAllValues();
		assertThat(alleKanten).anyMatch(kante -> kante.getGeometry().equals(lineString1));
		assertThat(alleKanten).anyMatch(kante -> kante.getGeometry().equals(lineString2));
		assertThat(alleKanten).anyMatch(kante -> kante.getGeometry().equals(lineString3));
		assertThat(alleKanten).anyMatch(kante -> kante.getGeometry().equals(lineString4));
		assertThat(alleKanten).anyMatch(kante -> kante.getGeometry().equals(lineString5));
		assertThat(alleKanten).allMatch(kante -> kante.getQuelle().equals(QuellSystem.RadNETZ));
	}

	@Test
	public void addImportedFeature_RadNetz_KanteMitVollstaendigenAttributen() {
		// arrange
		Coordinate coordinate1 = new Coordinate(10, 10);
		Coordinate coordinate2 = new Coordinate(15, 15);
		ImportedFeature feature = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.attribute(new HashMap<>())
			.addAttribut("LRVN_KAT", 3)
			.addAttribut("ORTSLAGE", "Außerorts")
			.addAttribut("STRASSE", "Vzul 40 oder 50 km/h")
			.addAttribut("RICHTUNG", "Zweirichtungsverkehr")
			.addAttribut("WEGETYP", "Führung auf der Fahrbahn (unmarkiert)")
			.addAttribut("BELAGART", "Wassergebundene Decke")
			.addAttribut("LANDKREIS", "Klein Dorflingen")
			.addAttribut("LAENGE", 1337d)
			.addAttribut("LICHT", "Nicht vorhanden")
			.addAttribut("sonstiges Attribut", "landschaftlich hübsch")
			.build();
		List<ImportedFeature> features = new ArrayList<>();
		features.add(feature);

		// act
		radNetzNetzbildungService.bildeRadNetz(features.stream(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(netzService).saveKante(kanteCaptor.capture());
		Kante result = kanteCaptor.getValue();

		assertThat(result.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks())
			.isEqualTo(Richtung.BEIDE_RICHTUNGEN);
		assertThat(result.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts())
			.isEqualTo(Richtung.BEIDE_RICHTUNGEN);

		assertThat(
			result.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0).getOrtslage())
				.contains(KantenOrtslage.AUSSERORTS);
		assertThat(result.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0)
			.getHoechstgeschwindigkeit())
				.isEqualTo(Hoechstgeschwindigkeit.MAX_50_KMH);

		assertThat(result.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).hasSize(1);
		assertThat(
			result.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0).getBelagArt())
				.isEqualTo(BelagArt.WASSERGEBUNDENE_DECKE);
		assertThat(result.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0)
			.getOberflaechenbeschaffenheit())
				.isEqualTo(Oberflaechenbeschaffenheit.UNBEKANNT);
	}

	@Test
	public void testBildeRadNetz_hatGeschwindigkeitAttribute_wennAlleAttributeLeer() {
		// arrange
		Coordinate coordinate1 = new Coordinate(10, 10);
		Coordinate coordinate2 = new Coordinate(15, 15);
		ImportedFeature feature = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.attribute(new HashMap<>())
			.addAttribut("LRVN_KAT", 3)
			.addAttribut("ORTSLAGE", "Außerorts")
			.addAttribut("RICHTUNG", "Zweirichtungsverkehr")
			.addAttribut("WEGETYP", "Führung auf der Fahrbahn (unmarkiert)")
			.addAttribut("BELAGART", "Wassergebundene Decke")
			.addAttribut("LANDKREIS", "Klein Dorflingen")
			.addAttribut("LAENGE", 1337d)
			.addAttribut("LICHT", "Nicht vorhanden")
			.build();
		List<ImportedFeature> features = new ArrayList<>();
		features.add(feature);

		// act
		radNetzNetzbildungService.bildeRadNetz(features.stream(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(netzService).saveKante(kanteCaptor.capture());
		Kante result = kanteCaptor.getValue();
		assertThat(result.getHoechsteNetzklassen().isPresent());
		assertThat(result.getHoechsteNetzklassen().get()).containsExactlyInAnyOrder(Netzklasse.RADNETZ_ALLTAG);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testBildeRadNetz_filterVeraltet_NetzServiceWirdNichtFuerVeralteteFeaturesAufgerufen() {
		// arrange
		List<ImportedFeature> features = new ArrayList<>();
		ImportedFeature nichtVeraltet = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(5, 6), new Coordinate(9, 12))
			.build();
		features.add(nichtVeraltet);
		features.add(ImportedFeatureTestDataProvider.withLineString().addAttribut("Status_WW", 2).build());
		features.add(ImportedFeatureTestDataProvider.withLineString().addAttribut("Status_WW", 2.0).build());
		features.add(ImportedFeatureTestDataProvider.withLineString().addAttribut("Status_WW", "2").build());

		LineString lineString = (LineString) nichtVeraltet.getGeometrie();

		// act
		this.radNetzNetzbildungService.bildeRadNetz(features.stream(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(netzService, times(1)).saveKante(kanteCaptor.capture());
		Kante result = kanteCaptor.getValue();
		assertThat(result.getGeometry()).isEqualTo(lineString);
	}

	@Test
	public void testBildeRadNetz_knotenStream_matchenderKnotenErhaeltAttribute() {
		// arrange
		List<ImportedFeature> lineStringFeatures = new ArrayList<>();
		ImportedFeature lineStringFeature = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(5, 6), new Coordinate(9, 12))
			.build();
		lineStringFeatures.add(lineStringFeature);

		List<ImportedFeature> pointFeatures = new ArrayList<>();
		ImportedFeature pointFeature = ImportedFeatureTestDataProvider.defaultRadNetzPunkt().point(new Coordinate(5, 6))
			.addAttribut("AufnTYP", "Knotenpunkt").addAttribut("Anm_Nach", "Mein Kommentar dazu")
			.build();
		pointFeatures.add(pointFeature);

		// act
		this.radNetzNetzbildungService.bildeRadNetz(lineStringFeatures.stream(), pointFeatures.stream(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(netzService, times(1)).saveKante(kanteCaptor.capture());
		KnotenAttribute result = kanteCaptor.getValue().getVonKnoten().getKnotenAttribute();
		assertThat(result.getKommentar()).contains(Kommentar.of("Mein Kommentar dazu"));
	}

	@Test
	public void testBildeRadNetz_knotenStreamLeichtVersetzterKnoten_matchenderKnotenErhaeltAttribute() {
		// arrange
		List<ImportedFeature> lineStringFeatures = new ArrayList<>();
		ImportedFeature lineStringFeature = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(5, 6), new Coordinate(9, 12))
			.build();
		lineStringFeatures.add(lineStringFeature);

		List<ImportedFeature> pointFeatures = new ArrayList<>();
		ImportedFeature pointFeature = ImportedFeatureTestDataProvider.defaultRadNetzPunkt().point(new Coordinate(6, 6))
			.addAttribut("AufnTYP", "Knotenpunkt").addAttribut("Anm_Nach", "Mein Kommentar dazu")
			.build();
		pointFeatures.add(pointFeature);

		// act
		this.radNetzNetzbildungService.bildeRadNetz(lineStringFeatures.stream(), pointFeatures.stream(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(netzService, times(1)).saveKante(kanteCaptor.capture());
		KnotenAttribute result = kanteCaptor.getValue().getVonKnoten().getKnotenAttribute();
		assertThat(result.getKommentar()).contains(Kommentar.of("Mein Kommentar dazu"));
	}

	@Test
	public void testBildeRadNetz_knotenStreamKnotenNichtInDerNaehe_keinKnotenErhaeltAttribute() {
		// arrange
		List<ImportedFeature> lineStringFeatures = new ArrayList<>();
		ImportedFeature lineStringFeature = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(5, 6), new Coordinate(9, 12))
			.build();
		lineStringFeatures.add(lineStringFeature);

		List<ImportedFeature> pointFeatures = new ArrayList<>();
		ImportedFeature pointFeature = ImportedFeatureTestDataProvider.defaultRadNetzPunkt()
			.point(new Coordinate(40, 40))
			.addAttribut("AufnTYP", "Knotenpunkt").addAttribut("Anm_Nach", "Mein Kommentar dazu")
			.build();
		pointFeatures.add(pointFeature);

		// act
		this.radNetzNetzbildungService.bildeRadNetz(lineStringFeatures.stream(), pointFeatures.stream(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(netzService, times(1)).saveKante(kanteCaptor.capture());
		KnotenAttribute vonKnotenResult = kanteCaptor.getValue().getVonKnoten().getKnotenAttribute();
		assertThat(vonKnotenResult.getKommentar()).isEmpty();
		KnotenAttribute nachKnotenResult = kanteCaptor.getValue().getNachKnoten().getKnotenAttribute();
		assertThat(nachKnotenResult.getKommentar()).isEmpty();
	}

	@Test
	public void testBildeRadNetz_knotenStreamKnotenMitAufnTypUngleichKnotenpunkt_keinKnotenErhaeltAttribute() {
		// arrange
		List<ImportedFeature> lineStringFeatures = new ArrayList<>();
		ImportedFeature lineStringFeature = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(5, 6), new Coordinate(9, 12))
			.build();
		lineStringFeatures.add(lineStringFeature);

		List<ImportedFeature> pointFeatures = new ArrayList<>();
		ImportedFeature pointFeature = ImportedFeatureTestDataProvider.defaultRadNetzPunkt().point(new Coordinate(6, 6))
			.addAttribut("AufnTYP", "Barriere").addAttribut("Anm_Nach", "Mein Kommentar dazu")
			.build();
		pointFeatures.add(pointFeature);

		// act
		this.radNetzNetzbildungService.bildeRadNetz(lineStringFeatures.stream(), pointFeatures.stream(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(netzService, times(1)).saveKante(kanteCaptor.capture());
		KnotenAttribute vonKnotenResult = kanteCaptor.getValue().getVonKnoten().getKnotenAttribute();
		assertThat(vonKnotenResult.getKommentar()).isEmpty();
		KnotenAttribute nachKnotenResult = kanteCaptor.getValue().getNachKnoten().getKnotenAttribute();
		assertThat(nachKnotenResult.getKommentar()).isEmpty();
	}

	@Test
	public void testBildeRadNetz_knotenStreamKnotenMitGeometryTypeUngleichPoint_keinKnotenErhaeltAttribute() {
		// arrange
		List<ImportedFeature> lineStringFeatures = new ArrayList<>();
		ImportedFeature lineStringFeature = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(5, 6), new Coordinate(9, 12))
			.build();
		lineStringFeatures.add(lineStringFeature);

		List<ImportedFeature> pointFeatures = new ArrayList<>();
		ImportedFeature lineStringFeature2 = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(6, 6), new Coordinate(6, 8))
			.addAttribut("AufnTYP", "Knotenpunkt").addAttribut("Anm_Nach", "Mein Kommentar dazu")
			.build();
		pointFeatures.add(lineStringFeature2);

		// act
		this.radNetzNetzbildungService.bildeRadNetz(lineStringFeatures.stream(), pointFeatures.stream(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(protokollService).handle(any(GeometrieTypNichtUnterstuetztException.class), any());
	}

	@Test
	public void testBildeRadNetz_knotenStreamVeralteterKnoten_keinKnotenErhaeltAttribute() {
		// arrange
		List<ImportedFeature> lineStringFeatures = new ArrayList<>();
		ImportedFeature lineStringFeature = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(5, 6), new Coordinate(9, 12))
			.build();
		lineStringFeatures.add(lineStringFeature);

		List<ImportedFeature> pointFeatures = new ArrayList<>();
		ImportedFeature pointFeature = ImportedFeatureTestDataProvider.defaultRadNetzPunkt().point(new Coordinate(6, 6))
			.addAttribut("AufnTYP", "Knotenpunkt").addAttribut("Anm_Nach", "Mein Kommentar dazu")
			.addAttribut("STATUS_WW", "2")
			.build();
		pointFeatures.add(pointFeature);

		// act
		this.radNetzNetzbildungService.bildeRadNetz(lineStringFeatures.stream(), pointFeatures.stream(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(netzService, times(1)).saveKante(kanteCaptor.capture());
		KnotenAttribute vonKnotenResult = kanteCaptor.getValue().getVonKnoten().getKnotenAttribute();
		assertThat(vonKnotenResult.getKommentar()).isEmpty();
		KnotenAttribute nachKnotenResult = kanteCaptor.getValue().getNachKnoten().getKnotenAttribute();
		assertThat(nachKnotenResult.getKommentar()).isEmpty();
	}

	@Test
	public void testtestBildeRadNetz_kurzerLineStringUndFuerBeideEndpunktePassenderKnotenBereitsVorhanden_VonUndNachKnotenNichtIdentisch() {
		// arrange
		List<ImportedFeature> lineStringFeatures = new ArrayList<>();

		ImportedFeature lineStringFeature1 = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(1.75, 1.75), new Coordinate(100, 100))
			.build();
		lineStringFeatures.add(lineStringFeature1);

		ImportedFeature lineStringFeature2 = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(1, 1), new Coordinate(2.5, 2.5))
			.build();
		lineStringFeatures.add(lineStringFeature2);

		// act
		this.radNetzNetzbildungService.bildeRadNetz(lineStringFeatures.stream(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(netzService, times(2)).saveKante(kanteCaptor.capture());
		List<Kante> kanten = kanteCaptor.getAllValues();
		Kante kurzeKante = kanten.get(1);
		assertThat(kurzeKante.getVonKnoten().getKoordinate()).isNotEqualTo(kurzeKante.getNachKnoten().getKoordinate());
	}

	@Test
	public void testBildeRadNetz_knotenUberschreibenSich_protokollService() {
		// arrange
		Coordinate coordinate1 = new Coordinate(10, 0);
		Coordinate coordinate2 = new Coordinate(30, 0);
		ImportedFeature feature = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.build();
		List<ImportedFeature> features = new ArrayList<>();
		features.add(feature);

		ImportedFeature knoten1 = ImportedFeatureTestDataProvider.defaultRadNetzPunkt().point(new Coordinate(9, 0))
			.attribute(new HashMap<>()).addAttribut("AufnTYP", "Knotenpunkt")
			.addAttribut("Anm_Nach", "test").build();

		ImportedFeature knoten2 = ImportedFeatureTestDataProvider.defaultRadNetzPunkt().point(new Coordinate(11, 0))
			.attribute(new HashMap<>()).addAttribut("AufnTYP", "Knotenpunkt")
			.addAttribut("Anm_Nach", "test").build();

		List<ImportedFeature> knotenFeatures = List.of(knoten1, knoten2);

		// act
		radNetzNetzbildungService.bildeRadNetz(features.stream(), knotenFeatures.stream(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(protokollService).handle(any(KnotenDuplikatException.class));
	}

	@Test
	public void testBildeRadNetz_knotenUberschreibenSich_ersterKnotenLeer_keinProtokollService() {
		// arrange
		Coordinate coordinate1 = new Coordinate(10, 0);
		Coordinate coordinate2 = new Coordinate(30, 0);
		ImportedFeature feature = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.build();
		List<ImportedFeature> features = new ArrayList<>();
		features.add(feature);

		ImportedFeature knoten1 = ImportedFeatureTestDataProvider.defaultRadNetzPunkt().point(new Coordinate(9, 0))
			.attribute(new HashMap<>()).addAttribut("AufnTYP", "Knotenpunkt").build();

		ImportedFeature knoten2 = ImportedFeatureTestDataProvider.defaultRadNetzPunkt().point(new Coordinate(11, 0))
			.attribute(new HashMap<>()).addAttribut("AufnTYP", "Knotenpunkt")
			.addAttribut("Anm_Nach", "test").build();

		List<ImportedFeature> knotenFeatures = List.of(knoten1, knoten2);

		// act
		radNetzNetzbildungService.bildeRadNetz(features.stream(), knotenFeatures.stream(),
			new RadNetzNetzbildungStatistik());

		// assert
		verify(protokollService, never()).handle(any(KnotenDuplikatException.class));
	}

	@Test
	public void testBildeRadNetz_featuresMitIdentischerGeometrieUndIdentischenAttributen_nurEineKanteGespeichert() {
		// arrange
		Coordinate coordinate1 = new Coordinate(10, 0);
		Coordinate coordinate2 = new Coordinate(30, 0);
		ImportedFeature feature = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.addAttribut("ORTSLAGE", "Außerorts")
			.addAttribut("RICHTUNG", "Zweirichtungsverkehr")
			.addAttribut("STATUS_WW", "1")
			.build();

		ImportedFeature feature2 = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.addAttribut("ORTSLAGE", "Außerorts")
			.addAttribut("RICHTUNG", "Zweirichtungsverkehr")
			.addAttribut("STATUS_WW", "1")
			.build();

		List<ImportedFeature> features = new ArrayList<>();
		features.add(feature);
		features.add(feature2);

		// act
		radNetzNetzbildungService.bildeRadNetz(features.stream(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		verifyNoInteractions(protokollService);
		verify(netzService, times(1)).saveKante(any());
	}

	@Test
	public void testBildeRadNetz_featuresMitIdentischerGeometrieUndVerschiedenenAttributen_beideKantenGespeichert() {
		// arrange
		Coordinate coordinate1 = new Coordinate(10, 0);
		Coordinate coordinate2 = new Coordinate(30, 0);
		ImportedFeature feature = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.addAttribut("ANM", "Was")
			.addAttribut("RICHTUNG", "Zweirichtungsverkehr")
			.addAttribut("STATUS_WW", "1")
			.build();

		ImportedFeature feature2 = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.addAttribut("ANM", "Was anderes") // <- verschieden
			.addAttribut("RICHTUNG", "Zweirichtungsverkehr")
			.addAttribut("STATUS_WW", "1")
			.build();

		List<ImportedFeature> features = new ArrayList<>();
		features.add(feature);
		features.add(feature2);

		// act
		radNetzNetzbildungService.bildeRadNetz(features.stream(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		verifyNoInteractions(protokollService);
		verify(netzService, times(2)).saveKante(any());
	}
}
