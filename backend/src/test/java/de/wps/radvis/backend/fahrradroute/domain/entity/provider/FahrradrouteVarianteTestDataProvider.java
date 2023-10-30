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

package de.wps.radvis.backend.fahrradroute.domain.entity.provider;

import java.util.List;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteVariante;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.VarianteKategorie;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;

public class FahrradrouteVarianteTestDataProvider {
	public static FahrradrouteVariante.FahrradrouteVarianteTfisBuilder defaultTfis() {
		return FahrradrouteVariante.tfisVarianteBuilder()
			.abschnittsweiserKantenBezug(List.of(new AbschnittsweiserKantenBezug(
				KanteTestDataProvider.withDefaultValues().build(), LinearReferenzierterAbschnitt.of(0, 1))))
			.linearReferenzierteProfilEigenschaften(List.of())
			.kategorie(VarianteKategorie.ALTERNATIVSTRECKE)
			.geometrie(GeometryTestdataProvider.createLineString())
			.tfisId(TfisId.of("VarianteID"));
	}
}
