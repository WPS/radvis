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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteVariante;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class SaveFahrradrouteCommandConverter {

	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final KanteResolver kanteResolver;

	public SaveFahrradrouteCommandConverter(
		VerwaltungseinheitResolver verwaltungseinheitResolver,
		KanteResolver kanteResolver) {
		require(verwaltungseinheitResolver, notNullValue());
		require(kanteResolver, notNullValue());
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
		this.kanteResolver = kanteResolver;
	}

	public void apply(SaveFahrradrouteCommand command, Fahrradroute fahrradroute) {
		List<FahrradrouteVariante> varianten = command.getVarianten().stream().map(varianteSaveCommand -> {
			List<AbschnittsweiserKantenBezug> netzbezug = new ArrayList<>();
			if (varianteSaveCommand.getKantenIDs().size() > 0) {
				List<Kante> allById = kanteResolver.getKanten(new HashSet<>(varianteSaveCommand.getKantenIDs()));
				netzbezug = varianteSaveCommand.getKantenIDs().stream()
					.map(kanteId -> allById.stream()
						.filter(kante -> kante.getId().equals(kanteId)).findFirst().orElseThrow())
					.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0.0, 1.0)))
					.collect(Collectors.toList());
			}
			return FahrradrouteVariante.builder()
				.id(varianteSaveCommand.getId())
				.kategorie(varianteSaveCommand.getKategorie())
				.stuetzpunkte(varianteSaveCommand.getStuetzpunkte())
				.abschnittsweiserKantenBezug(netzbezug)
				.geometrie(Optional.ofNullable(varianteSaveCommand.getGeometrie()).map(
					geom -> GeoJsonConverter.create3DJtsLineStringFromGeoJson(geom,
						KoordinatenReferenzSystem.ETRS89_UTM32_N)).orElse(null))
				.linearReferenzierteProfilEigenschaften(
					LinearReferenzierteProfilEigenschaftenCommandConverter.convert(
						varianteSaveCommand.getProfilEigenschaften()))
				.customProfileId(varianteSaveCommand.getCustomProfileId())
				.build();
		}).collect(Collectors.toList());

		List<Long> kantenIDs = command.getKantenIDs();
		List<AbschnittsweiserKantenBezug> netzbezug = new ArrayList<>();
		if (!kantenIDs.isEmpty()) {
			List<Kante> allById = kanteResolver.getKanten(new HashSet<>(kantenIDs));
			netzbezug = kantenIDs.stream()
				.map(kanteId -> allById.stream()
					.filter(kante -> kante.getId().equals(kanteId)).findFirst().orElseThrow())
				.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0.0, 1.0)))
				.toList();
		}

		Long verantwortlichId = command.getVerantwortlichId();
		Verwaltungseinheit verantwortlich = verantwortlichId != null
			? verwaltungseinheitResolver.resolve(verantwortlichId)
			: null;

		LineString netzbezugLineString = null;
		if (command.getRoutenVerlauf() != null) {
			netzbezugLineString = GeoJsonConverter.create3DJtsLineStringFromGeoJson(
				command.getRoutenVerlauf(), KoordinatenReferenzSystem.ETRS89_UTM32_N);
		}

		fahrradroute.updateAttribute(
			command.getToubizId(),
			command.getName(),
			command.getKurzbeschreibung(),
			command.getBeschreibung(),
			command.getKategorie(),
			command.getTourenkategorie(),
			command.getOffizielleLaenge(),
			command.getHomepage(),
			verantwortlich,
			command.getEmailAnsprechpartner(),
			command.getLizenz(),
			command.getLizenzNamensnennung(),
			varianten,
			netzbezug,
			(LineString) command.getStuetzpunkte(),
			netzbezugLineString,
			LinearReferenzierteProfilEigenschaftenCommandConverter.convert(command.getProfilEigenschaften()),
			command.getCustomProfileId()
		);
	}
}
