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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriBuilder;

import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.karte.domain.entity.HintergrundKarte;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;
import reactor.util.retry.Retry;

public class HintergrundKartenService {
	private final HintergrundKartenRepository hintergrundKartenRepository;
	private final CommonConfigurationProperties commonConfigurationProperties;
	private final WebClient.Builder webClientBuilder;
	private final Map<HintergrundKarte, WebClient> webClientCache;
	private ReactorClientHttpConnector connector;

	public HintergrundKartenService(
		HintergrundKartenRepository hintergrundKartenRepository,
		CommonConfigurationProperties commonConfigurationProperties,
		WebClient.Builder webClientBuilder) {
		require(hintergrundKartenRepository, notNullValue());
		require(webClientBuilder, notNullValue());
		this.webClientBuilder = webClientBuilder;
		this.hintergrundKartenRepository = hintergrundKartenRepository;
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.webClientCache = new ConcurrentHashMap<>();

		initConnector();
	}

	public Optional<Mono<byte[]>> getTileWMS(String hintergrundKarteKey, MultiValueMap<String, String> params) {
		require(hintergrundKarteKey, notNullValue());
		require(params, notNullValue());
		require(allParamsPresentForWMS(params), "Nicht alle notwendigen Parameter für einen WMS Dienst vorhanden");

		Optional<HintergrundKarte> hintergrundKarteOptional = hintergrundKartenRepository.find(hintergrundKarteKey);

		if (hintergrundKarteOptional.isEmpty()) {
			return Optional.empty();
		}

		HintergrundKarte hintergrundKarte = hintergrundKarteOptional.get();
		WebClient webClient = getWebClient(hintergrundKarte);

		return Optional.of(makeApiCall(webClient, uriBuilder -> uriBuilder
			.path(hintergrundKarte.getPath())
			.query(hintergrundKarte.getQuery())
			.queryParams(params)
			.build()));
	}

	public Optional<Mono<byte[]>> getTileXYZ(String hintergrundKarteKey, Map<String, Integer> params) {
		require(hintergrundKarteKey, notNullValue());
		require(params, notNullValue());
		require(allParamsPresentForXYZ(params), "Nicht alle notwendigen Parameter für einen WMS Dienst vorhanden");

		Optional<HintergrundKarte> hintergrundKarteOptional = hintergrundKartenRepository.find(hintergrundKarteKey);

		if (hintergrundKarteOptional.isEmpty()) {
			return Optional.empty();
		}

		HintergrundKarte hintergrundKarte = hintergrundKarteOptional.get();
		WebClient webClient = getWebClient(hintergrundKarte);

		return Optional.of(makeApiCall(webClient, uriBuilder -> uriBuilder
			.path(hintergrundKarte.getPath())
			.query(hintergrundKarte.getQuery())
			.build(params)));
	}

	private Mono<byte[]> makeApiCall(WebClient webClient, Function<UriBuilder, URI> uriBuilderURIFunction) {
		return webClient.get()
			.uri(uriBuilderURIFunction)

			.exchangeToMono(clientResponse -> {
				if (clientResponse.statusCode()
					.equals(HttpStatus.OK)) {
					return clientResponse.bodyToMono(byte[].class);
				} else {
					throw new ResponseStatusException(clientResponse.statusCode());
				}
			}).retryWhen(Retry.max(5));
	}

	private static boolean allParamsPresentForWMS(MultiValueMap<String, String> params) {
		List<String> keys = Arrays.asList("width", "height", "bbox", "crs");

		return keys.stream().allMatch(params::containsKey);
	}

	private static boolean allParamsPresentForXYZ(Map<String, Integer> params) {
		List<String> keys = Arrays.asList("x", "y", "z");

		return keys.stream().allMatch(params::containsKey);
	}

	private WebClient getWebClient(HintergrundKarte hintergrundKarte) {
		return webClientCache.computeIfAbsent(hintergrundKarte, this::createWebClient);
	}

	private WebClient createWebClient(HintergrundKarte hintergrundKarte) {
		// Das explizite Setzen des clientConnectore ist ein Workaround, um "Connection prematurely closed" zu
		// verhindern.
		// siehe https://github.com/spring-projects/spring-framework/issues/22464
		return webClientBuilder
			.baseUrl(hintergrundKarte.getDomain())
			.clientConnector(connector)
			.build();
	}

	private void initConnector() {
		HttpClient httpClient = HttpClient.create().compress(true);

		if (commonConfigurationProperties.getProxyAdress() != null) {
			String proxyName = commonConfigurationProperties.getProxyAdress();
			int proxyPort = commonConfigurationProperties.getProxyPort();
			httpClient = httpClient.proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
				.host(proxyName)
				.port(proxyPort));
		}

		connector = new ReactorClientHttpConnector(
			httpClient);
	}
}
