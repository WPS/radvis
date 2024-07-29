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

package de.wps.radvis.backend.ortssuche.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.ortssuche.domain.OrtsSucheConfigurationProperties;
import de.wps.radvis.backend.ortssuche.domain.OrtssucheRepository;
import de.wps.radvis.backend.ortssuche.domain.entity.OrtsSucheErgebnis;

public class OrtssucheRepositoryImplTest {
	private OrtssucheRepository bkgOrtsSucheService;

	@Mock
	private RestTemplate mockedRestTemplate;

	@Mock
	private CoordinateReferenceSystemConverter mockedCoordinateReferenceSystemConverter;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		this.bkgOrtsSucheService = new OrtssucheRepositoryImpl(mockedCoordinateReferenceSystemConverter,
			mockedRestTemplate,
			new OrtsSucheConfigurationProperties("https://foo.bar", "123", "file.json", 10, "1,2,3,4"));
	}

	@Test
	public void testsucheOrt_mitOrt_ruftKorrekteTransformationAuf() {
		// arrange
		BkgSuchErgebnisDto suchErgebnis = createSuchErgebnisDto();

		Mockito.when(mockedRestTemplate.getForObject(Mockito.anyString(), Mockito.any()))
			.thenReturn(suchErgebnis);

		Mockito.when(mockedCoordinateReferenceSystemConverter.transformCoordinateUnsafe(any(), any(), any()))
			.thenAnswer(i -> i.getArguments()[0]);
		Mockito.when(mockedCoordinateReferenceSystemConverter.transformGeometry(any(), any()))
			.thenAnswer(i -> i.getArguments()[0]);

		// act
		bkgOrtsSucheService.find("Wursthausen");

		// assert
		Mockito.verify(mockedCoordinateReferenceSystemConverter, Mockito.times(5)).transformCoordinateUnsafe(
			Mockito.any(),
			Mockito.eq(KoordinatenReferenzSystem.WGS84),
			Mockito.eq(KoordinatenReferenzSystem.ETRS89_UTM32_N));
		Mockito.verify(mockedCoordinateReferenceSystemConverter, Mockito.times(5)).transformGeometry(
			Mockito.any(),
			Mockito.eq(KoordinatenReferenzSystem.ETRS89_UTM32_N));
	}

	@Test
	public void testsucheOrt_mitSonderzeichen() {
		// arrange
		Mockito.when(mockedRestTemplate.getForObject(Mockito.anyString(), Mockito.any()))
			.thenReturn(createSuchErgebnisDto());
		Mockito.when(mockedCoordinateReferenceSystemConverter.transformCoordinateUnsafe(any(), any(), any()))
			.thenAnswer(i -> i.getArguments()[0]);
		Mockito.when(mockedCoordinateReferenceSystemConverter.transformGeometry(any(), any()))
			.thenAnswer(i -> i.getArguments()[0]);

		// act
		bkgOrtsSucheService.find("q äöüß &| +-!(){}[]^\"~*?:\\&&||");

		// assert
		ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
		verify(mockedRestTemplate).getForObject(urlCaptor.capture(), Mockito.any());

		Optional<String> queryParam = Arrays.stream(urlCaptor.getValue().split("&(?=[a-zA-Z])"))
			.filter(param -> param.startsWith("query="))
			.findFirst();
		assertThat(queryParam).isPresent();
		assertThat(queryParam.get()).isEqualTo(
			"query=q äöüß &| \\+\\-\\!\\(\\)\\{\\}\\[\\]\\^\\\"\\~\\*\\?\\:\\\\\\&&\\||");
	}

	@Test
	public void testsucheOrt_mitOrt_ergebnisKorrektSortieert() {
		// arrange
		BkgSuchErgebnisDto suchErgebnis = createSuchErgebnisDto();

		Mockito.when(mockedRestTemplate.getForObject(Mockito.anyString(), Mockito.any()))
			.thenReturn(suchErgebnis);

		Mockito.when(mockedCoordinateReferenceSystemConverter.transformCoordinateUnsafe(any(), any(), any()))
			.thenAnswer(i -> i.getArguments()[0]);
		Mockito.when(mockedCoordinateReferenceSystemConverter.transformGeometry(any(), any()))
			.thenAnswer(i -> i.getArguments()[0]);

		// act
		List<OrtsSucheErgebnis> result = bkgOrtsSucheService.find("Wursthausen");

		// assert

		assertThat(result.size()).isEqualTo(5);
		assertThat(result).extracting(OrtsSucheErgebnis::getName)
			.containsExactly("5", "3", "1", "4", "2");
	}

	@Test
	public void testsucheOrt_mitOrt_ordinatenGetauscht() {
		// arrange
		BkgSuchErgebnisDto suchErgebnis = createSuchErgebnisDto();

		Mockito.when(mockedRestTemplate.getForObject(Mockito.anyString(), Mockito.any()))
			.thenReturn(suchErgebnis);

		Mockito.when(mockedCoordinateReferenceSystemConverter.transformCoordinateUnsafe(any(), any(), any()))
			.thenAnswer(i -> i.getArguments()[0]);
		Mockito.when(mockedCoordinateReferenceSystemConverter.transformGeometry(any(), any()))
			.thenAnswer(i -> i.getArguments()[0]);

		// act
		List<OrtsSucheErgebnis> result = bkgOrtsSucheService.find("Wursthausen");

		// assert

		assertThat(result.size()).isEqualTo(5);
		assertThat(result).extracting(OrtsSucheErgebnis::getCenterCoordinate)
			.containsExactlyInAnyOrder(
				new double[] { 10, 1 },
				new double[] { 20, 2 },
				new double[] { 30, 3 },
				new double[] { 40, 4 },
				new double[] { 50, 5 });
		assertThat(result).extracting(OrtsSucheErgebnis::getExtent)
			.containsExactlyInAnyOrder(
				new double[] { 0, 0, 20, 20 },
				new double[] { 0, 0, 30, 30 },
				new double[] { 0, 0, 40, 10 },
				new double[] { 0, 0, 50, 30 },
				new double[] { 0, 0, 60, 10 });
	}

	private BkgSuchErgebnisDto createSuchErgebnisDto() {
		BkgSuchErgebnisDto dto = new BkgSuchErgebnisDto();

		List<BkgOrtsSucheDto> features = new ArrayList<>();
		features.add(createOrtsSucheDto("1", 1.85231f, 1, 10, createBbox(20, 20)));
		features.add(createOrtsSucheDto("2", 0.123123f, 2, 20, createBbox(30, 30)));
		features.add(createOrtsSucheDto("3", 2.564f, 3, 30, createBbox(10, 40)));
		features.add(createOrtsSucheDto("4", 0.234f, 4, 40, createBbox(30, 50)));
		features.add(createOrtsSucheDto("5", 6.789f, 5, 50, createBbox(10, 60)));
		dto.setFeatures(features);

		return dto;
	}

	private BkgOrtsSucheDto createOrtsSucheDto(String id, float score, double x, double y, Polygon bbox) {
		BkgOrtsSucheDto ort = new BkgOrtsSucheDto();
		BkgOrtsSucheEigenschaftenDto eig = new BkgOrtsSucheEigenschaftenDto();
		eig.setText(id);
		eig.setScore(score);
		eig.setBbox(bbox);
		ort.setProperties(eig);
		Point point = KoordinatenReferenzSystem.WGS84.getGeometryFactory()
			.createPoint(new Coordinate(x, y));
		ort.setGeometry(point);
		return ort;
	}

	private Polygon createBbox(double width, double height) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(0, 0), new Coordinate(width, 0),
			new Coordinate(width, height), new Coordinate(0, height), new Coordinate(0, 0) };
		return KoordinatenReferenzSystem.WGS84.getGeometryFactory().createPolygon(coordinates);
	}

}
