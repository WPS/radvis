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
import static org.assertj.core.api.Assertions.withPrecision;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.AttributProjektionsJobStatistik;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.Attributprojektionsbeschreibung;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KanteDublette;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenMapping;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.MappedKante;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.ProjektionsLaengeZuKurzException;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.ProjektionsLaengenVerhaeltnisException;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

// Explanation
class AttributProjektionsServiceAttributeTest {

	@Mock
	private AttributeProjektionsProtokollService attributeProjektionsProtokollService;
	@Mock
	private KantenMappingRepository kantenMappingRepository;
	private AttributProjektionsService attributProjektionsService;

	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N
		.getGeometryFactory();

	@BeforeEach
	void generalSetup() {
		MockitoAnnotations.openMocks(this);
		attributProjektionsService = new AttributProjektionsService(attributeProjektionsProtokollService,
			kantenMappingRepository);
	}

	@Nested
	class EineRadNETZAufZweiGrundNetz {

		private Kante leereGrundnetzkante1;
		private Kante leereGrundnetzkante2;
		private Kante.KanteBuilder vorgebauteLeereRadnetzKante1;
		private LineString ueberschneidung1;
		private LineString ueberschneidung2;

		@BeforeEach
		void setUpEineRadnetzAufZweiDLM() {
			// Bist auf die Attribute auf Radnetz sind die Kanten für jeden der Tests in dieser Klasse gleich
			leereGrundnetzkante1 = defaultGrundnetzkanteMitLeerenAttributen().id(1L)
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(10, 10),
					new Coordinate(15, 12),
					new Coordinate(30, 15)
				})).build();

			vorgebauteLeereRadnetzKante1 = defaultRadnetzkanteMitLeerenAttributenZumWeiterenBefuellen().id(2L)
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(3, 20),
					new Coordinate(5, 10),
					new Coordinate(15, 10),
					new Coordinate(30, 1)
				}));

			ueberschneidung1 = GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(13, 13),
				new Coordinate(15, 11)
			});

			leereGrundnetzkante2 = defaultGrundnetzkanteMitLeerenAttributen().id(3L)
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(2.8, 18),
					new Coordinate(5.3, 11),
				})).build();

			ueberschneidung2 = GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(3.1, 19),
				new Coordinate(5, 10)
			});
		}

		@Test
		void testProjiziereKantenAttributeAufGrundnetzKanten_AllesOkay_ErstelltAttributprojektionsbeschreibungenMitRichtigenWerten() {
			// arrange
			KantenAttribute radnetzAttribute = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.strassenName(StrassenName.of("Entenhausen")).build();

			Kante radnetzKante = vorgebauteLeereRadnetzKante1
				.kantenAttributGruppe(new KantenAttributGruppe(radnetzAttribute, new HashSet<>(), new HashSet<>()))
				.build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante,
				ueberschneidung1);
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante2, radnetzKante,
				ueberschneidung2);

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			List<Attributprojektionsbeschreibung> attributProjektionsbeschreibungenSortiert = new ArrayList<>(
				attributProjektionsbeschreibungen);
			attributProjektionsbeschreibungenSortiert
				.sort(Comparator.comparingLong(apb -> apb.getZielnetzKante().getId()));

			assertThat(attributProjektionsbeschreibungen).hasSize(2);

			// Projektion auf GrundnetzKante1
			Attributprojektionsbeschreibung attributprojektionsbeschreibung1 = attributProjektionsbeschreibungenSortiert
				.get(0);

			assertThat(attributprojektionsbeschreibung1.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());
			double expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1 = 5
				/ leereGrundnetzkante1.getGeometry().getLength();

			Map<KantenAttribute, Double> potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante1 = attributprojektionsbeschreibung1
				.getPotentiellInkonsistenteProjizierteKantenattributeZuAnteil();

			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante1).hasSize(1);

			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante1)
				.containsKey(radnetzAttribute);

			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante1.get(radnetzAttribute))
				.isEqualTo(expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1, withPrecision(0.002));

			// Projektion auf GrundnetzKante2
			Attributprojektionsbeschreibung attributprojektionsbeschreibung2 = attributProjektionsbeschreibungenSortiert
				.get(1);

			Map<KantenAttribute, Double> potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante2 = attributprojektionsbeschreibung2
				.getPotentiellInkonsistenteProjizierteKantenattributeZuAnteil();

			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante2).hasSize(1);

			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante2)
				.containsKey(radnetzAttribute);

			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante2.get(radnetzAttribute))
				.isEqualTo(1.0);
		}

		@Test
		void testPersistiereMapping() {
			// arrange
			Kante radnetzKante = vorgebauteLeereRadnetzKante1
				.build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante,
				ueberschneidung1);
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante2, radnetzKante,
				ueberschneidung2);

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			List<Attributprojektionsbeschreibung> attributProjektionsbeschreibungenSortiert = new ArrayList<>(
				attributProjektionsbeschreibungen);
			attributProjektionsbeschreibungenSortiert
				.sort(Comparator.comparingLong(apb -> apb.getZielnetzKante().getId()));

			assertThat(attributProjektionsbeschreibungen).hasSize(2);

			// Projektion auf GrundnetzKante1
			Attributprojektionsbeschreibung attributprojektionsbeschreibung1 = attributProjektionsbeschreibungenSortiert
				.get(0);

			assertThat(attributprojektionsbeschreibung1.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());
			double expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1 = 5
				/ leereGrundnetzkante1.getGeometry().getLength();

			// Projektion auf GrundnetzKante2
			Attributprojektionsbeschreibung attributprojektionsbeschreibung2 = attributProjektionsbeschreibungenSortiert
				.get(1);
			assertThat(attributprojektionsbeschreibung2.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante2.getId());

			ArgumentCaptor<KantenMapping> captor = ArgumentCaptor.forClass(KantenMapping.class);
			verify(kantenMappingRepository, times(2)).save(captor.capture());
			List<KantenMapping> allValues = captor.getAllValues();
			assertThat(allValues).size().isEqualTo(2);

			List<MappedKante> mappingAufGrundnetz1 = allValues.stream()
				.filter(val -> val.getGrundnetzKantenId().equals(leereGrundnetzkante1.getId()))
				.findFirst().get().getAbgebildeteKanten();
			assertThat(mappingAufGrundnetz1).size().isEqualTo(1);
			assertThat(mappingAufGrundnetz1.get(0).getLinearReferenzierterAbschnittAufGrundnetzKante().relativeLaenge())
				.isEqualTo(expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1, withPrecision(0.002));
			assertThat(mappingAufGrundnetz1.get(0).isRichtungenVertauscht()).isFalse();
			assertThat(mappingAufGrundnetz1.stream().map(MappedKante::getKanteId))
				.containsExactly(radnetzKante.getId());

			List<MappedKante> mappingAufGrundnetz2 = allValues.stream()
				.filter(val -> val.getGrundnetzKantenId().equals(leereGrundnetzkante2.getId()))
				.findFirst().get().getAbgebildeteKanten();
			assertThat(mappingAufGrundnetz2).size().isEqualTo(1);
			assertThat(mappingAufGrundnetz2.get(0).getLinearReferenzierterAbschnittAufGrundnetzKante().relativeLaenge())
				.isEqualTo(1., withPrecision(0.002));
			assertThat(mappingAufGrundnetz2.get(0).isRichtungenVertauscht()).isFalse();
			assertThat(mappingAufGrundnetz2.stream().map(MappedKante::getKanteId))
				.containsExactly(radnetzKante.getId());
		}

		@Test
		void testHaedischesMappingRadNETZAufDLM() {
			// arrange
			Kante radnetzKante = vorgebauteLeereRadnetzKante1
				.build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante,
				leereGrundnetzkante1.getGeometry());
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante2, radnetzKante,
				leereGrundnetzkante2.getGeometry());

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			List<Attributprojektionsbeschreibung> attributProjektionsbeschreibungenSortiert = new ArrayList<>(
				attributProjektionsbeschreibungen);
			attributProjektionsbeschreibungenSortiert
				.sort(Comparator.comparingLong(apb -> apb.getZielnetzKante().getId()));

			assertThat(attributProjektionsbeschreibungen).hasSize(2);

			// Projektion auf GrundnetzKante1
			Attributprojektionsbeschreibung attributprojektionsbeschreibung1 = attributProjektionsbeschreibungenSortiert
				.get(0);

			assertThat(attributprojektionsbeschreibung1.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());
			double expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1 = 1.0;

			// Projektion auf GrundnetzKante2
			Attributprojektionsbeschreibung attributprojektionsbeschreibung2 = attributProjektionsbeschreibungenSortiert
				.get(1);
			assertThat(attributprojektionsbeschreibung2.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante2.getId());

			ArgumentCaptor<KantenMapping> captor = ArgumentCaptor.forClass(KantenMapping.class);
			verify(kantenMappingRepository, times(2)).save(captor.capture());
			List<KantenMapping> allValues = captor.getAllValues();
			assertThat(allValues).size().isEqualTo(2);

			List<MappedKante> mappingAufGrundnetz1 = allValues.stream()
				.filter(val -> val.getGrundnetzKantenId().equals(leereGrundnetzkante1.getId()))
				.findFirst().get().getAbgebildeteKanten();
			assertThat(mappingAufGrundnetz1).size().isEqualTo(1);
			assertThat(mappingAufGrundnetz1.get(0).getLinearReferenzierterAbschnittAufGrundnetzKante().relativeLaenge())
				.isEqualTo(expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1, withPrecision(0.002));
			assertThat(mappingAufGrundnetz1.get(0).isRichtungenVertauscht()).isFalse();
			assertThat(mappingAufGrundnetz1.stream().map(MappedKante::getKanteId))
				.containsExactly(radnetzKante.getId());

			List<MappedKante> mappingAufGrundnetz2 = allValues.stream()
				.filter(val -> val.getGrundnetzKantenId().equals(leereGrundnetzkante2.getId()))
				.findFirst().get().getAbgebildeteKanten();
			assertThat(mappingAufGrundnetz2).size().isEqualTo(1);
			assertThat(mappingAufGrundnetz2.get(0).getLinearReferenzierterAbschnittAufGrundnetzKante().relativeLaenge())
				.isEqualTo(1., withPrecision(0.002));
			assertThat(mappingAufGrundnetz2.get(0).isRichtungenVertauscht()).isFalse();
			assertThat(mappingAufGrundnetz2.stream().map(MappedKante::getKanteId))
				.containsExactly(radnetzKante.getId());
		}

		@Test
		void testProjiziereNetzklasseAufGrundnetzKanten_AllesOkay_ErstelltAttributprojektionsbeschreibungenMitRichtigenWerten() {
			// arrange

			Kante radnetzKante = vorgebauteLeereRadnetzKante1.kantenAttributGruppe(new KantenAttributGruppe(
				KantenAttribute.builder()
					.build(),
				Set.of(de.wps.radvis.backend.netz.domain.valueObject.Netzklasse.RADNETZ_ALLTAG),
				new HashSet<>())).build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante,
				ueberschneidung1);
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante2, radnetzKante,
				ueberschneidung2);

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			List<Attributprojektionsbeschreibung> attributProjektionsbeschreibungenSortiert = new ArrayList<>(
				attributProjektionsbeschreibungen);
			attributProjektionsbeschreibungenSortiert
				.sort(Comparator.comparingLong(apb -> apb.getZielnetzKante().getId()));

			assertThat(attributProjektionsbeschreibungen).hasSize(2);

			// Projektion auf GrundnetzKante1
			Attributprojektionsbeschreibung attributprojektionsbeschreibung1 = attributProjektionsbeschreibungenSortiert
				.get(0);

			assertThat(attributprojektionsbeschreibung1.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());
			double expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1 = 5
				/ leereGrundnetzkante1.getGeometry().getLength();

			Map<Set<Netzklasse>, List<LinearReferenzierterAbschnitt>> potentiellInkonsistenteProjizierteNetzklassenAufGrundnetzKante1 = attributprojektionsbeschreibung1
				.getPotentiellInkonsistenteProjizierteNetzklassen();

			assertThat(potentiellInkonsistenteProjizierteNetzklassenAufGrundnetzKante1)
				.containsOnlyKeys(Set.of(Netzklasse.RADNETZ_ALLTAG));

			assertThat(
				potentiellInkonsistenteProjizierteNetzklassenAufGrundnetzKante1.get(Set.of(Netzklasse.RADNETZ_ALLTAG))
					.stream().mapToDouble(LinearReferenzierterAbschnitt::relativeLaenge).sum())
						.isCloseTo(expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1,
							Offset.offset(0.01));

			// Projektion auf GrundnetzKante2
			Attributprojektionsbeschreibung attributprojektionsbeschreibung2 = attributProjektionsbeschreibungenSortiert
				.get(1);

			Map<Set<Netzklasse>, List<LinearReferenzierterAbschnitt>> potentiellInkonsistenteProjizierteNetzklassenAufGrundnetzKante2 = attributprojektionsbeschreibung2
				.getPotentiellInkonsistenteProjizierteNetzklassen();

			assertThat(potentiellInkonsistenteProjizierteNetzklassenAufGrundnetzKante2)
				.containsOnlyKeys(Set.of(Netzklasse.RADNETZ_ALLTAG));

			assertThat(
				potentiellInkonsistenteProjizierteNetzklassenAufGrundnetzKante2.get(Set.of(Netzklasse.RADNETZ_ALLTAG))
					.stream().mapToDouble(LinearReferenzierterAbschnitt::relativeLaenge).sum()).isEqualTo(1);
		}

		@Test
		void testProjiziereFuehrungsformAttributeAufGrundnetzKanten_AllesOkay_ErstelltAttributprojektionsbeschreibungenMitRichtigenWerten() {
			List<FuehrungsformAttribute> radnetzFuehrungsformAttribute = List
				.of(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0., 1.)
					.belagArt(BelagArt.BETON)
					.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.UNBEKANNT)
					.bordstein(Bordstein.UNBEKANNT)
					.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
					.parkenTyp(KfzParkenTyp.UNBEKANNT)
					.parkenForm(KfzParkenForm.UNBEKANNT)
					.breite(Laenge.of(4.6))
					.build());

			FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(
				radnetzFuehrungsformAttribute, false);

			Kante radnetzKante = vorgebauteLeereRadnetzKante1.fuehrungsformAttributGruppe(fuehrungsformAttributGruppe)
				.build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante,
				ueberschneidung1);
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante2, radnetzKante,
				ueberschneidung2);

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			List<Attributprojektionsbeschreibung> attributProjektionsbeschreibungenSortiert = new ArrayList<>(
				attributProjektionsbeschreibungen);
			attributProjektionsbeschreibungenSortiert
				.sort(Comparator.comparingLong(apb -> apb.getZielnetzKante().getId()));

			assertThat(attributProjektionsbeschreibungen).hasSize(2);

			// Projektion auf GrundnetzKante1
			Attributprojektionsbeschreibung attributprojektionsbeschreibung1 = attributProjektionsbeschreibungenSortiert
				.get(0);

			assertThat(attributprojektionsbeschreibung1.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());
			double expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1 = 5
				/ leereGrundnetzkante1.getGeometry().getLength();

			List<FuehrungsformAttribute> potentiellInkonsistenteProjizierteFuehrungsformAttribute1 = attributprojektionsbeschreibung1
				.getSeitenbezogeneProjizierteAttribute()
				.get(0)
				.getFuehrungsformAttribute();
			assertThat(potentiellInkonsistenteProjizierteFuehrungsformAttribute1)
				.usingElementComparator((ga1, ga2) -> ga1.sindAttributeGleich(ga2) ? 0 : 1)
				.containsExactly(radnetzFuehrungsformAttribute.get(0));

			assertThat(potentiellInkonsistenteProjizierteFuehrungsformAttribute1)
				.extracting(FuehrungsformAttribute::getLinearReferenzierterAbschnitt)
				.usingComparatorForType(LineareReferenzTestProvider.lenientComparator,
					LinearReferenzierterAbschnitt.class)
				.containsExactly(
					LinearReferenzierterAbschnitt.of(0,
						expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1));

			// Projektion auf GrundnetzKante2
			Attributprojektionsbeschreibung attributprojektionsbeschreibung2 = attributProjektionsbeschreibungenSortiert
				.get(1);
			assertThat(attributprojektionsbeschreibung2.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante2.getId());

			List<FuehrungsformAttribute> potentiellInkonsistenteProjizierteFuehrungsformAttribute2 = attributprojektionsbeschreibung2
				.getSeitenbezogeneProjizierteAttribute()
				.get(0)
				.getFuehrungsformAttribute();
			assertThat(potentiellInkonsistenteProjizierteFuehrungsformAttribute2)
				.usingElementComparator((ga1, ga2) -> ga1.sindAttributeGleich(ga2) ? 0 : 1)
				.containsExactly(radnetzFuehrungsformAttribute.get(0));

			assertThat(potentiellInkonsistenteProjizierteFuehrungsformAttribute2)
				.extracting(FuehrungsformAttribute::getLinearReferenzierterAbschnitt)
				.usingComparatorForType(LineareReferenzTestProvider.lenientComparator,
					LinearReferenzierterAbschnitt.class)
				.containsExactly(LinearReferenzierterAbschnitt.of(0, 1));
		}

		@Test
		void testProjiziereZustaendigkeitAttributeAufGrundnetzKanten_AllesOkay_ErstelltAttributprojektionsbeschreibungenMitRichtigenWerten() {
			List<ZustaendigkeitAttribute> radnetzZustaendigkeitAttribute = List
				.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0., 1.)
					.vereinbarungsKennung(VereinbarungsKennung.of("123"))
					.unterhaltsZustaendiger(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
					.build());

			Kante radnetzKante = vorgebauteLeereRadnetzKante1
				.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(radnetzZustaendigkeitAttribute))
				.build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante,
				ueberschneidung1);
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante2, radnetzKante,
				ueberschneidung2);

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			List<Attributprojektionsbeschreibung> attributProjektionsbeschreibungenSortiert = new ArrayList<>(
				attributProjektionsbeschreibungen);
			attributProjektionsbeschreibungenSortiert
				.sort(Comparator.comparingLong(apb -> apb.getZielnetzKante().getId()));

			assertThat(attributProjektionsbeschreibungen).hasSize(2);

			// Projektion auf GrundnetzKante1
			Attributprojektionsbeschreibung attributprojektionsbeschreibung1 = attributProjektionsbeschreibungenSortiert
				.get(0);

			assertThat(attributprojektionsbeschreibung1.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());
			double expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1 = 5
				/ leereGrundnetzkante1.getGeometry().getLength();

			List<ZustaendigkeitAttribute> potentiellInkonsistenteProjizierteZustaendigkeitAttribute1 = attributprojektionsbeschreibung1
				.getPotentiellInkonsistenteProjizierteZustaendigkeitAttribute();
			assertThat(potentiellInkonsistenteProjizierteZustaendigkeitAttribute1)
				.usingElementComparator((ga1, ga2) -> ga1.sindAttributeGleich(ga2) ? 0 : 1)
				.containsExactly(radnetzZustaendigkeitAttribute.get(0));

			assertThat(potentiellInkonsistenteProjizierteZustaendigkeitAttribute1)
				.extracting(ZustaendigkeitAttribute::getLinearReferenzierterAbschnitt)
				.usingComparatorForType(LineareReferenzTestProvider.lenientComparator,
					LinearReferenzierterAbschnitt.class)
				.containsExactly(
					LinearReferenzierterAbschnitt.of(0,
						expectedFractionForProjizierteKantenAttributeAufGrundnetzKante1));

			// Projektion auf GrundnetzKante2
			Attributprojektionsbeschreibung attributprojektionsbeschreibung2 = attributProjektionsbeschreibungenSortiert
				.get(1);
			assertThat(attributprojektionsbeschreibung2.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante2.getId());

			List<ZustaendigkeitAttribute> potentiellInkonsistenteProjizierteZustaendigkeitAttribute2 = attributprojektionsbeschreibung2
				.getPotentiellInkonsistenteProjizierteZustaendigkeitAttribute();
			assertThat(potentiellInkonsistenteProjizierteZustaendigkeitAttribute2)
				.usingElementComparator((ga1, ga2) -> ga1.sindAttributeGleich(ga2) ? 0 : 1)
				.containsExactly(radnetzZustaendigkeitAttribute.get(0));

			assertThat(potentiellInkonsistenteProjizierteZustaendigkeitAttribute2)
				.extracting(ZustaendigkeitAttribute::getLinearReferenzierterAbschnitt)
				.usingComparatorForType(LineareReferenzTestProvider.lenientComparator,
					LinearReferenzierterAbschnitt.class)
				.containsExactly(LinearReferenzierterAbschnitt.of(0, 1));
		}
	}

	@Nested
	class ZweiRadNETZAufEineGrundNetz {
		private Kante leereGrundnetzkante1;
		private Kante.KanteBuilder vorgebauteLeereRadnetzKante1;
		private Kante.KanteBuilder vorgebauteLeereRadnetzKante2;

		private LineString ueberschneidung1;
		private LineString ueberschneidung2;

		@BeforeEach
		void setUpZweiRadnetzAufEineDLM() {
			// Bist auf die Attribute auf Radnetz sind die Kanten für jeden der Tests in dieser Klasse gleich
			leereGrundnetzkante1 = defaultGrundnetzkanteMitLeerenAttributen().id(1L)
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(10, 10),
					new Coordinate(15, 12),
					new Coordinate(30, 15)
				})).build();

			vorgebauteLeereRadnetzKante1 = defaultRadnetzkanteMitLeerenAttributenZumWeiterenBefuellen().id(2L)
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(3, 20),
					new Coordinate(5, 10),
					new Coordinate(15, 10),
				}));

			ueberschneidung1 = GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(13, 13),
				new Coordinate(15, 11)
			});

			vorgebauteLeereRadnetzKante2 = defaultRadnetzkanteMitLeerenAttributenZumWeiterenBefuellen().id(2L)
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(15, 10),
					new Coordinate(23, 12.5),
					new Coordinate(33, 15),
					new Coordinate(40, 17)
				}));

			ueberschneidung2 = GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(16, 11.5),
				new Coordinate(25, 13),
				new Coordinate(31, 15),
			});

		}

		@Test
		void testProjiziereKantenAttributeAufGrundnetzKante_AttributeIdentisch_ErstelltAttributprojektionsbeschreibungenMitRichtigenWerten() {
			KantenAttribute radnetzAttribute1 = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.strassenName(StrassenName.of("Entenhausen")).build();

			// identischer Inhalt zu radnetzAttribut1
			KantenAttribute radnetzAttribute2 = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.strassenName(StrassenName.of("Entenhausen")).build();

			Kante radnetzKante1 = vorgebauteLeereRadnetzKante1
				.kantenAttributGruppe(new KantenAttributGruppe(radnetzAttribute1, new HashSet<>(), new HashSet<>()))
				.build();

			Kante radnetzKante2 = vorgebauteLeereRadnetzKante2
				.kantenAttributGruppe(new KantenAttributGruppe(radnetzAttribute2, new HashSet<>(), new HashSet<>()))
				.build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante1,
				ueberschneidung1);
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante2,
				ueberschneidung2);

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			assertThat(attributProjektionsbeschreibungen).hasSize(1);
			Attributprojektionsbeschreibung attributProjektionsbeschreibung = attributProjektionsbeschreibungen
				.iterator().next();

			assertThat(attributProjektionsbeschreibung.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());
			double expectedFractionForProjizierteKantenAttributeAufGrundnetzKante = 19. / 20.;

			Map<KantenAttribute, Double> potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante = attributProjektionsbeschreibung
				.getPotentiellInkonsistenteProjizierteKantenattributeZuAnteil();

			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante).hasSize(1);
			// Wir testen hier beide, weil unsere Implementation darauf basiert, dass KantenAttribute Value Objects
			// sind, und deshalb Tests fliegen müssen wenn das nicht mehr der Fall ist
			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante)
				.containsKey(radnetzAttribute1);
			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante)
				.containsKey(radnetzAttribute2);

			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante.get(radnetzAttribute1))
				.isEqualTo(expectedFractionForProjizierteKantenAttributeAufGrundnetzKante, withPrecision(0.02));
		}

		@Test
		void testProjiziereKantenAttributeAufGrundnetzKante_AttributeVerschieden_ErstelltAttributprojektionsbeschreibungenMitRichtigenWerten() {
			KantenAttribute radnetzAttribute1 = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.strassenName(StrassenName.of("Entenhausen")).build();

			KantenAttribute radnetzAttribute2 = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.kommentar(Kommentar.of("123"))
				.strassenName(StrassenName.of("Entenhausen")).build();

			Kante radnetzKante1 = vorgebauteLeereRadnetzKante1
				.kantenAttributGruppe(new KantenAttributGruppe(radnetzAttribute1, new HashSet<>(), new HashSet<>()))
				.build();

			Kante radnetzKante2 = vorgebauteLeereRadnetzKante2
				.kantenAttributGruppe(new KantenAttributGruppe(radnetzAttribute2, new HashSet<>(), new HashSet<>()))
				.build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante1,
				ueberschneidung1);
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante2,
				ueberschneidung2);

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			assertThat(attributProjektionsbeschreibungen).hasSize(1);
			Attributprojektionsbeschreibung attributProjektionsbeschreibung = attributProjektionsbeschreibungen
				.iterator().next();

			assertThat(attributProjektionsbeschreibung.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());

			Map<KantenAttribute, Double> potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante = attributProjektionsbeschreibung
				.getPotentiellInkonsistenteProjizierteKantenattributeZuAnteil();

			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante).hasSize(2);
			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante)
				.containsKey(radnetzAttribute1);
			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante.get(radnetzAttribute1))
				.isEqualTo(5. / 20., withPrecision(0.02));

			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante)
				.containsKey(radnetzAttribute2);
			assertThat(potentiellInkonsistenteProjizierteKantenattributeZuLRAufGrundnetzKante.get(radnetzAttribute2))
				.isEqualTo(14. / 20, withPrecision(0.02));
		}

		@Test
		void testProjiziereNetzklasseAufGrundnetzKante_NetzklassenGleich_ErstelltAttributprojektionsbeschreibungenMitRichtigenWerten() {
			// arrange

			Kante radnetzKante1 = vorgebauteLeereRadnetzKante1.kantenAttributGruppe(new KantenAttributGruppe(
				KantenAttribute.builder()
					.build(),
				Set.of(Netzklasse.RADNETZ_ALLTAG), new HashSet<>()))
				.build();
			Kante radnetzKante2 = vorgebauteLeereRadnetzKante2.kantenAttributGruppe(new KantenAttributGruppe(
				KantenAttribute.builder()
					.build(),
				Set.of(Netzklasse.RADNETZ_ALLTAG), new HashSet<>()))
				.build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante1,
				ueberschneidung1);
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante2,
				ueberschneidung2);

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			assertThat(attributProjektionsbeschreibungen).hasSize(1);
			Attributprojektionsbeschreibung attributProjektionsbeschreibung = attributProjektionsbeschreibungen
				.iterator().next();

			assertThat(attributProjektionsbeschreibung.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());
			double expectedFractionForProjizierteKantenAttributeAufGrundnetzKante = 19. / 20.;

			Map<Set<Netzklasse>, List<LinearReferenzierterAbschnitt>> potentiellInkonsistenteProjizierteNetzklassen = attributProjektionsbeschreibung
				.getPotentiellInkonsistenteProjizierteNetzklassen();

			assertThat(potentiellInkonsistenteProjizierteNetzklassen)
				.containsOnlyKeys(Set.of(Netzklasse.RADNETZ_ALLTAG));

			assertThat(potentiellInkonsistenteProjizierteNetzklassen.get(Set.of(Netzklasse.RADNETZ_ALLTAG)).stream()
				.mapToDouble(LinearReferenzierterAbschnitt::relativeLaenge).sum())
					.isEqualTo(expectedFractionForProjizierteKantenAttributeAufGrundnetzKante, withPrecision(0.02));
		}

		@Test
		void testProjiziereNetzklasseAufGrundnetzKante_NetzklassenVerschieden_ErstelltAttributprojektionsbeschreibungenMitRichtigenWerten() {
			// arrange
			Kante radnetzKante1 = vorgebauteLeereRadnetzKante1.kantenAttributGruppe(new KantenAttributGruppe(
				KantenAttribute.builder()
					.build(),
				Set.of(Netzklasse.RADNETZ_ALLTAG), new HashSet<>()))
				.build();
			Kante radnetzKante2 = vorgebauteLeereRadnetzKante2.kantenAttributGruppe(new KantenAttributGruppe(
				KantenAttribute.builder()
					.build(),
				Set.of(Netzklasse.RADNETZ_ZIELNETZ), new HashSet<>()))
				.build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante1,
				ueberschneidung1);
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante2,
				ueberschneidung2);

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			assertThat(attributProjektionsbeschreibungen).hasSize(1);
			Attributprojektionsbeschreibung attributProjektionsbeschreibung = attributProjektionsbeschreibungen
				.iterator().next();

			assertThat(attributProjektionsbeschreibung.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());

			Map<Set<Netzklasse>, List<LinearReferenzierterAbschnitt>> potentiellInkonsistenteProjizierteNetzklassen = attributProjektionsbeschreibung
				.getPotentiellInkonsistenteProjizierteNetzklassen();

			assertThat(potentiellInkonsistenteProjizierteNetzklassen).hasSize(2);

			assertThat(potentiellInkonsistenteProjizierteNetzklassen)
				.containsOnlyKeys(Set.of(Netzklasse.RADNETZ_ALLTAG), Set.of(Netzklasse.RADNETZ_ZIELNETZ));

			assertThat(potentiellInkonsistenteProjizierteNetzklassen.get(Set.of(Netzklasse.RADNETZ_ALLTAG)).stream()
				.mapToDouble(LinearReferenzierterAbschnitt::relativeLaenge).sum())
					.isEqualTo(5. / 20., withPrecision(0.02));

			assertThat(potentiellInkonsistenteProjizierteNetzklassen.get(Set.of(Netzklasse.RADNETZ_ZIELNETZ)).stream()
				.mapToDouble(LinearReferenzierterAbschnitt::relativeLaenge).sum())
					.isEqualTo(14. / 20., withPrecision(0.02));
		}

		@Test
		void testPersistiereMapping() {
			// arrange
			Kante radnetzKante1 = vorgebauteLeereRadnetzKante1.build();
			Kante radnetzKante2 = vorgebauteLeereRadnetzKante2.build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante1,
				ueberschneidung1);
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante2,
				ueberschneidung2);

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			assertThat(attributProjektionsbeschreibungen).hasSize(1);
			Attributprojektionsbeschreibung attributProjektionsbeschreibung = attributProjektionsbeschreibungen
				.iterator().next();

			assertThat(attributProjektionsbeschreibung.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());

			ArgumentCaptor<KantenMapping> captor = ArgumentCaptor.forClass(KantenMapping.class);
			verify(kantenMappingRepository, times(1)).save(captor.capture());
			List<KantenMapping> allValues = captor.getAllValues();

			assertThat(allValues).size().isEqualTo(1);
			KantenMapping kantenMapping = allValues.get(0);
			List<MappedKante> mappingAufGrundnetz = kantenMapping.getAbgebildeteKanten()
				.stream().sorted(Comparator.comparing(MappedKante::getKanteId)).collect(Collectors.toList());
			assertThat(kantenMapping.getGrundnetzKantenId()).isEqualTo(leereGrundnetzkante1.getId());
			assertThat(mappingAufGrundnetz).size().isEqualTo(2);

			MappedKante mappedKante1 = mappingAufGrundnetz.get(0);
			assertThat(mappedKante1.getLinearReferenzierterAbschnittAufGrundnetzKante().relativeLaenge())
				.isEqualTo(5. / 20., withPrecision(0.02));
			assertThat(mappedKante1.isRichtungenVertauscht()).isFalse();
			assertThat(mappedKante1.getKanteId()).isEqualTo(radnetzKante1.getId());

			MappedKante mappedKante2 = mappingAufGrundnetz.get(1);
			assertThat(mappedKante2.getLinearReferenzierterAbschnittAufGrundnetzKante().relativeLaenge())
				.isEqualTo(14. / 20., withPrecision(0.02));
			assertThat(mappedKante2.isRichtungenVertauscht()).isFalse();
			assertThat(mappedKante2.getKanteId()).isEqualTo(radnetzKante2.getId());
		}

		// Exemplarisch nur für ZustaendigkeitAttribute, da alle gleich behandelt werden
		@Test
		void testProjiziereLinearReferenzierteAttributeAufGrundnetzKante_AllesOkay_ErstelltAttributprojektionsbeschreibungenMitRichtigenWerten() {
			List<ZustaendigkeitAttribute> radnetzZustaendigkeitAttribute1 = List
				.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0., 1.)
					.vereinbarungsKennung(VereinbarungsKennung.of("123"))
					.unterhaltsZustaendiger(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
					.build());

			Kante radnetzKante1 = vorgebauteLeereRadnetzKante1
				.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(radnetzZustaendigkeitAttribute1))
				.build();

			List<ZustaendigkeitAttribute> radnetzZustaendigkeitAttribute2 = List
				.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0., 1 / 5.)
					.vereinbarungsKennung(VereinbarungsKennung.of("456"))
					.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
					.build(),
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(1 / 5., 1.)
						.vereinbarungsKennung(VereinbarungsKennung.of("789"))
						.unterhaltsZustaendiger(
							VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(6L).build())
						.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(7L).build())
						.build());
			Kante radnetzKante2 = vorgebauteLeereRadnetzKante2
				.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(radnetzZustaendigkeitAttribute2))
				.build();

			KanteDublette kanteDublette1 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante1,
				ueberschneidung1);
			KanteDublette kanteDublette2 = erstelleKanteDubletteMock(leereGrundnetzkante1, radnetzKante2,
				ueberschneidung2);

			List<KanteDublette> dubletten = List.of(kanteDublette1, kanteDublette2);

			// act
			Collection<Attributprojektionsbeschreibung> attributProjektionsbeschreibungen = attributProjektionsService
				.projiziereAttributeAufGrundnetzKanten(dubletten, new AttributProjektionsJobStatistik(), "");

			// assert
			assertKeineExceptionsAnProtokollService();

			assertThat(attributProjektionsbeschreibungen).hasSize(1);
			Attributprojektionsbeschreibung attributProjektionsbeschreibung = attributProjektionsbeschreibungen
				.iterator().next();

			assertThat(attributProjektionsbeschreibung.getZielnetzKante().getId())
				.isEqualTo(leereGrundnetzkante1.getId());

			List<ZustaendigkeitAttribute> potentiellInkonsistenteProjizierteZustaendigkeitAttribute1 = attributProjektionsbeschreibung
				.getPotentiellInkonsistenteProjizierteZustaendigkeitAttribute();
			assertThat(potentiellInkonsistenteProjizierteZustaendigkeitAttribute1)
				.usingElementComparator((ga1, ga2) -> ga1.sindAttributeGleich(ga2) ? 0 : 1)
				.containsExactlyInAnyOrder(radnetzZustaendigkeitAttribute1.get(0),
					radnetzZustaendigkeitAttribute2.get(0), radnetzZustaendigkeitAttribute2.get(1));

			assertThat(potentiellInkonsistenteProjizierteZustaendigkeitAttribute1)
				.extracting(ZustaendigkeitAttribute::getLinearReferenzierterAbschnitt)
				.usingComparatorForType(LineareReferenzTestProvider.lenientComparator,
					LinearReferenzierterAbschnitt.class)
				.containsExactlyInAnyOrder(
					LinearReferenzierterAbschnitt.of(0, 0.25),
					LinearReferenzierterAbschnitt.of(0.25 + 1 / 20., 0.5),
					LinearReferenzierterAbschnitt.of(0.5, 1.));
		}
	}

	private void assertKeineExceptionsAnProtokollService() {
		verify(attributeProjektionsProtokollService, never())
			.handle(any(ProjektionsLaengeZuKurzException.class), any());
		verify(attributeProjektionsProtokollService, never())
			.handle(any(ProjektionsLaengenVerhaeltnisException.class), any());
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
		GeschwindigkeitAttribute grundnetzGeschwindigkeitsAttribute = GeschwindigkeitsAttributeTestDataProvider
			.withGrundnetzDefaultwerte().build();

		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte().build();

		return KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(new KantenAttributGruppe(grundnetzAttribute, new HashSet<>(), new HashSet<>()))
			.quelle(QuellSystem.DLM)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().build())
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe
				.builder()
				.geschwindigkeitAttribute(List.of(grundnetzGeschwindigkeitsAttribute))
				.build())
			.fuehrungsformAttributGruppe(fuehrungsformAttributGruppe);
	}

	private Kante.KanteBuilder defaultRadnetzkanteMitLeerenAttributenZumWeiterenBefuellen() {
		KantenAttribute grundnetzAttribute = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
			.build();
		GeschwindigkeitAttribute grundnetzGeschwindigkeitsAttribute = GeschwindigkeitsAttributeTestDataProvider
			.withGrundnetzDefaultwerte().build();
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte().build();

		return KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(new KantenAttributGruppe(grundnetzAttribute, new HashSet<>(), new HashSet<>()))
			.quelle(QuellSystem.DLM)
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder()
					.geschwindigkeitAttribute(List.of(grundnetzGeschwindigkeitsAttribute))
					.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().build())
			.fuehrungsformAttributGruppe(fuehrungsformAttributGruppe);
	}
}
