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

package de.wps.radvis.backend.netzfehler.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.repository.FahrradrouteFilterRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.konsistenz.KonsistenzregelVerletzungTestdataProvider;
import de.wps.radvis.backend.konsistenz.pruefung.domain.KonsistenzregelVerletzungsRepository;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelVerletzung;
import de.wps.radvis.backend.konsistenz.pruefung.domain.event.KonsistenzregelVerletzungenDeletedEvent;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;
import de.wps.radvis.backend.netzfehler.domain.valueObject.KonsistenzregelVerletzungReferenz;

class AnpassungswunschServiceTest {

	static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(),
		KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

	@Mock
	private AnpassungswunschRepository anpassungswunschRepositoryMock;
	@Mock
	private KonsistenzregelVerletzungsRepository konsistenzregelVerletzungsRepository;
	@Mock
	private FahrradrouteFilterRepository fahrradrouteRepository;
	@Mock
	private MailService mailService;

	AnpassungswunschService anpassungswunschService;
	double distanzZuFahrradrouteInMetern = 20;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		when(anpassungswunschRepositoryMock.save(any())).thenAnswer(invocationOnMock -> {
			Anpassungswunsch anpassungswunsch = invocationOnMock.getArgument(0, Anpassungswunsch.class);
			ReflectionTestUtils.setField(anpassungswunsch, "id", 42L);
			return anpassungswunsch;
		});

