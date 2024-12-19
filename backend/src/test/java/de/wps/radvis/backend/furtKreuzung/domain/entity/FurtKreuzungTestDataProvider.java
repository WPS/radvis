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

package de.wps.radvis.backend.furtKreuzung.domain.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenKommentar;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenTyp;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class FurtKreuzungTestDataProvider {

	public static FurtKreuzung.FurtKreuzungBuilder withDefaultValues() {
		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		final FurtKreuzungNetzBezug netzbezug = new FurtKreuzungNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante, LinearReferenzierterAbschnitt.of(0, 1),
				Seitenbezug.LINKS)),
			Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
			Collections.emptySet());

		return FurtKreuzung.builder().netzbezug(netzbezug)
			.verantwortlicheOrganisation(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Eine verantwortliche Organisation")
				.organisationsArt(OrganisationsArt.KREIS)
				.build())
			.typ(
				FurtenKreuzungenTyp.KREUZUNG)
			.radnetzKonform(true).kommentar(new FurtenKreuzungenKommentar("Dies ist ein Kommentar"))
			.knotenForm(KnotenForm.MINIKREISVERKEHR_24_M).musterloesung(Optional.empty())
			.lichtsignalAnlageEigenschaften(Optional.empty());
	}

	public static FurtKreuzung.FurtKreuzungBuilder onKante(Kante... kanten) {
		return withDefaultValues()
			.netzbezug(new FurtKreuzungNetzBezug(
				Arrays.stream(kanten).map(k -> new AbschnittsweiserKantenSeitenBezug(k, LinearReferenzierterAbschnitt
					.of(0, 1), Seitenbezug.LINKS)).collect(Collectors.toSet()),
				Arrays.stream(kanten).map(k -> new PunktuellerKantenSeitenBezug(k, LineareReferenz.of(0.25),
					Seitenbezug.BEIDSEITIG)).collect(Collectors.toSet()),
				Collections.emptySet()
			));
	}
}
