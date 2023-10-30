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

package de.wps.radvis.backend.fahrradroute.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.data.util.Lazy;

import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.entity.Importprotokoll;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescriptionTestDataProvider;
import de.wps.radvis.backend.common.domain.valueObject.ImportprotokollTyp;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteNetzBezugAenderung;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteVariante;
import de.wps.radvis.backend.fahrradroute.domain.entity.ProfilInformationenUpdateStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.UpdateAbgeleiteteRoutenInfoStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteVarianteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.event.FahrradrouteCreatedEvent;
import de.wps.radvis.backend.fahrradroute.domain.event.FahrradrouteUpdatedEvent;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteNetzBezugAenderungRepository;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteViewRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteProfilEigenschaften;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Hoehenunterschied;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.VarianteKategorie;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.exception.KeineRouteGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilRoutingResult;
import de.wps.radvis.backend.matching.domain.valueObject.RoutingResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.event.KanteDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KanteTopologieChangedEvent;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;

class FahrradrouteServiceTest {
	@Mock
	FahrradrouteRepository fahrradrouteRepository;
	@Mock
	FahrradrouteViewRepository fahrradrouteViewRepository;
	@Mock
	GraphhopperRoutingRepository graphhopperRoutingRepository;
	@Mock
	FahrradrouteNetzBezugAenderungRepository fahrradrouteNetzBezugAenderungRepository;
	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Mock
	BenutzerService benutzerService;

	@Captor
	private ArgumentCaptor<List<Fahrradroute>> fahrradroutenCaptor;
	@Captor
	private ArgumentCaptor<List<String>> jobNamesCaptor;

