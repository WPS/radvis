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

import static org.valid4j.Assertive.require;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@ConfigurationProperties("radvis.anpassungswuensche")
@Slf4j
public class AnpassungswuenscheConfigurationProperties {

	@Getter
	private final double distanzZuFahrradrouteInMetern;

	@Getter
	private final Map<AnpassungswunschKategorie, String> emailProKategorie;

	public AnpassungswuenscheConfigurationProperties(
		double distanzZuFahrradrouteInMetern,
		Map<AnpassungswunschKategorie, String> emailProKategorie
	) {
		require(distanzZuFahrradrouteInMetern > 0, "distanzZuFahrradrouteInMetern > 0");
		this.distanzZuFahrradrouteInMetern = distanzZuFahrradrouteInMetern;
		this.emailProKategorie = emailProKategorie == null ? new HashMap<>() : emailProKategorie.entrySet().stream()
			// leere Strings rausfiltern
			.filter(e -> !e.getValue().isBlank())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		String kategorienOhneEmail = Arrays.stream(AnpassungswunschKategorie.values())
			.filter(anpassungswunschKategorie -> !this.emailProKategorie.containsKey(anpassungswunschKategorie))
			.map(Enum::name)
			.collect(Collectors.joining(", "));

		log.info("Für folgende Anpassungswunschkategoerien ist keine Email hinterlegt: {}", kategorienOhneEmail);

	}
}