		Map<AnpassungswunschKategorie, String> emailProKategorie = Map.of(
			AnpassungswunschKategorie.DLM, "dlm@dlm.de",
			AnpassungswunschKategorie.RADVIS, "radvis@radvis.de"
		);
		anpassungswunschService = new AnpassungswunschService(
			anpassungswunschRepositoryMock,
			konsistenzregelVerletzungsRepository,
			fahrradrouteRepository,
			mailService,
			"https://basis.url.de",
			emailProKategorie,
			distanzZuFahrradrouteInMetern
		);
	}

	@Test
	void create_AnpassungswunschWirdErstellt() {
		// Act
		anpassungswunschService.create(geometryFactory.createPoint(new Coordinate(1.5, 10.5)),
			"Dies ist eine Beschreibung", AnpassungswunschStatus.OFFEN, AnpassungswunschKategorie.DLM,
			BenutzerTestDataProvider.defaultBenutzer().build(), Optional.empty(), Optional.empty());

		// Assert
		ArgumentCaptor<Anpassungswunsch> captor = ArgumentCaptor.forClass(Anpassungswunsch.class);
		verify(anpassungswunschRepositoryMock).save(captor.capture());

		Anpassungswunsch anpassungswunsch = captor.getValue();

		assertThat(anpassungswunsch.getBeschreibung()).isEqualTo("Dies ist eine Beschreibung");
		assertThat(anpassungswunsch.getGeometrie().getCoordinate()).isEqualTo(new Coordinate(1.5, 10.5));
		assertThat(anpassungswunsch.getKonsistenzregelVerletzungReferenz()).isNotPresent();

		verifyNoInteractions(mailService);
	}

	@Test
	void getUrsaechlicheKonsistenzregelVerletzung() {
		// Arrange
		KonsistenzregelVerletzung expectedKonsistenzregelVerletzung = KonsistenzregelVerletzungTestdataProvider
			.defaultVerletzung()
			.build();
		when(konsistenzregelVerletzungsRepository.findByTypAndIdentity(
			"Toller Typ", "123")).thenReturn(Optional.of(
				expectedKonsistenzregelVerletzung));

		// Act
		KonsistenzregelVerletzung actualKonsistenzregelVerletzung = anpassungswunschService
			.getUrsaechlicheKonsistenzregelVerletzung(
				KonsistenzregelVerletzungReferenz.of("123", "Toller Typ"));

		// Assert
		assertThat(actualKonsistenzregelVerletzung).isEqualTo(expectedKonsistenzregelVerletzung);
	}

	@Test
	void getUrsaechlicheKonsistenzregelVerletzung_null_wennKeineReferenz() {
		// Act
		KonsistenzregelVerletzung actualKonsistenzregelVerletzung = anpassungswunschService
			.getUrsaechlicheKonsistenzregelVerletzung(
				null);

		// Assert
		assertThat(actualKonsistenzregelVerletzung).isNull();
	}

	@Test
	void create_AnpassungswunschErhaeltFehlerprotokollAttribute() {
		// Arrange
		Point point = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(0, 1));

		when(konsistenzregelVerletzungsRepository.findById(65703643L)).thenReturn(Optional.of(
			KonsistenzregelVerletzungTestdataProvider.defaultVerletzung().typ("Toller Typ")
				.details(new KonsistenzregelVerletzungsDetails(point,
					"Details Beschreibung", "123"))
				.build()));

		// Act
		anpassungswunschService.create(geometryFactory.createPoint(new Coordinate(1.5, 10.5)),
			"Dies ist eine Beschreibung", AnpassungswunschStatus.OFFEN, AnpassungswunschKategorie.DLM,
			BenutzerTestDataProvider.defaultBenutzer().build(), Optional.empty(),
			Optional.of("KonsistenzregelVerletzung/65703643"));

		// Assert
		ArgumentCaptor<Anpassungswunsch> captor = ArgumentCaptor.forClass(Anpassungswunsch.class);
		verify(anpassungswunschRepositoryMock).save(captor.capture());

		Anpassungswunsch anpassungswunsch = captor.getValue();

		assertThat(anpassungswunsch.getKonsistenzregelVerletzungReferenz()).isPresent();
		assertThat(anpassungswunsch.getKonsistenzregelVerletzungReferenz().get()).isEqualTo(
			KonsistenzregelVerletzungReferenz.of("123", "Toller Typ"));
	}

	@Test
	void onKonsistenzregelVerletzungenGeloescht_StatusIstAngepasst() {
		// Arrange
		Anpassungswunsch wunsch1 = Anpassungswunsch.builder()
			.geometrie(geometryFactory.createPoint(new Coordinate(1, 1)))
			.beschreibung("beschreibung 1")
			.status(AnpassungswunschStatus.OFFEN).kategorie(AnpassungswunschKategorie.DLM)
			.benutzerLetzteAenderung(BenutzerTestDataProvider.defaultBenutzer().build())
			.kommentarListe(new KommentarListe())
			.status(AnpassungswunschStatus.ERLEDIGT)
			.build();
		Anpassungswunsch wunsch2 = Anpassungswunsch.builder()
			.geometrie(geometryFactory.createPoint(new Coordinate(2, 2)))
			.beschreibung("beschreibung 2").kategorie(AnpassungswunschKategorie.DLM)
			.kommentarListe(new KommentarListe())
			.benutzerLetzteAenderung(BenutzerTestDataProvider.defaultBenutzer().build())
			.status(AnpassungswunschStatus.OFFEN)
			.build();
		when(anpassungswunschRepositoryMock.findByKonsistenzregelVerletzungReferenzIn(any())).thenReturn(
			Stream.of(wunsch1, wunsch2));

		// Act
		KonsistenzregelVerletzungenDeletedEvent event = new KonsistenzregelVerletzungenDeletedEvent("toller Typ",
			List.of("123", "456"));
		anpassungswunschService.onKonsistenzregelVerletzungenGeloescht(event);

		// Assert
		assertThat(wunsch1.getStatus()).isEqualTo(AnpassungswunschStatus.ERLEDIGT);
		assertThat(wunsch2.getStatus()).isEqualTo(AnpassungswunschStatus.UMGESETZT);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void getAlleAnpassungswuensche_filtersFahrradrouten() {
		// arrange
		when(fahrradrouteRepository.getAllGeometries(any()))
			.thenReturn(
				List.of(GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(0, 100))));
		Anpassungswunsch anpassungswunschWithin = AnpassungswunschTestDataProvider
			.withPosition(distanzZuFahrradrouteInMetern, 0).id(1L).build();
		Anpassungswunsch anpassungswunschOutside = AnpassungswunschTestDataProvider
			.withPosition(distanzZuFahrradrouteInMetern + 1, 0).id(2L).build();
		when(anpassungswunschRepositoryMock.findAll())
			.thenReturn(List.of(anpassungswunschWithin, anpassungswunschOutside));

		// act
		List<Long> nebenFahrradroutenIds = List.of(123L, 234L);
		List<Anpassungswunsch> anpassungswuensche = anpassungswunschService.getAlleAnpassungswuensche(false,
			nebenFahrradroutenIds).toList();

		// assert
		ArgumentCaptor<List> fahrradrouteListCaptor = ArgumentCaptor.forClass(List.class);
		verify(fahrradrouteRepository).getAllGeometries(fahrradrouteListCaptor.capture());
		assertThat(fahrradrouteListCaptor.getValue()).containsExactlyElementsOf(nebenFahrradroutenIds);
		assertThat(anpassungswuensche).hasSize(1);
		assertThat(anpassungswuensche).contains(anpassungswunschWithin);
		assertThat(anpassungswuensche).doesNotContain(anpassungswunschOutside);
	}

	@Test
	void getAlleAnpassungswuensche_noFahrradrouteGeometries_doesNotFilter() {
		// arrange
		when(fahrradrouteRepository.getAllGeometries(any())).thenReturn(Collections.emptyList());
		Anpassungswunsch anpassungswunschWithin = AnpassungswunschTestDataProvider
			.withPosition(distanzZuFahrradrouteInMetern, 0).id(1L).build();
		when(anpassungswunschRepositoryMock.findAll())
			.thenReturn(List.of(anpassungswunschWithin));

		// act
		List<Long> nebenFahrradroutenIds = List.of(123L, 234L);
		List<Anpassungswunsch> anpassungswuensche = anpassungswunschService.getAlleAnpassungswuensche(false,
			nebenFahrradroutenIds).toList();

		// assert
		assertThat(anpassungswuensche).containsExactly(anpassungswunschWithin);
	}

	@Test
	void getAlleAnpassungswuensche_noFahrradrouteFilter_doesNotFilter() {
		// arrange
		Anpassungswunsch anpassungswunschWithin = AnpassungswunschTestDataProvider
			.withPosition(distanzZuFahrradrouteInMetern, 0).id(1L).build();
		when(anpassungswunschRepositoryMock.findAll())
			.thenReturn(List.of(anpassungswunschWithin));

		// act
		List<Anpassungswunsch> anpassungswuensche = anpassungswunschService.getAlleAnpassungswuensche(false,
			Collections.emptyList()).toList();

		// assert
		verify(fahrradrouteRepository, never()).getAllGeometries(any());
		assertThat(anpassungswuensche).containsExactly(anpassungswunschWithin);
	}

	@Test
	void versendeInfoMailZuNeuemAnpassungswunsch_EmailNichtVorhanden_emailWirdNichtVerschickt() {
		// Act
		anpassungswunschService.versendeInfoMailZuNeuemAnpassungswunsch(
			AnpassungswunschTestDataProvider.defaultValue()
				.kategorie(AnpassungswunschKategorie.TT_SIB)
				.id(42L)
				.build()
		);

		// Assert
		verifyNoInteractions(mailService);
	}

	@Test
	void versendeInfoMailZuNeuemAnpassungswunsch_keineIdGesetzt_requireViolation() {
		// Act & Assert
		assertThatExceptionOfType(RequireViolation.class).isThrownBy(
			() -> anpassungswunschService.versendeInfoMailZuNeuemAnpassungswunsch(
				AnpassungswunschTestDataProvider.defaultValue()
					.kategorie(AnpassungswunschKategorie.RADVIS)
					.id(null)
					.build()
			));
	}

	@Test
	void versendeInfoMailZuNeuemAnpassungswunsch_EmailVorhanden_EmailWirdVerschickt() {
		// Act
		anpassungswunschService.versendeInfoMailZuNeuemAnpassungswunsch(
			AnpassungswunschTestDataProvider.defaultValue()
				.kategorie(AnpassungswunschKategorie.RADVIS)
				.id(42L)
				.build()
		);

		// Assert
		String expectedLink = "https://basis.url.de" + FrontendLinks.anpassungswunschDetailView(42L);
		verify(mailService).sendMail(
			List.of("radvis@radvis.de"),
			"RadVIS: Neuer Anpassungswunsch",
			"Es gibt einen neuen Anpassungswunsch in RadVIS: " + expectedLink
		);
	}
}
