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

package de.wps.radvis.backend.massnahme.domain.entity.provider;

import java.util.stream.Collectors;

import de.wps.radvis.backend.massnahme.domain.dbView.MassnahmeListenDbView;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class MassnahmeListenDbViewTestDataProvider {
	public static MassnahmeListenDbView.MassnahmeListenDbViewBuilder withMassnahme(Massnahme massnahme) {
		return MassnahmeListenDbView.builder()
			.id(massnahme.getId())
			.netzklassen(String.join(";",
				massnahme.getNetzklassen().stream().map(n -> n.toString()).collect(Collectors.toList())))
			.letzteAenderung(massnahme.getLetzteAenderung()).planungErforderlich(massnahme.getPlanungErforderlich())
			.unterhaltName(massnahme.getunterhaltsZustaendiger().map(Verwaltungseinheit::getName).orElse(""))
			.unterhaltOrganisationsArt(
				massnahme.getunterhaltsZustaendiger()
					.map(Verwaltungseinheit::getOrganisationsArt)
					.orElse(null))
			.geometry(massnahme.getNetzbezug().getGeometrie()).bezeichnung(massnahme.getBezeichnung())
			.umsetzungsstandStatus(
				massnahme.getUmsetzungsstand().map(ust -> ust.getUmsetzungsstandStatus()).orElse(null));
	}
}
