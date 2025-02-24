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

package de.wps.radvis.backend.netz.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.security.access.AccessDeniedException;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.RadVisDomainEvent;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.entity.VersionedId;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteDeleteStatistik;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometrien;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenDeleteStatistik;
import de.wps.radvis.backend.netz.domain.entity.NahegelegeneneKantenDbView;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.event.KanteGeometrieChangedEvent;
import de.wps.radvis.backend.netz.domain.event.KantenDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KnotenDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.RadNetzZugehoerigkeitChangedEvent;
import de.wps.radvis.backend.netz.domain.event.RadNetzZugehoerigkeitEntferntEvent;
import de.wps.radvis.backend.netz.domain.repository.FahrtrichtungAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.FuehrungsformAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.GeschwindigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.repository.ZustaendigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Bauwerksmangel;
import de.wps.radvis.backend.netz.domain.valueObject.BauwerksmangelArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KantenSeite;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.QuerungshilfeDetails;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;

class NetzServiceTest {

	private final String lineUtm32 = "LINESTRING (414468 5316872, 414485 5316925)";

	@Mock
	private KantenRepository kantenRepositoryMock;

	@Mock
	private KnotenRepository knotenRepositoryMock;

	@Mock
	private ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppeRepository;

	@Mock
	private FahrtrichtungAttributGruppeRepository fahrtrichtungAttributGruppeRepository;

	@Mock
	private GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository;

	@Mock
	private FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppeRepository;

	@Mock
	private KantenAttributGruppeRepository kantenAttributGruppeRepository;

	@Mock
	private VerwaltungseinheitResolver verwaltungseinheitResolver;

	@Mock
	private EntityManager entityManager;

