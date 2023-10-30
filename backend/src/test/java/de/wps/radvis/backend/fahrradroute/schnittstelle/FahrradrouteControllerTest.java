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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisherSensitiveTest;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.fahrradroute.domain.FahrradrouteService;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.schnittstelle.view.AbschnittsweiserKantenBezugView;
import de.wps.radvis.backend.fahrradroute.schnittstelle.view.FahrradrouteDetailView;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import lombok.Getter;
import lombok.Setter;

class FahrradrouteControllerTest implements RadVisDomainEventPublisherSensitiveTest {
	private FahrradrouteController fahrradrouteController;

	@Mock
	private FahrradrouteService fahrradrouteService;
	@Mock
	private FahrradrouteGuard fahrradrouteGuard;
	@Mock
	private SaveFahrradrouteCommandConverter saveFahrradrouteCommandConverter;
	@Mock
	private CreateFahrradrouteCommandConverter createFahrradrouteCommandConverter;
	@Mock
	private BenutzerResolver benutzerResolver;
	@Mock
	private Authentication authentication;
	@Mock
	private KanteResolver kanteResolver;

	@Getter
	@Setter
	MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@Mock
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Mock
	FahrradrouteRepository fahrradrouteRepository;

	@Captor
	ArgumentCaptor<List<String>> jobNamesCaptor;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		fahrradrouteController = new FahrradrouteController(fahrradrouteService, fahrradrouteGuard,
			saveFahrradrouteCommandConverter, createFahrradrouteCommandConverter, benutzerResolver,
			jobExecutionDescriptionRepository, fahrradrouteRepository);
	}

	@Test
	public void getFahrradroute() {
		// Arrange
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.id(1L)
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();

		when(fahrradrouteService.get(fahrradroute.getId())).thenReturn(fahrradroute);
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);
		when(fahrradrouteGuard.darfFahrradrouteBearbeiten(eq(benutzer), any())).thenReturn(false);

		// Act
		FahrradrouteDetailView fahrradrouteDetailView = fahrradrouteController.getFahrradroute(authentication,
			fahrradroute.getId());

		// Assert
		assertThat(fahrradrouteDetailView.getId()).isEqualTo(fahrradroute.getId());
		assertThat(fahrradrouteDetailView.getName()).isEqualTo(fahrradroute.getName());
		assertThat(fahrradrouteDetailView.getToubizId()).isEqualTo(fahrradroute.getToubizId());
		assertThat(fahrradrouteDetailView.getKurzbeschreibung()).isEqualTo(fahrradroute.getKurzbeschreibung());
		assertThat(fahrradrouteDetailView.getBeschreibung()).isEqualTo(fahrradroute.getBeschreibung());
		assertThat(fahrradrouteDetailView.getTourenkategorie()).isEqualTo(fahrradroute.getTourenkategorie());
		assertThat(fahrradrouteDetailView.getOffizielleLaenge()).isEqualTo(
			fahrradroute.getOffizielleLaenge().get().getValue());
		assertThat(fahrradrouteDetailView.getHomepage()).isEqualTo(fahrradroute.getHomepage());
		assertThat(fahrradrouteDetailView.getVerantwortlich().getId()).isEqualTo(
			fahrradroute.getVerantwortlich().get().getId());
		assertThat(fahrradrouteDetailView.getVerantwortlich().getName()).isEqualTo(
			fahrradroute.getVerantwortlich().get().getName());
		assertThat(fahrradrouteDetailView.getVerantwortlich().getIdUebergeordneteOrganisation()).isNull();
		assertThat(fahrradrouteDetailView.getVerantwortlich().getOrganisationsArt()).isEqualTo(
			fahrradroute.getVerantwortlich().get().getOrganisationsArt());
		assertThat(fahrradrouteDetailView.getEmailAnsprechpartner()).isEqualTo(fahrradroute.getEmailAnsprechpartner());
		assertThat(fahrradrouteDetailView.getLizenz()).isEqualTo(fahrradroute.getLizenz());
		assertThat(fahrradrouteDetailView.getLizenzNamensnennung()).isEqualTo(fahrradroute.getLizenzNamensnennung());
		assertThat(fahrradrouteDetailView.isCanEditAttribute()).isFalse();
		assertThat(fahrradrouteDetailView.isVeroeffentlicht()).isFalse();

		AbschnittsweiserKantenBezug kantenBezug = new ArrayList<>(fahrradroute.getAbschnittsweiserKantenBezug()).get(0);
		AbschnittsweiserKantenBezugView abschnittsweiserKantenBezugView = new AbschnittsweiserKantenBezugView(
			kantenBezug.getKante().getId(), kantenBezug.getKante().getGeometry(),
			kantenBezug.getLinearReferenzierterAbschnitt());
		assertThat(fahrradrouteDetailView.getKantenBezug()).containsExactlyInAnyOrder(abschnittsweiserKantenBezugView);
	}

	@Test
	public void saveFahrradroute() {
		// Arrange
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.id(1L)
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();

		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);
		when(fahrradrouteService.loadForModification(fahrradroute.getId(),
			fahrradroute.getVersion())).thenReturn(fahrradroute);
		when(fahrradrouteGuard.darfFahrradrouteBearbeiten(eq(benutzer), any())).thenReturn(true);

		SaveFahrradrouteCommand saveFahrradrouteCommand = SaveFahrradrouteCommandTestDataProvider.withDefaultValue()
			.id(fahrradroute.getId())
			.version(fahrradroute.getVersion())
			.build();

		when(fahrradrouteService.saveFahrradroute(fahrradroute)).thenReturn(fahrradroute);

		// Act
		FahrradrouteDetailView fahrradrouteDetailView = fahrradrouteController.saveFahrradroute(authentication,
			saveFahrradrouteCommand);

		// Assert
		verify(fahrradrouteGuard, times(1)).saveFahrradroute(authentication, saveFahrradrouteCommand, fahrradroute);
		verify(saveFahrradrouteCommandConverter, times(1)).apply(eq(saveFahrradrouteCommand), eq(fahrradroute));

		assertThat(fahrradrouteDetailView.getId()).isEqualTo(fahrradroute.getId());
		assertThat(fahrradrouteDetailView.getName()).isEqualTo(fahrradroute.getName());
		assertThat(fahrradrouteDetailView.getToubizId()).isEqualTo(fahrradroute.getToubizId());
		assertThat(fahrradrouteDetailView.getKurzbeschreibung()).isEqualTo(fahrradroute.getKurzbeschreibung());
		assertThat(fahrradrouteDetailView.getBeschreibung()).isEqualTo(fahrradroute.getBeschreibung());
		assertThat(fahrradrouteDetailView.getTourenkategorie()).isEqualTo(fahrradroute.getTourenkategorie());
		assertThat(fahrradrouteDetailView.getOffizielleLaenge()).isEqualTo(
			fahrradroute.getOffizielleLaenge().get().getValue());
		assertThat(fahrradrouteDetailView.getHomepage()).isEqualTo(fahrradroute.getHomepage());
		assertThat(fahrradrouteDetailView.getVerantwortlich().getId()).isEqualTo(
			fahrradroute.getVerantwortlich().get().getId());
		assertThat(fahrradrouteDetailView.getVerantwortlich().getName()).isEqualTo(
			fahrradroute.getVerantwortlich().get().getName());
		assertThat(fahrradrouteDetailView.getVerantwortlich().getIdUebergeordneteOrganisation()).isNull();
		assertThat(fahrradrouteDetailView.getVerantwortlich().getOrganisationsArt()).isEqualTo(
			fahrradroute.getVerantwortlich().get().getOrganisationsArt());
		assertThat(fahrradrouteDetailView.getEmailAnsprechpartner()).isEqualTo(fahrradroute.getEmailAnsprechpartner());
		assertThat(fahrradrouteDetailView.getLizenz()).isEqualTo(fahrradroute.getLizenz());
		assertThat(fahrradrouteDetailView.getLizenzNamensnennung()).isEqualTo(fahrradroute.getLizenzNamensnennung());
		assertThat(fahrradrouteDetailView.isCanEditAttribute()).isTrue();
		assertThat(fahrradrouteDetailView.isVeroeffentlicht()).isFalse();

		AbschnittsweiserKantenBezug kantenBezug = new ArrayList<>(fahrradroute.getAbschnittsweiserKantenBezug()).get(0);
		AbschnittsweiserKantenBezugView abschnittsweiserKantenBezugView = new AbschnittsweiserKantenBezugView(
			kantenBezug.getKante().getId(), kantenBezug.getKante().getGeometry(),
			kantenBezug.getLinearReferenzierterAbschnitt());
		assertThat(fahrradrouteDetailView.getKantenBezug()).containsExactlyInAnyOrder(abschnittsweiserKantenBezugView);
	}

	@Test
	public void changeVeroeffentlicht() {
		// Arrange
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.id(1L)
			.version(1L)
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();

		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);
		when(fahrradrouteService.loadForModification(fahrradroute.getId(),
			fahrradroute.getVersion())).thenReturn(fahrradroute);
		when(fahrradrouteGuard.darfFahrradrouteBearbeiten(eq(benutzer), any())).thenReturn(true);

		ChangeFahrradrouteVeroeffentlichtCommand command = ChangeFahrradrouteVeroeffentlichtCommand.builder()
			.id(fahrradroute.getId())
			.version(fahrradroute.getVersion())
			.veroeffentlicht(true)
			.build();

		when(fahrradrouteService.saveFahrradroute(fahrradroute)).thenReturn(fahrradroute);

		// Act
		FahrradrouteDetailView fahrradrouteDetailView = fahrradrouteController.changeVeroeffentlicht(authentication,
			command);

		// Assert
		verify(fahrradrouteGuard, times(1)).changeVeroeffentlicht(authentication, command);
		assertThat(fahrradroute.isVeroeffentlicht()).isTrue();

		assertThat(fahrradrouteDetailView.getId()).isEqualTo(fahrradroute.getId());
		assertThat(fahrradrouteDetailView.getName()).isEqualTo(fahrradroute.getName());
		assertThat(fahrradrouteDetailView.getToubizId()).isEqualTo(fahrradroute.getToubizId());
		assertThat(fahrradrouteDetailView.getKurzbeschreibung()).isEqualTo(fahrradroute.getKurzbeschreibung());
		assertThat(fahrradrouteDetailView.getBeschreibung()).isEqualTo(fahrradroute.getBeschreibung());
		assertThat(fahrradrouteDetailView.getTourenkategorie()).isEqualTo(fahrradroute.getTourenkategorie());
		assertThat(fahrradrouteDetailView.getOffizielleLaenge()).isEqualTo(
			fahrradroute.getOffizielleLaenge().get().getValue());
		assertThat(fahrradrouteDetailView.getHomepage()).isEqualTo(fahrradroute.getHomepage());
		assertThat(fahrradrouteDetailView.getVerantwortlich().getId()).isEqualTo(
			fahrradroute.getVerantwortlich().get().getId());
		assertThat(fahrradrouteDetailView.getVerantwortlich().getName()).isEqualTo(
			fahrradroute.getVerantwortlich().get().getName());
		assertThat(fahrradrouteDetailView.getVerantwortlich().getIdUebergeordneteOrganisation()).isNull();
		assertThat(fahrradrouteDetailView.getVerantwortlich().getOrganisationsArt()).isEqualTo(
			fahrradroute.getVerantwortlich().get().getOrganisationsArt());
		assertThat(fahrradrouteDetailView.getEmailAnsprechpartner()).isEqualTo(fahrradroute.getEmailAnsprechpartner());
		assertThat(fahrradrouteDetailView.getLizenz()).isEqualTo(fahrradroute.getLizenz());
		assertThat(fahrradrouteDetailView.getLizenzNamensnennung()).isEqualTo(fahrradroute.getLizenzNamensnennung());
		assertThat(fahrradrouteDetailView.isCanEditAttribute()).isFalse();
		assertThat(fahrradrouteDetailView.isVeroeffentlicht()).isTrue();

		AbschnittsweiserKantenBezug kantenBezug = new ArrayList<>(fahrradroute.getAbschnittsweiserKantenBezug()).get(0);
		AbschnittsweiserKantenBezugView abschnittsweiserKantenBezugView = new AbschnittsweiserKantenBezugView(
			kantenBezug.getKante().getId(), kantenBezug.getKante().getGeometry(),
			kantenBezug.getLinearReferenzierterAbschnitt());
		assertThat(fahrradrouteDetailView.getKantenBezug()).containsExactlyInAnyOrder(abschnittsweiserKantenBezugView);
	}

	@Test
	public void deleteFahrradroute() {
		// Arrange
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.id(1L)
			.version(1L)
			.build();

		when(fahrradrouteService.loadForModification(fahrradroute.getId(), fahrradroute.getVersion()))
			.thenReturn(fahrradroute);

		DeleteFahrradrouteCommand deleteFahrradrouteCommand = DeleteFahrradrouteCommand.builder()
			.id(fahrradroute.getId())
			.version(fahrradroute.getVersion())
			.build();

		// Act
		fahrradrouteController.deleteFahrradroute(fahrradroute.getId(), authentication, deleteFahrradrouteCommand);

		// Assert
		verify(fahrradrouteGuard, times(1)).deleteFahrradroute(authentication, fahrradroute);
		ArgumentCaptor<Fahrradroute> deletedFahrradroute = ArgumentCaptor.forClass(Fahrradroute.class);
		verify(fahrradrouteService).saveFahrradroute(deletedFahrradroute.capture());
		assertThat(deletedFahrradroute.getValue()).isEqualTo(fahrradroute);
		assertThat(deletedFahrradroute.getValue().isGeloescht()).isEqualTo(true);
	}

	@Test
	public void createFahrradroute() {
		// Arrange
		Long kanteId = 753456L;

		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		when(kanteResolver.getKante(kanteId)).thenReturn(kante);
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build();
		when(benutzerResolver.fromAuthentication(any()))
			.thenReturn(BenutzerTestDataProvider.defaultBenutzer().organisation(organisation).build());

		LineString stuetzpunkte = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(0, 0), new Coordinate(1, 1) });

		CreateFahrradrouteCommand createFahrradrouteCommand = CreateFahrradrouteCommandTestDataProvider
			.withKante(kanteId)
			.name("name")
			.beschreibung("beschreibung")
			.kategorie(Kategorie.RADSCHNELLWEG)
			.stuetzpunkte(stuetzpunkte)
			.build();

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.id(18745L)
			.build();
		when(createFahrradrouteCommandConverter.convert(authentication, createFahrradrouteCommand)).thenReturn(
			fahrradroute);
		when(fahrradrouteService.saveFahrradroute(any())).thenReturn(fahrradroute);

		// Act
		Long id = fahrradrouteController.createFahrradroute(authentication, createFahrradrouteCommand);

		// Assert
		ArgumentCaptor<Fahrradroute> savedFahrradroute = ArgumentCaptor.forClass(Fahrradroute.class);
		verify(fahrradrouteService).saveFahrradroute(savedFahrradroute.capture());
		verify(fahrradrouteGuard).createFahrradroute(authentication, createFahrradrouteCommand);
		assertThat(savedFahrradroute.getValue()).isEqualTo(fahrradroute);
		assertThat(id).isEqualTo(fahrradroute.getId());
	}
}
