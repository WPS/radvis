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

package de.wps.radvis.backend.weitereKartenebenen.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.DateiLayer;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.DateiLayerRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.GeoserverRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.WeitereKartenebenenRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.DateiLayerFormat;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverDatastoreName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverLayerName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverStyleName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Name;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Quellangabe;
import jakarta.persistence.EntityNotFoundException;

public class DateiLayerServiceTest {

	private DateiLayerService dateiLayerService;

	@Mock
	private DateiLayerRepository dateiLayerRepository;

	@Mock
	private WeitereKartenebenenRepository weitereKartenebenenRepository;

	@Mock
	private GeoserverRepository geoserverRepository;

	@BeforeEach
	public void setup() throws IOException, InterruptedException, URISyntaxException {
		MockitoAnnotations.openMocks(this);

		when(geoserverRepository.createDataStoreAndLayer(any(), any(), any())).thenReturn(
			GeoserverLayerName.of("foobar"));

		when(geoserverRepository.createStyle(any(), any())).thenAnswer(arg -> arg.getArguments()[0]);

		dateiLayerService = new DateiLayerService(dateiLayerRepository, weitereKartenebenenRepository,
			geoserverRepository);
	}

	@Test
	public void test_createLayer() throws IOException, InterruptedException, URISyntaxException {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		MultipartFile multipartFile = mock(MultipartFile.class);
		Name name = Name.of("EIN-burger Kostet##12,34 €");
		Quellangabe quellangabe = Quellangabe.of("test");

		// act
		DateiLayer dateiLayer = dateiLayerService.createDateiLayer(name, quellangabe, benutzer,
			DateiLayerFormat.GEOJSON, multipartFile);

		// assert
		verify(geoserverRepository).createDataStoreAndLayer(any(), eq(DateiLayerFormat.GEOJSON), eq(multipartFile));
		assertThat(dateiLayer.getDateiLayerFormat()).isEqualTo(DateiLayerFormat.GEOJSON);
		assertThat(dateiLayer.getName()).isEqualTo(name);
		assertThat(dateiLayer.getGeoserverLayerName().getValue()).isEqualTo("foobar");
		assertThat(dateiLayer.getGeoserverDatastoreName().getValue()).matches("ein-burger_kostet_12_34_\\d+");
		assertThat(dateiLayer.getQuellangabe()).isEqualTo(quellangabe);
		assertThat(dateiLayer.getBenutzer()).isEqualTo(benutzer);
	}

	@Test
	public void test_loeschen() throws IOException, InterruptedException, EntityNotFoundException, URISyntaxException {
		// Arrange
		long dateiLayerId = 123L;
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		DateiLayer dateiLayer = DateiLayer.builder()
			.name(Name.of("name"))
			.quellangabe(Quellangabe.of("quallige quelle"))
			.geoserverLayerName(GeoserverLayerName.of("geoserver layer name"))
			.geoserverDatastoreName(GeoserverDatastoreName.of("datastore-name"))
			.benutzer(benutzer)
			.dateiLayerFormat(DateiLayerFormat.GEOJSON)
			.erstelltAm(LocalDateTime.now())
			.build();
		when(dateiLayerRepository.findById(dateiLayerId)).thenReturn(Optional.of(dateiLayer));

		// Act
		dateiLayerService.deleteDateiLayer(dateiLayerId);

		// Assert
		InOrder inOrder = inOrder(weitereKartenebenenRepository, dateiLayerRepository, geoserverRepository);
		inOrder.verify(weitereKartenebenenRepository).deleteAllByDateiLayerId(dateiLayerId);
		inOrder.verify(dateiLayerRepository).deleteById(dateiLayerId);
		inOrder.verify(geoserverRepository).removeDatastoreAndLayer(dateiLayer.getGeoserverDatastoreName());
	}

