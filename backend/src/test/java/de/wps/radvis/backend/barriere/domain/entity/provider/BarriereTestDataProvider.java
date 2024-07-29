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

package de.wps.radvis.backend.barriere.domain.entity.provider;

import java.util.Collections;
import java.util.Set;

import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.entity.BarriereNetzBezug;
import de.wps.radvis.backend.barriere.domain.valueObject.BarrierenForm;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class BarriereTestDataProvider {

	public static Barriere.BarriereBuilder withDefaultValues() {
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Coole Organisation").organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();

		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		final BarriereNetzBezug netzbezug = new BarriereNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS)),
			Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
			Collections.emptySet());

		return Barriere.builder()
			.verantwortlicheOrganisation(organisation)
			.netzbezug(netzbezug)
			.barrierenForm(BarrierenForm.SONSTIGE_BARRIERE);
	}

}
