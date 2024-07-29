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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.AttributProjektionsJobStatistik;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.Attributprojektionsbeschreibung;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KanteDublette;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenMapping;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.ProjektionsLaengeZuKurzException;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.ProjektionsLaengenVerhaeltnisException;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class AttributProjektionsServiceSpecialCasesTest {

	@Mock
	private AttributeProjektionsProtokollService attributeProjektionsProtokollService;
	@Mock
	private KantenMappingRepository kantenMappingRepository;
	private AttributProjektionsService attributProjektionsService;

	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N
		.getGeometryFactory();

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		attributProjektionsService = new AttributProjektionsService(attributeProjektionsProtokollService,
			kantenMappingRepository);
	}

	@Test
	public void testeProjiziereAttributeAufGrundnetzKanten_KommtMitLeerenDublettenKlar() {
		// arrange
		List<KanteDublette> kantenDubletteSet = new ArrayList<>();
		// act
		Collection<Attributprojektionsbeschreibung> result = attributProjektionsService
			.projiziereAttributeAufGrundnetzKanten(kantenDubletteSet, new AttributProjektionsJobStatistik(), "");
		// assert
		assertThat(result).isEmpty();
	}

	@Test
	public void testeProjiziereAttributeAufGrundnetzKanten_RueckprojektionZuKurz_NichtAufAttributprojektionsbeschreibung() {
		// arrange
		List<KanteDublette> dubletten = new ArrayList<>();

		Kante grundnetzKante = defaultGrundnetzkanteMitLeerenAttributen().id(1L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(15, 10),
				new Coordinate(30, 10)
			}))
			.build();

		Kante radnetzKante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ).id(2L)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue().build())
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte().build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider
				.withGrundnetzDefaultwerte().build())
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 11),
				new Coordinate(15, 20),
				new Coordinate(20, 35),
				new Coordinate(30, 0)
			}))
			.build();

		// sehr schräg, zurückprojiziert auf grundnetzKante Länge 1.9
		LineString ueberschneidung = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(10, 10),
			new Coordinate(11.9, 13),
		});

		KanteDublette kanteDubletteNormal = erstelleKanteDubletteMock(grundnetzKante, radnetzKante, ueberschneidung);
		dubletten.add(kanteDubletteNormal);

		// act
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
			.projiziereAttributeAufGrundnetzKanten(dubletten, statistik, "");

		// assert
		assertThat(attributProjektionsbeschreibungen).isEmpty();
		verify(attributeProjektionsProtokollService).handle(any(ProjektionsLaengeZuKurzException.class), any());
		verify(kantenMappingRepository, Mockito.never()).save(any());
		assertThat(statistik.rueckProjektionsLaengeZuKurz).isEqualTo(1);
	}

	@Test
	public void testeProjiziereAttributeAufGrundnetzKanten_RueckprojektionsVerhaeltnisZuGross_NichtAufAttributprojektionsbeschreibung() {
		// arrange
		List<KanteDublette> dubletten = new ArrayList<>();

		Kante grundnetzKante = defaultGrundnetzkanteMitLeerenAttributen().id(1L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(15, 10),
				new Coordinate(30, 10)
			}))
			.build();

		Kante radnetzKante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ).id(2L)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue().build())
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte().build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider
				.withGrundnetzDefaultwerte().build())
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 11),
				new Coordinate(15, 20),
				new Coordinate(20, 35),
				new Coordinate(30, 0)
			}))
			.build();

		// sehr schräg, zurückprojiziert Laenge 5 aber Ueberschneidung hat Laenge ~16
		LineString ueberschneidung = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(10, 10),
			new Coordinate(15, 19),
		});

		KanteDublette kanteDubletteNormal = erstelleKanteDubletteMock(grundnetzKante, radnetzKante, ueberschneidung);
		dubletten.add(kanteDubletteNormal);

		// act
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
			.projiziereAttributeAufGrundnetzKanten(dubletten, statistik, "");

		// assert
		assertThat(attributProjektionsbeschreibungen).isEmpty();
		verify(attributeProjektionsProtokollService).handle(any(ProjektionsLaengenVerhaeltnisException.class), any());
		verify(kantenMappingRepository, Mockito.never()).save(any());
		assertThat(statistik.rueckProjektionsLaengenVerhaeltnisZuUnterschiedlich).isEqualTo(1);
	}

	@Test
	void testProjiziereAttributeAufGrundnetzKanten_OSMLinestringReversed_GibtSelbesErgebnis() {
		// Arrange
		List<KanteDublette> dubletten = new ArrayList<>();

		Kante grundnetzKante = defaultGrundnetzkanteMitLeerenAttributen().id(1L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(15, 10),
				new Coordinate(30, 10)
			}))
			.build();

		KantenAttribute radnetzAttribute = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
			.strassenName(StrassenName.of("Entenhausen")).build();
		GeschwindigkeitAttribute radnetzGeschwindigkeitsattribute = GeschwindigkeitAttribute.builder()
			.ortslage(KantenOrtslage.AUSSERORTS)
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH)
			.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
				Hoechstgeschwindigkeit.MAX_9_KMH)
			.build();

		List<FuehrungsformAttribute> radnetzFuehrungsformAttribute = List
			.of(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0., 1.)
				.belagArt(BelagArt.BETON)
				.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.UNBEKANNT)
				.bordstein(Bordstein.UNBEKANNT)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
				.parkenTyp(KfzParkenTyp.PARKEN_VERBOTEN)
				.parkenForm(KfzParkenForm.PARKBUCHTEN)
				.breite(Laenge.of(4.6))
				.build());

		List<ZustaendigkeitAttribute> radnetzZustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0., 1.)
				.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
				.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
				.vereinbarungsKennung(VereinbarungsKennung.of("123")).build());

		Kante radnetzKante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ).id(2L)
			.kantenAttributGruppe(new KantenAttributGruppe(radnetzAttribute, new HashSet<>(), new HashSet<>()))
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe
				.builder()
				.geschwindigkeitAttribute(List.of(radnetzGeschwindigkeitsattribute))
				.build())
			.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(radnetzFuehrungsformAttribute, false))
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(radnetzZustaendigkeitAttribute))
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(3, 15),
				new Coordinate(5, 15),
				new Coordinate(15, 15),
				new Coordinate(30, 0)
			}))
			.build();

		LineString ueberschneidung = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(10, 10),
			new Coordinate(13, 10),
			new Coordinate(15, 10)
		});
		LineString ueberschneidungReversed = ueberschneidung.reverse();

		KanteDublette kanteDubletteNormal = erstelleKanteDubletteMock(grundnetzKante, radnetzKante, ueberschneidung);
		dubletten.add(kanteDubletteNormal);

		KanteDublette kanteDubletteUeberschneidungReversed = erstelleKanteDubletteMock(grundnetzKante, radnetzKante,
			ueberschneidungReversed);
		List<KanteDublette> reversedDublette = List.of(kanteDubletteUeberschneidungReversed);

		// act
		Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungenNormal = attributProjektionsService
			.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");
		Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungenUeberschneidungReversed = attributProjektionsService
			.projiziereAttributeAufGrundnetzKanten(reversedDublette, new AttributProjektionsJobStatistik(), "");

		// assert beschreibungNormal
		assertThat(attributProjektionsbeschreibungenNormal).size().isEqualTo(1);

		// assert beschreibungUeberschneidungReversed
		assertThat(attributProjektionsbeschreibungenUeberschneidungReversed).size().isEqualTo(1);
		Attributprojektionsbeschreibung beschreibungUeberschneidungReversed = attributProjektionsbeschreibungenUeberschneidungReversed
			.iterator().next();
		assertThat(beschreibungUeberschneidungReversed.getZielnetzKante()).isEqualTo(grundnetzKante);
		assertThat(beschreibungUeberschneidungReversed.getProjizierteKanteIds()).size().isEqualTo(1);

	}

	@Test
	void testProjiziereAttributeAufGrundnetzKanten_GrundnetzkanteUndQuellnetzkanteAndersrumOrientiert() {
		// Arrange
		List<KanteDublette> dubletten = new ArrayList<>();

		Kante grundnetzKante = defaultGrundnetzkanteMitLeerenAttributen().id(1L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(50, 100),
				new Coordinate(200, 100),
				new Coordinate(300, 100) // 250m lang
			}))
			.build();

		List<ZustaendigkeitAttribute> radnetzZustaendigkeitAttribute = List
			.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0., 0.4)
				.vereinbarungsKennung(VereinbarungsKennung.of("Erste zwei Fünftel"))
				.build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 0.6)
					.vereinbarungsKennung(VereinbarungsKennung.of("Drittes Fünftel"))
					.build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("Letzte zwei Fünftel"))
					.build());

		Kante radnetzKante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ).id(2L)
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(radnetzZustaendigkeitAttribute))
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(200, 200),
				new Coordinate(200, 100),
				new Coordinate(50, 100) // 250m lang
			}))
			.build();

		LineString ueberschneidung = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(50, 100),
			new Coordinate(125, 100),
			new Coordinate(200, 100) // 150m lang, 3/5 Überschneidung
		}); // Es schneiden sich die ersten drei Fünftel der Grundnetzkante mit den letzten drei Fünftel der
		// Quellnetzkante

		KanteDublette kanteDubletteNormal = erstelleKanteDubletteMock(grundnetzKante, radnetzKante, ueberschneidung);
		dubletten.add(kanteDubletteNormal);

		// act
		Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungenNormal = attributProjektionsService
			.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

		// assert beschreibungNormal
		assertThat(attributProjektionsbeschreibungenNormal.size()).isEqualTo(1);
		List<ZustaendigkeitAttribute> zustaendigkeitProjiziert = attributProjektionsbeschreibungenNormal
			.iterator().next()
			.getPotentiellInkonsistenteProjizierteZustaendigkeitAttribute();
		zustaendigkeitProjiziert
			.sort(Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst));

		assertThat(zustaendigkeitProjiziert).size().isEqualTo(2);
		assertThat(zustaendigkeitProjiziert.get(0).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0., 0.4));
		assertThat(zustaendigkeitProjiziert.get(1).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.4, 0.6));
		assertThat(zustaendigkeitProjiziert.get(0).getVereinbarungsKennung())
			.contains(VereinbarungsKennung.of("Letzte zwei Fünftel"));
		assertThat(zustaendigkeitProjiziert.get(1).getVereinbarungsKennung())
			.contains(VereinbarungsKennung.of("Drittes Fünftel"));

		ArgumentCaptor<KantenMapping> captor = ArgumentCaptor.forClass(KantenMapping.class);
		verify(kantenMappingRepository, times(1)).save(captor.capture());
		List<KantenMapping> allValues = captor.getAllValues();
		assertThat(allValues).size().isEqualTo(1);
		KantenMapping kantenMapping = allValues.get(0);

		assertThat(kantenMapping.getAbgebildeteKanten()).size().isEqualTo(1);
		assertThat(kantenMapping.getAbgebildeteKanten().get(0).isRichtungenVertauscht()).isTrue();
	}

	private KanteDublette erstelleKanteDubletteMock(Kante fuehrend, Kante untergeordnet,
		LineString ueberschneidungsLinestring) {
		KanteDublette dublette = mock(KanteDublette.class);
		when(dublette.getZielnetzKante()).thenReturn(fuehrend);
		when(dublette.getQuellnetzKante()).thenReturn(untergeordnet);
		when(dublette.getZielnetzUeberschneidung()).thenReturn(ueberschneidungsLinestring);

		return dublette;
	}

	private Kante.KanteBuilder defaultGrundnetzkanteMitLeerenAttributen() {
		KantenAttribute grundnetzAttribute = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
			.build();
		GeschwindigkeitAttributGruppe grundnetzGeschwindigkeitsAttributGruppe = GeschwindigkeitsAttributeTestDataProvider
			.gruppeWithGrundnetzDefaultwerte().build();
		FuehrungsformAttributGruppe grundnetzFuehrungsformAttribute = FuehrungsformAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte().build();

		return KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(new KantenAttributGruppe(grundnetzAttribute, new HashSet<>(), new HashSet<>()))
			.quelle(QuellSystem.DLM)
			.geschwindigkeitAttributGruppe(grundnetzGeschwindigkeitsAttributGruppe)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().build())
			.fuehrungsformAttributGruppe(grundnetzFuehrungsformAttribute);
	}

}
