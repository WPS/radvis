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

package de.wps.radvis.backend.netz.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.event.KanteGeometrieChangedEvent;
import de.wps.radvis.backend.netz.domain.event.RadNetzZugehoerigkeitChangedEvent;
import de.wps.radvis.backend.netz.domain.valueObject.Absenkung;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Beschilderung;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;

class KanteTest {

	private Laenge minimaleSegmentLaenge = Laenge.of(1.0);

	@BeforeEach
	void beforeEach() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void getKnoten() {
		// arrange
		Coordinate vonKoordinate = new Coordinate(10, 10);
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(vonKoordinate, QuellSystem.LGL).build();
		Coordinate nachKoordinate = new Coordinate(20, 20);
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(nachKoordinate, QuellSystem.LGL).build();

		LineString geometry = getGeometryFactory()
			.createLineString(new Coordinate[] { vonKoordinate, nachKoordinate });

		Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.LGL).vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(geometry).build();

		// act
		Knoten vonResult = kante.getVonKnoten();
		Knoten nachResult = kante.getNachKnoten();

		// assert
		assertEquals(vonKnoten, vonResult);
		assertEquals(nachKnoten, nachResult);
	}

	@Test
	public void getGeometry() {
		// arrange
		Coordinate vonKoordinate = new Coordinate(10, 10);
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(vonKoordinate, QuellSystem.LGL).build();
		Coordinate nachKoordinate = new Coordinate(20, 20);
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(nachKoordinate, QuellSystem.LGL).build();

		LineString geometry = getGeometryFactory()
			.createLineString(new Coordinate[] { vonKoordinate, nachKoordinate });

		Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.LGL).vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(geometry).build();

		// act
		LineString result = kante.getGeometry();

		// assert
		assertEquals(geometry, result);
	}

	@Test
	public void getQuelle() {
		// arrange
		QuellSystem quelle = QuellSystem.LGL;
		Coordinate vonKoordinate = new Coordinate(10, 10);
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(vonKoordinate, QuellSystem.LGL).build();
		Coordinate nachKoordinate = new Coordinate(20, 20);
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(nachKoordinate, QuellSystem.LGL).build();

		LineString geometry = getGeometryFactory()
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });

		Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.LGL).vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(geometry).build();
		// act
		QuellSystem result = kante.getQuelle();

		// assert
		assertEquals(quelle, result);
	}

	@Test
	public void testKante_geometryKorrekt_KanteWirdErstellt() {
		// arrange
		Coordinate vonKoordinate = new Coordinate(10, 10);
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(vonKoordinate, QuellSystem.LGL).build();
		Coordinate nachKoordinate = new Coordinate(20, 20);
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(nachKoordinate, QuellSystem.LGL).build();

		LineString geometry = getGeometryFactory()
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });

		// act
		Kante kante = KanteTestDataProvider.fromKnotenUndQuelle(vonKnoten, nachKnoten, QuellSystem.DLM)
			.geometry(geometry).build();
		// assert
		assertNotNull(kante);
	}

	@Test
	public void testKante_KnotenGeometrienIdentisch_wirftRequireException() {
		// arrange
		Coordinate vonKoordinate = new Coordinate(10, 10);
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(vonKoordinate, QuellSystem.LGL).build();
		Coordinate nachKoordinate = new Coordinate(10, 10);
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(nachKoordinate, QuellSystem.LGL).build();

		LineString geometry = getGeometryFactory()
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });

		// act & assert
		assertThatExceptionOfType(RequireViolation.class)
			.isThrownBy(() -> KanteTestDataProvider.fromKnotenUndQuelle(vonKnoten, nachKnoten, QuellSystem.DLM)
				.geometry(geometry).build());
	}

	@Test
	public void testKante_QuelleDLMDlmIdNull_wirftRequireException() {
		// act & assert
		assertThatExceptionOfType(RequireViolation.class)
			.isThrownBy(() -> KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM).dlmId(null).build());
	}

	@Test
	public void testKante_QuelleNichtDLMDlmIdNichtNull_wirftRequireException() {
		// act & assert
		assertThatExceptionOfType(RequireViolation.class)
			.isThrownBy(
				() -> KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).dlmId(DlmId.of("123"))
					.build());
	}

	@Test
	public void isAufOsmGematched_OsmKnotenIdsVorhanden_liefertTrue() {
		// arrange

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 20, QuellSystem.LGL)
			.build();

		// act
		boolean result = kante.isAbgebildet();

		// assert
		assertTrue(result);

	}

	@Test
	public void isAufOsmGematched_ohneAufDlmAbgebildeteGeometry_liefertFalse() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ)
			.aufDlmAbgebildeteGeometry(null)
			.build();

		// act
		boolean result = kante.isAbgebildet();

		// assert
		assertFalse(result);

	}

	@Test
	public void getLaengeBerechnet() {
		// arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(100, 0, 121, 0, QuellSystem.LGL)
			.build();

		// act + assert
		assertThat(kante.getLaengeBerechnet().getValue()).isEqualTo(21.0);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void aendereGeometrie_MitGeaenderterGeometrie_AendertGeometrieUndLaengeUndOsmMatching() {
		// Arrange
		LineString osmGeometry = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 11), new Coordinate(21, 21) });
		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppe.builder().build();
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 20, QuellSystem.RadVis)
			.aufDlmAbgebildeteGeometry(osmGeometry).kantenAttributGruppe(kantenAttributGruppe).id(42L).build();

		LineString newGeometry = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(10, 10), new Coordinate(12, 1), new Coordinate(18, 5),
				new Coordinate(20, 20) });
		final var domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);

		// Act
		kante.aendereGeometrieManuell(newGeometry);

		// Assert
		assertThat(kante.getGeometry()).isEqualTo(newGeometry);
		assertThat(kante.getKantenLaengeInCm()).isEqualTo((int) (newGeometry.getLength() * 100));
		assertThat(kante.getZugehoerigeDlmGeometrie()).isNull();

		ArgumentCaptor<KanteGeometrieChangedEvent> captor = ArgumentCaptor.forClass(KanteGeometrieChangedEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
		assertThat(captor.getValue().getKanteId()).isEqualTo(42L);
		domainPublisherMock.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void aendereGeometrie_MitUnveraenderterGeometrie_AendertNichts() {
		// Arrange
		LineString osmGeometry = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppe.builder().build();
		LineString geometry = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).geometry(geometry)
			.aufDlmAbgebildeteGeometry(osmGeometry).kantenAttributGruppe(kantenAttributGruppe).build();
		LineString newGeometry = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		final var domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);

		// Act
		kante.aendereGeometrieManuell(newGeometry);

		// Assert
		assertThat(kante.getGeometry()).isEqualTo(geometry);
		assertThat(kante.getKantenLaengeInCm()).isEqualTo((int) (geometry.getLength() * 100));
		domainPublisherMock.verifyNoInteractions();
		domainPublisherMock.close();
	}

	@Test
	public void aendereGeometrie_BeiUnerlaubterQuelle_FührtZuFehler() {
		// Arrange
		LineString osmGeometry = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppe.builder().build();
		LineString geometry = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(geometry).quelle(QuellSystem.DLM)
			.aufDlmAbgebildeteGeometry(osmGeometry).kantenAttributGruppe(kantenAttributGruppe).build();
		LineString newGeometry = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(12, 1), new Coordinate(32, 15) });
		final var domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);

		// Act & Assert
		assertThrows(RequireViolation.class, () -> kante.aendereGeometrieManuell(newGeometry));
		domainPublisherMock.verifyNoInteractions();
		domainPublisherMock.close();
	}

	@Test
	public void getHoechsteNetzklasse() {
		// Arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppe
				.builder()
				.id(123L)
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KREISNETZ_ALLTAG))
				.build())
			.build();
		final var domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);

		// Act + Assert
		Optional<Set<Netzklasse>> netzklassen = kante.getHoechsteNetzklassen();
		assertThat(netzklassen).isPresent();
		assertThat(netzklassen.get()).containsExactlyInAnyOrder(Netzklasse.RADNETZ_ALLTAG);

		kante.getKantenAttributGruppe().update(Set.of(Netzklasse.KREISNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG),
			kante.getKantenAttributGruppe().getIstStandards(), kante.getKantenAttributGruppe().getKantenAttribute());
		netzklassen = kante.getHoechsteNetzklassen();
		assertThat(netzklassen).isPresent();
		assertThat(netzklassen.get()).containsExactlyInAnyOrder(Netzklasse.KREISNETZ_ALLTAG);

		kante.getKantenAttributGruppe().update(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG),
			kante.getKantenAttributGruppe().getIstStandards(), kante.getKantenAttributGruppe().getKantenAttribute());
		netzklassen = kante.getHoechsteNetzklassen();
		assertThat(netzklassen).isPresent();
		assertThat(netzklassen.get()).containsExactlyInAnyOrder(Netzklasse.KOMMUNALNETZ_ALLTAG);

		kante.getKantenAttributGruppe().update(Collections.emptySet(),
			kante.getKantenAttributGruppe().getIstStandards(), kante.getKantenAttributGruppe().getKantenAttribute());
		netzklassen = kante.getHoechsteNetzklassen();
		assertThat(netzklassen).isEmpty();

		kante.getKantenAttributGruppe().update(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT),
			kante.getKantenAttributGruppe().getIstStandards(), kante.getKantenAttributGruppe().getKantenAttribute());
		netzklassen = kante.getHoechsteNetzklassen();
		assertThat(netzklassen).isPresent();
		assertThat(netzklassen.get()).containsExactlyInAnyOrder(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT);

		ArgumentCaptor<RadNetzZugehoerigkeitChangedEvent> captor = ArgumentCaptor.forClass(
			RadNetzZugehoerigkeitChangedEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()), times(2));
		assertThat(captor.getValue().getKantenAttributGruppeId()).isEqualTo(kante.getKantenAttributGruppe().getId());
		domainPublisherMock.close();
	}

	@Test
	void testeGetHoechsteNetzklasse_richtigeReihenfolge() {
		// arrange + act
		Set<Netzklasse> netzklassen = new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT,
			Netzklasse.RADNETZ_ZIELNETZ,
			Netzklasse.RADSCHNELLVERBINDUNG, Netzklasse.RADVORRANGROUTEN,
			Netzklasse.KREISNETZ_FREIZEIT, Netzklasse.KREISNETZ_ALLTAG,
			Netzklasse.KOMMUNALNETZ_FREIZEIT, Netzklasse.KOMMUNALNETZ_ALLTAG));
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
			.build();

		Optional<Set<Netzklasse>> RADNETZ_ALLTAG = kante.getHoechsteNetzklassen();
		netzklassen.remove(Netzklasse.RADNETZ_ALLTAG);
		kante.ueberschreibeNetzklassen(netzklassen);
		Optional<Set<Netzklasse>> RADNETZ_FREIZEIT = kante.getHoechsteNetzklassen();
		netzklassen.remove(Netzklasse.RADNETZ_FREIZEIT);
		kante.ueberschreibeNetzklassen(netzklassen);
		Optional<Set<Netzklasse>> RADNETZ_ZIELNETZ = kante.getHoechsteNetzklassen();
		netzklassen.remove(Netzklasse.RADNETZ_ZIELNETZ);
		kante.ueberschreibeNetzklassen(netzklassen);
		Optional<Set<Netzklasse>> RADSCHNELLVERBINDUNG = kante.getHoechsteNetzklassen();
		netzklassen.remove(Netzklasse.RADSCHNELLVERBINDUNG);
		kante.ueberschreibeNetzklassen(netzklassen);
		Optional<Set<Netzklasse>> RADVORRANGROUTEN = kante.getHoechsteNetzklassen();
		netzklassen.remove(Netzklasse.RADVORRANGROUTEN);
		kante.ueberschreibeNetzklassen(netzklassen);

		// Alltag und Freizeit haben die gleiche Priorität, deswegen ist es nicht deterministisch,
		// wer als erstes kommt in der Reihenfolge, wenn beide im Set sind
		netzklassen.remove(Netzklasse.KREISNETZ_ALLTAG);
		kante.ueberschreibeNetzklassen(netzklassen);
		Optional<Set<Netzklasse>> KREISNETZ_FREIZEIT = kante.getHoechsteNetzklassen();
		netzklassen.remove(Netzklasse.KREISNETZ_FREIZEIT);
		netzklassen.add(Netzklasse.KREISNETZ_ALLTAG);
		kante.ueberschreibeNetzklassen(netzklassen);
		Optional<Set<Netzklasse>> KREISNETZ_ALLTAG = kante.getHoechsteNetzklassen();
		netzklassen.remove(Netzklasse.KREISNETZ_ALLTAG);
		kante.ueberschreibeNetzklassen(netzklassen);

		netzklassen.remove(Netzklasse.KOMMUNALNETZ_ALLTAG);
		kante.ueberschreibeNetzklassen(netzklassen);
		Optional<Set<Netzklasse>> KOMMUNALNETZ_FREIZEIT = kante.getHoechsteNetzklassen();
		netzklassen.remove(Netzklasse.KOMMUNALNETZ_FREIZEIT);
		netzklassen.add(Netzklasse.KOMMUNALNETZ_ALLTAG);
		kante.ueberschreibeNetzklassen(netzklassen);
		Optional<Set<Netzklasse>> KOMMUNALNETZ_ALLTAG = kante.getHoechsteNetzklassen();

		// assert
		assertThat(RADNETZ_ALLTAG).contains(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT));
		assertThat(RADNETZ_FREIZEIT).contains(Set.of(Netzklasse.RADNETZ_FREIZEIT));
		assertThat(RADNETZ_ZIELNETZ).contains(Set.of(Netzklasse.RADNETZ_ZIELNETZ));
		assertThat(RADSCHNELLVERBINDUNG).contains(Set.of(Netzklasse.RADSCHNELLVERBINDUNG));
		assertThat(RADVORRANGROUTEN).contains(Set.of(Netzklasse.RADVORRANGROUTEN));
		assertThat(KREISNETZ_FREIZEIT).contains(Set.of(Netzklasse.KREISNETZ_FREIZEIT));
		assertThat(KREISNETZ_ALLTAG).contains(Set.of(Netzklasse.KREISNETZ_ALLTAG));
		assertThat(KOMMUNALNETZ_FREIZEIT).contains(Set.of(Netzklasse.KOMMUNALNETZ_FREIZEIT));
		assertThat(KOMMUNALNETZ_ALLTAG).contains(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG));
	}

	@Test
	public void hasNetzklasse() {
		// Arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppe
				.builder()
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KREISNETZ_ALLTAG))
				.build())
			.build();

		// Assert
		assertTrue(kante.hasNetzklasse(Netzklasse.KOMMUNALNETZ_ALLTAG));
		assertTrue(kante.hasNetzklasse(Netzklasse.RADNETZ_ALLTAG));
		assertTrue(kante.hasNetzklasse(Netzklasse.KREISNETZ_ALLTAG));
		assertFalse(kante.hasNetzklasse(Netzklasse.RADSCHNELLVERBINDUNG));
	}

	@Test
	public void KanteKonstruktorMitKnoten_erstelltKante() {
		// arrange
		Coordinate coordinate1 = new Coordinate(0, 0);
		Knoten knoten = KnotenTestDataProvider.withCoordinateAndQuelle(coordinate1, QuellSystem.DLM).id(1L)
			.version(2L).build();

		Coordinate coordinate2 = new Coordinate(0, 1);
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(coordinate2, QuellSystem.DLM).id(2L)
			.version(2L).build();

		// act
		Kante kante = new Kante(knoten, knoten2);

		// assert
		assertThat((kante.getGeometry()).getStartPoint().getCoordinate()).isEqualTo(coordinate1);
		assertThat((kante.getGeometry()).getEndPoint().getCoordinate()).isEqualTo(coordinate2);
		assertThat(kante.getQuelle()).isEqualTo(QuellSystem.RadVis);
	}

	@Test
	public void updateVerlauf_isZweiseitig_beideGleich() {
		LineString verlaufLinks = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		LineString verlaufRechts = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig().build();
		kante.updateVerlauf(verlaufLinks, verlaufRechts);

		assertThat(kante.getVerlaufLinks()).contains(verlaufLinks);
		assertThat(kante.getVerlaufRechts()).contains(verlaufRechts);
	}

	@Test
	public void updateVerlauf_isZweiseitig_beideNull() {
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig().build();
		kante.updateVerlauf(null, null);

		assertThat(kante.getVerlaufLinks()).isEmpty();
		assertThat(kante.getVerlaufRechts()).isEmpty();
	}

	@Test
	public void updateVerlauf_isZweiseitig_unterschiedlich() {
		LineString verlaufLinks = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		LineString verlaufRechts = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(100, 0) });
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig().build();
		kante.updateVerlauf(verlaufLinks, verlaufRechts);

		assertThat(kante.getVerlaufLinks()).contains(verlaufLinks);
		assertThat(kante.getVerlaufRechts()).contains(verlaufRechts);
	}

	@Test
	public void updateVerlauf_isNotZweiseitig_unterschiedlich() {
		LineString verlaufLinks = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		LineString verlaufRechts = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(100, 0) });
		Kante kante = KanteTestDataProvider.withDefaultValues().isZweiseitig(false).build();
		assertThrows(RequireViolation.class, () -> kante.updateVerlauf(verlaufLinks, verlaufRechts));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void changeSeitenbezug_toFalse() {
		// Arrange
		LineString verlaufLinks = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		LineString verlaufRechts = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig().verlaufLinks(verlaufLinks)
			.verlaufRechts(verlaufRechts)
			.build();

		// Act
		kante.changeSeitenbezug(false);

		// Assert
		assertThat(kante.isZweiseitig()).isFalse();
		assertThat(kante.getVerlaufLinks().get()).isEqualTo(verlaufLinks);
		assertThat(kante.getVerlaufRechts().get()).isEqualTo(verlaufLinks);
		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void changeSeitenbezug_toTrue() {
		// Arrange
		LineString verlaufLinks = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		LineString verlaufRechts = getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(11, 0), new Coordinate(31, 0) });
		Kante kante = KanteTestDataProvider.withDefaultValues().isZweiseitig(false).verlaufLinks(verlaufLinks)
			.verlaufRechts(verlaufRechts).build();

		// Act
		kante.changeSeitenbezug(true);

		// Assert
		assertThat(kante.isZweiseitig()).isTrue();
		assertThat(kante.getVerlaufLinks().get()).isEqualTo(verlaufLinks);
		assertThat(kante.getVerlaufRechts().get()).isEqualTo(verlaufRechts);
		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante.getFahrtrichtungAttributGruppe().isZweiseitig()).isTrue();
	}

	@Test
	public void kanteLaenge() {
		// Arrange
		LineString lineString = getGeometryFactory().createLineString(
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(10, 10) });
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.geometry(lineString)
			.build();

		// Act
		int result = kante.getKantenLaengeInCm();

		// Assert
		assertThat(result).isEqualTo(Math.round(lineString.getLength() * 100));
	}

	@Test
	public void updateGeometryTest_kleineVerschiebung_OhneProbleme() {
		LineString ursprungsLinestring = getGeometryFactory().createLineString(
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(5, 1), new Coordinate(10, 1) });

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(1, 1, 10, 1, QuellSystem.DLM)
			.geometry(ursprungsLinestring)
			.build();

		Knoten vonKnoten = kante.getVonKnoten();
		Knoten nachKnoten = kante.getNachKnoten();

		LineString neuerLinestring = getGeometryFactory().createLineString(
			new Coordinate[] { new Coordinate(0.8, 1), new Coordinate(5, 2), new Coordinate(10, 2) });

		kante.updateDLMGeometry(neuerLinestring);

		// keine TopologieÄnderung
		assertThat(vonKnoten).isEqualTo(kante.getVonKnoten());
		assertThat(nachKnoten).isEqualTo(kante.getNachKnoten());
		assertThat(vonKnoten.getKoordinate()).isEqualTo(neuerLinestring.getCoordinates()[0]);
		assertThat(nachKnoten.getKoordinate())
			.isEqualTo(neuerLinestring.getCoordinates()[neuerLinestring.getCoordinates().length - 1]);

		assertThat(kante.getGeometry().equals(neuerLinestring)).isTrue();
		assertThat(kante.getZugehoerigeDlmGeometrie().equals(neuerLinestring)).isTrue();
		assertThat(kante.getKantenLaengeInCm()).isEqualTo(932);
	}

	@Test
	public void updateGeometryTest_großeVerschiebungMitTopologischerKonsequenz_SchafftProbleme() {
		LineString ursprungsLinestring = getGeometryFactory().createLineString(
			new Coordinate[] { new Coordinate(1, 1), new Coordinate(5, 1), new Coordinate(10, 1) });

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(1, 1, 10, 1, QuellSystem.DLM)
			.geometry(ursprungsLinestring)
			.build();

		LineString neuerLinestring = getGeometryFactory().createLineString(
			new Coordinate[] { new Coordinate(0.5, 3), new Coordinate(5, 3), new Coordinate(10.5, 3) });

		assertThatThrownBy(() -> {
			kante.updateDLMGeometry(neuerLinestring);
		}).isInstanceOf(RequireViolation.class);
	}

	@Test
	void testgetUeberschneidunsanteilWith_ueberschneidungVorhanden() {
		// arrange
		LineString kanteLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(10, 20));

		LineString pottentielleUeberschneidung = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 5),
			new Coordinate(0, 0),
			new Coordinate(5, 10));

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.geometry(kanteLineString)
			.build();

		// act&assert
		assertThat(kante.getUeberschneidunsanteilWith(pottentielleUeberschneidung)).isCloseTo(0.5, Offset.offset(0.03));
	}

	@Test
	void testgetUeberschneidunsanteilWith_keineUeberschneidungAberGeometrienBildenStrecke() {
		// arrange
		LineString kanteLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(10, 20));

		LineString pottentielleUeberschneidung = GeometryTestdataProvider.createLineString(
			new Coordinate(10, 20),
			new Coordinate(20, 20));

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.geometry(kanteLineString)
			.build();

		// act&assert
		assertThat(kante.getUeberschneidunsanteilWith(pottentielleUeberschneidung)).isZero();
	}

	@Test
	void testgetUeberschneidunsanteilWith_sehrKurzeUeberschneidung() {
		// arrange
		LineString kanteLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(100, 100)

		);

		LineString pottentielleUeberschneidung = GeometryTestdataProvider.createLineString(
			new Coordinate(98, 98),
			new Coordinate(200, 200));

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.geometry(kanteLineString)
			.build();

		// act&assert
		assertThat(kante.getUeberschneidunsanteilWith(pottentielleUeberschneidung)).isCloseTo(0.02,
			Offset.offset(0.001));
	}

	@Test
	void testgetUeberschneidunsanteilWith_UeberschneidungslinestringBesitztKurveGroesser90Grad() {
		// arrange
		LineString kanteLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(100, 100),
			new Coordinate(120, 100));

		LineString pottentielleUeberschneidung = GeometryTestdataProvider.createLineString(
			new Coordinate(100, 100),
			new Coordinate(120, 100),
			new Coordinate(120, 120),
			new Coordinate(110, 120));

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.geometry(kanteLineString)
			.build();

		// act&assert
		assertThat(kante.getUeberschneidunsanteilWith(pottentielleUeberschneidung)).isEqualTo(1);
	}

	@Test
	void testgetUeberschneidunsanteilWith_KreisLinestring_TrifftAnfangspunktZweiMal() {
		// arrange
		LineString kanteLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(100, 100),
			new Coordinate(120, 100));

		LineString pottentielleUeberschneidung = GeometryTestdataProvider.createLineString(
			new Coordinate(100, 100),
			new Coordinate(120, 100),
			new Coordinate(120, 120),
			new Coordinate(100, 120),
			new Coordinate(100, 100));

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.geometry(kanteLineString)
			.build();

		// act&assert
		assertThat(kante.getUeberschneidunsanteilWith(pottentielleUeberschneidung)).isEqualTo(1);
	}

	// dieser Test funktioniert momentan nicht mit der aktuellen Implementation von
	// Linestrings.Ueberschneidungslinestring
	@Test
	void testgetUeberschneidunsanteilWith_KreisLinestring_TrifftKanteAmEndeNochmalFrueherInPunktKoordinate() {
		// arrange
		LineString kanteLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(100, 100),
			new Coordinate(120, 100));

		LineString pottentielleUeberschneidung = GeometryTestdataProvider.createLineString(
			new Coordinate(105, 100),
			new Coordinate(120, 100),
			new Coordinate(120, 120),
			new Coordinate(100, 120),
			new Coordinate(100, 100));

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.geometry(kanteLineString)
			.build();

		// act&assert
		assertThat(kante.getUeberschneidunsanteilWith(pottentielleUeberschneidung)).isEqualTo(0.75);
	}

	@Test
	void testgetUeberschneidunsanteilWith_MitUnterfuehrung() {
		// arrange
		LineString kanteLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(100, 100),
			new Coordinate(120, 100));

		LineString pottentielleUeberschneidung = GeometryTestdataProvider.createLineString(
			new Coordinate(105, 100),
			new Coordinate(120, 100),
			new Coordinate(120, 120),
			new Coordinate(110, 120),
			new Coordinate(110, 90),
			new Coordinate(100, 90));

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.geometry(kanteLineString)
			.build();

		// act&assert
		// nicht simple Überschneidungslinestring ergibt keine Überschneidung
		assertThat(kante.getUeberschneidunsanteilWith(pottentielleUeberschneidung)).isZero();
	}

	@Test
	void testgetUeberschneidunsanteilWith_UeberschneidungBenutztKanteAlsUmweg() {
		// arrange
		LineString kanteLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(99, 100),
			new Coordinate(101, 120));

		LineString pottentielleUeberschneidung = GeometryTestdataProvider.createLineString(
			new Coordinate(80, 120),
			new Coordinate(80, 100),
			new Coordinate(99, 100),
			new Coordinate(99.5, 105),
			new Coordinate(99, 100),
			new Coordinate(105, 100));

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.geometry(kanteLineString)
			.build();

		// act&assert
		// nicht simple Ueberschneidungslinestring ergeben keine Überschneidung
		assertThat(kante.getUeberschneidunsanteilWith(pottentielleUeberschneidung)).isZero();
	}

	@Test
	void testgetUeberschneidunsanteilWith_KanteBesitztKurveGroesser90Grad() {
		// arrange
		LineString kanteLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(100, 100),
			new Coordinate(120, 100),
			new Coordinate(120, 120),
			new Coordinate(80, 120) // Länge 80 meter
		);

		LineString pottentielleUeberschneidung = GeometryTestdataProvider.createLineString(
			new Coordinate(90, 120),
			new Coordinate(100, 100),
			new Coordinate(120, 100));

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.geometry(kanteLineString)
			.build();

		// act&assert
		assertThat(kante.getUeberschneidunsanteilWith(pottentielleUeberschneidung)).isEqualTo(0.25);
	}

	@Test
	void testeDefragmentieren_zustaendigkeit_fuehrungsformEinseitig_geschwindigkeit_ersterUndLetztesElementZuKlein() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttributes = List.of(
			FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.005).belagArt(BelagArt.BETON)
				.build(),
			FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.005, 0.995).belagArt(BelagArt.ASPHALT)
				.build(),
			FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.995, 1.0)
				.belagArt(BelagArt.WASSERGEBUNDENE_DECKE).build());

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.005)
							.vereinbarungsKennung(VereinbarungsKennung.of("oh no")).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.005, 0.995)
							.vereinbarungsKennung(VereinbarungsKennung.of("oh yes")).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.995, 1.0)
							.vereinbarungsKennung(VereinbarungsKennung.of("oh boy")).build()))
					.build())
			.geschwindigkeitAttributGruppe(GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte()
				.geschwindigkeitAttribute(List.of(
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.0, 0.005)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build(),
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.005, 0.995)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH).build(),
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.995, 1.0)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_9_KMH).build()))
				.build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(fuehrungsformAttributes)
					.fuehrungsformAttributeRechts(fuehrungsformAttributes)
					.build())
			.build();

		// act
		kante.defragmentiereLinearReferenzierteAttribute(minimaleSegmentLaenge);

		// assert
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = kante.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute();
		assertThat(zustaendigkeitAttribute).containsExactly(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("oh yes")).build());

		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = kante.getGeschwindigkeitAttributGruppe()
			.getImmutableGeschwindigkeitAttribute();
		assertThat(geschwindigkeitAttribute).containsExactly(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH).build());

		List<FuehrungsformAttribute> fuehrungsformAttributeLinks = kante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks();
		assertThat(fuehrungsformAttributeLinks).containsExactly(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).belagArt(BelagArt.ASPHALT).build());
	}

	@Test
	void testeDefragmentieren_zustaendigkeit_fuehrungsformEinseitig_geschwindigkeit_nichtDefragmentiert() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttributes = List.of(
			FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.4).belagArt(BelagArt.BETON)
				.build(),
			FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.4, 1.).belagArt(BelagArt.ASPHALT)
				.build());

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.6)
							.vereinbarungsKennung(VereinbarungsKennung.of("oh no")).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1.0)
							.vereinbarungsKennung(VereinbarungsKennung.of("oh boy")).build()))
					.build())
			.geschwindigkeitAttributGruppe(GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte()
				.geschwindigkeitAttribute(List.of(
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.0, 0.009)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build(),
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.009, 1.0)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_9_KMH).build()))
				.build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(fuehrungsformAttributes)
					.fuehrungsformAttributeRechts(fuehrungsformAttributes)
					.build())
			.build();

		// act
		kante.defragmentiereLinearReferenzierteAttribute(minimaleSegmentLaenge);
		// mehrmals ausführen sollte auch kein Unterschied machen wenn nicht defragmentiert
		kante.defragmentiereLinearReferenzierteAttribute(minimaleSegmentLaenge);

		// assert
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = kante.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute();
		assertThat(zustaendigkeitAttribute).containsExactlyInAnyOrder(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.6)
				.vereinbarungsKennung(VereinbarungsKennung.of("oh no")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1.0)
				.vereinbarungsKennung(VereinbarungsKennung.of("oh boy")).build());

		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = kante.getGeschwindigkeitAttributGruppe()
			.getImmutableGeschwindigkeitAttribute();
		assertThat(geschwindigkeitAttribute).containsExactlyInAnyOrder(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.0, 0.009)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.009, 1.0)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_9_KMH).build());

		List<FuehrungsformAttribute> fuehrungsformAttributeLinks = kante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks();
		assertThat(fuehrungsformAttributeLinks).containsExactlyInAnyOrder(
			FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.4).belagArt(BelagArt.BETON)
				.build(),
			FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.4, 1.).belagArt(BelagArt.ASPHALT)
				.build());
	}

	@Test
	void testeDefragmentieren_fuehrungsform_ersterUndLetztesElementZuKlein_zweiseitig() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.005)
							.belagArt(BelagArt.BETON)
							.build(),
						FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.005, 0.99)
							.belagArt(BelagArt.ASPHALT)
							.build(),
						FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.99, 1.0)
							.belagArt(BelagArt.ASPHALT)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.005)
							.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
							.build(),
						FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.005, 0.99)
							.belagArt(BelagArt.BETON)
							.build(),
						FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.99, 1.0)
							.belagArt(BelagArt.BETON)
							.build()))
					.build())
			.build();

		// act
		kante.defragmentiereLinearReferenzierteAttribute(minimaleSegmentLaenge);

		// assert
		List<FuehrungsformAttribute> fuehrungsformAttributeLinks = kante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks();
		assertThat(fuehrungsformAttributeLinks).hasSize(1);
		assertThat(fuehrungsformAttributeLinks.get(0).linearReferenzierterAbschnitt).isEqualTo(
			LinearReferenzierterAbschnitt.of(0.0, 1.0));
		assertThat(fuehrungsformAttributeLinks.get(0).getBelagArt()).isEqualTo(BelagArt.ASPHALT);

		List<FuehrungsformAttribute> fuehrungsformAttributeRechts = kante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts();
		assertThat(fuehrungsformAttributeRechts).hasSize(1);
		assertThat(fuehrungsformAttributeRechts.get(0).linearReferenzierterAbschnitt).isEqualTo(
			LinearReferenzierterAbschnitt.of(0.0, 1.0));
		assertThat(fuehrungsformAttributeRechts.get(0).getBelagArt()).isEqualTo(BelagArt.BETON);
	}

	@Nested
	class isZweiseitigkeitKonsistent {
		FuehrungsformAttribute fuehrungsformAttribute;

		@BeforeEach
		void beforeEach() {
			fuehrungsformAttribute = new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0, 1),
				BelagArt.SONSTIGER_BELAG, Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
				Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER, Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR,
				KfzParkenTyp.LAENGS_PARKEN, KfzParkenForm.PARKBUCHTEN, Laenge.of(3), Benutzungspflicht.NICHT_VORHANDEN,
				Beschilderung.UNBEKANNT,
				Collections.emptySet(),
				Absenkung.UNBEKANNT,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT);
		}

		@Test
		void true_alle_true() {
			// Arrange
			final var fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(fuehrungsformAttribute),
				true);
			final var fahrtrichtungAttributGruppe = new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, true);

			// Act
			final var result = Kante.isZweiseitigkeitKonsistent(true, fuehrungsformAttributGruppe,
				fahrtrichtungAttributGruppe);

			// Assert
			assertThat(result).isTrue();
		}

		@Test
		void true_alle_false() {
			// Arrange
			final var fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(fuehrungsformAttribute),
				false);
			final var fahrtrichtungAttributGruppe = new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, false);

			// Act
			final var result = Kante.isZweiseitigkeitKonsistent(false, fuehrungsformAttributGruppe,
				fahrtrichtungAttributGruppe);

			// Assert
			assertThat(result).isTrue();
		}

		@Test
		void false_einer_anders() {
			// Arrange
			final var fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(fuehrungsformAttribute),
				true);
			final var fahrtrichtungAttributGruppe = new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, false);

			// Act
			final var result = Kante.isZweiseitigkeitKonsistent(true, fuehrungsformAttributGruppe,
				fahrtrichtungAttributGruppe);

			// Assert
			assertThat(result).isFalse();
		}

		@Test
		void builder_schlaegt_fehl() {
			// Act + Assert
			assertThatThrownBy(() -> KanteTestDataProvider.withDefaultValuesAndZweiseitig().isZweiseitig(false).build())
				.isInstanceOf(RequireViolation.class);
		}

		@Test
		void builder_schlaegt_nicht_fehl() {
			// Act
			Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig().build();

			// Assert
			assertThat(kante).isNotNull();
		}
	}

	@Test
	void mergeSegmenteKleinerAls_coversAllLinearReferenzierteAttributGruppen() {
		// arrange
		FuehrungsformAttributGruppe fuehrungsformAttributGruppeMock = mock(FuehrungsformAttributGruppe.class);
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppeMock = mock(GeschwindigkeitAttributGruppe.class);
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppeMock = mock(ZustaendigkeitAttributGruppe.class);
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
			.fuehrungsformAttributGruppe(fuehrungsformAttributGruppeMock)
			.geschwindigkeitAttributGruppe(geschwindigkeitAttributGruppeMock)
			.zustaendigkeitAttributGruppe(zustaendigkeitAttributGruppeMock).build();

		// act
		kante.mergeSegmenteKleinerAls(Laenge.of(12));

		// assert
		verify(fuehrungsformAttributGruppeMock).mergeSegmentsKleinerAls(eq(LineareReferenz.of(0.12)));
		verify(geschwindigkeitAttributGruppeMock).mergeSegmentsKleinerAls(eq(LineareReferenz.of(0.12)));
		verify(zustaendigkeitAttributGruppeMock).mergeSegmentsKleinerAls(eq(LineareReferenz.of(0.12)));
	}

	@Test
	void mergeSegmenteKleinerAll_minimalLengthGreaterKantenLengt_uses1() {
		// arrange
		FuehrungsformAttributGruppe fuehrungsformAttributGruppeMock = mock(FuehrungsformAttributGruppe.class);
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppeMock = mock(GeschwindigkeitAttributGruppe.class);
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppeMock = mock(ZustaendigkeitAttributGruppe.class);
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
			.fuehrungsformAttributGruppe(fuehrungsformAttributGruppeMock)
			.geschwindigkeitAttributGruppe(geschwindigkeitAttributGruppeMock)
			.zustaendigkeitAttributGruppe(zustaendigkeitAttributGruppeMock).build();

		// act
		kante.mergeSegmenteKleinerAls(Laenge.of(102));

		// assert
		verify(fuehrungsformAttributGruppeMock).mergeSegmentsKleinerAls(eq(LineareReferenz.of(1.0)));
		verify(geschwindigkeitAttributGruppeMock).mergeSegmentsKleinerAls(eq(LineareReferenz.of(1.0)));
		verify(zustaendigkeitAttributGruppeMock).mergeSegmentsKleinerAls(eq(LineareReferenz.of(1.0)));
	}

	private GeometryFactory getGeometryFactory() {
		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
	}
}
