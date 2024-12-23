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

package de.wps.radvis.backend.application.schnittstelle;

import java.security.Principal;
import java.util.List;

import org.springframework.web.servlet.HandlerInterceptor;

import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestLoggingInterceptor implements HandlerInterceptor {
	private final List<String> endpointPrefixesToIgnore = List.of(
		"/api/netzausschnitt",
		"/api/hintergrundkarte",
		"/api/ortssuche",
		"/api/fahrradroute/routing"
	);
	private final List<String> endpointSuffixesToIgnore = List.of(
		"/session"
	);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (endpointPrefixesToIgnore.stream().anyMatch(endpoint -> request.getRequestURI().startsWith(endpoint))
			|| endpointSuffixesToIgnore.stream().anyMatch(endpoint -> request.getRequestURI().endsWith(endpoint))) {
			return true;
		}

		Principal userPrincipal = request.getUserPrincipal();

		String userId = "UNKNOWN";
		if (userPrincipal instanceof RadVisAuthentication) {
			Benutzer benutzer = ((RadVisAuthentication) userPrincipal).getBenutzer();
			userId = benutzer != null ? benutzer.getId().toString() : "UNKNOWN";
		}

		log.debug("Request from user {}: {} {}", userId, request.getMethod(), request.getRequestURI());
		return true;
	}
}
