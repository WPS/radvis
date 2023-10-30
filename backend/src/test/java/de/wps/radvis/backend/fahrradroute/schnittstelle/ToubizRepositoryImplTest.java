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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.fahrradroute.domain.entity.ImportedToubizRoute;
import de.wps.radvis.backend.fahrradroute.domain.repository.ToubizRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;

class ToubizRepositoryImplTest {

	private ToubizRepository toubizRepository;

	@Mock
	private RestTemplate mockedRestTemplate;
	@Mock
	private ToubizConfigurationProperties toubizConfigurationProperties;
	@Mock
	private CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		when(toubizConfigurationProperties.getBaseUrl()).thenReturn("https://base-url.test");
		when(toubizConfigurationProperties.getFilterCategory()).thenReturn("filter-categorie");
		when(toubizConfigurationProperties.getToken()).thenReturn("dies-ist-ein-api-token");

		toubizRepository = new ToubizRepositoryImpl(coordinateReferenceSystemConverter,
			mockedRestTemplate,
			toubizConfigurationProperties);
	}

	@Test
	void importRouten_1Seite() {
		// arrange
		ToubizFahrradrouteErgebnisDto ergebnisDto = getToubizFahrradrouteErgebnisDto(
			"TestId",
			"TestFahrradroute",
			"Das ist eine Testbeschreibung",
			5000.0f, 10000.0f,
			"");

		when(mockedRestTemplate.getForObject(Mockito.anyString(), Mockito.eq(ToubizFahrradrouteErgebnisDto.class)))
			.thenReturn(ergebnisDto);

		ToubizFahrradrouteTagsErgebnisDto tagsErgebnisDto = new ToubizFahrradrouteTagsErgebnisDto();
		tagsErgebnisDto.setPayload(Collections.emptyList());

		when(mockedRestTemplate.getForObject(Mockito.anyString(), Mockito.eq(ToubizFahrradrouteTagsErgebnisDto.class)))
			.thenReturn(tagsErgebnisDto);

		// Koordinaten in UTM32_N
		Geometry originalGeometrie = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(1, 1));
		when(coordinateReferenceSystemConverter.transformGeometry(Mockito.any(), Mockito.any()))
			.thenReturn(originalGeometrie);

		// act
		List<ImportedToubizRoute> importedToubizRoutes = toubizRepository.importRouten();

		// assert
		assertThat(importedToubizRoutes).hasSize(1);
		assertThat(importedToubizRoutes.get(0).getToubizId()).isEqualTo(ToubizId.of("TestId"));
		assertThat(importedToubizRoutes.get(0).getName()).isEqualTo(FahrradrouteName.of("TestFahrradroute"));
		assertThat(importedToubizRoutes.get(0).getBeschreibung()).isEqualTo("Das ist eine Testbeschreibung");

		assertThat(importedToubizRoutes.get(0).getOriginalGeometrie().getCoordinates()).containsExactly(
			new Coordinate(0, 0),
			new Coordinate(1, 1));

		ArgumentCaptor<Geometry> captor = ArgumentCaptor.forClass(Geometry.class);
		verify(coordinateReferenceSystemConverter, times(1)).transformGeometry(captor.capture(), Mockito.any());
		assertThat(captor.getValue().getCoordinates()).containsExactly(
			new Coordinate(5000.0f, 5000.0f),
			new Coordinate(10000.0f, 10000.0f));
	}

	@Test
	void importRouten_mehrereSeiten() {
		// arrange
		// Ergebnisse der Seite 1
		ToubizFahrradrouteErgebnisDto ergebnisDtoSeite1 = getToubizFahrradrouteErgebnisDto("TestId",
			"TestFahrradroute",
			"Das ist eine Testbeschreibung",
			5000.0f, 10000.0f,
			"https://linkToNextImportPage.de");

		when(mockedRestTemplate.getForObject(Mockito.anyString(), Mockito.eq(ToubizFahrradrouteErgebnisDto.class)))
			.thenReturn(ergebnisDtoSeite1);

		// Ergebnisse der Seite 2
		ToubizFahrradrouteErgebnisDto ergebnisDtoSeite2 = getToubizFahrradrouteErgebnisDto(
			"TestId2",
			"TestFahrradroute2",
			"Das ist eine Testbeschreibung2",
			5002.0f, 10002.0f,
			"");

		when(mockedRestTemplate.getForObject("https://linkToNextImportPage.de", ToubizFahrradrouteErgebnisDto.class))
			.thenReturn(ergebnisDtoSeite2);

		ToubizFahrradrouteTagsErgebnisDto tagsErgebnisDto = new ToubizFahrradrouteTagsErgebnisDto();
		tagsErgebnisDto.setPayload(Collections.emptyList());

		when(mockedRestTemplate.getForObject(Mockito.anyString(), Mockito.eq(ToubizFahrradrouteTagsErgebnisDto.class)))
			.thenReturn(tagsErgebnisDto);

		// Koordinaten in UTM32_N
		Geometry originalGeometrie1 = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(1, 1));
		Geometry originalGeometrie2 = GeometryTestdataProvider.createLineString(new Coordinate(20, 20),
			new Coordinate(22, 22));
		when(coordinateReferenceSystemConverter.transformGeometry(Mockito.any(), Mockito.any()))
			.thenReturn(originalGeometrie1)
			.thenReturn(originalGeometrie2);

		// act
		List<ImportedToubizRoute> importedToubizRoutes = toubizRepository.importRouten();

		// assert
		ArgumentCaptor<Geometry> captor = ArgumentCaptor.forClass(Geometry.class);
		verify(coordinateReferenceSystemConverter, times(2)).transformGeometry(captor.capture(), Mockito.any());
		assertThat(importedToubizRoutes).hasSize(2);
		assertThat(captor.getAllValues()).hasSize(2);

		assertThat(importedToubizRoutes).extracting(ImportedToubizRoute::getToubizId)
			.containsExactly(ToubizId.of("TestId"), ToubizId.of("TestId2"));
		assertThat(importedToubizRoutes).extracting(ImportedToubizRoute::getName)
			.containsExactly(FahrradrouteName.of("TestFahrradroute"), FahrradrouteName.of("TestFahrradroute2"));
		assertThat(importedToubizRoutes).extracting(ImportedToubizRoute::getBeschreibung)
			.containsExactly("Das ist eine Testbeschreibung", "Das ist eine Testbeschreibung2");

		assertThat(importedToubizRoutes).extracting(ImportedToubizRoute::getOriginalGeometrie)
			.extracting(Geometry::getCoordinates)
			.containsExactly(
				new Coordinate[] {
						new Coordinate(0, 0),
						new Coordinate(1, 1)
				},
				new Coordinate[] {
						new Coordinate(20, 20),
						new Coordinate(22, 22)
				});

		assertThat(captor.getAllValues()).extracting(Geometry::getCoordinates).containsExactly(
			new Coordinate[] {
				new Coordinate(5000.0f, 5000.0f),
				new Coordinate(10000.0f, 10000.0f)
			},
			new Coordinate[] {
				new Coordinate(5002.0f, 5002.0f),
				new Coordinate(10002.0f, 10002.0f)
			});
	}

	@Test
	void importRouten_mehrereSeiten_zeichnetLandesradfernwegeAus() {
		// arrange
		// Ergebnisse der Seite 1
		String testId1 = "TestId";
		ToubizFahrradrouteErgebnisDto ergebnisDtoSeite1 = getToubizFahrradrouteErgebnisDto(testId1,
			"TestFahrradroute",
			"Das ist eine Testbeschreibung",
			5000.0f, 10000.0f,
			"https://linkToNextImportPage.de");

		when(mockedRestTemplate.getForObject(Mockito.anyString(), Mockito.eq(ToubizFahrradrouteErgebnisDto.class)))
			.thenReturn(ergebnisDtoSeite1);

		ToubizFahrradrouteTagsErgebnisDto tagsErgebnisDto = new ToubizFahrradrouteTagsErgebnisDto();
		tagsErgebnisDto.setPayload(Collections.emptyList());

		when(mockedRestTemplate.getForObject(Mockito.contains(testId1),
			Mockito.eq(ToubizFahrradrouteTagsErgebnisDto.class)))
			.thenReturn(tagsErgebnisDto);

		// Ergebnisse der Seite 2
		String testId2 = "TestId2";
		ToubizFahrradrouteErgebnisDto ergebnisDtoSeite2 = getToubizFahrradrouteErgebnisDto(
			testId2,
			"TestFahrradroute2",
			"Das ist eine Testbeschreibung2",
			5002.0f, 10002.0f,
			"");

		when(mockedRestTemplate.getForObject("https://linkToNextImportPage.de", ToubizFahrradrouteErgebnisDto.class))
			.thenReturn(ergebnisDtoSeite2);

		ToubizFahrradrouteTagsErgebnisDto tagsErgebnisDto2 = new ToubizFahrradrouteTagsErgebnisDto();
		tagsErgebnisDto2.setPayload(List.of("landesradfernweg", "randomtag"));

		when(mockedRestTemplate.getForObject(Mockito.contains(testId2),
			Mockito.eq(ToubizFahrradrouteTagsErgebnisDto.class)))
			.thenReturn(tagsErgebnisDto2);

		// Koordinaten in UTM32_N
		Geometry originalGeometrie1 = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(1, 1));
		Geometry originalGeometrie2 = GeometryTestdataProvider.createLineString(new Coordinate(20, 20),
			new Coordinate(22, 22));
		when(coordinateReferenceSystemConverter.transformGeometry(Mockito.any(), Mockito.any()))
			.thenReturn(originalGeometrie1)
			.thenReturn(originalGeometrie2);

		// act
		List<ImportedToubizRoute> importedToubizRoutes = toubizRepository.importRouten();

		// assert
		assertThat(importedToubizRoutes).hasSize(2);

		assertThat(importedToubizRoutes).extracting(ImportedToubizRoute::getToubizId)
			.containsExactly(ToubizId.of(testId1), ToubizId.of(testId2));

		assertThat(importedToubizRoutes).filteredOn(ImportedToubizRoute::isLandesradfernweg)
			.extracting(ImportedToubizRoute::getToubizId)
			.containsExactly(ToubizId.of(testId2));

	}

	@Test
	void importRouten_OriginalGeometrieCannotBeNull() {
		// arrange
		ToubizFahrradrouteErgebnisDto ergebnisDto = getToubizFahrradrouteErgebnisDto(
			"TestId",
			"TestFahrradroute",
			"Das ist eine Testbeschreibung",
			5000.0f, 10000.0f,
			"");

		when(mockedRestTemplate.getForObject(Mockito.anyString(), Mockito.eq(ToubizFahrradrouteErgebnisDto.class)))
			.thenReturn(ergebnisDto);

		ToubizFahrradrouteTagsErgebnisDto tagsErgebnisDto = new ToubizFahrradrouteTagsErgebnisDto();
		tagsErgebnisDto.setPayload(Collections.emptyList());

		when(mockedRestTemplate.getForObject(Mockito.anyString(), Mockito.eq(ToubizFahrradrouteTagsErgebnisDto.class)))
			.thenReturn(tagsErgebnisDto);

		when(coordinateReferenceSystemConverter.transformGeometry(Mockito.any(), Mockito.any()))
			.thenReturn(null);

		// act
		List<ImportedToubizRoute> importedToubizRoutes = toubizRepository.importRouten();

		// assert
		assertThat(importedToubizRoutes).hasSize(0);
	}

	private ToubizFahrradrouteErgebnisDto getToubizFahrradrouteErgebnisDto(String fahrradrouteId,
		String fahrradrouteName,
		String beschreibung, float coord1xy, float coord2xy, String nextPageLink) {
		ToubizFahrradrouteErgebnisDto ergebnisDtoSeite = new ToubizFahrradrouteErgebnisDto();
		// Payload
		ToubizFahrradroutePayloadDto fahrradroutePayloadDto = new ToubizFahrradroutePayloadDto();
		fahrradroutePayloadDto.setId(fahrradrouteId);
		fahrradroutePayloadDto.setName(fahrradrouteName);
		fahrradroutePayloadDto.setDescription(beschreibung);
		ToubizFahrradroutePayloadTourDto tourDto = new ToubizFahrradroutePayloadTourDto();
		// Koordinaten nicht in UTM32_N
		tourDto.setPoints(List.of(
			List.of(coord1xy, coord1xy),
			List.of(coord2xy, coord2xy)));
		TrackInformationDto trackInformation = new TrackInformationDto();
		trackInformation.setDistance(Math.round(coord2xy - coord1xy));
		tourDto.setTrackInformation(trackInformation);
		fahrradroutePayloadDto.setTour(tourDto);
		ergebnisDtoSeite.setPayload(List.of(fahrradroutePayloadDto));

		// Link to next page
		ToubizFahrradrouteLinksDto fahrradrouteLinksDto = new ToubizFahrradrouteLinksDto();
		fahrradrouteLinksDto.setNextPage(nextPageLink);
		ergebnisDtoSeite.set_links(fahrradrouteLinksDto);
		return ergebnisDtoSeite;
	}
}
