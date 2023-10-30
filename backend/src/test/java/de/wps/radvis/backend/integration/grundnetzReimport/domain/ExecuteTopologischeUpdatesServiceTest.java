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
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jaitools.jts.CoordinateSequence2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.DLMReimportJobStatistik;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.KnotenTupel;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.SplitUpdate;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.TopologischesUpdate;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.StartUndEndpunktGleichException;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FahrtrichtungAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class ExecuteTopologischeUpdatesServiceTest {

	private ExecuteTopologischeUpdatesService executeTopologischeUpdatesService;
	private final DLMReimportJobStatistik dlmReimportJobStatistik = new DLMReimportJobStatistik();
	@Mock
	private FindKnotenFromIndexService findKnotenFromIndexService;
	@Mock
	private KantenMappingRepository kantenMappingRepository;
	private final GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	Comparator<LinearReferenzierterAbschnitt> LR_COMPARATOR_WITH_PRECISION = (a,
		b) -> Math.abs(a.getVonValue() - b.getVonValue()) < 0.000000001
			&& Math.abs(a.getBisValue() - b.getBisValue()) < 0.000000001 ? 0 : -1;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		executeTopologischeUpdatesService = new ExecuteTopologischeUpdatesService(findKnotenFromIndexService,
			kantenMappingRepository);
		this.dlmReimportJobStatistik.reset();
	}

	@Test
	void kanteIstZuWeitEntferntUndHatUeberschneidung_KanteWirdResettet() throws StartUndEndpunktGleichException {

		// arrange
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = FahrtrichtungAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte().build();
		fahrtrichtungAttributGruppe.setRichtung(Richtung.GEGEN_RICHTUNG, Richtung.GEGEN_RICHTUNG);
		fahrtrichtungAttributGruppe.changeSeitenbezug(true);
		Kante ursprungsKante = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.isZweiseitig(true)
			.geometry(GeometryTestdataProvider.createLineString(
				new Coordinate(10, 10),
				new Coordinate(20, 10)))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue().kantenAttribute(
				KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
					.strassenName(StrassenName.of("Rainbow Road"))
					.strassenNummer(StrassenNummer.of("42"))
					.beleuchtung(Beleuchtung.VORHANDEN)
					.build())
				.build())
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppe)
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder()
				.geschwindigkeitAttribute(
					List.of(GeschwindigkeitAttribute.builder()
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH)
						.build()))
				.build())
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
				.zustaendigkeitAttribute(List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("I hate this")).build()))
				.build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true)
				.fuehrungsformAttributeLinks(
					List.of(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0, 1).bordstein(
						Bordstein.KOMPLETT_ABGESENKT).build()))
				.build())
			.vonKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).id(3L).build())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).id(4L).build())
			.build();

		LineString ursprungsKanteGeometry = ursprungsKante.getGeometry();
		LineString verticalVerschoben = GeometryTestdataProvider
			.getLinestringVerschobenUmCoordinate(ursprungsKanteGeometry, 0, 31);

		TopologischesUpdate topologischesUpdate = new TopologischesUpdate(ursprungsKante,
			verticalVerschoben.getStartPoint(),
			verticalVerschoben.getEndPoint(), verticalVerschoben);

		KnotenIndex knotenIndex = new KnotenIndex();
		when(findKnotenFromIndexService.getNeuenKnotenTupelFuerUpdate(topologischesUpdate, knotenIndex)).thenReturn(
			new KnotenTupel(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 41), QuellSystem.DLM).id(1L).build(),
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 41), QuellSystem.DLM).id(2L)
					.build()));

		DLMReimportJobStatistik statistik = new DLMReimportJobStatistik();

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante result = executeTopologischeUpdatesService
			.executeSimplesTopologischesUpdate(topologischesUpdate, statistik, knotenIndex,
				topologischStarkVeraenderteKanten);

		// assert
		Mockito.verify(kantenMappingRepository).deleteByGrundnetzKantenId(1L);

		assertThat(statistik.durchSimpleTopologicalUpdateSehrWeitEntferntVonUrsprungskante).isEqualTo(1);
		assertThat(result.getGeometry().equals(verticalVerschoben)).isTrue();
		assertThat(result.getVonKnoten().getPoint().equals(verticalVerschoben.getStartPoint())).isTrue();
		assertThat(result.getNachKnoten().getPoint().equals(verticalVerschoben.getEndPoint())).isTrue();

		assertThat(result.getKantenAttributGruppe().getKantenAttribute().getStrassenName())
			.contains(StrassenName.of("Rainbow Road"));
		assertThat(result.getKantenAttributGruppe().getKantenAttribute().getStrassenNummer())
			.contains(StrassenNummer.of("42"));
		assertThat(result.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung())
			.isEqualTo(Beleuchtung.UNBEKANNT);

		assertThat(result.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks())
			.isEqualTo(FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().build()
				.getFahrtrichtungLinks());
		assertThat(result.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts())
			.isEqualTo(
				FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().build()
					.getFahrtrichtungRechts());
		assertThat(result.getFahrtrichtungAttributGruppe().isZweiseitig())
			.isEqualTo(FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().build().isZweiseitig());

		assertThat(result.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute())
			.isEqualTo(Set.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte().build()));

		assertThat(result.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute()).isEqualTo(
			ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute().build()
				.getImmutableZustaendigkeitAttribute());

		assertThat(result.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).isEqualTo(
			FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().build()
				.getImmutableFuehrungsformAttributeLinks());

		assertThat(result.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts()).isEqualTo(
			FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().build()
				.getImmutableFuehrungsformAttributeRechts());

		assertThat(topologischStarkVeraenderteKanten).containsExactly(entry(ursprungsKante, ursprungsKanteGeometry));
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_LaengereGeometrieEinSegment_UebernahmeDerZustaendigkeitAttributgruppe()
		throws Exception {
		// arrange
		LineString lineStringKante = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(2, 0), new Coordinate(2, 2) });
		final Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L)
			.build();
		ZustaendigkeitAttribute zustaendigkeitAttribute = ZustaendigkeitAttributGruppeTestDataProvider
			.withLineareReferenz(0.0, 1.0)
			.erhaltsZustaendiger(organisation)
			.vereinbarungsKennung(VereinbarungsKennung.of("ABC")).build();
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			List.of(zustaendigkeitAttribute));
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(lineStringKante)
			.zustaendigkeitAttributGruppe(zustaendigkeitAttributGruppe).build();

		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(2, 0), new Coordinate(2, 5) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = resultKante.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute();
		assertThat(resultList.size()).isEqualTo(1);
		ZustaendigkeitAttribute resultAttribute = resultList.get(0);
		assertThat(resultAttribute.getErhaltsZustaendiger()).contains(organisation);
		assertThat(resultAttribute.getVereinbarungsKennung()).contains(VereinbarungsKennung.of("ABC"));
		assertThat(resultAttribute.getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultAttribute.getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_LaengereGeometrieEinSegment_UebernahmeDerFuehrungsformAttributgruppe()
		throws Exception {
		// arrange
		LineString lineStringKante = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(2, 0), new Coordinate(2, 2) });
		FuehrungsformAttribute fuehrungsformAttribute = FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.0,
			1.0).belagArt(
				BelagArt.ASPHALT)
			.breite(Laenge.of(2.0)).build();
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(
			List.of(fuehrungsformAttribute), false);
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(lineStringKante)
			.fuehrungsformAttributGruppe(fuehrungsformAttributGruppe).build();

		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(2, 0), new Coordinate(2, 5) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<FuehrungsformAttribute> resultList = resultKante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks();
		assertThat(resultList.size()).isEqualTo(1);
		FuehrungsformAttribute resultAttribute = resultList.get(0);
		assertThat(resultAttribute.getBelagArt()).isEqualTo(BelagArt.ASPHALT);
		assertThat(resultAttribute.getBreite()).isPresent();
		assertThat(resultAttribute.getBreite().get().getValue()).isEqualTo(2.0);
		assertThat(resultAttribute.getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultAttribute.getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_ZweiseitigLaengereGeometrie_UebernahmeDerFuehrungsformAttributgruppe()
		throws Exception {
		// arrange

		Kante kante = this.buildTestKanteFuehrungsform(
			List.of(
				LinearReferenzierterAbschnitt.of(0, 0.4),
				LinearReferenzierterAbschnitt.of(0.4, 0.6),
				LinearReferenzierterAbschnitt.of(0.6, 1)),
			List.of(
				LinearReferenzierterAbschnitt.of(0, 0.25),
				LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1)),
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(20, 0), new Coordinate(20, 20) }, true);
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(20, 0), new Coordinate(20, 40) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert

		List<FuehrungsformAttribute> resultListLinks = resultKante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks();
		assertThat(resultListLinks.size()).isEqualTo(4);
		assertThat(resultListLinks).containsExactlyInAnyOrder(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.27)
				.breite(Laenge.of(1))
				.build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.27, 0.4)
				.breite(Laenge.of(2))
				.build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 0.67)
				.breite(Laenge.of(3))
				.build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.67, 1)
				.build());

		List<FuehrungsformAttribute> resultListRechts = resultKante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts();
		assertThat(resultListRechts.size()).isEqualTo(4);
		assertThat(resultListRechts).containsExactlyInAnyOrder(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.17)
				.breite(Laenge.of(0.01))
				.build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.17, 0.5)
				.breite(Laenge.of(0.02))
				.build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 0.67)
				.breite(Laenge.of(0.03))
				.build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.67, 1)
				.build());

		assertThat(LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			resultListLinks.stream().map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt).collect(
				Collectors.toList()))).isTrue();
		assertThat(LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			resultListRechts.stream().map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt).collect(
				Collectors.toList()))).isTrue();

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_ZweiseitigMitUmkehrDerStationierungsrichtungLaengereGeometrie_UebernahmeDerFuehrungsformAttributgruppe()
		throws Exception {
		// arrange

		Kante kante = this.buildTestKanteFuehrungsform(
			List.of(
				LinearReferenzierterAbschnitt.of(0, 0.4),
				LinearReferenzierterAbschnitt.of(0.4, 0.6),
				LinearReferenzierterAbschnitt.of(0.6, 1)),
			List.of(
				LinearReferenzierterAbschnitt.of(0, 0.25),
				LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1)),
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(200, 0), new Coordinate(200, 200) },
			true);
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(200, 400), new Coordinate(200, 0), new Coordinate(0, 0) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<FuehrungsformAttribute> resultListLinks = resultKante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks();
		assertThat(resultListLinks.size()).isEqualTo(4);
		assertThat(resultListLinks)
			.usingRecursiveFieldByFieldElementComparator()
			.usingComparatorForType(LR_COMPARATOR_WITH_PRECISION, LinearReferenzierterAbschnitt.class)
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.33)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.33, 0.5)
					.breite(Laenge.of(0.03))
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 0.83)
					.breite(Laenge.of(0.02))
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.83, 1)
					.breite(Laenge.of(0.01))
					.build());

		List<FuehrungsformAttribute> resultListRechts = resultKante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts();
		assertThat(resultListRechts.size()).isEqualTo(4);
		assertThat(resultListRechts)
			.usingRecursiveFieldByFieldElementComparator()
			.usingComparatorForType(LR_COMPARATOR_WITH_PRECISION, LinearReferenzierterAbschnitt.class)
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.33)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.33, 0.6)
					.breite(Laenge.of(3))
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 0.73)
					.breite(Laenge.of(2))
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.73, 1)
					.breite(Laenge.of(1))
					.build());
		assertThat(LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			resultListLinks.stream().map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt).collect(
				Collectors.toList()))).isTrue();
		assertThat(LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			resultListRechts.stream().map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt).collect(
				Collectors.toList()))).isTrue();

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_ExtentHinten_UebernahmeBeiderZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.5), LinearReferenzierterAbschnitt.of(0.5, 1.0)),
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(1, 0), new Coordinate(1, 1) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(1, 0), new Coordinate(1, 2) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("0");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.33);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.33);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void error_neuerAnfangZuDichtAnEndeDerKante() throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.34), LinearReferenzierterAbschnitt.of(0.34, 1.0)),
			new Coordinate[] {
					new Coordinate(10, 10),
					new Coordinate(10, 20),
					new Coordinate(20, 20),
					new Coordinate(20, 10) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] {
					// Sobald die nächsten beiden Koordinaten nicht hinzugefügt werden, haben wir keinen Error mehr.
					new Coordinate(20, 20),
					new Coordinate(20, 10),

					new Coordinate(10, 10),
					new Coordinate(10, 20)
			});
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		assertThat(resultKante.getGeometry().getCoordinates()).isEqualTo(lineStringFeature.getCoordinates());
		assertThat(this.dlmReimportJobStatistik.fatalErrorOccurred).isFalse();
		assertThat(dlmReimportJobStatistik.isProjektionsReihenfolgeReversed).isEqualTo(1);
		assertThat(resultKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute()).hasSize(1);
		assertThat(resultKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.containsExactly(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1).build());
		assertThat(this.dlmReimportJobStatistik.fatalErrorOccurred).isFalse();

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void error_neuerAnfangZuDichtAnEndeDerKante_realdaten() throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.34), LinearReferenzierterAbschnitt.of(0.34, 1.0)),
			new Coordinate[] {
					new Coordinate(565159.18, 5340709.27),
					new Coordinate(565143.4, 5340710.08),
					new Coordinate(565140.07, 5340712.06),
					new Coordinate(565136.71, 5340717.07),
					new Coordinate(565137.72, 5340760.35) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] {
					// Sobald die nächsten beiden Koordinaten nicht hinzugefügt werden, haben wir keinen Error mehr.
					new Coordinate(565140.53, 5340759.94),
					new Coordinate(565145.3, 5340746.76),
					new Coordinate(565156.88, 5340727.08),
					new Coordinate(565159.42, 5340720.41),
					new Coordinate(565156.41, 5340714.69),
					new Coordinate(565150.39, 5340712.57),
					new Coordinate(565144.82, 5340713.42),
					new Coordinate(565140.53, 5340716.44),
					new Coordinate(565138.63, 5340723.58),
					new Coordinate(565140.00, 5340758.00)
			});

		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		assertThat(resultKante.getGeometry().getCoordinates()).isEqualTo(lineStringFeature.getCoordinates());
		assertThat(this.dlmReimportJobStatistik.fatalErrorOccurred).isFalse();
		assertThat(dlmReimportJobStatistik.isProjektionsReihenfolgeReversed).isEqualTo(1);
		assertThat(resultKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute()).hasSize(1);
		assertThat(resultKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.containsExactly(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1).build());
		assertThat(this.dlmReimportJobStatistik.fatalErrorOccurred).isFalse();

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void error_neuerAnfangZuDichtAnEndeDerKante_realdaten_reversed() throws Exception {

		Coordinate[] koordinaten = new Coordinate[] {
				new Coordinate(565137.72, 5340760.35),
				new Coordinate(565136.71, 5340717.07),
				new Coordinate(565140.07, 5340712.06),
				new Coordinate(565143.4, 5340710.08),
				new Coordinate(565159.18, 5340709.27)
		};

		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.34), LinearReferenzierterAbschnitt.of(0.34, 1.0)),
			koordinaten);
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] {
					// Sobald die nächsten beiden Koordinaten nicht hinzugefügt werden, haben wir keinen Error mehr.
					new Coordinate(565140.53, 5340759.94),
					new Coordinate(565145.3, 5340746.76),
					new Coordinate(565156.88, 5340727.08),
					new Coordinate(565159.42, 5340720.41),
					new Coordinate(565156.41, 5340714.69),
					new Coordinate(565150.39, 5340712.57),
					new Coordinate(565144.82, 5340713.42),
					new Coordinate(565140.53, 5340716.44),
					new Coordinate(565138.63, 5340723.58),
					new Coordinate(565140.53, 5340750.00)
			});

		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		assertThat(resultKante.getGeometry().getCoordinates()).isEqualTo(lineStringFeature.getCoordinates());
		assertThat(this.dlmReimportJobStatistik.fatalErrorOccurred).isFalse();
		assertThat(dlmReimportJobStatistik.isProjektionsReihenfolgeReversed).isEqualTo(1);
		assertThat(resultKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute()).hasSize(1);
		assertThat(resultKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.containsExactly(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1).build());

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_ExtentVorne_UebernahmeBeiderZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.5), LinearReferenzierterAbschnitt.of(0.5, 1.0)),
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(0, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("0");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.67);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.67);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_ExtentVorneUndHinten_UebernahmeBeiderZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.5), LinearReferenzierterAbschnitt.of(0.5, 1.0)),
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(0, 1), new Coordinate(2, 1), new Coordinate(2, 3) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("0");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_ShortenHinten_UebernahmeDerVorderenBeidenZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 4) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("0");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_ShortenVorne_UebernahmeDerHinterenBeidenZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(0, 1), new Coordinate(2, 1), new Coordinate(2, 3) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 3) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("2");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.67);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.67);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_ShortenVorneUndHinten_UebernahmeDerBeidenMittlerenZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.5),
				LinearReferenzierterAbschnitt.of(0.5, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(0, 1), new Coordinate(2, 1), new Coordinate(2, 3) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("2");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_VerschiebungNachLinks_UebernahmeDerVorderenBeidenZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 4) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(-1, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("0");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.75);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.75);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_VerschiebungNachRechts_UebernahmeDerHinterenBeidenZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(-1, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 4) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("2");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.25);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.25);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_OrthogonaleVerschiebungKleinerToleranz_UebernahmeDerZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(0, 2), new Coordinate(1, 2), new Coordinate(1, 3) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(1);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_OrthogonaleVerschiebungGroesserToleranz_KeineUebernahme()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(0, 2), new Coordinate(1, 2), new Coordinate(1, 3) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(20, 22), new Coordinate(21, 22), new Coordinate(21, 23) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(1);
		assertThat(resultList.get(0).getVereinbarungsKennung()).isNotPresent();
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_KeineUeberschneidung_KeineUebernahmeVonZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(-1, 1), new Coordinate(2, 1) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(2, 1), new Coordinate(2, 4) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(1);
		assertThat(resultList.get(0).getVereinbarungsKennung()).isNotPresent();
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	// Ab hier werden die Tests nochmal mit umgedrehter Stationierungsrichtung ausgeführt
	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_ExtentHinten_UebernahmeBeiderZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.5), LinearReferenzierterAbschnitt.of(0.5, 1.0)),
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(1, 0), new Coordinate(1, 1) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(1, 2), new Coordinate(1, 0), new Coordinate(0, 0) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("0");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.67);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.67);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_ExtentVorne_UebernahmeBeiderZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.5), LinearReferenzierterAbschnitt.of(0.5, 1.0)),
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(2, 2), new Coordinate(2, 1), new Coordinate(0, 1) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("0");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.33);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.33);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_ExtentVorneUndHinten_UebernahmeBeiderZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.5), LinearReferenzierterAbschnitt.of(0.5, 1.0)),
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(2, 3), new Coordinate(2, 1), new Coordinate(0, 1) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("0");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtungUndEndpunkteMatchen_KeineUebernahme()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 1.00)),
			new Coordinate[] { new Coordinate(539886.19, 5388187.42),
					new Coordinate(539874.52, 5388182.91), new Coordinate(539868.69, 5388181.34),
					new Coordinate(539864.19, 5388180.12) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new CoordinateSequence2D(538956.9, 5387980.36, 538981.27, 5387982.34, 539006.53, 5387986.25, 539056.51,
				5388000.16, 539081.5, 5388006.36, 539101.41, 5388010.11, 539132.6, 5388015.94, 539151.39, 5388017.63,
				539166.99, 5388017.44, 539182.02, 5388014.25, 539230.31, 5388001.28, 539263.2, 5387992.66, 539274.41,
				5387990.19, 539289.89, 5387987.88, 539308.41, 5387987.59, 539320.53, 5387988.74, 539339.89, 5387990.58,
				539390.91, 5387995.05, 539408.67, 5387996.28, 539426.69, 5387997.52, 539440.4, 5387997.75, 539459.17,
				5387992.64, 539472.87, 5387988.38, 539483.77, 5387984.99, 539503.81, 5387981.91, 539527.92, 5387982.38,
				539560.42, 5387987.14, 539583.67, 5387992.59, 539597.99, 5387995.95, 539633.8, 5388004.24, 539657.16,
				5388010.54, 539673.58, 5388012.19, 539694.68, 5388016.8, 539713.52, 5388022.46, 539735.68, 5388030.61,
				539757.04, 5388035.73, 539772.3, 5388039.13, 539777.89, 5388041.76, 539786.07, 5388045.6, 539799.65,
				5388059.16, 539815.06, 5388078.28, 539825.16, 5388097.98, 539835.64, 5388131.09, 539839.4, 5388146.92,
				539843.49, 5388161.12, 539845.51, 5388166.96, 539847.89, 5388171.16, 539850.07, 5388173.7, 539853.17,
				5388176.36, 539856.15, 5388178.39, 539859.76, 5388179.9, 539863.81, 5388181.4, 539868.57, 5388182.63));
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(1);
		assertThat(resultList.get(0).getVereinbarungsKennung()).isNotPresent();
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_ShortenHinten_UebernahmeDerVorderenBeidenZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 4) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(2, 2), new Coordinate(2, 1), new Coordinate(1, 1) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("0");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_ShortenVorne_UebernahmeDerHinterenBeidenZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(0, 1), new Coordinate(2, 1), new Coordinate(2, 3) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(2, 3), new Coordinate(2, 1), new Coordinate(1, 1) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("2");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.33);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.33);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_ShortenVorneUndHinten_UebernahmeDerBeidenMittlerenZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.5),
				LinearReferenzierterAbschnitt.of(0.5, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(0, 1), new Coordinate(2, 1), new Coordinate(2, 3) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(2, 2), new Coordinate(2, 1), new Coordinate(1, 1) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("2");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.5);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_VerschiebungNachLinks_UebernahmeDerVorderenBeidenZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 4) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(2, 2), new Coordinate(2, 1), new Coordinate(-1, 1) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("0");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.25);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.25);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_VerschiebungNachRechts_UebernahmeDerHinterenBeidenZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(-1, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(2, 4), new Coordinate(2, 1), new Coordinate(1, 1) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("2");
		assertThat(resultList.get(1).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(0.75);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.75);
		assertThat(resultList.get(1).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_OrthogonaleVerschiebungKleinerToleranz_UebernahmeDerZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(0, 2), new Coordinate(1, 2), new Coordinate(1, 3) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(2, 2), new Coordinate(2, 1), new Coordinate(1, 1) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(1);
		assertThat(resultList.get(0).getVereinbarungsKennung().get().getValue()).isEqualTo("1");
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_OrthogonaleVerschiebungGroesserToleranz_KeineUebernahme()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(0, 2), new Coordinate(1, 2), new Coordinate(1, 3) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(21, 23), new Coordinate(21, 22), new Coordinate(20, 22) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(1);
		assertThat(resultList.get(0).getVereinbarungsKennung()).isNotPresent();
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_KeineUeberschneidung_KeineUebernahmeVonZustaendigkeitAttribute()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.75),
				LinearReferenzierterAbschnitt.of(0.75, 1.0)),
			new Coordinate[] { new Coordinate(-1, 1), new Coordinate(2, 1) });
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(2, 4), new Coordinate(2, 1) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultList.size()).isEqualTo(1);
		assertThat(resultList.get(0).getVereinbarungsKennung()).isNotPresent();
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.0);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_GleicheRichtung_OrthogonaleVerschiebungKleinerToleranz_UebernahmeDerFahrtrichtungsUndGeschwindigkeitsAttribute()
		throws Exception {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.geometry(geometryFactory.createLineString(
				new Coordinate[] { new Coordinate(0, 2), new Coordinate(1, 2), new Coordinate(1, 3) }))
			.quelle(QuellSystem.DLM)
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.geschwindigkeitAttribute(List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_70_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_100_KMH)
					.build()))
				.build())
			.build();

		kante.getFahrtrichtungAttributGruppe().changeSeitenbezug(true);
		kante.getFahrtrichtungAttributGruppe().setRichtung(Richtung.GEGEN_RICHTUNG, Richtung.BEIDE_RICHTUNGEN);

		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 2) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		assertThat(resultKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
			Richtung.GEGEN_RICHTUNG);
		assertThat(resultKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
			Richtung.BEIDE_RICHTUNGEN);

		assertThat(
			resultKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0)
				.getHoechstgeschwindigkeit())
					.isEqualTo(Hoechstgeschwindigkeit.MAX_70_KMH);

		assertThat(
			resultKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0)
				.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung())
					.contains(Hoechstgeschwindigkeit.MAX_100_KMH);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_UmgedrehteRichtung_OrthogonaleVerschiebungKleinerToleranz_UebernahmeDerInvertiertenFahrtrichtungsUndGeschwindigkeitsAttribute()
		throws Exception {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.geometry(geometryFactory.createLineString(
				new Coordinate[] { new Coordinate(0, 2), new Coordinate(1, 2), new Coordinate(1, 3) }))
			.quelle(QuellSystem.DLM)
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.geschwindigkeitAttribute(List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_70_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_100_KMH)
					.build()))
				.build())
			.build();

		kante.getFahrtrichtungAttributGruppe().changeSeitenbezug(true);
		kante.getFahrtrichtungAttributGruppe().setRichtung(Richtung.GEGEN_RICHTUNG, Richtung.BEIDE_RICHTUNGEN);

		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(2, 2), new Coordinate(2, 1), new Coordinate(1, 1) });
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		assertThat(resultKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
			Richtung.BEIDE_RICHTUNGEN);
		assertThat(resultKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
			Richtung.IN_RICHTUNG);

		assertThat(
			resultKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0)
				.getHoechstgeschwindigkeit())
					.isEqualTo(Hoechstgeschwindigkeit.MAX_100_KMH);

		assertThat(
			resultKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0)
				.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung())
					.contains(Hoechstgeschwindigkeit.MAX_70_KMH);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_realdaten_projektionsreihenfolgeAendertSichUnerwartet_wirft_fehler()
		throws Exception {
		// arrange
		Kante kante = this.buildTestKante(
			List.of(LinearReferenzierterAbschnitt.of(0.0, 0.915), LinearReferenzierterAbschnitt.of(0.915, 1)),
			new CoordinateSequence2D(521278.5, 5398757.26, 521314.62, 5398763.78, 521347.83, 5398769.21, 521372.47,
				5398773.39, 521391.16, 5398776.99, 521392.47, 5398766.51).toCoordinateArray());
		LineString lineStringFeature = this.geometryFactory.createLineString(
			new CoordinateSequence2D(521293.18, 5398762.72, 521392.01, 5398779.39));
		this.configureTopologieUpdateServiceMock(lineStringFeature);

		// act
		HashMap<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();
		Kante resultKante = this.executeTopologischeUpdatesService.executeSimplesTopologischesUpdate(
			new TopologischesUpdate(kante, lineStringFeature.getStartPoint(), lineStringFeature.getEndPoint(),
				lineStringFeature),
			this.dlmReimportJobStatistik, new KnotenIndex(), topologischStarkVeraenderteKanten);

		// assert
		List<ZustaendigkeitAttribute> resultList = this.extractSortedResultAttributeList(resultKante);
		assertThat(resultKante.getGeometry().getCoordinates()).containsExactly(lineStringFeature.getCoordinates());
		assertThat(resultList.size()).isEqualTo(1);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()).isEqualTo(0.0);
		assertThat(resultList.get(0).getLinearReferenzierterAbschnitt().getBisValue()).isEqualTo(1.);
		assertThat(dlmReimportJobStatistik.fatalErrorOccurred).isFalse();
		assertThat(dlmReimportJobStatistik.isProjektionsReihenfolgeReversed).isEqualTo(1);

		assertThat(topologischStarkVeraenderteKanten).isEmpty();
	}

	private Kante buildTestKante(List<LinearReferenzierterAbschnitt> lineareReferenzen, Coordinate[] koordinaten) {
		LineString lineStringKante = this.geometryFactory.createLineString(koordinaten);
		List<ZustaendigkeitAttribute> zustaendigkeitAttributeList = new ArrayList<>();
		for (int i = 0; i < lineareReferenzen.size(); i++) {
			ZustaendigkeitAttribute zustaendigkeitAttribute = ZustaendigkeitAttributGruppeTestDataProvider
				.withLineareReferenz(
					lineareReferenzen.get(i).getVonValue(), lineareReferenzen.get(i).getBisValue())
				.vereinbarungsKennung(VereinbarungsKennung.of(String.valueOf(i)))
				.build();
			zustaendigkeitAttributeList.add(zustaendigkeitAttribute);
		}
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			zustaendigkeitAttributeList);
		return KanteTestDataProvider.withDefaultValues().geometry(lineStringKante)
			.zustaendigkeitAttributGruppe(zustaendigkeitAttributGruppe).build();
	}

	private Kante buildTestKanteFuehrungsform(List<LinearReferenzierterAbschnitt> lineareReferenzenLinks,
		List<LinearReferenzierterAbschnitt> lineareReferenzenRechts, Coordinate[] koordinaten,
		boolean zweiseitig) {
		require(!zweiseitig || lineareReferenzenLinks.size() == lineareReferenzenRechts.size());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe;
		final var fahrtrichtungAttributGruppe = FahrtrichtungAttributGruppe.builder();
		if (!zweiseitig) {
			List<FuehrungsformAttribute> fuehrungsformAttributeList = new ArrayList<>();
			for (int i = 0; i < lineareReferenzenLinks.size(); i++) {
				FuehrungsformAttribute fuehrungsformAttribute = FuehrungsformAttributeTestDataProvider
					.withLineareReferenz(
						lineareReferenzenLinks.get(i).getVonValue(), lineareReferenzenLinks.get(i).getBisValue())
					.breite(Laenge.of(i + 1))
					.build();
				fuehrungsformAttributeList.add(fuehrungsformAttribute);
			}
			fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(
				fuehrungsformAttributeList, false);
		} else {
			List<FuehrungsformAttribute> fuehrungsformAttributeListLinks = new ArrayList<>();
			List<FuehrungsformAttribute> fuehrungsformAttributeListRechts = new ArrayList<>();
			for (int i = 0; i < lineareReferenzenLinks.size(); i++) {
				FuehrungsformAttribute fuehrungsformAttributeLinks = FuehrungsformAttributeTestDataProvider
					.withLineareReferenz(
						lineareReferenzenLinks.get(i).getVonValue(), lineareReferenzenLinks.get(i).getBisValue())
					.breite(Laenge.of(i + 1))
					.build();
				FuehrungsformAttribute fuehrungsformAttributeRechts = FuehrungsformAttributeTestDataProvider
					.withLineareReferenz(
						lineareReferenzenRechts.get(i).getVonValue(), lineareReferenzenRechts.get(i).getBisValue())
					.breite(Laenge.of((i + 1) * 0.01))
					.build();
				fuehrungsformAttributeListLinks.add(fuehrungsformAttributeLinks);
				fuehrungsformAttributeListRechts.add(fuehrungsformAttributeRechts);
			}
			fuehrungsformAttributGruppe = FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.fuehrungsformAttributeLinks(fuehrungsformAttributeListLinks)
				.fuehrungsformAttributeRechts(fuehrungsformAttributeListRechts)
				.isZweiseitig(true)
				.build();
			fahrtrichtungAttributGruppe.isZweiseitig(true);
		}

		LineString lineStringKante = this.geometryFactory.createLineString(koordinaten);
		return KanteTestDataProvider.withDefaultValuesAndZweiseitig().geometry(lineStringKante)
			.fuehrungsformAttributGruppe(fuehrungsformAttributGruppe)
			.build();
	}

	private List<ZustaendigkeitAttribute> extractSortedResultAttributeList(Kante kante) {
		return kante.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().stream().sorted(
				Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
					LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toUnmodifiableList());
	}

	private void configureTopologieUpdateServiceMock(LineString lineStringFeature) throws Exception {
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(
			lineStringFeature.getStartPoint().getCoordinate(), QuellSystem.DLM).id(1L).build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(
			lineStringFeature.getEndPoint().getCoordinate(), QuellSystem.DLM).id(2L).build();
		when(findKnotenFromIndexService.findOrCreateKnotenTupel(ArgumentMatchers.any(Point.class),
			ArgumentMatchers.any(Point.class), any())).thenReturn(new KnotenTupel(vonKnoten, nachKnoten));
		when(findKnotenFromIndexService.getNeuenKnotenTupelFuerUpdate(any(), any()))
			.thenReturn(new KnotenTupel(vonKnoten, nachKnoten));
	}

	@Nested
	class TesteFuehreSplitsDurch {
		@Test
		void testFuehreSplitsDurch_kanteEinseitigMitEinemSplitPartnerUndKeineAenderungDerStationierungsrichtung() {
			// arrange
			// Knoten der ursprünglichen, gesplitteten Kante
			Knoten altVon = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
				.id(10L)
				.build();

			Knoten altNach = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(20L)
				.build();

			// Führungsform
			List<FuehrungsformAttribute> fuehrungsformAttributeLinks = List.of(
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.25))
					.belagArt(BelagArt.BETON)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.25, 0.75))
					.belagArt(BelagArt.NATURSTEINPFLASTER)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.75, 1))
					.belagArt(BelagArt.ASPHALT)
					.build());

			// Zustaendigkeit

			List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L).build())
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.75))
					.build(),
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(200L).build())
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.75, 1))
					.build());

			// Fahrtrichtung

			Richtung fahrtrichtungLinks = Richtung.IN_RICHTUNG;

			// Geschwindigkeit

			GeschwindigkeitAttribute geschwindigkeitAttribute = GeschwindigkeitsAttributeTestDataProvider
				.withGrundnetzDefaultwerte()
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_70_KMH)
				.build();
			// gesplitteteKante
			Kante gesplitteteKante = KanteTestDataProvider.fromKnoten(altVon, altNach).quelle(QuellSystem.DLM)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
						.umfeld(Umfeld.GEWERBEGEBIET)
						.strassenName(StrassenName.of("lol"))
						.strassenNummer(StrassenNummer.of("7"))
						.build())
					.build())
				.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(fuehrungsformAttributeLinks)
					.fuehrungsformAttributeRechts(fuehrungsformAttributeLinks)
					.build())
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(zustaendigkeitAttribute)
					.build())
				.fahrtrichtungAttributGruppe(new FahrtrichtungAttributGruppe(fahrtrichtungLinks, false))
				.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.geschwindigkeitAttribute(List.of(geschwindigkeitAttribute))
					.build())
				.id(1L)
				.build();

			Knoten neuNach = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 5), QuellSystem.DLM)
				.id(30L)
				.build();

			// SplitUpdate
			Kante splitPartner = KanteTestDataProvider.fromKnoten(neuNach, gesplitteteKante.getNachKnoten())
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
						.strassenName(StrassenName.of("Rainbow Road"))
						.strassenNummer(StrassenNummer.of("42")).build())
					.build())
				.id(2L)
				.quelle(QuellSystem.DLM)
				.build();

			SplitUpdate splitUpdate = new SplitUpdate(gesplitteteKante, GeometryTestdataProvider
				.createLineString(new Coordinate(0, 0), new Coordinate(5, 5)),
				gesplitteteKante.getVonKnoten(), neuNach, List.of(gesplitteteKante, splitPartner));

			// act
			executeTopologischeUpdatesService.executeSplitUpdate(splitUpdate);

			// assert
			// KantenAttribute
			assertThat(gesplitteteKante.getKantenAttributGruppe().getKantenAttribute().getUmfeld()).isEqualTo(
				Umfeld.GEWERBEGEBIET);
			assertThat(gesplitteteKante.getKantenAttributGruppe().getKantenAttribute().getStrassenNummer()).contains(
				StrassenNummer.of("7"));
			assertThat(gesplitteteKante.getKantenAttributGruppe().getKantenAttribute().getStrassenName()).contains(
				StrassenName.of("lol"));
			assertThat(splitPartner.getKantenAttributGruppe().getKantenAttribute().getUmfeld()).isEqualTo(
				Umfeld.GEWERBEGEBIET);
			assertThat(splitPartner.getKantenAttributGruppe().getKantenAttribute().getStrassenNummer()).contains(
				StrassenNummer.of("42"));
			assertThat(splitPartner.getKantenAttributGruppe().getKantenAttribute().getStrassenName()).contains(
				StrassenName.of("Rainbow Road"));

			// Führungsform
			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.hasSize(2);
			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.containsExactlyInAnyOrder(
					fuehrungsformAttributeLinks.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.5)),
					fuehrungsformAttributeLinks.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.5, 1)));

			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.containsExactlyInAnyOrderElementsOf(
					gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.hasSize(2);
			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.containsExactlyInAnyOrder(
					fuehrungsformAttributeLinks.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.5)),
					fuehrungsformAttributeLinks.get(2).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.5, 1)));

			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.containsExactlyInAnyOrderElementsOf(
					splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

			// Zustaendigkeit
			assertThat(gesplitteteKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.hasSize(1);
			assertThat(gesplitteteKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrder(
					zustaendigkeitAttribute.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 1)));

			assertThat(splitPartner.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute()).hasSize(2);
			assertThat(splitPartner.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrder(
					zustaendigkeitAttribute.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.5)),
					zustaendigkeitAttribute.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.5, 1)));

			// Fahrrichtung
			assertThat(gesplitteteKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(gesplitteteKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(splitPartner.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(splitPartner.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				Richtung.IN_RICHTUNG);

			// Geschwindigkeit
			assertThat(gesplitteteKante.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute()).isEqualTo(
				Set.of(geschwindigkeitAttribute));

			assertThat(splitPartner.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute()).isEqualTo(
				Set.of(geschwindigkeitAttribute));

		}

		@Test
		void testFuehreSplitsDurch_kanteEinseitigMitEinemSplitPartnerUndAenderungDerStationierungsrichtung() {
			// arrange
			// Knoten der ursprünglichen, gesplitteten Kante
			Knoten altVon = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
				.id(10L)
				.build();

			Knoten altNach = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(20L)
				.build();

			// Führungsform
			List<FuehrungsformAttribute> fuehrungsformAttributeLinks = List.of(
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.25))
					.belagArt(BelagArt.BETON)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.25, 0.75))
					.belagArt(BelagArt.NATURSTEINPFLASTER)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.75, 1))
					.belagArt(BelagArt.ASPHALT)
					.build());

			// Zustaendigkeit

			List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L).build())
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.75))
					.build(),
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(200L).build())
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.75, 1))
					.build());

			// Fahrtrichtung

			Richtung fahrtrichtungLinks = Richtung.IN_RICHTUNG;

			// Geschwindigkeit

			GeschwindigkeitAttribute geschwindigkeitAttribute = GeschwindigkeitsAttributeTestDataProvider
				.withGrundnetzDefaultwerte()
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_70_KMH)
				.build();
			// gesplitteteKante
			Kante gesplitteteKante = KanteTestDataProvider.fromKnoten(altVon, altNach).quelle(QuellSystem.DLM)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
						.umfeld(Umfeld.GEWERBEGEBIET)
						.strassenNummer(StrassenNummer.of("42"))
						.strassenName(StrassenName.of("Rainbow Avenue"))
						.build())
					.build())
				.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(fuehrungsformAttributeLinks)
					.fuehrungsformAttributeRechts(fuehrungsformAttributeLinks)
					.build())
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(zustaendigkeitAttribute)
					.build())
				.fahrtrichtungAttributGruppe(new FahrtrichtungAttributGruppe(fahrtrichtungLinks, false))
				.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.geschwindigkeitAttribute(List.of(geschwindigkeitAttribute))
					.build())
				.id(1L)
				.build();

			// SplitUpdate
			Knoten neuNach = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 5), QuellSystem.DLM)
				.id(30L)
				.build();

			Kante splitPartner = KanteTestDataProvider.fromKnoten(gesplitteteKante.getNachKnoten(), neuNach)
				.id(2L)
				.quelle(QuellSystem.DLM)
				.build();

			SplitUpdate splitUpdate = new SplitUpdate(gesplitteteKante,
				GeometryTestdataProvider
					.createLineString(new Coordinate[] { new Coordinate(0, 0), new Coordinate(5, 5) }),
				gesplitteteKante.getVonKnoten(),
				neuNach,
				List.of(gesplitteteKante, splitPartner));

			// act
			executeTopologischeUpdatesService.executeSplitUpdate(splitUpdate);

			// assert
			// KantenAttribute
			assertThat(gesplitteteKante.getKantenAttributGruppe().getKantenAttribute().getUmfeld()).isEqualTo(
				Umfeld.GEWERBEGEBIET);
			assertThat(gesplitteteKante.getKantenAttributGruppe().getKantenAttribute().getStrassenNummer()).contains(
				StrassenNummer.of("42"));
			assertThat(gesplitteteKante.getKantenAttributGruppe().getKantenAttribute().getStrassenName()).contains(
				StrassenName.of("Rainbow Avenue"));
			assertThat(splitPartner.getKantenAttributGruppe().getKantenAttribute().getUmfeld()).isEqualTo(
				Umfeld.GEWERBEGEBIET);
			assertThat(splitPartner.getKantenAttributGruppe().getKantenAttribute().getStrassenNummer()).contains(
				StrassenNummer.of("42"));
			assertThat(splitPartner.getKantenAttributGruppe().getKantenAttribute().getStrassenName()).contains(
				StrassenName.of("Rainbow Avenue"));
			// Führungsform
			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.hasSize(2);
			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.containsExactlyInAnyOrder(
					fuehrungsformAttributeLinks.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.5)),
					fuehrungsformAttributeLinks.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.5, 1)));

			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.containsExactlyInAnyOrderElementsOf(
					gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.hasSize(2);
			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.containsExactlyInAnyOrder(
					// Umkehrung der Stationierungsrichtung!
					fuehrungsformAttributeLinks.get(2).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.5)),
					fuehrungsformAttributeLinks.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.5, 1)));

			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.containsExactlyInAnyOrderElementsOf(
					splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

			// Zustaendigkeit
			assertThat(gesplitteteKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.hasSize(1);
			assertThat(gesplitteteKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrder(
					zustaendigkeitAttribute.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 1)));

			assertThat(splitPartner.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute()).hasSize(2);
			assertThat(splitPartner.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrder(
					// Umkehrung der Stationierungsrichtung!
					zustaendigkeitAttribute.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.5)),
					zustaendigkeitAttribute.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.5, 1)));

			// Fahrrichtung
			assertThat(gesplitteteKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(gesplitteteKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				Richtung.IN_RICHTUNG);
			// Umkehrung der Stationierungsrichtung!
			assertThat(splitPartner.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				Richtung.GEGEN_RICHTUNG);
			assertThat(splitPartner.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				Richtung.GEGEN_RICHTUNG);

			// Geschwindigkeit
			assertThat(gesplitteteKante.getGeschwindigkeitAttributGruppe()
				.getGeschwindigkeitAttribute()).containsExactlyInAnyOrderElementsOf(
					List.of(geschwindigkeitAttribute));

			// Umkehrung der Stationierungsrichtung!
			assertThat(splitPartner.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute()).isEqualTo(
				Set.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_70_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_100_KMH)
					.build()));

		}

		@Test
		void testFuehreSplitsDurch_kanteZweiseitigMitEinemSplitPartnerUndAenderungDerStationierungsrichtung() {
			// arrange
			// Knoten der ursprünglichen, gesplitteten Kante
			Knoten altVon = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
				.id(10L)
				.build();

			Knoten altNach = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(20L)
				.build();

			// Führungsform
			List<FuehrungsformAttribute> fuehrungsformAttributeLinks = List.of(
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.25))
					.belagArt(BelagArt.BETON)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.25, 0.75))
					.belagArt(BelagArt.NATURSTEINPFLASTER)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.75, 1))
					.belagArt(BelagArt.ASPHALT)
					.build());

			List<FuehrungsformAttribute> fuehrungsformAttributeRechts = List.of(
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.4))
					.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 0.6))
					.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.6, 1))
					.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_FORST)
					.build());

			// Zustaendigkeit

			List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L).build())
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.75))
					.build(),
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(200L).build())
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.75, 1))
					.build());

			// Fahrtrichtung

			Richtung fahrtrichtungLinks = Richtung.IN_RICHTUNG;
			Richtung fahrtrichtungRechts = Richtung.BEIDE_RICHTUNGEN;

			// Geschwindigkeit

			GeschwindigkeitAttribute geschwindigkeitAttribute = GeschwindigkeitsAttributeTestDataProvider
				.withGrundnetzDefaultwerte()
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_70_KMH)
				.build();
			// gesplitteteKante
			Kante gesplitteteKante = KanteTestDataProvider.fromKnoten(altVon, altNach).quelle(QuellSystem.DLM)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
						.umfeld(Umfeld.GEWERBEGEBIET)
						.build())
					.build())
				.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(fuehrungsformAttributeLinks)
					.fuehrungsformAttributeRechts(fuehrungsformAttributeRechts)
					.isZweiseitig(true)
					.build())
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(zustaendigkeitAttribute)
					.build())
				.fahrtrichtungAttributGruppe(
					new FahrtrichtungAttributGruppe(fahrtrichtungLinks, fahrtrichtungRechts, true))
				.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.geschwindigkeitAttribute(List.of(geschwindigkeitAttribute))
					.build())
				.id(1L)
				.isZweiseitig(true)
				.build();

			// SplitUpdate
			Knoten neuNach = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 5), QuellSystem.DLM)
				.id(30L)
				.build();

			Kante splitPartner = KanteTestDataProvider.fromKnoten(gesplitteteKante.getNachKnoten(), neuNach)
				.id(2L)
				.quelle(QuellSystem.DLM)
				.build();

			SplitUpdate splitUpdate = new SplitUpdate(gesplitteteKante,
				GeometryTestdataProvider
					.createLineString(new Coordinate[] { new Coordinate(0, 0), new Coordinate(5, 5) }),
				gesplitteteKante.getVonKnoten(), neuNach, List.of(gesplitteteKante, splitPartner));

			// act
			executeTopologischeUpdatesService.executeSplitUpdate(splitUpdate);

			// assert
			// KantenAttribute
			assertThat(gesplitteteKante.getKantenAttributGruppe().getKantenAttribute().getUmfeld()).isEqualTo(
				Umfeld.GEWERBEGEBIET);
			assertThat(splitPartner.getKantenAttributGruppe().getKantenAttribute().getUmfeld()).isEqualTo(
				Umfeld.GEWERBEGEBIET);
			// Führungsform
			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.hasSize(2);
			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.containsExactlyInAnyOrder(
					fuehrungsformAttributeLinks.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.5)),
					fuehrungsformAttributeLinks.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.5, 1)));

			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.hasSize(2);
			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.containsExactlyInAnyOrder(
					fuehrungsformAttributeRechts.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.8)),
					fuehrungsformAttributeRechts.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.8, 1)));

			// Umkehrung der Stationierungsrichtung!
			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.hasSize(2);
			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.containsExactlyInAnyOrder(
					fuehrungsformAttributeRechts.get(2).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.8)),
					fuehrungsformAttributeRechts.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.8, 1)));

			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.hasSize(2);
			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.containsExactlyInAnyOrder(
					fuehrungsformAttributeLinks.get(2).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.5)),
					fuehrungsformAttributeLinks.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.5, 1)));

			// Zustaendigkeit
			assertThat(gesplitteteKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.hasSize(1);
			assertThat(gesplitteteKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrder(
					zustaendigkeitAttribute.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 1)));

			assertThat(splitPartner.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute()).hasSize(2);
			assertThat(splitPartner.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrder(
					// Umkehrung der Stationierungsrichtung!
					zustaendigkeitAttribute.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.5)),
					zustaendigkeitAttribute.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.5, 1)));

			// Fahrrichtung
			assertThat(gesplitteteKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(gesplitteteKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				Richtung.BEIDE_RICHTUNGEN);
			// Umkehrung der Stationierungsrichtung!
			assertThat(splitPartner.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				Richtung.BEIDE_RICHTUNGEN);
			assertThat(splitPartner.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				Richtung.GEGEN_RICHTUNG);

			// Geschwindigkeit
			assertThat(gesplitteteKante.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute()).isEqualTo(
				Set.of(geschwindigkeitAttribute));

			// Umkehrung der Stationierungsrichtung!
			assertThat(splitPartner.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute()).isEqualTo(
				Set.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_70_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_100_KMH)
					.build()));

		}

		@Test
		void testFuehreSplitsDurch_kanteEinseitigZwischenZweiSplitPartnernUndKeineAenderungDerStationierungsrichtung() {
			// arrange
			// Knoten der ursprünglichen, gesplitteten Kante
			Knoten altVon = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
				.id(10L)
				.build();

			Knoten altNach = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(15, 15), QuellSystem.DLM)
				.id(20L)
				.build();

			// Führungsform
			List<FuehrungsformAttribute> fuehrungsformAttributeLinks = List.of(
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.25))
					.belagArt(BelagArt.BETON)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.25, 0.75))
					.belagArt(BelagArt.NATURSTEINPFLASTER)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.75, 1))
					.belagArt(BelagArt.ASPHALT)
					.build());

			// Zustaendigkeit

			List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L).build())
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.75))
					.build(),
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(200L).build())
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.75, 1))
					.build());

			// Fahrtrichtung

			Richtung fahrtrichtungLinks = Richtung.IN_RICHTUNG;

			// Geschwindigkeit

			GeschwindigkeitAttribute geschwindigkeitAttribute = GeschwindigkeitsAttributeTestDataProvider
				.withGrundnetzDefaultwerte()
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_70_KMH)
				.build();
			// gesplitteteKante
			Kante gesplitteteKante = KanteTestDataProvider.fromKnoten(altVon, altNach).quelle(QuellSystem.DLM)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
						.umfeld(Umfeld.GEWERBEGEBIET)
						.build())
					.build())
				.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(fuehrungsformAttributeLinks)
					.fuehrungsformAttributeRechts(fuehrungsformAttributeLinks)
					.build())
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(zustaendigkeitAttribute)
					.build())
				.fahrtrichtungAttributGruppe(new FahrtrichtungAttributGruppe(fahrtrichtungLinks, false))
				.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.geschwindigkeitAttribute(List.of(geschwindigkeitAttribute))
					.build())
				.id(1L)
				.build();

			// SplitUpdate
			Knoten neuVon = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 5), QuellSystem.DLM)
				.id(30L)
				.build();
			Knoten neuNach = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
				.id(30L)
				.build();

			Kante splitPartner1 = KanteTestDataProvider.fromKnoten(altVon, neuVon)
				.id(2L)
				.quelle(QuellSystem.DLM)
				.build();
			Kante splitPartner2 = KanteTestDataProvider.fromKnoten(neuNach, altNach)
				.id(3L)
				.quelle(QuellSystem.DLM)
				.build();

			SplitUpdate splitUpdate = new SplitUpdate(gesplitteteKante, GeometryTestdataProvider
				.createLineString(new Coordinate[] { new Coordinate(5, 5), new Coordinate(10, 10) }),
				neuVon, neuNach, List.of(splitPartner1, gesplitteteKante, splitPartner2));

			// act
			executeTopologischeUpdatesService.executeSplitUpdate(splitUpdate);

			// assert
			// KantenAttribute
			assertThat(gesplitteteKante.getKantenAttributGruppe().getKantenAttribute().getUmfeld()).isEqualTo(
				Umfeld.GEWERBEGEBIET);
			assertThat(splitPartner1.getKantenAttributGruppe().getKantenAttribute().getUmfeld()).isEqualTo(
				Umfeld.GEWERBEGEBIET);
			assertThat(splitPartner2.getKantenAttributGruppe().getKantenAttribute().getUmfeld()).isEqualTo(
				Umfeld.GEWERBEGEBIET);

			// Führungsform
			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.hasSize(1);
			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.containsExactlyInAnyOrder(
					fuehrungsformAttributeLinks.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 1)));

			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.containsExactlyInAnyOrderElementsOf(
					gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

			assertThat(splitPartner1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.hasSize(2);
			assertThat(splitPartner1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.containsExactlyInAnyOrder(
					fuehrungsformAttributeLinks.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.75)),
					fuehrungsformAttributeLinks.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.75, 1)));

			assertThat(splitPartner1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.containsExactlyInAnyOrderElementsOf(
					splitPartner1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

			assertThat(splitPartner2.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.hasSize(2);

			assertThat(splitPartner2.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.usingRecursiveFieldByFieldElementComparator()
				.usingComparatorForType(LR_COMPARATOR_WITH_PRECISION, LinearReferenzierterAbschnitt.class)
				.containsExactlyInAnyOrder(
					fuehrungsformAttributeLinks.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.25)),
					fuehrungsformAttributeLinks.get(2).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.25, 1)));

			assertThat(splitPartner2.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.containsExactlyInAnyOrderElementsOf(
					splitPartner2.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

			// Zustaendigkeit
			assertThat(gesplitteteKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.hasSize(1);
			assertThat(gesplitteteKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrder(
					zustaendigkeitAttribute.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 1)));

			assertThat(splitPartner1.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.hasSize(1);
			assertThat(splitPartner1.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrder(
					zustaendigkeitAttribute.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 1)));

			assertThat(splitPartner2.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.hasSize(2);
			assertThat(splitPartner2.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.usingRecursiveFieldByFieldElementComparator()
				.usingComparatorForType(LR_COMPARATOR_WITH_PRECISION, LinearReferenzierterAbschnitt.class)
				.containsExactlyInAnyOrder(
					zustaendigkeitAttribute.get(0).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 0.25)),
					zustaendigkeitAttribute.get(1).withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0.25, 1)));

			// Fahrrichtung
			assertThat(gesplitteteKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(gesplitteteKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(splitPartner1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(splitPartner1.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(splitPartner2.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(splitPartner2.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				Richtung.IN_RICHTUNG);

			// Geschwindigkeit
			assertThat(gesplitteteKante.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute()).isEqualTo(
				Set.of(geschwindigkeitAttribute));

			assertThat(splitPartner1.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute()).isEqualTo(
				Set.of(geschwindigkeitAttribute));

			assertThat(splitPartner2.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute()).isEqualTo(
				Set.of(geschwindigkeitAttribute));
		}

		@Test
		void testFuehreSplitsDurch_teilKanteLiegtAusserhalbDerOriginalGeom_esFindetKeineProjektionstatt() {
			// arrange
			// Knoten der ursprünglichen, gesplitteten Kante
			Knoten altVon = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(514965.56, 5372032.2),
				QuellSystem.DLM)
				.id(10L)
				.build();
			Knoten altNach = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(514986.89, 5371993.17),
				QuellSystem.DLM)
				.id(20L)
				.build();

			// Führungsform
			List<FuehrungsformAttribute> fuehrungsformAttributeLinks = List.of(
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.25))
					.belagArt(BelagArt.BETON)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.25, 0.75))
					.belagArt(BelagArt.NATURSTEINPFLASTER)
					.build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.75, 1))
					.belagArt(BelagArt.ASPHALT)
					.build());

			// Zustaendigkeit

			List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L).build())
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.75))
					.build(),
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(200L).build())
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.75, 1))
					.build());

			// Fahrtrichtung

			Richtung fahrtrichtungLinks = Richtung.IN_RICHTUNG;

			// Geschwindigkeit

			GeschwindigkeitAttribute geschwindigkeitAttribute = GeschwindigkeitsAttributeTestDataProvider
				.withGrundnetzDefaultwerte()
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_70_KMH)
				.build();
			// gesplitteteKante
			Kante gesplitteteKante = KanteTestDataProvider.fromKnoten(altVon, altNach)
				.geometry(GeometryTestdataProvider.createLineString(
					new Coordinate(514965.73, 5372032.2),
					new Coordinate(514973.26, 5372026.45),
					new Coordinate(514978.82, 5372013.75),
					new Coordinate(514986.89, 5371993.17)))
				.quelle(QuellSystem.DLM)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
						.umfeld(Umfeld.GEWERBEGEBIET)
						.build())
					.build())
				.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(fuehrungsformAttributeLinks)
					.fuehrungsformAttributeRechts(fuehrungsformAttributeLinks)
					.build())
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(zustaendigkeitAttribute)
					.build())
				.fahrtrichtungAttributGruppe(new FahrtrichtungAttributGruppe(fahrtrichtungLinks, false))
				.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.geschwindigkeitAttribute(List.of(geschwindigkeitAttribute))
					.build())
				.id(1L)
				.build();

			// SplitUpdate
			Knoten neuVon = altVon;
			Knoten neuNach = altNach;

			Kante splitPartner = KanteTestDataProvider.fromKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(514964.63, 5372032.2), QuellSystem.DLM)
					.id(30L).build(),
				altVon)
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(514964.63, 5372032.2),
					new Coordinate(514965.56, 5372033.23)))
				.id(2L)
				.quelle(QuellSystem.DLM)
				.build();

			SplitUpdate splitUpdate = new SplitUpdate(gesplitteteKante, GeometryTestdataProvider
				.createLineString(
					new Coordinate(514965.53, 5372032.2),
					new Coordinate(514973.26, 5372026.45),
					new Coordinate(514978.82, 5372013.75),
					new Coordinate(514986.89, 5371993.17)),
				neuVon, neuNach, List.of(splitPartner, gesplitteteKante));
			// act
			executeTopologischeUpdatesService.executeSplitUpdate(splitUpdate);

			// assert
			// KantenAttribute
			assertThat(gesplitteteKante.getKantenAttributGruppe().getKantenAttribute().getUmfeld()).isEqualTo(
				Umfeld.GEWERBEGEBIET);
			assertThat(splitPartner.getKantenAttributGruppe().getKantenAttribute().getUmfeld()).isEqualTo(
				Umfeld.UNBEKANNT);

			// Führungsform
			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.hasSize(3);
			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.containsExactlyInAnyOrderElementsOf(fuehrungsformAttributeLinks);

			assertThat(gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.containsExactlyInAnyOrderElementsOf(
					gesplitteteKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.hasSize(1);
			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.containsExactly(
					FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte().linearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 1)).build());

			assertThat(splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.containsExactlyInAnyOrderElementsOf(
					splitPartner.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

			// Zustaendigkeit
			assertThat(gesplitteteKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.hasSize(2);
			assertThat(gesplitteteKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactlyInAnyOrderElementsOf(zustaendigkeitAttribute);

			assertThat(splitPartner.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.hasSize(1);
			assertThat(splitPartner.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
				.containsExactly(
					ZustaendigkeitAttribute.builder().linearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0, 1)).build());

			// Fahrrichtung
			assertThat(gesplitteteKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(gesplitteteKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				Richtung.IN_RICHTUNG);
			assertThat(splitPartner.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				Richtung.UNBEKANNT);
			assertThat(splitPartner.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				Richtung.UNBEKANNT);

			// Geschwindigkeit
			assertThat(gesplitteteKante.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute()).isEqualTo(
				Set.of(geschwindigkeitAttribute));

			assertThat(splitPartner.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute()).isEqualTo(
				Set.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte().build()));

		}
	}

}
