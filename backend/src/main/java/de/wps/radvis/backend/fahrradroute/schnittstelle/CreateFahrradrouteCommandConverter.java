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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;

public class CreateFahrradrouteCommandConverter {

	private final BenutzerResolver benutzerResolver;
	private final KanteResolver kanteResolver;

	public CreateFahrradrouteCommandConverter(BenutzerResolver benutzerResolver, KanteResolver kanteResolver) {
		require(benutzerResolver, notNullValue());
		require(kanteResolver, notNullValue());
		this.benutzerResolver = benutzerResolver;
		this.kanteResolver = kanteResolver;
	}

	public Fahrradroute convert(Authentication authentication, CreateFahrradrouteCommand command) {
		require(authentication, notNullValue());
		Benutzer benutzer = this.benutzerResolver.fromAuthentication(authentication);

		List<AbschnittsweiserKantenBezug> netzbezug = new ArrayList<>();
		if (command.getKantenIDs().size() > 0) {
			netzbezug = command.getKantenIDs().stream()
				.map(kanteResolver::getKante)
				.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0.0, 1.0)))
				.collect(Collectors.toList());
		}

		return new Fahrradroute(
			FahrradrouteName.of(command.getName()),
			command.getBeschreibung(),
			command.getKategorie(),
			benutzer.getOrganisation(),
			netzbezug,
			GeoJsonConverter.create3DJtsLineStringFromGeoJson(command.getRoutenVerlauf(),
				KoordinatenReferenzSystem.ETRS89_UTM32_N),
			(LineString) command.getStuetzpunkte(),
			LinearReferenzierteProfilEigenschaftenCommandConverter.convert(command.getProfilEigenschaften()),
			command.getCustomProfileId()
		);
	}
}