	private MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;
	private FahrradrouteService service;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
		service = new FahrradrouteService(fahrradrouteRepository, fahrradrouteViewRepository,
			Lazy.of(graphhopperRoutingRepository), fahrradrouteNetzBezugAenderungRepository,
			jobExecutionDescriptionRepository, benutzerService);
	}

	@AfterEach
	void cleanUp() {
		domainPublisherMock.close();
	}

	@Test
	void protokolliereNetzbezugAenderungNurFuerRadVisTyp_kanteLoeschen() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(123L).build();
		Fahrradroute radvisRoute = FahrradrouteTestDataProvider.onKante(kante).id(2L)
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE).build();
		Fahrradroute toubizRoute = FahrradrouteTestDataProvider.onKante(kante).id(3L)
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE).build();
		Fahrradroute tfisRoute = FahrradrouteTestDataProvider.onKante(kante).id(4L)
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE).build();

		when(fahrradrouteRepository.findByKanteIdInNetzBezug(kante.getId()))
			.thenReturn(List.of(radvisRoute, toubizRoute, tfisRoute));

		KanteDeletedEvent deleteEvent = new KanteDeletedEvent(
			kante.getId(), GeometryTestdataProvider.createLineString(), NetzAenderungAusloeser.DLM_REIMPORT_JOB,
			LocalDateTime.now());
		ArgumentCaptor<FahrradrouteNetzBezugAenderung> captor = ArgumentCaptor
			.forClass(FahrradrouteNetzBezugAenderung.class);
		when(fahrradrouteNetzBezugAenderungRepository.save(captor.capture())).thenReturn(null);

		// act
		service.onKanteGeloescht(deleteEvent);

		// assert
		verify(fahrradrouteNetzBezugAenderungRepository, times(1)).save(any());
		assertThat(captor.getValue().getFahrradroute().getFahrradrouteTyp()).isEqualTo(FahrradrouteTyp.RADVIS_ROUTE);
		assertThat(captor.getValue().isAenderungInHauptroute()).isTrue();
	}

	@Test
	void protokolliereNetzbezugAenderungAuchFuerVarianten_kanteLoeschen() {
		// arrange
		// route hat eine Kante, die ist jedoch unveraendert
		Kante kanteInHauptroute = KanteTestDataProvider.withDefaultValues().id(1L).build();
		// aber Variante hat eine Kante, die schon veraendert wurde
		long idVonKanteInVariante = 2L;
		Kante kanteInVariante = KanteTestDataProvider.withDefaultValues().id(idVonKanteInVariante).build();

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.onKante(kanteInHauptroute).id(42L)
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE).build();

		List<AbschnittsweiserKantenBezug> bezuege = new ArrayList<>();
		bezuege.add(new AbschnittsweiserKantenBezug(kanteInVariante, LinearReferenzierterAbschnitt.of(0, 1)));
		fahrradroute.replaceFahrradrouteVarianten(
			List.of(FahrradrouteVarianteTestDataProvider.defaultTfis().abschnittsweiserKantenBezug(bezuege)
				.build()));

		when(fahrradrouteRepository.findByKanteIdInNetzBezug(idVonKanteInVariante)).thenReturn(List.of(fahrradroute));

		// Kante in Variante soll entfernt werden
		KanteDeletedEvent deleteEvent = new KanteDeletedEvent(idVonKanteInVariante,
			GeometryTestdataProvider.createLineString(), NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now());
		ArgumentCaptor<FahrradrouteNetzBezugAenderung> captor = ArgumentCaptor.forClass(
			FahrradrouteNetzBezugAenderung.class);

		when(fahrradrouteNetzBezugAenderungRepository.save(captor.capture())).thenReturn(null);

		// act
		service.onKanteGeloescht(deleteEvent);

		// assert
		verify(fahrradrouteNetzBezugAenderungRepository, times(1)).save(any());
		assertThat(captor.getValue().getFahrradroute()).isEqualTo(fahrradroute);
		assertThat(captor.getValue().isAenderungInHauptroute()).isFalse();
	}

	@Test
	void protokolliereNetzbezugAenderungNurFuerRadVisTyp_geometrieAendern() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(123L).build();
		Fahrradroute radvisRoute = FahrradrouteTestDataProvider.onKante(kante).id(2L)
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE).build();
		Fahrradroute toubizRoute = FahrradrouteTestDataProvider.onKante(kante).id(3L)
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE).build();
		Fahrradroute tfisRoute = FahrradrouteTestDataProvider.onKante(kante).id(4L)
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE).build();

		when(fahrradrouteRepository.findByKanteIdInNetzBezug(kante.getId()))
			.thenReturn(List.of(radvisRoute, toubizRoute, tfisRoute));

		KanteTopologieChangedEvent event = new KanteTopologieChangedEvent(
			kante.getId(), GeometryTestdataProvider.createLineString(), NetzAenderungAusloeser.DLM_REIMPORT_JOB,
			LocalDateTime.now());
		ArgumentCaptor<FahrradrouteNetzBezugAenderung> captor = ArgumentCaptor
			.forClass(FahrradrouteNetzBezugAenderung.class);
		when(fahrradrouteNetzBezugAenderungRepository.save(captor.capture())).thenReturn(null);

		// act
		service.onKanteTopologieChanged(event);

		// assert
		verify(fahrradrouteNetzBezugAenderungRepository, times(1)).save(any());
		assertThat(captor.getValue().getFahrradroute().getFahrradrouteTyp()).isEqualTo(FahrradrouteTyp.RADVIS_ROUTE);
		assertThat(captor.getValue().isAenderungInHauptroute()).isTrue();
	}

	@Test
	void protokolliereNetzbezugAenderungAuchFuerVarianten_geometrieAendern() {
		// arrange
		// Hauptroute ist unveraendert
		Kante kanteInHauptroute = KanteTestDataProvider.withDefaultValues().id(1L).build();
		// aber Variante hat sich schon veraendert
		long idVonKanteInVariante = 2L;
		Kante kanteInVariante = KanteTestDataProvider.withDefaultValues().id(idVonKanteInVariante).build();

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.onKante(kanteInHauptroute).id(42L)
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE).build();

		fahrradroute.replaceFahrradrouteVarianten(List.of(FahrradrouteVarianteTestDataProvider.defaultTfis()
			.abschnittsweiserKantenBezug(
				List.of(new AbschnittsweiserKantenBezug(kanteInVariante, LinearReferenzierterAbschnitt.of(0, 1))))
			.build()));

		when(fahrradrouteRepository.findByKanteIdInNetzBezug(idVonKanteInVariante))
			.thenReturn(List.of(fahrradroute));

		KanteTopologieChangedEvent event = new KanteTopologieChangedEvent(
			idVonKanteInVariante, GeometryTestdataProvider.createLineString(), NetzAenderungAusloeser.DLM_REIMPORT_JOB,
			LocalDateTime.now());
		ArgumentCaptor<FahrradrouteNetzBezugAenderung> captor = ArgumentCaptor
			.forClass(FahrradrouteNetzBezugAenderung.class);
		when(fahrradrouteNetzBezugAenderungRepository.save(captor.capture())).thenReturn(null);

		// act
		service.onKanteTopologieChanged(event);

		// assert
		verify(fahrradrouteNetzBezugAenderungRepository, times(1)).save(any());
		assertThat(captor.getValue().getFahrradroute()).isEqualTo(fahrradroute);
		assertThat(captor.getValue().isAenderungInHauptroute()).isFalse();
	}

	@Nested
	class WithFahrradroute {

		private Fahrradroute fahrradroute;
		private Fahrradroute fahrradrouteWithoutNetzbezugLineString;

		@BeforeEach
		void setup() {
			List<Coordinate> coordinates = List.of(new Coordinate(0, 0), new Coordinate(100, 0),
				new Coordinate(100, 100));
			LineString lineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createLineString(coordinates.toArray(new Coordinate[0]));

			Kante kante = KanteTestDataProvider.withDefaultValues()
				.build();
			AbschnittsweiserKantenBezug abschnittsweiserKantenBezug = new AbschnittsweiserKantenBezug(kante,
				LinearReferenzierterAbschnitt.of(0, 1));

			FahrradrouteVariante fahrradrouteVariante = FahrradrouteVariante.tfisVarianteBuilder()
				.kategorie(VarianteKategorie.ZUBRINGERSTRECKE)
				.linearReferenzierteProfilEigenschaften(List.of())
				.geometrie(lineString)
				.abschnittsweiserKantenBezug(List.of(abschnittsweiserKantenBezug))
				.tfisId(TfisId.of("vollstaendigeVariante"))
				.build();

			FahrradrouteVariante fahrradrouteVariante_keinNetzbezugLS = FahrradrouteVariante.tfisVarianteBuilder()
				.kategorie(VarianteKategorie.ZUBRINGERSTRECKE)
				.linearReferenzierteProfilEigenschaften(List.of())
				.geometrie(null)
				.abschnittsweiserKantenBezug(List.of(abschnittsweiserKantenBezug))
				.tfisId(TfisId.of("varianteOhneNetzbezugLS"))
				.build();

			fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
				.netzbezugLineString(lineString)
				.iconLocation(lineString.getStartPoint())
				.abschnittsweiserKantenBezug(List.of(abschnittsweiserKantenBezug))
				.varianten(List.of(fahrradrouteVariante, fahrradrouteVariante_keinNetzbezugLS))
				.build();

			fahrradrouteWithoutNetzbezugLineString = FahrradrouteTestDataProvider.withDefaultValues()
				.iconLocation(lineString.getStartPoint())
				.abschnittsweiserKantenBezug(List.of(abschnittsweiserKantenBezug))
				.netzbezugLineString(null)
				.build();
		}

		@Test
		void createdEventAddsRoutingInformation() throws KeineRouteGefundenException {
			// Arrange
			LineString lineString = (LineString) fahrradroute.getNetzbezugLineString().get();
			when(graphhopperRoutingRepository.route(List.of(lineString.getCoordinates()),
				GraphhopperRoutingRepository.DEFAULT_PROFILE_ID, false)).thenReturn(
				new RoutingResult(List.of(1L, 2L),
					KanteTestDataProvider.withDefaultValues()
						.geometry(lineString)
						.build()
						.getGeometry(),
					Hoehenunterschied.of(123d),
					Hoehenunterschied.of(234d)));

			// Act
			service.onFahrradrouteCreated(new FahrradrouteCreatedEvent(fahrradroute));

			// Assert
			assertThat(fahrradroute.getAnstieg()).contains(Hoehenunterschied.of(123d));
			assertThat(fahrradroute.getAbstieg()).contains(Hoehenunterschied.of(234d));
		}

		@Test
		void updatedEventAddsRoutingInformationAndSaves() throws KeineRouteGefundenException {
			// Arrange
			LineString lineString = (LineString) fahrradroute.getNetzbezugLineString().get();
			when(graphhopperRoutingRepository.route(List.of(lineString.getCoordinates()),
				GraphhopperRoutingRepository.DEFAULT_PROFILE_ID, false)).thenReturn(
				new RoutingResult(List.of(1L, 2L),
					KanteTestDataProvider.withDefaultValues().build().getGeometry(),
					Hoehenunterschied.of(123d),
					Hoehenunterschied.of(234d)));

			// Act
			service.onFahrradrouteUpdated(new FahrradrouteUpdatedEvent(fahrradroute));

			// Assert
			verify(fahrradrouteRepository, times(1)).save(fahrradroute);
			assertThat(fahrradroute.getAnstieg()).contains(Hoehenunterschied.of(123d));
			assertThat(fahrradroute.getAbstieg()).contains(Hoehenunterschied.of(234d));
		}

		@Test
		void noNetzbezugLineString_createdEventDoesNotUpdateRoutingInformation() throws KeineRouteGefundenException {
			// Act
			service.onFahrradrouteCreated(new FahrradrouteCreatedEvent(fahrradrouteWithoutNetzbezugLineString));

			// Assert
			verify(graphhopperRoutingRepository, never()).route(any(), eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID), anyBoolean());
			assertThat(fahrradrouteWithoutNetzbezugLineString.getAnstieg()).isEmpty();
			assertThat(fahrradrouteWithoutNetzbezugLineString.getAbstieg()).isEmpty();
		}

		@Test
		void noNetzbezugLineString_updatedEventDoesNotUpdateRoutingInformation() throws KeineRouteGefundenException {
			// Act
			service.onFahrradrouteUpdated(new FahrradrouteUpdatedEvent(fahrradrouteWithoutNetzbezugLineString));

			// Assert
			verify(graphhopperRoutingRepository, never()).route(any(), eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID), anyBoolean());
			assertThat(fahrradrouteWithoutNetzbezugLineString.getAnstieg()).isEmpty();
			assertThat(fahrradrouteWithoutNetzbezugLineString.getAbstieg()).isEmpty();
		}

		@Test
		void updateProfilInformationen() throws KeineRouteGefundenException {
			// Arrange
			when(fahrradrouteRepository.findAllByFahrradrouteTypNot(FahrradrouteTyp.TOUBIZ_ROUTE))
				.thenReturn(Stream.of(fahrradroute));

			LinearReferenzierteProfilEigenschaften profilEigenschaften1 = new LinearReferenzierteProfilEigenschaften(
				FahrradrouteProfilEigenschaften.of(BelagArt.ASPHALT, Radverkehrsfuehrung.BEGEGNUNBSZONE),
				LinearReferenzierterAbschnitt.of(0, 1));
			RoutingResult routingResult = new RoutingResult(List.of(1L, 2L),
				KanteTestDataProvider.withDefaultValues()
					.geometry((LineString) fahrradroute.getNetzbezugLineString().get())
					.build()
					.getGeometry(),
				Hoehenunterschied.of(123d),
				Hoehenunterschied.of(234d));
			ProfilRoutingResult profilRoutingResult = new ProfilRoutingResult(routingResult,
				List.of(profilEigenschaften1));
			when(graphhopperRoutingRepository.routeMitProfileigenschaften(
				List.of(fahrradroute.getNetzbezugLineString().get().getCoordinates()),
				GraphhopperRoutingRepository.DEFAULT_PROFILE_ID,
				false))
				.thenReturn(profilRoutingResult);

			// Act
			ProfilInformationenUpdateStatistik profilInformationenUpdateStatistik = new ProfilInformationenUpdateStatistik();
			service.updateProfilEigenschaftenVonRadvisUndTfisRouten(profilInformationenUpdateStatistik);

			// Assert
			verify(fahrradrouteRepository, times(1)).saveAll(fahrradroutenCaptor.capture());
			List<Fahrradroute> savedFahrradrouten = fahrradroutenCaptor.getValue();
			assertThat(savedFahrradrouten).hasSize(1);
			assertThat(savedFahrradrouten.get(0).getLinearReferenzierteProfilEigenschaften()).containsExactly(
				profilEigenschaften1);
			assertThat(profilInformationenUpdateStatistik.anzahlProfilInfosAktualisiert).isEqualTo(1);
		}

		@Test
		void updateAbgeleiteteRoutenInfos() throws KeineRouteGefundenException {
			// Arrange
			when(fahrradrouteRepository.findAllByFahrradrouteTypNot(FahrradrouteTyp.TOUBIZ_ROUTE))
				.thenReturn(Stream.of(fahrradroute));

			RoutingResult routingResult = new RoutingResult(List.of(1L, 2L),
				KanteTestDataProvider.withDefaultValues()
					.geometry((LineString) fahrradroute.getNetzbezugLineString().get())
					.build()
					.getGeometry(),
				Hoehenunterschied.of(123d),
				Hoehenunterschied.of(234d));
			when(graphhopperRoutingRepository.route(
				List.of(fahrradroute.getNetzbezugLineString().get().getCoordinates()),
				GraphhopperRoutingRepository.DEFAULT_PROFILE_ID, false))
				.thenReturn(routingResult);

			// Act
			UpdateAbgeleiteteRoutenInfoStatistik updateAbgeleiteteRoutenInfoStatistik = new UpdateAbgeleiteteRoutenInfoStatistik();
			service.updateAbgeleiteteRoutenInformationVonRadvisUndTfis(updateAbgeleiteteRoutenInfoStatistik);

			// Assert
			verify(fahrradrouteRepository, times(1)).saveAll(fahrradroutenCaptor.capture());
			List<Fahrradroute> savedFahrradrouten = fahrradroutenCaptor.getValue();
			assertThat(savedFahrradrouten).hasSize(1);
			assertThat(savedFahrradrouten.get(0).getAnstieg()).isPresent();
			assertThat(savedFahrradrouten.get(0).getAnstieg().get()).isEqualTo(Hoehenunterschied.of(123d));
			assertThat(savedFahrradrouten.get(0).getAbstieg()).isPresent();
			assertThat(savedFahrradrouten.get(0).getAbstieg().get()).isEqualTo(Hoehenunterschied.of(234d));
			assertThat(updateAbgeleiteteRoutenInfoStatistik.anzahlRoutenErfolgreichAktualisiert).isEqualTo(1);

			FahrradrouteVariante fV = savedFahrradrouten.get(0)
				.findFahrradrouteVariante(TfisId.of("vollstaendigeVariante")).get();
			assertThat(fV.getAnstieg()).isPresent();
			assertThat(fV.getAnstieg().get()).isEqualTo(Hoehenunterschied.of(123d));
			assertThat(fV.getAbstieg()).isPresent();
			assertThat(fV.getAbstieg().get()).isEqualTo(Hoehenunterschied.of(234d));
		}

		@Test
		void updateAbgeleiteteRoutenInfos_varianteOhneGeometrieKeinFehler() throws KeineRouteGefundenException {
			// Arrange
			when(fahrradrouteRepository.findAllByFahrradrouteTypNot(FahrradrouteTyp.TOUBIZ_ROUTE))
				.thenReturn(Stream.of(fahrradroute));

			RoutingResult routingResult = new RoutingResult(List.of(1L, 2L),
				KanteTestDataProvider.withDefaultValues()
					.geometry((LineString) fahrradroute.getNetzbezugLineString().get())
					.build()
					.getGeometry(),
				Hoehenunterschied.of(123d),
				Hoehenunterschied.of(234d));
			when(graphhopperRoutingRepository.route(
				List.of(fahrradroute.getNetzbezugLineString().get().getCoordinates()),
				GraphhopperRoutingRepository.DEFAULT_PROFILE_ID, false))
				.thenReturn(routingResult);

			// Act
			UpdateAbgeleiteteRoutenInfoStatistik updateAbgeleiteteRoutenInfoStatistik = new UpdateAbgeleiteteRoutenInfoStatistik();
			assertDoesNotThrow(
				() -> service.updateAbgeleiteteRoutenInformationVonRadvisUndTfis(updateAbgeleiteteRoutenInfoStatistik));

			// Assert
			verify(fahrradrouteRepository, times(1)).saveAll(fahrradroutenCaptor.capture());
			List<Fahrradroute> savedFahrradrouten = fahrradroutenCaptor.getValue();

			FahrradrouteVariante fV = savedFahrradrouten.get(0)
				.findFahrradrouteVariante(TfisId.of("varianteOhneNetzbezugLS")).get();
			assertThat(fV.getAnstieg()).isEmpty();
			assertThat(fV.getAbstieg()).isEmpty();
		}
	}

	@Test
	void getAlleFahrradroutenImportprotokolle() {
		// arrange
		int anzahlTage = 2;
		JobExecutionDescription tfisImportJob = JobExecutionDescriptionTestDataProvider.withDefaultValues()
			.executionStart(LocalDateTime.now().minusDays(1)).id(1l)
			.name(FahrradroutenTfisImportJob.class.getSimpleName()).build();
		JobExecutionDescription tfisUpdateJob = JobExecutionDescriptionTestDataProvider.withDefaultValues()
			.executionStart(LocalDateTime.now().minusHours(1)).id(2l)
			.name(FahrradroutenTfisUpdateJob.class.getSimpleName()).build();
		JobExecutionDescription toubizImportJob = JobExecutionDescriptionTestDataProvider.withDefaultValues()
			.executionStart(LocalDateTime.now().minusDays(3)).id(3l)
			.name(FahrradroutenToubizImportJob.class.getSimpleName()).build();
		when(jobExecutionDescriptionRepository.findAllByNameInAfterOrderByExecutionStartDesc(any(), any()))
			.thenReturn(List.of(tfisImportJob, tfisUpdateJob, toubizImportJob));

		// act
		LocalDate now = LocalDate.now();
		List<Importprotokoll> alleFahrradroutenImportprotokolle = service
			.getAllImportprotokolleAfter(now.atStartOfDay().minusDays(anzahlTage));

		// assert

		ArgumentCaptor<LocalDateTime> afterCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

		verify(jobExecutionDescriptionRepository, times(1)).findAllByNameInAfterOrderByExecutionStartDesc(
			afterCaptor.capture(), jobNamesCaptor.capture());

		assertThat(afterCaptor.getValue()).isEqualToIgnoringHours(now.atStartOfDay().minusDays(anzahlTage));

		assertThat(jobNamesCaptor.getValue()).containsExactlyInAnyOrder(
			FahrradroutenTfisImportJob.JOB_NAME,
			FahrradroutenTfisUpdateJob.JOB_NAME,
			FahrradroutenToubizImportJob.JOB_NAME);

		Importprotokoll tfisImportProtokoll = Importprotokoll.builder()
			.id(tfisImportJob.getId()).startZeit(tfisImportJob.getExecutionStart())
			.importQuelle("TFIS")
			.importprotokollTyp(ImportprotokollTyp.FAHRRADROUTE)
			.build();

		Importprotokoll tfisUpdateProtokoll = Importprotokoll.builder()
			.id(tfisUpdateJob.getId()).startZeit(tfisUpdateJob.getExecutionStart())
			.importQuelle("TFIS")
			.importprotokollTyp(ImportprotokollTyp.FAHRRADROUTE)
			.build();

		Importprotokoll toubizImportProtokoll = Importprotokoll.builder()
			.id(toubizImportJob.getId()).startZeit(toubizImportJob.getExecutionStart())
			.importQuelle("Toubiz")
			.importprotokollTyp(ImportprotokollTyp.FAHRRADROUTE)
			.build();

		assertThat(alleFahrradroutenImportprotokolle)
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactly(tfisImportProtokoll, tfisUpdateProtokoll, toubizImportProtokoll);
	}
}