	@Test
	public void test_changeOrAddStyleForLayer_keinStyleVorhanden()
		throws IOException, InterruptedException, URISyntaxException {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		MultipartFile multipartFile = mock(MultipartFile.class);
		when(multipartFile.getOriginalFilename()).thenReturn("crazyStyles.sld");

		long dateiLayerId = 123L;
		DateiLayer dateiLayer = DateiLayer.builder()
			.name(Name.of("name"))
			.quellangabe(Quellangabe.of("quallige quelle"))
			.geoserverLayerName(GeoserverLayerName.of("geoserver layer name"))
			.geoserverDatastoreName(GeoserverDatastoreName.of("datastore-name"))
			.benutzer(benutzer)
			.dateiLayerFormat(DateiLayerFormat.GEOJSON)
			.erstelltAm(LocalDateTime.now())
			.build();
		when(dateiLayerRepository.findById(dateiLayerId)).thenReturn(Optional.of(dateiLayer));

		// act
		dateiLayerService.changeOrAddStyleForLayer(dateiLayerId, multipartFile);

		// assert
		ArgumentCaptor<GeoserverStyleName> styleNameCaptor = ArgumentCaptor.forClass(GeoserverStyleName.class);
		verify(geoserverRepository).createStyle(styleNameCaptor.capture(), eq(multipartFile));
		verify(geoserverRepository).addStyleToLayer(eq(dateiLayer.getGeoserverLayerName()),
			eq(styleNameCaptor.getValue()), eq(true));
		verifyNoMoreInteractions(geoserverRepository);
		assertThat(dateiLayer.getGeoserverStyleName()).isEqualTo(styleNameCaptor.getValue());
		assertThat(dateiLayer.getSldFilename()).isEqualTo("crazyStyles.sld");
	}

	@Test
	public void test_changeOrAddStyleForLayer_styleVorhanden()
		throws IOException, InterruptedException, URISyntaxException {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		MultipartFile multipartFile = mock(MultipartFile.class);
		when(multipartFile.getOriginalFilename()).thenReturn("crazyNewStyles.sld");

		long dateiLayerId = 123L;
		DateiLayer dateiLayer = DateiLayer.builder()
			.id(dateiLayerId)
			.name(Name.of("name"))
			.quellangabe(Quellangabe.of("quallige quelle"))
			.geoserverLayerName(GeoserverLayerName.of("geoserver layer name"))
			.geoserverDatastoreName(GeoserverDatastoreName.of("datastore-name"))
			.benutzer(benutzer)
			.dateiLayerFormat(DateiLayerFormat.GEOJSON)
			.erstelltAm(LocalDateTime.now())
			.build();

		GeoserverStyleName oldGeoserverStyleName = GeoserverStyleName.of("old_name");
		dateiLayer.setStyle(oldGeoserverStyleName, "plainOldStyles.sld");

		when(dateiLayerRepository.findById(dateiLayerId)).thenReturn(Optional.of(dateiLayer));

		// act
		dateiLayerService.changeOrAddStyleForLayer(dateiLayerId, multipartFile);

		// assert
		ArgumentCaptor<GeoserverStyleName> styleNameCaptor = ArgumentCaptor.forClass(GeoserverStyleName.class);
		verify(geoserverRepository).createStyle(styleNameCaptor.capture(), eq(multipartFile));
		verify(geoserverRepository).addStyleToLayer(eq(dateiLayer.getGeoserverLayerName()),
			eq(styleNameCaptor.getValue()), eq(true));
		verify(geoserverRepository).deleteStyle(eq(oldGeoserverStyleName));
		verifyNoMoreInteractions(geoserverRepository);
		assertThat(dateiLayer.getGeoserverStyleName()).isEqualTo(styleNameCaptor.getValue());
		assertThat(dateiLayer.getSldFilename()).isEqualTo("crazyNewStyles.sld");
	}

	@Test
	public void test_deleteStyle() throws IOException, InterruptedException, URISyntaxException {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		MultipartFile multipartFile = mock(MultipartFile.class);
		when(multipartFile.getOriginalFilename()).thenReturn("crazyNewStyles.sld");

		long dateiLayerId = 123L;
		DateiLayer dateiLayer = DateiLayer.builder()
			.id(dateiLayerId)
			.name(Name.of("name"))
			.quellangabe(Quellangabe.of("quallige quelle"))
			.geoserverLayerName(GeoserverLayerName.of("geoserver layer name"))
			.geoserverDatastoreName(GeoserverDatastoreName.of("datastore-name"))
			.benutzer(benutzer)
			.dateiLayerFormat(DateiLayerFormat.GEOJSON)
			.erstelltAm(LocalDateTime.now())
			.build();

		GeoserverStyleName geoserverStyleName = GeoserverStyleName.of("old_name");
		dateiLayer.setStyle(geoserverStyleName, "plainOldStyles.sld");

		when(dateiLayerRepository.findById(dateiLayerId)).thenReturn(Optional.of(dateiLayer));

		// act
		dateiLayerService.deleteStyle(dateiLayerId);

		// assert
		verify(geoserverRepository).deleteStyle(eq(geoserverStyleName));
		verifyNoMoreInteractions(geoserverRepository);
		assertThat(dateiLayer.getGeoserverStyleName()).isNull();
		assertThat(dateiLayer.getSldFilename()).isNull();
	}
}