	private NetzService netzService;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		netzService = new NetzService(kantenRepositoryMock, knotenRepositoryMock,
			zustaendigkeitAttributGruppeRepository, fahrtrichtungAttributGruppeRepository,
			geschwindigkeitAttributGruppeRepository, fuehrungsformAttributGruppeRepository,
			kantenAttributGruppeRepository, verwaltungseinheitResolver, entityManager, 1.0,
			Laenge.of(10), 10, 15.0, 0.1);
	}

	@Test
	void testLoadKanteForModification_wirftExceptionBeiKantenVersionMismatch() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(123L).version(2L).build();
		when(kantenRepositoryMock.findById(kante.getId())).thenReturn(Optional.of(kante));

		// act & assert
		assertThatThrownBy(() -> netzService.loadKanteForModification(kante.getId(), 1L))
			.isInstanceOf(OptimisticLockException.class);
	}

	@Test
	void testLoadKnotenForModification_wirftExceptionBeiKnotenVersionMismatch() {
		// arrange
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().id(123L).version(2L).build();
		when(knotenRepositoryMock.findById(knoten.getId())).thenReturn(Optional.of(knoten));

		// act & assert
		assertThatThrownBy(() -> netzService.loadKnotenForModification(knoten.getId(), 1L))
			.isInstanceOf(OptimisticLockException.class);
	}

	@Test
	void berechneOrtslage_KantenSindAlleAusserorts_KnotenIstAusserorts() {
		// arrange
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().id(123L).version(2L).build();

		final GeschwindigkeitAttribute geschwindigkeitsattribute = GeschwindigkeitAttribute.builder()
			.ortslage(KantenOrtslage.AUSSERORTS)
			.build();

		final GeschwindigkeitAttributGruppe attributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(
				List.of(geschwindigkeitsattribute))
			.build();

		when(kantenRepositoryMock.getAdjazenteKanten(knoten)).thenReturn(List.of(
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(attributGruppe)
				.build(),
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(attributGruppe)
				.build(),
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(attributGruppe)
				.build()));

		// act
		KnotenOrtslage knotenOrtslage = netzService.berechneOrtslage(knoten);

		// assert
		assertThat(knotenOrtslage).isEqualTo(KnotenOrtslage.AUSSERORTS);
	}

	@Test
	void berechneOrtslage_KantenSindAlleInnerorts_KnotenIstInnerorts() {
		// arrange
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().id(123L).version(2L).build();

		final GeschwindigkeitAttribute geschwindigkeitsattribute = GeschwindigkeitAttribute.builder()
			.ortslage(KantenOrtslage.INNERORTS)
			.build();

		final GeschwindigkeitAttributGruppe attributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(
				List.of(geschwindigkeitsattribute))
			.build();

		when(kantenRepositoryMock.getAdjazenteKanten(knoten)).thenReturn(List.of(
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(attributGruppe)
				.build(),
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(attributGruppe)
				.build(),
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(attributGruppe)
				.build()));

		// act
		KnotenOrtslage knotenOrtslage = netzService.berechneOrtslage(knoten);

		// assert
		assertThat(knotenOrtslage).isEqualTo(KnotenOrtslage.INNERORTS);
	}

	@Test
	void berechneOrtslage_KantenSindSowohlInnerortsAlsAuchAusserorts_KnotenIstOrtseingangsbereich() {
		// arrange
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().id(123L).version(2L).build();

		when(kantenRepositoryMock.getAdjazenteKanten(knoten)).thenReturn(List.of(
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(
					List.of(GeschwindigkeitAttribute.builder()
						.ortslage(KantenOrtslage.INNERORTS)
						.build()))
					.build())
				.build(),
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(
					List.of(GeschwindigkeitAttribute.builder()
						.ortslage(KantenOrtslage.AUSSERORTS)
						.build()))
					.build())
				.build(),
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(
					List.of(GeschwindigkeitAttribute.builder()
						.ortslage(KantenOrtslage.INNERORTS)
						.build()))
					.build())
				.build()));

		// act
		KnotenOrtslage knotenOrtslage = netzService.berechneOrtslage(knoten);

		// assert
		assertThat(knotenOrtslage).isEqualTo(KnotenOrtslage.ORTSEINGANGSBEREICH);
	}

	@Test
	void berechneOrtslage_EineKanteHatKeineOrtslageRestIstInnerorts_KnotenIstInnenorts() {
		// arrange
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().id(123L).version(2L).build();

		final GeschwindigkeitAttribute geschwindigkeitsattribute = GeschwindigkeitAttribute.builder()
			.ortslage(KantenOrtslage.INNERORTS)
			.build();

		final GeschwindigkeitAttributGruppe attributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(
				List.of(geschwindigkeitsattribute))
			.build();

		when(kantenRepositoryMock.getAdjazenteKanten(knoten)).thenReturn(List.of(
			KanteTestDataProvider.withDefaultValues()
				.geschwindigkeitAttributGruppe(attributGruppe)
				.build(),
			KanteTestDataProvider.withDefaultValues()
				.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder().build())
				.build(),
			KanteTestDataProvider.withDefaultValues()
				.geschwindigkeitAttributGruppe(attributGruppe)
				.build()));

		// act
		KnotenOrtslage knotenOrtslage = netzService.berechneOrtslage(knoten);

		// assert
		assertThat(knotenOrtslage).isEqualTo(KnotenOrtslage.INNERORTS);
	}

	@Test
	void berechneOrtslage_EineKanteHatKeineOrtslageRestIstAusserorts_KnotenIstAusserorts() {
		// arrange
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().id(123L).version(2L).build();

		final GeschwindigkeitAttribute geschwindigkeitsattribute = GeschwindigkeitAttribute.builder()
			.ortslage(KantenOrtslage.AUSSERORTS)
			.build();

		final GeschwindigkeitAttributGruppe attributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(
				List.of(geschwindigkeitsattribute))
			.build();

		when(kantenRepositoryMock.getAdjazenteKanten(knoten)).thenReturn(List.of(
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(attributGruppe)
				.build(),
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder().build()).build(),
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(attributGruppe).build()));

		// act
		KnotenOrtslage knotenOrtslage = netzService.berechneOrtslage(knoten);

		// assert
		assertThat(knotenOrtslage).isEqualTo(KnotenOrtslage.AUSSERORTS);
	}

	@Test
	void berechneOrtslage_EineKanteHatKeineOrtslageRestIstGemischt_KnotenIstOrtseingangsbereich() {
		// arrange
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().id(123L).version(2L).build();

		when(kantenRepositoryMock.getAdjazenteKanten(knoten)).thenReturn(List.of(
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(
					List.of(GeschwindigkeitAttribute.builder()
						.ortslage(KantenOrtslage.AUSSERORTS)
						.build()))
					.build())
				.build(),
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder().build()).build(),
			KanteTestDataProvider.withDefaultValues().geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(
					List.of(GeschwindigkeitAttribute.builder()
						.ortslage(KantenOrtslage.INNERORTS)
						.build()))
					.build())
				.build()));

		// act
		KnotenOrtslage knotenOrtslage = netzService.berechneOrtslage(knoten);

		// assert
		assertThat(knotenOrtslage).isEqualTo(KnotenOrtslage.ORTSEINGANGSBEREICH);
	}

	@Test
	void berechneOrtslage_KantenMitUnterschiedlichenKantensegmenten_angrenzendeSegmenteInnerOrts_KnotenIstInnerorts() {
		// arrange

		final Coordinate coordinatePunktAnfang = new Coordinate(0, 0);
		Knoten anfangsknoten = KnotenTestDataProvider.withCoordinateAndQuelle(coordinatePunktAnfang, QuellSystem.LGL)
			.id(123L).version(2L)
			.build();
		final Coordinate coordinatePunktTestKnoten = new Coordinate(0, 50);
		Knoten testKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(coordinatePunktTestKnoten, QuellSystem.LGL)
			.id(123L).version(2L)
			.build();
		final Coordinate coordinatePunktEnde = new Coordinate(0, 100);
		Knoten endKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(coordinatePunktEnde, QuellSystem.LGL)
			.id(123L).version(2L)
			.build();

		final GeschwindigkeitAttribute attributeAanfang = GeschwindigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.ortslage(KantenOrtslage.AUSSERORTS)
			.build();
		final GeschwindigkeitAttribute attributeAangrenzend = GeschwindigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
			.ortslage(KantenOrtslage.INNERORTS)
			.build();
		final GeschwindigkeitAttribute attributeBangrenzend = GeschwindigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.ortslage(KantenOrtslage.INNERORTS)
			.build();
		final GeschwindigkeitAttribute attributeBende = GeschwindigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
			.ortslage(KantenOrtslage.AUSSERORTS)
			.build();

		when(kantenRepositoryMock.getAdjazenteKanten(endKnoten)).thenReturn(List.of(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 50, QuellSystem.DLM)
				.vonKnoten(anfangsknoten).nachKnoten(testKnoten)
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(
						List.of(attributeAanfang, attributeAangrenzend)).build())
				.build(),
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 50, 0, 100, QuellSystem.DLM)
				.vonKnoten(testKnoten).nachKnoten(endKnoten)
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(
						List.of(attributeBangrenzend, attributeBende)).build())
				.build()));

		// act
		KnotenOrtslage knotenOrtslage = netzService.berechneOrtslage(testKnoten);

		// assert
		assertThat(knotenOrtslage).isEqualTo(KnotenOrtslage.INNERORTS);
	}

	@Test
	void berechneOrtslage_KantenMitUnterschiedlichenKantensegmenten_angrenzendeSegmenteGemischt_KnotenIstOrtseingangsbereich() {
		// arrange

		final Coordinate coordinatePunktAnfang = new Coordinate(0, 0);
		Knoten anfangsknoten = KnotenTestDataProvider.withCoordinateAndQuelle(coordinatePunktAnfang, QuellSystem.LGL)
			.id(123L).version(2L)
			.build();
		final Coordinate coordinatePunktTestKnoten = new Coordinate(0, 50);
		Knoten testKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(coordinatePunktTestKnoten, QuellSystem.LGL)
			.id(123L).version(2L)
			.build();
		final Coordinate coordinatePunktEnde = new Coordinate(0, 100);
		Knoten endKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(coordinatePunktEnde, QuellSystem.LGL)
			.id(123L).version(2L)
			.build();

		final GeschwindigkeitAttribute attributeAanfang = GeschwindigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.ortslage(KantenOrtslage.AUSSERORTS)
			.build();
		final GeschwindigkeitAttribute attributeAangrenzend = GeschwindigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
			.ortslage(KantenOrtslage.INNERORTS)
			.build();
		final GeschwindigkeitAttribute attributeBangrenzend = GeschwindigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.ortslage(KantenOrtslage.AUSSERORTS)
			.build();
		final GeschwindigkeitAttribute attributeBende = GeschwindigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
			.ortslage(KantenOrtslage.INNERORTS)
			.build();

		when(kantenRepositoryMock.getAdjazenteKanten(endKnoten)).thenReturn(List.of(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 50, QuellSystem.DLM)
				.vonKnoten(anfangsknoten).nachKnoten(testKnoten)
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(
						List.of(attributeAanfang, attributeAangrenzend)).build())
				.build(),
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 50, 0, 100, QuellSystem.DLM)
				.vonKnoten(testKnoten).nachKnoten(endKnoten)
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(
						List.of(attributeBangrenzend, attributeBende)).build())
				.build()));

		// act
		KnotenOrtslage knotenOrtslage = netzService.berechneOrtslage(testKnoten);

		// assert
		assertThat(knotenOrtslage).isEqualTo(KnotenOrtslage.ORTSEINGANGSBEREICH);
	}

	@Test
	void berechneOffets() throws Exception {
		// arrange
		LineString ursprungskante = createLineString(lineUtm32, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		// act
		Geometry links = netzService.berechneNebenkante(ursprungskante, KantenSeite.LINKS);
		Geometry rechts = netzService.berechneNebenkante(ursprungskante, KantenSeite.RECHTS);

		// assert
		assertThat(links.getCoordinates()[0].getX()).isLessThan(rechts.getCoordinates()[0].getX());
		assertThat(links.getCoordinates()[0].getY()).isGreaterThan(rechts.getCoordinates()[0].getY());
		assertThat(links.getCoordinates()[1].getX()).isLessThan(rechts.getCoordinates()[1].getX());
		assertThat(links.getCoordinates()[1].getY()).isGreaterThan(rechts.getCoordinates()[1].getY());
	}

	@Test
	void createGrundnetzKante_gibtNeueGrundnetzKante() {
		// arrange
		Coordinate coordinate1 = new Coordinate(0, 0);
		Knoten knoten = KnotenTestDataProvider.withCoordinateAndQuelle(coordinate1, QuellSystem.DLM).id(1L)
			.version(2L).build();
		when(knotenRepositoryMock.findById(knoten.getId())).thenReturn(Optional.of(knoten));

		Coordinate coordinate2 = new Coordinate(0, 1);
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(coordinate2, QuellSystem.DLM).id(2L)
			.version(2L).build();
		when(knotenRepositoryMock.findById(knoten2.getId())).thenReturn(Optional.of(knoten2));

		when(kantenRepositoryMock.save(any())).thenAnswer(
			(Answer<Kante>) invocation -> (Kante) invocation.getArguments()[0]);

		// act
		Kante kante = netzService.createGrundnetzKante(knoten.getId(), knoten2.getId(), Status.FIKTIV);

		// assert
		assertThat(kante.getGeometry().getStartPoint().getCoordinate()).isEqualTo(coordinate1);
		assertThat(kante.getGeometry().getEndPoint().getCoordinate()).isEqualTo(coordinate2);
		assertThat(kante.isGrundnetz()).isTrue();
		assertThat(kante.getQuelle()).isEqualTo(QuellSystem.RadVis);
		Mockito.verify(kantenRepositoryMock).save(kante);
	}

	@Test
	void createGrundnetzKanteWithNewBisKnoten_gibtNeueGrundnetzKante_mitNeuemKnoten() {
		// arrange
		Coordinate coordinateVon = new Coordinate(0, 0);
		Knoten knoten = KnotenTestDataProvider.withCoordinateAndQuelle(coordinateVon, QuellSystem.DLM).id(1L)
			.version(2L).build();
		when(knotenRepositoryMock.findById(knoten.getId())).thenReturn(Optional.of(knoten));

		Point pointBis = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createPoint(new Coordinate(0, 1));

		when(kantenRepositoryMock.save(any())).thenAnswer(
			(Answer<Kante>) invocation -> (Kante) invocation.getArguments()[0]);

		// act
		Kante kante = netzService.createGrundnetzKanteWithNewBisKnoten(knoten.getId(), pointBis, Status.FIKTIV);

		// assert
		assertThat(kante.getGeometry().getStartPoint().getCoordinate()).isEqualTo(coordinateVon);
		assertThat(kante.getGeometry().getEndPoint().getCoordinate()).isEqualTo(pointBis.getCoordinate());
		assertThat(kante.isGrundnetz()).isTrue();
		assertThat(kante.getNachKnoten().getQuelle()).isEqualTo(QuellSystem.RadVis);
		assertThat(kante.getNachKnoten().getKoordinate()).isEqualTo(pointBis.getCoordinate());
		assertThat(kante.getQuelle()).isEqualTo(QuellSystem.RadVis);
		Mockito.verify(kantenRepositoryMock).save(kante);
	}

	@Test
	public void aktualisiereFuehrungsformen() {
		// arrange
		FuehrungsformAttributGruppe oldAttributGruppeWithBreite = FuehrungsformAttributGruppe.builder()
			.id(1L)
			.version(2L)
			.isZweiseitig(true)
			.fuehrungsformAttributeRechts(List.of(
				FuehrungsformAttribute.builder()
					.breite(Laenge.of(123))
					.build()))
			.fuehrungsformAttributeLinks(List.of(
				FuehrungsformAttribute.builder()
					.breite(Laenge.of(234))
					.build()))
			.build();
		FuehrungsformAttributGruppe oldAttributGruppeWithBordstein = FuehrungsformAttributGruppe.builder()
			.id(5L)
			.version(6L)
			.isZweiseitig(false)
			.fuehrungsformAttributeRechts(List.of(
				FuehrungsformAttribute.builder()
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build()))
			.fuehrungsformAttributeLinks(List.of(
				FuehrungsformAttribute.builder()
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build()))
			.build();

		FuehrungsformAttributGruppe newAttributGruppeWithBreite = FuehrungsformAttributGruppe.builder()
			.id(1L)
			.version(2L)
			.isZweiseitig(true)
			.fuehrungsformAttributeRechts(List.of(
				FuehrungsformAttribute.builder()
					.breite(Laenge.of(1000))
					.build()))
			.fuehrungsformAttributeLinks(List.of(
				FuehrungsformAttribute.builder()
					.breite(Laenge.of(2000))
					.build()))
			.build();
		FuehrungsformAttributGruppe newAttributGruppeWithBordstein = FuehrungsformAttributGruppe.builder()
			.id(5L)
			.version(6L)
			.isZweiseitig(false)
			.fuehrungsformAttributeRechts(List.of(
				FuehrungsformAttribute.builder()
					.bordstein(Bordstein.KOMPLETT_ABGESENKT)
					.build()))
			.fuehrungsformAttributeLinks(List.of(
				FuehrungsformAttribute.builder()
					.bordstein(Bordstein.KOMPLETT_ABGESENKT)
					.build()))
			.build();
		List<FuehrungsformAttributGruppe> newAttributGruppen = List.of(
			newAttributGruppeWithBreite,
			newAttributGruppeWithBordstein);

		when(fuehrungsformAttributGruppeRepository.findById(oldAttributGruppeWithBreite.getId())).thenReturn(
			Optional.of(oldAttributGruppeWithBreite));
		when(fuehrungsformAttributGruppeRepository.findById(oldAttributGruppeWithBordstein.getId())).thenReturn(
			Optional.of(oldAttributGruppeWithBordstein));

		// act
		netzService.aktualisiereFuehrungsformen(newAttributGruppen);

		// assert
		assertThat(oldAttributGruppeWithBreite.getImmutableFuehrungsformAttributeLinks().get(0).getBreite()).isEqualTo(
			newAttributGruppeWithBreite.getImmutableFuehrungsformAttributeLinks().get(0).getBreite());
		assertThat(oldAttributGruppeWithBreite.getImmutableFuehrungsformAttributeRechts().get(0).getBreite()).isEqualTo(
			newAttributGruppeWithBreite.getImmutableFuehrungsformAttributeRechts().get(0).getBreite());

		assertThat(
			oldAttributGruppeWithBordstein.getImmutableFuehrungsformAttributeLinks().get(0).getBordstein()).isEqualTo(
				newAttributGruppeWithBordstein.getImmutableFuehrungsformAttributeLinks().get(0).getBordstein());
		assertThat(
			oldAttributGruppeWithBordstein.getImmutableFuehrungsformAttributeRechts().get(0).getBordstein()).isEqualTo(
				newAttributGruppeWithBordstein.getImmutableFuehrungsformAttributeRechts().get(0).getBordstein());
	}

	@Test
	public void aktualisiereFuehrungsformen_wirftExceptionBeiInvaliderZweiseitigkeit() {
		// arrange
		FuehrungsformAttributGruppe oldAttributGruppe = FuehrungsformAttributGruppe.builder()
			.id(5L)
			.version(6L)
			.isZweiseitig(false)
			.fuehrungsformAttributeRechts(List.of(
				FuehrungsformAttribute.builder()
					.bordstein(Bordstein.KOMPLETT_ABGESENKT)
					.build()))
			.fuehrungsformAttributeLinks(List.of(
				FuehrungsformAttribute.builder()
					.bordstein(Bordstein.KOMPLETT_ABGESENKT)
					.build()))
			.build();
		FuehrungsformAttributGruppe newAttributGruppe = FuehrungsformAttributGruppe.builder()
			.id(5L)
			.version(6L)
			.isZweiseitig(true)
			.fuehrungsformAttributeRechts(List.of(
				FuehrungsformAttribute.builder()
					.bordstein(Bordstein.KOMPLETT_ABGESENKT)
					.build()))
			.fuehrungsformAttributeLinks(List.of(
				FuehrungsformAttribute.builder()
					.bordstein(Bordstein.KEINE_ABSENKUNG) // rechts/links unterschiedlich
					.build()))
			.build();
		List<FuehrungsformAttributGruppe> newAttributGruppen = List.of(newAttributGruppe);

		when(fuehrungsformAttributGruppeRepository.findById(5L)).thenReturn(Optional.of(oldAttributGruppe));

		// act + assert
		assertThatThrownBy(() -> netzService.aktualisiereFuehrungsformen(newAttributGruppen))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("Unterschiedliche Attribute bei einseitiger Kante");
	}

	@Nested
	public class withDomainEvents {

		MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

		ArgumentCaptor<RadVisDomainEvent> eventCaptor;

		@BeforeEach
		void setupDomainPublisherMock() {
			domainPublisherMock = mockStatic(
				RadVisDomainEventPublisher.class);
			eventCaptor = ArgumentCaptor.forClass(RadVisDomainEvent.class);
		}

		@AfterEach
		void cleanUp() {
			domainPublisherMock.close();
		}

		@Test
		public void testAktualisiereKantenAttribute() {
			// arrange
			KantenAttributGruppe oldKantenAttributGruppe = KantenAttributGruppe.builder()
				.id(1L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
				.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
				.kantenAttribute(KantenAttribute.builder().build())
				.build();
			KantenAttributGruppe newKantenAttributGruppe = KantenAttributGruppe.builder()
				.id(1L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.RADVORRANGROUTEN))
				.istStandards(Set.of(IstStandard.RADSCHNELLVERBINDUNG))
				.kantenAttribute(KantenAttribute.builder().beleuchtung(Beleuchtung.VORHANDEN).build())
				.build();

			when(kantenAttributGruppeRepository.findById(1L)).thenReturn(Optional.of(oldKantenAttributGruppe));

			// act
			netzService.aktualisiereKantenAttribute(List.of(newKantenAttributGruppe));

			// assert
			assertThat(oldKantenAttributGruppe.getNetzklassen()).isEqualTo(newKantenAttributGruppe.getNetzklassen());
			assertThat(oldKantenAttributGruppe.getIstStandards()).isEqualTo(newKantenAttributGruppe.getIstStandards());
			assertThat(oldKantenAttributGruppe.getKantenAttribute().getBeleuchtung()).isEqualTo(
				newKantenAttributGruppe.getKantenAttribute().getBeleuchtung());
			domainPublisherMock.verify(
				() -> RadVisDomainEventPublisher.publish(eventCaptor.capture()), times(2));
			assertThat(eventCaptor.getAllValues())
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactly(
					new RadNetzZugehoerigkeitChangedEvent(oldKantenAttributGruppe.getId(), false),
					new RadNetzZugehoerigkeitEntferntEvent(oldKantenAttributGruppe.getId()));
		}

		@Test
		public void testAktualisiereKantenAttribute_mehrereAttributGruppenMitRueckstufung_alleInRueckstufungsEvent() {
			// arrange
			KantenAttributGruppe oldKantenAttributGruppe = KantenAttributGruppe.builder()
				.id(1L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
				.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
				.kantenAttribute(KantenAttribute.builder().build())
				.build();
			KantenAttributGruppe newKantenAttributGruppe = KantenAttributGruppe.builder()
				.id(1L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.RADVORRANGROUTEN))
				.istStandards(Set.of(IstStandard.RADSCHNELLVERBINDUNG))
				.kantenAttribute(KantenAttribute.builder().beleuchtung(Beleuchtung.VORHANDEN).build())
				.build();

			KantenAttributGruppe oldKantenAttributGruppe2 = KantenAttributGruppe.builder()
				.id(2L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
				.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
				.kantenAttribute(KantenAttribute.builder().build())
				.build();
			KantenAttributGruppe newKantenAttributGruppe2 = KantenAttributGruppe.builder()
				.id(2L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.RADVORRANGROUTEN))
				.istStandards(Set.of(IstStandard.RADSCHNELLVERBINDUNG))
				.kantenAttribute(KantenAttribute.builder().beleuchtung(Beleuchtung.VORHANDEN).build())
				.build();

			when(kantenAttributGruppeRepository.findById(1L)).thenReturn(Optional.of(oldKantenAttributGruppe));
			when(kantenAttributGruppeRepository.findById(2L)).thenReturn(Optional.of(oldKantenAttributGruppe2));

			// act
			netzService.aktualisiereKantenAttribute(List.of(newKantenAttributGruppe, newKantenAttributGruppe2));

			// assert
			assertThat(oldKantenAttributGruppe.getNetzklassen()).isEqualTo(newKantenAttributGruppe.getNetzklassen());
			assertThat(oldKantenAttributGruppe.getIstStandards()).isEqualTo(newKantenAttributGruppe.getIstStandards());
			assertThat(oldKantenAttributGruppe.getKantenAttribute().getBeleuchtung()).isEqualTo(
				newKantenAttributGruppe.getKantenAttribute().getBeleuchtung());

			assertThat(oldKantenAttributGruppe2.getNetzklassen()).isEqualTo(newKantenAttributGruppe2.getNetzklassen());
			assertThat(oldKantenAttributGruppe2.getIstStandards()).isEqualTo(
				newKantenAttributGruppe2.getIstStandards());
			assertThat(oldKantenAttributGruppe2.getKantenAttribute().getBeleuchtung()).isEqualTo(
				newKantenAttributGruppe2.getKantenAttribute().getBeleuchtung());
			domainPublisherMock.verify(
				() -> RadVisDomainEventPublisher.publish(eventCaptor.capture()), times(3));
			assertThat(eventCaptor.getAllValues())
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactly(
					new RadNetzZugehoerigkeitChangedEvent(oldKantenAttributGruppe.getId(), false),
					new RadNetzZugehoerigkeitChangedEvent(oldKantenAttributGruppe2.getId(), false),
					new RadNetzZugehoerigkeitEntferntEvent(oldKantenAttributGruppe.getId(),
						oldKantenAttributGruppe2.getId()));
		}

		@Test
		public void testAktualisiereKantenAttribute_mehrereAttributGruppenTeilweiseMitRueckstufung_teilweiseInRueckstufungsEvent() {
			// arrange
			KantenAttributGruppe oldKantenAttributGruppe = KantenAttributGruppe.builder()
				.id(1L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
				.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
				.kantenAttribute(KantenAttribute.builder().build())
				.build();
			KantenAttributGruppe newKantenAttributGruppe = KantenAttributGruppe.builder()
				.id(1L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.RADVORRANGROUTEN))
				.istStandards(Set.of(IstStandard.RADSCHNELLVERBINDUNG))
				.kantenAttribute(KantenAttribute.builder().beleuchtung(Beleuchtung.VORHANDEN).build())
				.build();

			KantenAttributGruppe oldKantenAttributGruppeOhneRueckstufung = KantenAttributGruppe.builder()
				.id(2L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT))
				.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
				.kantenAttribute(KantenAttribute.builder().build())
				.build();
			KantenAttributGruppe newKantenAttributGruppeOhneRueckstufung = KantenAttributGruppe.builder()
				.id(2L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
				.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
				.kantenAttribute(KantenAttribute.builder().beleuchtung(Beleuchtung.NICHT_VORHANDEN).build())
				.build();

			KantenAttributGruppe oldKantenAttributGruppeHochstufung = KantenAttributGruppe.builder()
				.id(3L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.KREISNETZ_ALLTAG))
				.istStandards(Set.of())
				.kantenAttribute(KantenAttribute.builder().build())
				.build();
			KantenAttributGruppe newKantenAttributGruppeHochstufung = KantenAttributGruppe.builder()
				.id(3L)
				.version(2L)
				.netzklassen(Set.of(Netzklasse.KREISNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG))
				.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
				.kantenAttribute(
					KantenAttribute.builder().beleuchtung(Beleuchtung.RETROREFLEKTIERENDE_RANDMARKIERUNG).build())
				.build();

			when(kantenAttributGruppeRepository.findById(1L)).thenReturn(Optional.of(oldKantenAttributGruppe));
			when(kantenAttributGruppeRepository.findById(2L)).thenReturn(
				Optional.of(oldKantenAttributGruppeOhneRueckstufung));
			when(kantenAttributGruppeRepository.findById(3L)).thenReturn(
				Optional.of(oldKantenAttributGruppeHochstufung));
			// act
			netzService.aktualisiereKantenAttribute(
				List.of(newKantenAttributGruppe, newKantenAttributGruppeOhneRueckstufung,
					newKantenAttributGruppeHochstufung));

			// assert
			assertThat(oldKantenAttributGruppe.getNetzklassen()).isEqualTo(newKantenAttributGruppe.getNetzklassen());
			assertThat(oldKantenAttributGruppe.getIstStandards()).isEqualTo(newKantenAttributGruppe.getIstStandards());
			assertThat(oldKantenAttributGruppe.getKantenAttribute().getBeleuchtung()).isEqualTo(
				newKantenAttributGruppe.getKantenAttribute().getBeleuchtung());

			assertThat(oldKantenAttributGruppeOhneRueckstufung.getNetzklassen()).isEqualTo(
				newKantenAttributGruppeOhneRueckstufung.getNetzklassen());
			assertThat(oldKantenAttributGruppeOhneRueckstufung.getIstStandards()).isEqualTo(
				newKantenAttributGruppeOhneRueckstufung.getIstStandards());
			assertThat(oldKantenAttributGruppeOhneRueckstufung.getKantenAttribute().getBeleuchtung()).isEqualTo(
				newKantenAttributGruppeOhneRueckstufung.getKantenAttribute().getBeleuchtung());

			assertThat(oldKantenAttributGruppeHochstufung.getNetzklassen()).isEqualTo(
				newKantenAttributGruppeHochstufung.getNetzklassen());
			assertThat(oldKantenAttributGruppeHochstufung.getIstStandards()).isEqualTo(
				newKantenAttributGruppeHochstufung.getIstStandards());
			assertThat(oldKantenAttributGruppeHochstufung.getKantenAttribute().getBeleuchtung()).isEqualTo(
				newKantenAttributGruppeHochstufung.getKantenAttribute().getBeleuchtung());
			domainPublisherMock.verify(
				() -> RadVisDomainEventPublisher.publish(eventCaptor.capture()), times(3));
			assertThat(eventCaptor.getAllValues())
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactly(
					new RadNetzZugehoerigkeitChangedEvent(oldKantenAttributGruppe.getId(), false),
					new RadNetzZugehoerigkeitChangedEvent(oldKantenAttributGruppeHochstufung.getId(), true),
					new RadNetzZugehoerigkeitEntferntEvent(oldKantenAttributGruppe.getId()));

		}

		@Test
		public void deleteVerwaisteDlmKnoten() {
			Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
				.id(2L).build();
			Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(500, 500), QuellSystem.DLM)
				.id(1L).build();
			when(knotenRepositoryMock.findVerwaisteDLMKnoten()).thenReturn(List.of(knoten1, knoten2));

			netzService.deleteVerwaisteDLMKnoten(NetzAenderungAusloeser.DLM_REIMPORT_JOB, new KnotenDeleteStatistik());

			domainPublisherMock.verify(
				() -> RadVisDomainEventPublisher.publish(eventCaptor.capture()), times(1));
			assertThat(eventCaptor.getValue()).isInstanceOf(KnotenDeletedEvent.class);
			KnotenDeletedEvent capturedEvent = (KnotenDeletedEvent) eventCaptor.getValue();
			assertThat(capturedEvent.getKnoten()).containsExactlyInAnyOrder(knoten1, knoten2);
		}

		@Test
		public void testAktualisiereVerlaeufeUndGeometrien() {
			// arrange
			Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
				.build();
			Knoten bisKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(500, 500), QuellSystem.DLM)
				.build();
			Kante kante = KanteTestDataProvider.fromKnoten(vonKnoten, bisKnoten)
				.id(2L)
				.version(3L)
				.quelle(QuellSystem.RadVis)
				.dlmId(null)
				.isZweiseitig(true)
				.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
					.isZweiseitig(true)
					.build())
				.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
					.isZweiseitig(true)
					.build())
				.verlaufLinks(GeometryTestdataProvider.createLineString(new Coordinate(1, 2), new Coordinate(2, 3)))
				.verlaufRechts(GeometryTestdataProvider.createLineString(new Coordinate(7, 8), new Coordinate(8, 9)))
				.build();
			when(kantenRepositoryMock.findById(kante.getId())).thenReturn(Optional.of(kante));

			LineString newGeometry = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
				new Coordinate(100, 200), new Coordinate(200, 300), new Coordinate(500, 500));
			KanteGeometrien kanteGeometrien = KanteGeometrien.builder()
				.id(kante.getId())
				.version(kante.getVersion())
				.geometry(newGeometry)
				.verlaufLinks(GeometryTestdataProvider.createLineString(new Coordinate(10, 20), new Coordinate(20, 30)))
				.verlaufRechts(
					GeometryTestdataProvider.createLineString(new Coordinate(70, 80), new Coordinate(80, 90)))
				.build();

			// act
			netzService.aktualisiereVerlaeufeUndGeometrien(List.of(kanteGeometrien));

			// assert
			domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(eventCaptor.capture()));
			assertThat(eventCaptor.getValue()).usingRecursiveComparison()
				.isEqualTo(new KanteGeometrieChangedEvent(kante.getId()));
			assertThat(kante.getGeometry().getCoordinates()).isEqualTo(newGeometry.getCoordinates());
			assertThat(kante.getVerlaufLinks()).isPresent();
			assertThat(kante.getVerlaufLinks().get().getCoordinateN(0)).isEqualTo(new Coordinate(10, 20));
			assertThat(kante.getVerlaufLinks().get().getCoordinateN(1)).isEqualTo(new Coordinate(20, 30));
			assertThat(kante.getVerlaufRechts()).isPresent();
			assertThat(kante.getVerlaufRechts().get().getCoordinateN(0)).isEqualTo(new Coordinate(70, 80));
			assertThat(kante.getVerlaufRechts().get().getCoordinateN(1)).isEqualTo(new Coordinate(80, 90));
		}

		@Test
		public void testAktualisiereVerlaeufeUndGeometrien_identischeGeometrie_keinEvent() {
			// arrange
			Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 200), QuellSystem.DLM)
				.build();
			Knoten bisKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 300), QuellSystem.DLM)
				.build();
			Kante kante = KanteTestDataProvider.fromKnoten(vonKnoten, bisKnoten)
				.id(2L)
				.version(3L)
				.quelle(QuellSystem.RadVis)
				.dlmId(null)
				.isZweiseitig(true)
				.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
					.isZweiseitig(true)
					.build())
				.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
					.isZweiseitig(true)
					.build())
				.build();
			when(kantenRepositoryMock.findById(kante.getId())).thenReturn(Optional.of(kante));

			KanteGeometrien kanteGeometrien = KanteGeometrien.builder()
				.id(kante.getId())
				.version(kante.getVersion())
				.geometry(kante.getGeometry())
				.build();

			// act
			netzService.aktualisiereVerlaeufeUndGeometrien(List.of(kanteGeometrien));

			// assert
			domainPublisherMock.verifyNoInteractions();
		}

		@Test
		void testDeleteKante() {
			// arrange
			ArgumentCaptor<Kante> kanteCaptor = ArgumentCaptor.forClass(Kante.class);
			doNothing().when(kantenRepositoryMock).delete(kanteCaptor.capture());
			Kante kanteToDelete = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).id(1L).build();

			// act
			KanteDeleteStatistik statistik = new KanteDeleteStatistik();
			netzService.deleteKante(kanteToDelete, statistik);

			// assert
			domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(eventCaptor.capture()));
			domainPublisherMock.verifyNoMoreInteractions();
			assertThat(eventCaptor.getValue()).usingRecursiveComparison().ignoringFields("geometry", "datum")
				.isEqualTo(
					new KantenDeletedEvent(List.of(kanteToDelete.getId()), List.of(kanteToDelete.getGeometry()),
						NetzAenderungAusloeser.RADVIS_KANTE_LOESCHEN, LocalDateTime.now(), statistik));
		}

		@Test
		void testDeleteKante_NotRadVis() {
			// arrange
			ArgumentCaptor<Kante> kanteCaptor = ArgumentCaptor.forClass(Kante.class);
			doNothing().when(kantenRepositoryMock).delete(kanteCaptor.capture());

			// act & assert

			assertThatThrownBy(() -> netzService.deleteKante(
				KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM).id(1L).build(),
				new KanteDeleteStatistik())).isInstanceOf(
					RequireViolation.class);

			domainPublisherMock.verifyNoInteractions();

		}
	}

	@Test
	public void testAktualisiereKantenZweiseitig() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.version(2L)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
				.isZweiseitig(true)
				.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
				.isZweiseitig(true)
				.build())
			.build();
		when(kantenRepositoryMock.findById(kante.getId())).thenReturn(Optional.of(kante));

		Kante kante2 = KanteTestDataProvider.withDefaultValues().id(2L).version(3L).isZweiseitig(false).build();
		when(kantenRepositoryMock.findById(kante2.getId())).thenReturn(Optional.of(kante2));

		HashMap<VersionedId, Boolean> versionToZweiseitigkeitMap = new HashMap<VersionedId, Boolean>();
		versionToZweiseitigkeitMap.put(new VersionedId(1L, 2L), false);
		versionToZweiseitigkeitMap.put(new VersionedId(2L, 3L), false);

		// act
		netzService.aktualisiereKantenZweiseitig(versionToZweiseitigkeitMap);

		// assert
		assertThat(kante.isZweiseitig()).isFalse();
		assertThat(kante2.isZweiseitig()).isFalse();
	}

	@Test
	public void testAktualisiereVerlaeufeUndGeometrien_wirftKeineExceptionBeiDlmKanteMitUnveränderterGeometrie() {
		// arrange
		LineString geometry = GeometryTestdataProvider.createLineString(new Coordinate(10, 20), new Coordinate(20, 30));
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(2L)
			.version(3L)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
				.isZweiseitig(true)
				.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
				.isZweiseitig(true)
				.build())
			.geometry(geometry)
			.build();
		when(kantenRepositoryMock.findById(kante.getId())).thenReturn(Optional.of(kante));

		KanteGeometrien kanteGeometrien = KanteGeometrien.builder()
			.id(kante.getId())
			.version(kante.getVersion())
			.geometry(geometry)
			.verlaufLinks(GeometryTestdataProvider.createLineString(new Coordinate(10, 20), new Coordinate(20, 30)))
			.verlaufRechts(GeometryTestdataProvider.createLineString(new Coordinate(70, 80), new Coordinate(80, 90)))
			.build();

		// act
		netzService.aktualisiereVerlaeufeUndGeometrien(List.of(kanteGeometrien));

		// assert
		assertThat(kante.getGeometry().getCoordinates()).isEqualTo(geometry.getCoordinates());
		assertThat(kante.getVerlaufLinks()).isPresent();
		assertThat(kante.getVerlaufLinks().get().getCoordinateN(0)).isEqualTo(new Coordinate(10, 20));
		assertThat(kante.getVerlaufLinks().get().getCoordinateN(1)).isEqualTo(new Coordinate(20, 30));
		assertThat(kante.getVerlaufRechts()).isPresent();
		assertThat(kante.getVerlaufRechts().get().getCoordinateN(0)).isEqualTo(new Coordinate(70, 80));
		assertThat(kante.getVerlaufRechts().get().getCoordinateN(1)).isEqualTo(new Coordinate(80, 90));
	}

	@Test
	public void testAktualisiereGeschwindigkeitAttribute() {
		// arrange
		VersionedId versionedId = new VersionedId(1L, 2L);

		GeschwindigkeitAttribute oldGeschwindigkeitAttribute = GeschwindigkeitAttribute.builder()
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH)
			.build();
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.id(versionedId.getId())
			.version(versionedId.getVersion())
			.geschwindigkeitAttribute(List.of(oldGeschwindigkeitAttribute))
			.build();
		when(geschwindigkeitAttributGruppeRepository.findById(geschwindigkeitAttributGruppe.getId())).thenReturn(
			Optional.of(geschwindigkeitAttributGruppe));

		GeschwindigkeitAttribute newGeschwindigkeitAttribute = GeschwindigkeitAttribute.builder()
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH)
			.build();

		HashMap<VersionedId, List<GeschwindigkeitAttribute>> versionedIdToAttributeMap = new HashMap<>();
		versionedIdToAttributeMap.put(versionedId, List.of(newGeschwindigkeitAttribute));

		// act
		netzService.aktualisiereGeschwindigkeitAttribute(versionedIdToAttributeMap);

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute()).hasSize(1);
		assertThat(new ArrayList<>(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute()).get(0)).isEqualTo(
			newGeschwindigkeitAttribute);
	}

	@Test
	public void testAktualisiereZustaendigkeitsAttribute() {
		// arrange
		VersionedId versionedId = new VersionedId(1L, 2L);

		ZustaendigkeitAttribute oldZustaendigkeitAttribute = ZustaendigkeitAttribute.builder()
			.vereinbarungsKennung(VereinbarungsKennung.of("abc123"))
			.build();
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = ZustaendigkeitAttributGruppe.builder()
			.id(versionedId.getId())
			.version(versionedId.getVersion())
			.zustaendigkeitAttribute(List.of(oldZustaendigkeitAttribute))
			.build();
		when(zustaendigkeitAttributGruppeRepository.findById(zustaendigkeitAttributGruppe.getId())).thenReturn(
			Optional.of(zustaendigkeitAttributGruppe));

		ZustaendigkeitAttribute newZustaendigkeitAttribute = ZustaendigkeitAttribute.builder()
			.vereinbarungsKennung(VereinbarungsKennung.of("ähmmm ... äh ... test123? ..."))
			.build();

		HashMap<VersionedId, List<ZustaendigkeitAttribute>> versionedIdToAttributeMap = new HashMap<>();
		versionedIdToAttributeMap.put(versionedId, List.of(newZustaendigkeitAttribute));

		// act
		netzService.aktualisiereZustaendigkeitsAttribute(versionedIdToAttributeMap);

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute().get(0)).isEqualTo(
			newZustaendigkeitAttribute);
	}

	@Test
	public void testAktualisiereFahrtrichtung() {
		// arrange
		VersionedId versionedId = new VersionedId(1L, 2L);

		FahrtrichtungAttributGruppe oldFahrtrichtungAttributGruppe = FahrtrichtungAttributGruppe.builder()
			.id(versionedId.getId())
			.version(versionedId.getVersion())
			.isZweiseitig(true)
			.build();
		when(fahrtrichtungAttributGruppeRepository.findById(versionedId.getId())).thenReturn(
			Optional.of(oldFahrtrichtungAttributGruppe));

		FahrtrichtungAttributGruppe newFahrtrichtungAttributGruppe = FahrtrichtungAttributGruppe.builder()
			.id(versionedId.getId())
			.version(versionedId.getVersion())
			.isZweiseitig(true)
			.fahrtrichtungLinks(Richtung.BEIDE_RICHTUNGEN)
			.fahrtrichtungRechts(Richtung.IN_RICHTUNG)
			.build();

		HashMap<VersionedId, FahrtrichtungAttributGruppe> versionedIdToAttributeMap = new HashMap<>();
		versionedIdToAttributeMap.put(versionedId, newFahrtrichtungAttributGruppe);

		// act
		netzService.aktualisiereFahrtrichtung(versionedIdToAttributeMap);

		// assert
		assertThat(oldFahrtrichtungAttributGruppe.getFahrtrichtungRechts()).isEqualTo(Richtung.IN_RICHTUNG);
		assertThat(oldFahrtrichtungAttributGruppe.getFahrtrichtungLinks()).isEqualTo(Richtung.BEIDE_RICHTUNGEN);
	}

	@Test
	public void testAktualisiereFahrtrichtung_wirftExceptionBeiInvalidenRichtungen() {
		// arrange
		VersionedId versionedId = new VersionedId(1L, 2L);

		FahrtrichtungAttributGruppe oldFahrtrichtungAttributGruppe = FahrtrichtungAttributGruppe.builder()
			.id(versionedId.getId())
			.version(versionedId.getVersion())
			.build();
		when(fahrtrichtungAttributGruppeRepository.findById(versionedId.getId())).thenReturn(
			Optional.of(oldFahrtrichtungAttributGruppe));

		FahrtrichtungAttributGruppe newFahrtrichtungAttributGruppe = FahrtrichtungAttributGruppe.builder()
			.id(versionedId.getId())
			.version(versionedId.getVersion())
			.isZweiseitig(true)
			.fahrtrichtungLinks(Richtung.BEIDE_RICHTUNGEN)
			.fahrtrichtungRechts(Richtung.IN_RICHTUNG)
			.build();

		HashMap<VersionedId, FahrtrichtungAttributGruppe> versionedIdToAttributeMap = new HashMap<>();
		versionedIdToAttributeMap.put(versionedId, newFahrtrichtungAttributGruppe);

		// act + assert
		assertThatThrownBy(() -> netzService.aktualisiereFahrtrichtung(versionedIdToAttributeMap))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("Unterschiedliche Fahrtrichtungen bei einseitiger Kante");

		assertThat(oldFahrtrichtungAttributGruppe.getFahrtrichtungRechts()).isEqualTo(Richtung.UNBEKANNT);
		assertThat(oldFahrtrichtungAttributGruppe.getFahrtrichtungLinks()).isEqualTo(Richtung.UNBEKANNT);
	}

	@Test
	public void testAktualisiereKnoten_mitQuerungshilfe() {
		// arrange
		Long knotenId = 1L;
		Long knotenVersion = 2L;
		Long gemeinde = 3L;
		Kommentar kommentar = Kommentar.of("./kommen.tar");
		Zustandsbeschreibung zustandsbeschreibung = Zustandsbeschreibung.of("schlecht");
		KnotenForm knotenForm = KnotenForm.MITTELINSEL_EINFACH;
		QuerungshilfeDetails querungshilfeDetails = QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL;

		Knoten knoten = Knoten.builder()
			.id(knotenId)
			.version(knotenVersion)
			.quelle(QuellSystem.RadVis)
			.build();
		when(knotenRepositoryMock.findById(knotenId)).thenReturn(Optional.of(knoten));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.KREIS)
			.name("\"Ein Kreis ist eine ebene geometrische Figur.\" - Wikipedia")
			.build();
		when(verwaltungseinheitResolver.resolve(gemeinde)).thenReturn(organisation);

		// act
		netzService.aktualisiereKnoten(knotenId, knotenVersion, gemeinde, kommentar, zustandsbeschreibung, knotenForm,
			querungshilfeDetails, null, null);

		// assert
		assertThat(knoten.getKnotenAttribute()).isNotNull();
		assertThat(knoten.getKnotenAttribute().getKommentar()).contains(kommentar);
		assertThat(knoten.getKnotenAttribute().getKnotenForm()).contains(knotenForm);
		assertThat(knoten.getKnotenAttribute().getQuerungshilfeDetails()).contains(querungshilfeDetails);
		assertThat(knoten.getKnotenAttribute().getZustandsbeschreibung()).contains(zustandsbeschreibung);
		assertThat(knoten.getKnotenAttribute().getGemeinde()).contains(organisation);
		assertThat(knoten.getKnotenAttribute().getBauwerksmangel()).isEmpty();
		assertThat(knoten.getKnotenAttribute().getBauwerksmangelArt()).isEmpty();
	}

	@Test
	public void testAktualisiereKnoten_mitBauwerksmangel() {
		// arrange
		Long knotenId = 1L;
		Long knotenVersion = 2L;
		Long gemeinde = 3L;
		Kommentar kommentar = Kommentar.of("./kommen.tar");
		Zustandsbeschreibung zustandsbeschreibung = Zustandsbeschreibung.of("schlecht");
		KnotenForm knotenForm = KnotenForm.UEBERFUEHRUNG;
		Bauwerksmangel bauwerksmangel = Bauwerksmangel.VORHANDEN;
		Set<BauwerksmangelArt> bauwerksmangelArt = Set.of(BauwerksmangelArt.ANDERER_MANGEL);

		Knoten knoten = Knoten.builder()
			.id(knotenId)
			.version(knotenVersion)
			.quelle(QuellSystem.RadVis)
			.build();
		when(knotenRepositoryMock.findById(knotenId)).thenReturn(Optional.of(knoten));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.KREIS)
			.name("\"Ein Kreis ist eine ebene geometrische Figur.\" - Wikipedia")
			.build();
		when(verwaltungseinheitResolver.resolve(gemeinde)).thenReturn(organisation);

		// act
		netzService.aktualisiereKnoten(knotenId, knotenVersion, gemeinde, kommentar, zustandsbeschreibung, knotenForm,
			null, bauwerksmangel, bauwerksmangelArt);

		// assert
		assertThat(knoten.getKnotenAttribute()).isNotNull();
		assertThat(knoten.getKnotenAttribute().getQuerungshilfeDetails()).isEmpty();
		assertThat(knoten.getKnotenAttribute().getBauwerksmangel()).contains(bauwerksmangel);
		assertThat(knoten.getKnotenAttribute().getBauwerksmangelArt()).contains(bauwerksmangelArt);
	}

	@Test
	public void testAktualisiereKnoten_wirftExceptionBeiRadNetzKnoten() {
		// arrange
		Long knotenId = 1L;
		Long knotenVersion = 2L;
		Long gemeinde = 3L;
		Kommentar kommentar = Kommentar.of("./kommen.tar");
		Zustandsbeschreibung zustandsbeschreibung = Zustandsbeschreibung.of("schlecht");
		KnotenForm knotenForm = KnotenForm.FAHRBAHNEINENGUNG;

		Knoten knoten = Knoten.builder()
			.id(knotenId)
			.version(knotenVersion)
			.quelle(QuellSystem.RadNETZ)
			.build();
		when(knotenRepositoryMock.findById(knotenId)).thenReturn(Optional.of(knoten));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.KREIS)
			.name("\"Ein Kreis ist eine ebene geometrische Figur.\" - Wikipedia")
			.build();
		when(verwaltungseinheitResolver.resolve(gemeinde)).thenReturn(organisation);

		// act + assert
		assertThatThrownBy(
			() -> netzService.aktualisiereKnoten(knotenId, knotenVersion, gemeinde, kommentar, zustandsbeschreibung,
				knotenForm, null, null, null))
					.isInstanceOf(AccessDeniedException.class)
					.hasMessage("RadNETZ-Knoten dürfen nicht bearbeitet werden.");

		assertThat(knoten.getKnotenAttribute().getKnotenForm()).isEmpty();
		assertThat(knoten.getKnotenAttribute().getKommentar()).isEmpty();
		assertThat(knoten.getKnotenAttribute().getZustandsbeschreibung()).isEmpty();
		assertThat(knoten.getKnotenAttribute().getGemeinde()).isEmpty();

	}

	@Test
	public void findErsatzKnoten_filterExcludeIds_returnsNearestMatch() {
		// arrange
		Long knotenId1 = 1l;
		Long knotenId2 = 2l;
		Long knotenId3 = 3l;
		Long knotenId4 = 4l;
		Long zuErsetzenderKnotenId = 5l;

		when(knotenRepositoryMock.findErsatzKnotenCandidates(eq(zuErsetzenderKnotenId), anyDouble()))
			.thenReturn(List.of(
				KnotenTestDataProvider.withDefaultValues().id(knotenId1).build(),
				KnotenTestDataProvider.withDefaultValues().id(knotenId2).build(),
				KnotenTestDataProvider.withDefaultValues().id(knotenId3).build(),
				KnotenTestDataProvider.withDefaultValues().id(knotenId4).build()));

		// act
		Optional<Knoten> findErsatzKnoten = netzService.findErsatzKnoten(zuErsetzenderKnotenId,
			List.of(knotenId1, knotenId3));

		// assert
		assertThat(findErsatzKnoten).isPresent();
		assertThat(findErsatzKnoten.get().getId()).isEqualTo(knotenId2);
	}

	@Test
	public void findErsatzKnoten_filterExcludeIds_returnsNoMatch() {
		// arrange
		Long knotenId1 = 1l;
		Long knotenId2 = 2l;
		Long zuErsetzenderKnotenId = 5l;

		when(knotenRepositoryMock.findErsatzKnotenCandidates(eq(zuErsetzenderKnotenId), anyDouble()))
			.thenReturn(List.of(
				KnotenTestDataProvider.withDefaultValues().id(knotenId1).build(),
				KnotenTestDataProvider.withDefaultValues().id(knotenId2).build()));

		// act
		Optional<Knoten> findErsatzKnoten = netzService.findErsatzKnoten(zuErsetzenderKnotenId,
			List.of(knotenId1, knotenId2));

		// assert
		assertThat(findErsatzKnoten).isEmpty();
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_keineKanten() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			any(), any())).thenReturn(
				Collections.emptyList());

		// Act
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		// Assert
		assertThat(seitenbezug).isEmpty();
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_ohnePassendeRadverkehrsfuehrungen() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		Kante kanteRechtsKeineRadverkehrsfuehrung = KanteTestDataProvider.withCoordinates(new Coordinate[] {
			new Coordinate(10, 0), new Coordinate(10, 50) })
			.id(2L)
			.build();
		Kante kanteRechtsUnbekannteRadverkehrsfuehrung = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(10,
			50, 10, 100,
			Radverkehrsfuehrung.UNBEKANNT)
			.id(3L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.RECHTS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(kante
						.getGeometry()), kanteRechtsKeineRadverkehrsfuehrung, kanteRechtsKeineRadverkehrsfuehrung
							.getGeometry()),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.5, 1).toSegment(kante
						.getGeometry()), kanteRechtsUnbekannteRadverkehrsfuehrung,
						kanteRechtsUnbekannteRadverkehrsfuehrung.getGeometry())
				));

		Kante kanteLinksKeineRadverkehrsfuehrung = KanteTestDataProvider.withCoordinates(new Coordinate[] {
			new Coordinate(-10, 0), new Coordinate(-10, 50) })
			.id(2L)
			.build();
		Kante kanteLinksUnbekannteRadverkehrsfuehrung = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(-10,
			50, -10, 100,
			Radverkehrsfuehrung.UNBEKANNT)
			.id(3L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.LINKS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(kante
						.getGeometry()), kanteLinksKeineRadverkehrsfuehrung, kanteLinksKeineRadverkehrsfuehrung
							.getGeometry()),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.5, 1).toSegment(kante
						.getGeometry()), kanteLinksUnbekannteRadverkehrsfuehrung,
						kanteLinksUnbekannteRadverkehrsfuehrung.getGeometry())
				));

		// Act
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		// Assert
		assertThat(seitenbezug).isEmpty();
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_ohnePassendeRadverkehrsfuehrungAberMitStrassennummer() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		Kante kanteRechtsKeineRadverkehrsfuehrung = KanteTestDataProvider.withCoordinates(new Coordinate[] {
			new Coordinate(10, 0), new Coordinate(10, 50) })
			.id(2L)
			.build();
		kanteRechtsKeineRadverkehrsfuehrung.getKantenAttributGruppe().getKantenAttribute().setStrassenNummer(
			StrassenNummer.of("L123"));
		Kante kanteRechtsUnbekannteRadverkehrsfuehrung = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(10,
			50, 10, 100,
			Radverkehrsfuehrung.UNBEKANNT)
			.id(3L)
			.build();
		kanteRechtsUnbekannteRadverkehrsfuehrung.getKantenAttributGruppe().getKantenAttribute().setStrassenNummer(
			StrassenNummer.of("L123"));
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.RECHTS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(kante
						.getGeometry()), kanteRechtsKeineRadverkehrsfuehrung, kanteRechtsKeineRadverkehrsfuehrung
							.getGeometry()),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.5, 1).toSegment(kante
						.getGeometry()), kanteRechtsUnbekannteRadverkehrsfuehrung,
						kanteRechtsUnbekannteRadverkehrsfuehrung.getGeometry())
				));

		Kante kanteLinksKeineRadverkehrsfuehrung = KanteTestDataProvider.withCoordinates(new Coordinate[] {
			new Coordinate(-10, 0), new Coordinate(-10, 50) })
			.id(2L)
			.build();
		Kante kanteLinksUnbekannteRadverkehrsfuehrung = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(-10,
			50, -10, 100,
			Radverkehrsfuehrung.UNBEKANNT)
			.id(3L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.LINKS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(kante
						.getGeometry()), kanteLinksKeineRadverkehrsfuehrung, kanteLinksKeineRadverkehrsfuehrung
							.getGeometry()),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.5, 1).toSegment(kante
						.getGeometry()), kanteLinksUnbekannteRadverkehrsfuehrung,
						kanteLinksUnbekannteRadverkehrsfuehrung.getGeometry())
				));

		// Act
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		// Assert
		assertThat(seitenbezug).contains(Seitenbezug.RECHTS);
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_mehrereParalleleKantenRechts() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		Kante kanteRechtsParallel1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(10, 0, 10, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		Kante kanteRechtsNichtParallel = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 100, 55, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		Kante kanteRechtsParallel2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(11, 50, 11, 150,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(4L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.RECHTS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(kante
						.getGeometry()), kanteRechtsParallel1, kanteRechtsParallel1.getGeometry()),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.9, 1).toSegment(kante
						.getGeometry()), kanteRechtsNichtParallel, LinearReferenzierterAbschnitt.of(0.8, 1).toSegment(
							kanteRechtsNichtParallel.getGeometry())),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.5, 1).toSegment(kante
						.getGeometry()), kanteRechtsParallel2, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(
							kanteRechtsParallel2.getGeometry()))
				));

		// Act
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		// Assert
		assertThat(seitenbezug).contains(Seitenbezug.RECHTS);
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_mehrereParalleleKantenLinks() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		Kante kanteLinksParallel1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(-10, 0, -10, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		Kante kanteLinksNichtParallel = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 100, -55, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		Kante kanteLinksParallel2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(-11, 50, -11, 150,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(4L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.LINKS), any())).thenReturn(
				List
					.of(
						new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(kante
							.getGeometry()), kanteLinksParallel1, kanteLinksParallel1.getGeometry()),
						new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.9, 1).toSegment(kante
							.getGeometry()), kanteLinksNichtParallel, LinearReferenzierterAbschnitt.of(0.8, 1)
								.toSegment(kanteLinksNichtParallel.getGeometry())),
						new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.5, 1).toSegment(kante
							.getGeometry()), kanteLinksParallel2, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(
								kanteLinksParallel2.getGeometry()))
					));

		// Act
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		// Assert
		assertThat(seitenbezug).contains(Seitenbezug.LINKS);
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_mehrereParalleleKantenBeidseitig() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		Kante kanteRechtsParallel1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(10, 0, 10, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		Kante kanteRechtsNichtParallel = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 100, 55, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		Kante kanteRechtsParallel2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(11, 50, 11, 150,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(4L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.RECHTS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(kante
						.getGeometry()), kanteRechtsParallel1, kanteRechtsParallel1.getGeometry()),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.9, 1).toSegment(kante
						.getGeometry()), kanteRechtsNichtParallel, LinearReferenzierterAbschnitt.of(0.8, 1).toSegment(
							kanteRechtsNichtParallel.getGeometry())),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.5, 1).toSegment(kante
						.getGeometry()), kanteRechtsParallel2, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(
							kanteRechtsParallel2.getGeometry()))
				));

		Kante kanteLinksParallel1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(-10, 0, -10, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		Kante kanteLinksNichtParallel = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 100, -55, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		Kante kanteLinksParallel2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(-11, 50, -11, 150,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(4L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.LINKS), any())).thenReturn(
				List
					.of(
						new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(kante
							.getGeometry()), kanteLinksParallel1, kanteLinksParallel1.getGeometry()),
						new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.9, 1).toSegment(kante
							.getGeometry()), kanteLinksNichtParallel, LinearReferenzierterAbschnitt.of(0.8, 1)
								.toSegment(kanteLinksNichtParallel.getGeometry())),
						new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.5, 1).toSegment(kante
							.getGeometry()), kanteLinksParallel2, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(
								kanteLinksParallel2.getGeometry()))
					));

		// Act
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		// Assert
		assertThat(seitenbezug).contains(Seitenbezug.BEIDSEITIG);
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_mehrereZuKurzeParalleleKanten_ueberdeckenZuWenig() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		// Alle überdecken zusammen nur 9% der kante -> zu wenig
		Kante kanteRechtsParallel1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(10, 10, 10, 19,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		Kante kanteRechtsParallel2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(11, 10, 11, 16,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		Kante kanteRechtsParallel3 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(12, 13, 12, 19,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(4L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.RECHTS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.1, 0.19).toSegment(kante
						.getGeometry()), kanteRechtsParallel1, kanteRechtsParallel1.getGeometry()),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.1, 0.19).toSegment(kante
						.getGeometry()), kanteRechtsParallel2, kanteRechtsParallel2.getGeometry()),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.1, 0.19).toSegment(kante
						.getGeometry()), kanteRechtsParallel3, kanteRechtsParallel3.getGeometry())
				));

		// Act
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		// Assert
		assertThat(seitenbezug).isEmpty();
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_mehrereZuKurzeParalleleKanten_ueberdeckenGenug() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		// Für sich überdecken sie jeweils nur 5% und damit zu wenig, insgesamt aber genug
		Kante kanteRechtsParallel1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(10, 0, 10, 5,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		Kante kanteRechtsParallel2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(10, 4, 10, 9,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		Kante kanteRechtsParallel3 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(10, 8, 10, 13,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(4L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.RECHTS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.05).toSegment(kante
						.getGeometry()), kanteRechtsParallel1, kanteRechtsParallel1.getGeometry()),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.04, 0.09).toSegment(kante
						.getGeometry()), kanteRechtsParallel2, kanteRechtsParallel2.getGeometry()),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.08, 0.13).toSegment(kante
						.getGeometry()), kanteRechtsParallel3, kanteRechtsParallel3.getGeometry())
				));

		// Act
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		// Assert
		assertThat(seitenbezug).contains(Seitenbezug.RECHTS);
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_gefundeneKantenNichtParallel() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		// Diagonale Kanten im 45° und -45° Winkel
		Kante kanteRechts1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(5, 0, 55, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		Kante kanteRechts2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 100, 55, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.RECHTS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.1).toSegment(kante
						.getGeometry()), kanteRechts1, LinearReferenzierterAbschnitt.of(0, 0.2).toSegment(kanteRechts1
							.getGeometry())),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.9, 1).toSegment(kante
						.getGeometry()), kanteRechts2, LinearReferenzierterAbschnitt.of(0.8, 1).toSegment(kanteRechts2
							.getGeometry()))
				));
		Kante kanteLinks1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(-5, 0, -55, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		Kante kanteLinks2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 100, -55, 50,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.LINKS), any())).thenReturn(
				List
					.of(
						new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.1).toSegment(kante
							.getGeometry()), kanteLinks1, LinearReferenzierterAbschnitt.of(0, 0.2).toSegment(kanteLinks1
								.getGeometry())),
						new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.9, 1).toSegment(kante
							.getGeometry()), kanteLinks2, LinearReferenzierterAbschnitt.of(0.8, 1).toSegment(kanteLinks2
								.getGeometry()))
					));

		// Act
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		// Assert
		assertThat(seitenbezug).isEmpty();
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_teilparalleleKantenRechts() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		// Paralleles Teilstück ist in der Nähe
		Kante kanteRechts1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(new Coordinate[] {
			new Coordinate(20, 200),
			new Coordinate(5, 100),
			new Coordinate(5, 80),
		}, Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		// Paralleles Teilstück ist NICHT in der Nähe
		Kante kanteRechts2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(new Coordinate[] {
			new Coordinate(5, 80),
			new Coordinate(20, 100),
			new Coordinate(20, 200),
		}, Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.RECHTS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.1).toSegment(kante
						.getGeometry()), kanteRechts1, LinearReferenzierterAbschnitt.of(0, 0.2).toSegment(kanteRechts1
							.getGeometry())),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.9, 1).toSegment(kante
						.getGeometry()), kanteRechts2, LinearReferenzierterAbschnitt.of(0.8, 1).toSegment(kanteRechts2
							.getGeometry()))
				));

		// Act
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		// Assert
		assertThat(seitenbezug).contains(Seitenbezug.RECHTS);
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_teilparalleleKantenLinks() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		// Paralleles Teilstück ist in der Nähe
		Kante kanteLinks1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(new Coordinate[] {
			new Coordinate(-20, 200),
			new Coordinate(-5, 100),
			new Coordinate(-5, 80),
		}, Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		// Paralleles Teilstück ist NICHT in der Nähe
		Kante kanteLinks2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(new Coordinate[] {
			new Coordinate(-5, 80),
			new Coordinate(-20, 100),
			new Coordinate(-20, 200),
		}, Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.LINKS), any())).thenReturn(
				List
					.of(
						new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.1).toSegment(kante
							.getGeometry()), kanteLinks1, LinearReferenzierterAbschnitt.of(0, 0.2).toSegment(kanteLinks1
								.getGeometry())),
						new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.9, 1).toSegment(kante
							.getGeometry()), kanteLinks2, LinearReferenzierterAbschnitt.of(0.8, 1).toSegment(kanteLinks2
								.getGeometry()))
					));

		// Act
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		// Assert
		assertThat(seitenbezug).contains(Seitenbezug.LINKS);
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_thresholdRelativeGesamtlaengeRechtsWirdKorrektGeprueft() {
		// Arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();
		Kante kante2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(1, 0, 1, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();

		// Deckt ~10% (etwas mehr, um float-Ungenauigkeiten zu umgehen) der Gesamtlänge ab -> ok
		Kante kanteRechtsParallelOk = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(10, 80.5, 10, 91,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		// Deckt 9% der Gesamtlänge ab -> nicht mehr ok
		Kante kanteRechtsParallelZuKurz = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(11, 91, 11, 101,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(4L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante1), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.RECHTS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante1, LinearReferenzierterAbschnitt.of(0.805, 0.91).toSegment(
						kante1.getGeometry()), kanteRechtsParallelOk, kanteRechtsParallelOk.getGeometry())
				));
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante2), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.RECHTS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante2, LinearReferenzierterAbschnitt.of(0.91, 1).toSegment(kante2
						.getGeometry()), kanteRechtsParallelZuKurz, LinearReferenzierterAbschnitt.of(0, 0.9).toSegment(
							kanteRechtsParallelZuKurz.getGeometry()))
				));

		// Act & Assert
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante1,
			LinearReferenzierterAbschnitt.of(0, 1));
		assertThat(seitenbezug).contains(Seitenbezug.RECHTS);

		seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante2, LinearReferenzierterAbschnitt.of(0, 1));
		assertThat(seitenbezug).isEmpty();
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_thresholdRelativeGesamtlaengeLinksWirdKorrektGeprueft() {
		// Arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();
		Kante kante2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(-1, 0, -1, 100,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();

		// Deckt ~10% (etwas mehr, um float-Ungenauigkeiten zu umgehen) der Gesamtlänge ab -> ok
		Kante kanteRechtsParallelOk = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(-10, 80.5, -10, 91,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		// Deckt 9% der Gesamtlänge ab -> nicht mehr ok
		Kante kanteRechtsParallelZuKurz = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(-11, 91, -11, 101,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(4L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante1), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.LINKS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante1, LinearReferenzierterAbschnitt.of(0.805, 0.91).toSegment(
						kante1.getGeometry()), kanteRechtsParallelOk, kanteRechtsParallelOk.getGeometry())
				));
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante2), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.LINKS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante2, LinearReferenzierterAbschnitt.of(0.91, 1).toSegment(kante2
						.getGeometry()), kanteRechtsParallelZuKurz, LinearReferenzierterAbschnitt.of(0, 0.9).toSegment(
							kanteRechtsParallelZuKurz.getGeometry()))
				));

		// Act & Assert
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante1,
			LinearReferenzierterAbschnitt.of(0, 1));
		assertThat(seitenbezug).contains(Seitenbezug.LINKS);

		seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante2, LinearReferenzierterAbschnitt.of(0, 1));
		assertThat(seitenbezug).isEmpty();
	}

	@Test
	public void getSeiteMitParallelenStrassenKanten_miniAbschnitteSindErlaubt() {
		// Arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 1,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(1L)
			.build();

		Kante kanteRechts1 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0, 0, 0.5,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(2L)
			.build();
		Kante kanteRechts2 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0.5, 0, 0.501,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(3L)
			.build();
		Kante kanteRechts3 = KanteTestDataProvider.withCoordinatesAndRadverkehrsfuehrung(0, 0.501, 0, 1,
			Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.id(4L)
			.build();
		when(kantenRepositoryMock.getNahegelegeneKantenAufSeite(eq(kante), eq(LinearReferenzierterAbschnitt.of(0, 1)),
			eq(Seitenbezug.RECHTS), any())).thenReturn(
				List.of(
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(kante
						.getGeometry()), kanteRechts1, LinearReferenzierterAbschnitt.of(0, 0.5).toSegment(kanteRechts1
							.getGeometry())),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.5, 0.501).toSegment(kante
						.getGeometry()), kanteRechts2, LinearReferenzierterAbschnitt.of(0.5, 0.501).toSegment(
							kanteRechts2.getGeometry())),
					new NahegelegeneneKantenDbView(kante, LinearReferenzierterAbschnitt.of(0.501, 1).toSegment(kante
						.getGeometry()), kanteRechts3, LinearReferenzierterAbschnitt.of(0.501, 1).toSegment(kanteRechts3
							.getGeometry()))
				));

		// Act & Assert
		Optional<Seitenbezug> seitenbezug = netzService.getSeiteMitParallelenStrassenKanten(kante,
			LinearReferenzierterAbschnitt.of(0, 1));
		assertThat(seitenbezug).contains(Seitenbezug.RECHTS);
	}

	private static GeometryFactory createGeometryFactory(Integer srid) {
		return KoordinatenReferenzSystem.ofSrid(srid).getGeometryFactory();
	}

	private static LineString createLineString(final String lineWkt, Integer srid) throws ParseException {
		WKTReader reader = new WKTReader(createGeometryFactory(srid));
		return (LineString) reader.read(lineWkt);
	}
}
