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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ToubizFahrradrouteLinksDto {
	private String nextPage;

	public Optional<String> getNextPageURLDecoded() {
		if (nextPage == null || nextPage.isEmpty()) {
			return Optional.empty();
		}

		try {
			return Optional.of(URLDecoder.decode(nextPage, StandardCharsets.UTF_8.name()));
		} catch (UnsupportedEncodingException e) {
			log.warn("Der Links zur naechsten Seite konnte nicht URLDecoded werden: " + nextPage);
			return Optional.empty();
		}
	}
}
