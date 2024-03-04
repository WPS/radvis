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

package de.wps.radvis.backend.karte.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.karte.domain.entity.HintergrundKarte;

class HintergrundKartenServiceTest {

	@Mock
	private HintergrundKartenRepository hintergrundKartenRepository;

	@Mock
	private CommonConfigurationProperties commonConfigurationProperties;

	@Mock(answer = Answers.RETURNS_SELF)
	private WebClient.Builder webClientBuilder;

	@Captor
	private ArgumentCaptor<Function<UriBuilder, URI>> uriBuilderToURICaptor;

	private HintergrundKartenService hintergrundKartenService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		hintergrundKartenService = new HintergrundKartenService(hintergrundKartenRepository,
			commonConfigurationProperties, webClientBuilder);
	}

	@Test
	void getTileXYZ_keyNichtVorhanden_gibtLeeresOptional() {
		//arrange
		when(hintergrundKartenRepository.find(any())).thenReturn(Optional.empty());

		//act & assert
		assertThat(hintergrundKartenService.getTileXYZ("test", new HashMap<>() {
			private static final long serialVersionUID = 1L;

			{
				put("x", 1);
				put("y", 2);
				put("z", 3);
			}
		})).isEmpty();
	}

	@Test
	void getTileWMS_keyNichtVorhanden_gibtLeeresOptional() {
		//arrange
		when(hintergrundKartenRepository.find(any())).thenReturn(Optional.empty());

		//act & assert
		assertThat(hintergrundKartenService.getTileWMS("test", new LinkedMultiValueMap<>() {
			private static final long serialVersionUID = 1L;

			{
				add("width", "1");
				add("height", "2");
				add("crs", "crs");
				add("bbox", "0,0,1,1");
			}
		})).isEmpty();
	}

	@Test
	void getTileXYZ_keyVorhanden_bautKorrekteURI() {
		//arrange
		when(hintergrundKartenRepository.find("xyz"))
			.thenReturn(Optional.of(new HintergrundKarte("https://xyz.domain.de/{x}/{y}?z={z}")));

		WebClient webClient = mock(WebClient.class, Answers.RETURNS_DEEP_STUBS);

		when(webClientBuilder.build()).thenReturn(webClient);

		//act
		hintergrundKartenService.getTileXYZ("xyz", new HashMap<>() {
			private static final long serialVersionUID = 1L;

			{
				put("x", 1);
				put("y", 2);
				put("z", 3);
			}
		});

		//assert
		verify(webClient.get()).uri(uriBuilderToURICaptor.capture());
		URI uri = uriBuilderToURICaptor.getValue().apply(UriComponentsBuilder.newInstance());
		assertThat(uri).hasPath("/1/2");
		assertThat(uri).hasParameter("z", "3");
	}

	@Test
	void getTileWMS_keyVorhanden_bautKorrekteURI() {
		//arrange
		when(hintergrundKartenRepository.find("wms"))
			.thenReturn(Optional.of(new HintergrundKarte("https://wms.domain.de/?a=a&b=b")));

		WebClient webClient = mock(WebClient.class, Answers.RETURNS_DEEP_STUBS);

		when(webClientBuilder.build()).thenReturn(webClient);

		//act
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>() {
			private static final long serialVersionUID = 1L;

			{
				add("width", "1");
				add("height", "2");
				add("crs", "crs");
				add("bbox", "0,0,1,1");
			}
		};
		hintergrundKartenService.getTileWMS("wms", params);

		//assert
		verify(webClient.get()).uri(uriBuilderToURICaptor.capture());
		URI uri = uriBuilderToURICaptor.getValue().apply(UriComponentsBuilder.newInstance());
		params.forEach((key, value) -> assertThat(uri).hasParameter(key, value.get(0)));
	}

}