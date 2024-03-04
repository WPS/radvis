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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
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

	AnpassungswunschService anpassungswunschService;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		anpassungswunschService = new AnpassungswunschService(anpassungswunschRepositoryMock,
			konsistenzregelVerletzungsRepository);
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
	}

	@Test
	void getUrsaechlicheKonsistenzregelVerletzung() {
		// Arrange
		KonsistenzregelVerletzung expectedKonsistenzregelVerletzung = KonsistenzregelVerletzungTestdataProvider.defaultVerletzung()
			.build();
		when(konsistenzregelVerletzungsRepository.findByTypAndIdentity(
			"Toller Typ", "123")).thenReturn(Optional.of(
			expectedKonsistenzregelVerletzung));

		// Act
		KonsistenzregelVerletzung actualKonsistenzregelVerletzung = anpassungswunschService.getUrsaechlicheKonsistenzregelVerletzung(
			KonsistenzregelVerletzungReferenz.of("123", "Toller Typ"));

		// Assert
		assertThat(actualKonsistenzregelVerletzung).isEqualTo(expectedKonsistenzregelVerletzung);
	}

	@Test
	void getUrsaechlicheKonsistenzregelVerletzung_null_wennKeineReferenz() {
		// Act
		KonsistenzregelVerletzung actualKonsistenzregelVerletzung = anpassungswunschService.getUrsaechlicheKonsistenzregelVerletzung(
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
					"Details Beschreibung", "123")).build()));

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
}
