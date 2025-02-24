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

package de.wps.radvis.backend.reverseproxy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Sendet der Client content des Typs x-www-form-urlencoded, wird das von Spring als Parameter ausgelesen, wodurch die
 * Werte aus dem Body verschwinden und nach der Weiterleitung nicht mehr gelesen werden können. Dieser Filter stellt den
 * Body wieder her.
 */
public class WriteFormFieldsToBodyFilter implements Filter {

	private boolean hasFormParameter(HttpServletRequest servletRequest) {
		return !Objects.isNull(servletRequest.getContentType())
			&& servletRequest.getContentType().contains("application/x-www-form-urlencoded");
	}

	/**
	 * Erstellt einen x-www-form-urlencoded Body aus den request Parametern.
	 */
	private @NotNull HttpServletRequestWrapper restoreBodyFromFormParameters(ServletRequest request,
		HttpServletRequest servletRequest) {
		String currentQueryString = Optional.ofNullable(servletRequest.getQueryString()).orElse("");

		String newBody = request.getParameterMap()
			.entrySet()
			.stream()
			.filter(entry -> {
				return !currentQueryString.contains(entry.getKey() + "=");
			})
			.map(entry -> {
				return entry.getKey() + "=" + Arrays.stream(entry.getValue())
					.map(value -> URLEncoder.encode(value, Charset.defaultCharset()))
					.collect(Collectors.joining(";"));
			})
			.collect(Collectors.joining("&"));
		HttpServletRequestWrapper newRequest = new BodyReplacingRequestWrapper(servletRequest, newBody);
		return newRequest;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
		HttpServletRequest servletRequest = (HttpServletRequest) request;
		if (hasFormParameter(servletRequest)) {
			HttpServletRequestWrapper newRequest = restoreBodyFromFormParameters(request, servletRequest);
			chain.doFilter(newRequest, response);
		} else {
			chain.doFilter(request, response);
		}

	}

	class BodyReplacingRequestWrapper extends HttpServletRequestWrapper {
		private final ByteArrayInputStream inputStream;

		public BodyReplacingRequestWrapper(HttpServletRequest servletRequest, String body) {
			super(servletRequest);
			this.inputStream = new ByteArrayInputStream(body.getBytes());
		}

		@Override
		public ServletInputStream getInputStream() {
			return new ServletInputStream() {

				@Override
				public int read() {
					return inputStream.read();
				}

				@Override
				public boolean isFinished() {
					return false;
				}

				@Override
				public boolean isReady() {
					return false;
				}

				@Override
				public void setReadListener(ReadListener readListener) {

				}
			};
		}

		@Override
		public BufferedReader getReader() {
			return new BufferedReader(new InputStreamReader(this.getInputStream()));
		}
	}

}
