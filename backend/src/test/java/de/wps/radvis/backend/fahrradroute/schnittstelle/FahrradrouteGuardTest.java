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

package de.wps.radvis.backend.fahrradroute.schnittstelle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisherSensitiveTest;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.fahrradroute.domain.FahrradrouteService;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

class FahrradrouteGuardTest implements RadVisDomainEventPublisherSensitiveTest {

	ZustaendigkeitsService zustaendigkeitsService;

	@Mock
	NetzService netzService;
	@Mock
	FahrradrouteService fahrradrouteService;
	@Mock
	BenutzerResolver benutzerResolver;
	@Mock
	Authentication authentication;
	@Mock
	OrganisationConfigurationProperties organisationConfigurationProperties;

	@Getter
	@Setter
	private MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	private Benutzer.BenutzerBuilder benutzer;
	private FahrradrouteGuard fahrradrouteGuard;
	private Kante kanteOutsideZustaendigkeitsbereich;
	private Kante kanteWithinZustaendigkeitsbereich;
	private Fahrradroute radvisRouteWithinZustaendigkeitsbereich;
	private Fahrradroute radvisRouteOutsideZustaendigkeitsbereich;
	private Fahrradroute toubizRouteWithinZustaendigkeitsbereich;
	private Set<Long> kantenIdsOutsideZustaendigkeitsbereich;

	@BeforeEach
	void beforeEach() {
		MockitoAnnotations.openMocks(this);
		when(organisationConfigurationProperties.getZustaendigkeitBufferInMeter()).thenReturn(500);
		zustaendigkeitsService = new ZustaendigkeitsService(organisationConfigurationProperties);
		fahrradrouteGuard = new FahrradrouteGuard(netzService, benutzerResolver, zustaendigkeitsService,
			fahrradrouteService);
		benutzer = BenutzerTestDataProvider.defaultBenutzer();

		LineString geometryWithinZustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N
			.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(10, 10), new Coordinate(50, 50) });
		kanteWithinZustaendigkeitsbereich = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.geometry(geometryWithinZustaendigkeitsbereich)
			.build();
		radvisRouteWithinZustaendigkeitsbereich = FahrradrouteTestDataProvider.withDefaultValues()
			.abschnittsweiserKantenBezug(
				List.of(new AbschnittsweiserKantenBezug(kanteWithinZustaendigkeitsbereich,
					LinearReferenzierterAbschnitt.of(0, 1))))
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.build();
		toubizRouteWithinZustaendigkeitsbereich = FahrradrouteTestDataProvider.withDefaultValues()
			.abschnittsweiserKantenBezug(
				List.of(new AbschnittsweiserKantenBezug(kanteWithinZustaendigkeitsbereich,
					LinearReferenzierterAbschnitt.of(0, 1))))
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.build();

		LineString geometryOutsideZustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N
			.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(1000, 1000), new Coordinate(5000, 5000) });
		kanteOutsideZustaendigkeitsbereich = KanteTestDataProvider.withDefaultValues()
			.id(2L)
			.geometry(geometryOutsideZustaendigkeitsbereich)
			.build();
		radvisRouteOutsideZustaendigkeitsbereich = FahrradrouteTestDataProvider.withDefaultValues()
			.abschnittsweiserKantenBezug(
				List.of(new AbschnittsweiserKantenBezug(kanteOutsideZustaendigkeitsbereich,
					LinearReferenzierterAbschnitt.of(0, 1))))
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.build();

		kantenIdsOutsideZustaendigkeitsbereich = radvisRouteOutsideZustaendigkeitsbereich
			.getAbschnittsweiserKantenBezug()
			.stream()
			.map(k -> k.getKante().getId())
			.collect(Collectors.toSet());
	}

	@Nested
	class KeinRecht {

		@BeforeEach
		public void setup() {
			when(benutzerResolver.fromAuthentication(any())).thenReturn(
				benutzer.rollen(Set.of(Rolle.RADVIS_BETRACHTER)).build());
		}

		@Test
		public void createFarradroute() {
			// Arrange
			long kanteId = 1L;
			when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kanteWithinZustaendigkeitsbereich));
			CreateFahrradrouteCommand command = CreateFahrradrouteCommandTestDataProvider.withKante(kanteId).build();

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.createFahrradroute(authentication, command));
		}

		@Test
		public void saveFarradroute() {
			// Arrange
			SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withDefaultValue().build();

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
					radvisRouteWithinZustaendigkeitsbereich));
		}

		@Test
		public void changeVeroeffentlicht() {
			// Arrange
			ChangeFahrradrouteVeroeffentlichtCommand command = ChangeFahrradrouteVeroeffentlichtCommand.builder()
				.veroeffentlicht(true).id(1L).version(2L).build();

			when(fahrradrouteService.loadForModification(eq(command.getId()),
				eq(command.getVersion()))).thenReturn(radvisRouteWithinZustaendigkeitsbereich);

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.changeVeroeffentlicht(authentication, command));
		}

		@Test
		public void deleteFarradroute() {
			// Arrange
			Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
				.kategorie(Kategorie.RADSCHNELLWEG)
				.build();

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.deleteFahrradroute(authentication, fahrradroute));
		}
	}

	@Nested
	class anhandTyp {
		@BeforeEach
		public void setup() {
			when(benutzerResolver.fromAuthentication(any())).thenReturn(
				benutzer.rollen(Set.of(Rolle.RADWEGE_ERFASSERIN)).build());
		}

		@Test
		public void lrfw_ok() {
			// Arrange
			long kanteId = 1L;
			when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kanteWithinZustaendigkeitsbereich));
			Fahrradroute landesradfernweg = FahrradrouteTestDataProvider.withDefaultValues()
				.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
				.kategorie(Kategorie.LANDESRADFERNWEG)
				.build();
			SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withKante(kanteId).build();

			// Act+Assert
			assertDoesNotThrow(
				() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
					landesradfernweg));

		}

		@Test
		public void radvis_ok() {
			// Arrange
			long kanteId = 1L;
			when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kanteWithinZustaendigkeitsbereich));
			Fahrradroute radvisRoute = FahrradrouteTestDataProvider.withDefaultValues()
				.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
				.build();
			SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withKante(kanteId).build();

			// Act+Assert
			assertDoesNotThrow(
				() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
					radvisRoute));

		}

		@Test
		public void else_denied() {
			// Arrange
			Fahrradroute toubizRoute = FahrradrouteTestDataProvider.withDefaultValues()
				.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
				.build();
			SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withDefaultValue().build();

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
					toubizRoute));

		}

	}

	@Nested
	class KeinRecht_weilAusserhalb {

		@BeforeEach
		public void setup() {
			when(benutzerResolver.fromAuthentication(any())).thenReturn(
				benutzer.rollen(Set.of(Rolle.RADWEGE_ERFASSERIN)).build());
		}

		@Test
		public void createFarradroute() {
			// Arrange
			long kanteId = 2L;
			when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kanteOutsideZustaendigkeitsbereich));
			CreateFahrradrouteCommand command = CreateFahrradrouteCommandTestDataProvider.withKante(kanteId)
				.build();

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.createFahrradroute(authentication, command));
		}

		@Test
		public void saveFarradroute() {
			// Arrange
			long kanteId = 2L;
			when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kanteOutsideZustaendigkeitsbereich));
			SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withKante(kanteId)
				.build();

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
					radvisRouteOutsideZustaendigkeitsbereich));
		}

		@Test
		public void changeVeroeffentlicht() {
			// Arrange
			ChangeFahrradrouteVeroeffentlichtCommand command = ChangeFahrradrouteVeroeffentlichtCommand.builder()
				.veroeffentlicht(true).id(1L).version(2L).build();

			when(netzService.getKanten(kantenIdsOutsideZustaendigkeitsbereich)).thenReturn(
				List.of(kanteOutsideZustaendigkeitsbereich));

			when(fahrradrouteService.loadForModification(eq(command.getId()),
				eq(command.getVersion()))).thenReturn(radvisRouteOutsideZustaendigkeitsbereich);

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.changeVeroeffentlicht(authentication, command));
		}

		@Test
		public void deleteFarradroute() {
			// Arrange
			Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
				.abschnittsweiserKantenBezug(List.of(
					new AbschnittsweiserKantenBezug(KanteTestDataProvider.withDefaultValues().id(2L).build(),
						LinearReferenzierterAbschnitt.of(0, 1))))
				.kategorie(Kategorie.RADSCHNELLWEG)
				.build();
			fahrradroute.getAbschnittsweiserKantenBezug().forEach(bezug ->
				when(netzService.getKanten(Set.of(bezug.getKante().getId())))
					.thenReturn(List.of(kanteOutsideZustaendigkeitsbereich)));

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.deleteFahrradroute(authentication, fahrradroute));
		}

		@Test
		public void saveFahrradroute_KantenBezug_SollIn_IstAusserhalb() {
			// Arrange
			SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withDefaultValue()
				.build();

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
					radvisRouteOutsideZustaendigkeitsbereich));
		}

		@Test
		public void saveFahrradroute_KantenBezug_SollAußerhalb_IstInnerhalb() {
			// Arrange
			long kanteId = 2L;
			when(netzService.getKanten(Set.of(kanteId))).thenReturn(
				List.of(kanteOutsideZustaendigkeitsbereich));
			SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withKante(kanteId)
				.build();

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
					radvisRouteWithinZustaendigkeitsbereich));
		}
	}

	@Nested
	class KeinRecht_weilAusserhalb_trotzAdmin {

		@BeforeEach
		public void setup() {
			when(benutzerResolver.fromAuthentication(any())).thenReturn(
				benutzer.rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR)).build());
		}

		@Test
		public void createFarradroute() {
			// Arrange
			long kanteId = 2L;
			when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kanteOutsideZustaendigkeitsbereich));
			CreateFahrradrouteCommand command = CreateFahrradrouteCommandTestDataProvider.withKante(kanteId)
				.build();

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.createFahrradroute(authentication, command));
		}

		@Test
		public void saveFarradroute() {
			// Arrange
			long kanteId = 2L;
			when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kanteOutsideZustaendigkeitsbereich));
			SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withKante(kanteId)
				.build();

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
					radvisRouteOutsideZustaendigkeitsbereich));
		}

		@Test
		public void changeVeroeffentlicht() {
			// Arrange
			ChangeFahrradrouteVeroeffentlichtCommand command = ChangeFahrradrouteVeroeffentlichtCommand.builder()
				.veroeffentlicht(true).id(1L).version(2L).build();

			when(netzService.getKanten(kantenIdsOutsideZustaendigkeitsbereich)).thenReturn(
				List.of(kanteOutsideZustaendigkeitsbereich));

			when(fahrradrouteService.loadForModification(eq(command.getId()),
				eq(command.getVersion()))).thenReturn(radvisRouteOutsideZustaendigkeitsbereich);

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.changeVeroeffentlicht(authentication, command));
		}

		@Test
		public void deleteFarradroute() {
			// Arrange
			Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
				.abschnittsweiserKantenBezug(
					List.of(new AbschnittsweiserKantenBezug(KanteTestDataProvider.withDefaultValues().id(2L).build(),
						LinearReferenzierterAbschnitt.of(0, 1))
					))
				.kategorie(Kategorie.RADSCHNELLWEG)
				.build();
			fahrradroute.getAbschnittsweiserKantenBezug().forEach(bezug ->
				when(netzService.getKanten(Set.of(bezug.getKante().getId())))
					.thenReturn(List.of(kanteOutsideZustaendigkeitsbereich)));

			// Act+Assert
			assertThrows(AccessDeniedException.class,
				() -> fahrradrouteGuard.deleteFahrradroute(authentication, fahrradroute));
		}
	}

	@Nested
	class RechtVorhanden_ImZustaendigkeitsbereich {

		@BeforeEach
		public void setup() {
			when(benutzerResolver.fromAuthentication(any())).thenReturn(
				benutzer.rollen(Set.of(Rolle.RADWEGE_ERFASSERIN)).build());
		}

		@Nested
		class RadVisRoute {

			@Test
			public void createFahrradrouteTeilweiseImZustaendigkeitsbereich() {
				// Arrange
				long kanteId = 1L;
				when(netzService.getKanten(Set.of(kanteId))).thenReturn(
					List.of(kanteWithinZustaendigkeitsbereich, kanteOutsideZustaendigkeitsbereich));
				CreateFahrradrouteCommand command = CreateFahrradrouteCommandTestDataProvider.withKante(kanteId)
					.build();

				// Act+Assert
				assertDoesNotThrow(() -> fahrradrouteGuard.createFahrradroute(authentication, command));
			}

			@Test
			public void createFahrradrouteKomplettImZustaendigkeitsbereich() {
				// Arrange
				long kanteId = 1L;
				when(netzService.getKanten(Set.of(kanteId))).thenReturn(
					List.of(kanteWithinZustaendigkeitsbereich));
				CreateFahrradrouteCommand command = CreateFahrradrouteCommandTestDataProvider.withKante(kanteId)
					.build();

				// Act+Assert
				assertDoesNotThrow(() -> fahrradrouteGuard.createFahrradroute(authentication, command));
			}

			@Test
			public void createFahrradrouteNichtImZustaendigkeitsbereich() {
				// Arrange
				long kanteId = 1L;
				when(netzService.getKanten(Set.of(kanteId))).thenReturn(
					List.of(kanteOutsideZustaendigkeitsbereich));
				CreateFahrradrouteCommand command = CreateFahrradrouteCommandTestDataProvider.withKante(kanteId)
					.build();

				// Act+Assert
				assertThrows(AccessDeniedException.class,
					() -> fahrradrouteGuard.createFahrradroute(authentication, command));
			}

			@Test
			public void saveFahrradroute() {
				// Arrange
				long kanteId = 1L;
				when(netzService.getKanten(Set.of(kanteId))).thenReturn(
					List.of(kanteWithinZustaendigkeitsbereich, kanteOutsideZustaendigkeitsbereich));
				SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withKante(kanteId)
					.build();

				// Act+Assert
				assertDoesNotThrow(() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
					radvisRouteWithinZustaendigkeitsbereich));
			}

			@Test
			public void saveFahrradrouteNichtImZustaendigkeitsbereich() {
				// Arrange
				long kanteId = 1L;
				when(netzService.getKanten(Set.of(kanteId))).thenReturn(
					List.of(kanteOutsideZustaendigkeitsbereich));
				SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withKante(kanteId)
					.build();

				// Act+Assert
				assertThrows(AccessDeniedException.class,
					() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
						radvisRouteOutsideZustaendigkeitsbereich));
			}

			@Test
			public void changeVeroeffentlicht() {
				// Arrange
				ChangeFahrradrouteVeroeffentlichtCommand command = ChangeFahrradrouteVeroeffentlichtCommand.builder()
					.veroeffentlicht(true).id(1L).version(2L).build();

				when(fahrradrouteService.loadForModification(eq(command.getId()),
					eq(command.getVersion()))).thenReturn(radvisRouteWithinZustaendigkeitsbereich);
				when(netzService.getKanten(Set.of(
					radvisRouteWithinZustaendigkeitsbereich.getAbschnittsweiserKantenBezug().get(0).getKante()
						.getId()))).thenReturn(
					List.of(kanteWithinZustaendigkeitsbereich, kanteOutsideZustaendigkeitsbereich));

				// Act+Assert
				assertDoesNotThrow(() -> fahrradrouteGuard.changeVeroeffentlicht(authentication, command));
			}

			@Test
			public void deleteFarradroute() {
				// Arrange
				radvisRouteWithinZustaendigkeitsbereich.getAbschnittsweiserKantenBezug().forEach(bezug ->
					when(netzService.getKante(bezug.getKante().getId()))
						.thenReturn(kanteWithinZustaendigkeitsbereich));
				when(netzService.getKanten(Set.of(
					radvisRouteWithinZustaendigkeitsbereich.getAbschnittsweiserKantenBezug().get(0).getKante()
						.getId()))).thenReturn(List.of(kanteWithinZustaendigkeitsbereich));

				// Act+Assert
				assertDoesNotThrow(() -> fahrradrouteGuard.deleteFahrradroute(authentication,
					radvisRouteWithinZustaendigkeitsbereich));
			}

			@Test
			public void deleteFarradrouteNotAllowedForLandesradfernweg() {
				// Arrange
				Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
					.kategorie(Kategorie.LANDESRADFERNWEG)
					.build();
				fahrradroute.getAbschnittsweiserKantenBezug().forEach(bezug ->
					when(netzService.getKante(bezug.getKante().getId()))
						.thenReturn(kanteOutsideZustaendigkeitsbereich));

				// Act+Assert
				assertThrows(AccessDeniedException.class,
					() -> fahrradrouteGuard.deleteFahrradroute(authentication, fahrradroute));
			}

			@Test
			public void deleteFarradrouteNotAllowedForDRoute() {
				// Arrange
				Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
					.kategorie(Kategorie.D_ROUTE)
					.build();
				fahrradroute.getAbschnittsweiserKantenBezug().forEach(bezug ->
					when(netzService.getKante(bezug.getKante().getId()))
						.thenReturn(kanteOutsideZustaendigkeitsbereich));

				// Act+Assert
				assertThrows(AccessDeniedException.class,
					() -> fahrradrouteGuard.deleteFahrradroute(authentication, fahrradroute));
			}
		}

		@Nested
		class ToubizRoute {
			@Test
			public void saveFahrradroute() {
				// Arrange
				SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withDefaultValue()
					.build();

				// Act+Assert
				assertThrows(AccessDeniedException.class,
					() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
						toubizRouteWithinZustaendigkeitsbereich));
			}

			@Test
			public void changeVeroeffentlicht() {
				// Arrange
				ChangeFahrradrouteVeroeffentlichtCommand command = ChangeFahrradrouteVeroeffentlichtCommand.builder()
					.veroeffentlicht(true).id(1L).version(2L).build();

				when(fahrradrouteService.loadForModification(eq(command.getId()),
					eq(command.getVersion()))).thenReturn(toubizRouteWithinZustaendigkeitsbereich);
				when(netzService.getKanten(Set.of(
					toubizRouteWithinZustaendigkeitsbereich.getAbschnittsweiserKantenBezug().get(0).getKante()
						.getId()))).thenReturn(List.of(kanteWithinZustaendigkeitsbereich));

				// Act+Assert
				assertDoesNotThrow(() -> fahrradrouteGuard.changeVeroeffentlicht(authentication, command));
			}

			@Test
			public void deleteFarradroute() {
				// Arrange
				toubizRouteWithinZustaendigkeitsbereich.getAbschnittsweiserKantenBezug().forEach(bezug ->
					when(netzService.getKante(bezug.getKante().getId()))
						.thenReturn(kanteWithinZustaendigkeitsbereich));
				when(netzService.getKanten(Set.of(
					toubizRouteWithinZustaendigkeitsbereich.getAbschnittsweiserKantenBezug().get(0).getKante()
						.getId()))).thenReturn(List.of(kanteWithinZustaendigkeitsbereich));

				// Act+Assert
				assertDoesNotThrow(() -> fahrradrouteGuard.deleteFahrradroute(authentication,
					toubizRouteWithinZustaendigkeitsbereich));
			}

			@Test
			public void deleteFarradrouteNotAllowedForLandesradfernweg() {
				// Arrange
				Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
					.kategorie(Kategorie.LANDESRADFERNWEG)
					.build();
				fahrradroute.getAbschnittsweiserKantenBezug().forEach(bezug ->
					when(netzService.getKante(bezug.getKante().getId()))
						.thenReturn(kanteWithinZustaendigkeitsbereich));

				// Act+Assert
				assertThrows(AccessDeniedException.class,
					() -> fahrradrouteGuard.deleteFahrradroute(authentication, fahrradroute));
			}

			@Test
			public void deleteFarradrouteNotAllowedForDRoute() {
				// Arrange
				Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
					.kategorie(Kategorie.D_ROUTE)
					.build();
				fahrradroute.getAbschnittsweiserKantenBezug().forEach(bezug ->
					when(netzService.getKante(bezug.getKante().getId()))
						.thenReturn(kanteWithinZustaendigkeitsbereich));

				// Act+Assert
				assertThrows(AccessDeniedException.class,
					() -> fahrradrouteGuard.deleteFahrradroute(authentication, fahrradroute));
			}
		}
	}

	@Nested
	class RechtVorhanden_Radroutenbearbeiter {

		@BeforeEach
		public void setup() {
			when(benutzerResolver.fromAuthentication(any())).thenReturn(
				benutzer.rollen(Set.of(Rolle.RADROUTEN_BEARBEITERIN)).build());
		}

		@Nested
		class ImZustaendigkeitsbereich {

			@Nested
			class RadVisRoute {

				@Test
				public void createFahrradroute() {
					// Arrange
					long kanteId = 1L;
					when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kanteWithinZustaendigkeitsbereich));
					CreateFahrradrouteCommand command = CreateFahrradrouteCommandTestDataProvider.withKante(kanteId)
						.build();

					// Act+Assert
					assertDoesNotThrow(() -> fahrradrouteGuard.createFahrradroute(authentication, command));
				}

				@Test
				public void saveFarradroute() {
					// Arrange
					SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withDefaultValue()
						.build();

					// Act+Assert
					assertDoesNotThrow(() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
						radvisRouteWithinZustaendigkeitsbereich));
				}

				@Test
				public void changeVeroeffentlicht() {
					// Arrange
					ChangeFahrradrouteVeroeffentlichtCommand command = ChangeFahrradrouteVeroeffentlichtCommand.builder()
						.veroeffentlicht(true).id(1L).version(2L).build();

					when(fahrradrouteService.loadForModification(eq(command.getId()),
						eq(command.getVersion()))).thenReturn(radvisRouteWithinZustaendigkeitsbereich);

					// Act+Assert
					assertDoesNotThrow(() -> fahrradrouteGuard.changeVeroeffentlicht(authentication, command));
				}

				@Test
				public void deleteFarradroute() {
					// Arrange
					Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
						.kategorie(Kategorie.RADSCHNELLWEG)
						.build();
					fahrradroute.getAbschnittsweiserKantenBezug().forEach(bezug ->
						when(netzService.getKante(bezug.getKante().getId()))
							.thenReturn(kanteOutsideZustaendigkeitsbereich));

					// Act+Assert
					assertDoesNotThrow(() -> fahrradrouteGuard.deleteFahrradroute(authentication, fahrradroute));
				}
			}

			@Nested
			class ToubizRoute {
				@Test
				public void saveFahrradroute() {
					// Arrange
					SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withDefaultValue()
						.build();

					// Act+Assert
					assertThrows(AccessDeniedException.class,
						() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
							toubizRouteWithinZustaendigkeitsbereich));
				}

				@Test
				public void changeVeroeffentlicht() {
					// Arrange
					ChangeFahrradrouteVeroeffentlichtCommand command = ChangeFahrradrouteVeroeffentlichtCommand.builder()
						.veroeffentlicht(true).id(1L).version(2L).build();

					when(fahrradrouteService.loadForModification(eq(command.getId()),
						eq(command.getVersion()))).thenReturn(toubizRouteWithinZustaendigkeitsbereich);

					// Act+Assert
					assertDoesNotThrow(() -> fahrradrouteGuard.changeVeroeffentlicht(authentication, command));
				}

				@Test
				public void deleteFarradroute() {
					// Arrange
					Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
						.kategorie(Kategorie.RADSCHNELLWEG)
						.build();
					fahrradroute.getAbschnittsweiserKantenBezug().forEach(bezug ->
						when(netzService.getKante(bezug.getKante().getId()))
							.thenReturn(kanteOutsideZustaendigkeitsbereich));

					// Act+Assert
					assertDoesNotThrow(() -> fahrradrouteGuard.deleteFahrradroute(authentication, fahrradroute));
				}
			}
		}

		@Nested
		class AusserhalbDesZustaendigkeitsbereich {

			@Test
			public void createFahrradroute() {
				// Arrange
				long kanteId = 2L;
				when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kanteOutsideZustaendigkeitsbereich));
				CreateFahrradrouteCommand command = CreateFahrradrouteCommandTestDataProvider.withKante(kanteId)
					.build();

				// Act+Assert
				assertDoesNotThrow(() -> fahrradrouteGuard.createFahrradroute(authentication, command));
			}

			@Test
			public void saveFahrradroute() {
				// Arrange
				SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withDefaultValue()
					.build();

				// Act+Assert

				assertDoesNotThrow(() -> fahrradrouteGuard.saveFahrradroute(authentication, command,
					radvisRouteOutsideZustaendigkeitsbereich));
			}

			@Test
			public void deleteFarradroute() {
				// Arrange
				Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
					.kategorie(Kategorie.RADSCHNELLWEG)
					.build();
				fahrradroute.getAbschnittsweiserKantenBezug().forEach(bezug ->
					when(netzService.getKante(bezug.getKante().getId()))
						.thenReturn(kanteOutsideZustaendigkeitsbereich));

				// Act+Assert
				assertDoesNotThrow(() -> fahrradrouteGuard.deleteFahrradroute(authentication, fahrradroute));
			}
		}
	}
}
