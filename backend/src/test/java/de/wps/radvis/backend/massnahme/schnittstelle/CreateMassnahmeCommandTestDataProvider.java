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

package de.wps.radvis.backend.massnahme.schnittstelle;

import java.util.Set;

import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.netz.schnittstelle.command.NetzbezugCommandTestDataProvider;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class CreateMassnahmeCommandTestDataProvider {
	public static CreateMassnahmeCommand.CreateMassnahmeCommandBuilder defaultValue() {
		return CreateMassnahmeCommand.builder()
			.bezeichnung(Bezeichnung.of("Massnahme"))
			.massnahmenkategorien(Set.of(Massnahmenkategorie.BORDABSENKUNGEN_HERSTELLEN_AUSSERORTS))
			.netzbezug(NetzbezugCommandTestDataProvider.defaultNetzbezug().build())
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.veroeffentlicht(true)
			.planungErforderlich(false)
			.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2021))
			.baulastZustaendigerId(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build().getId())
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.zustaendigerId(12l).sollStandard(SollStandard.BASISSTANDARD)
			.sonstigeKonzeptionsquelle("WAMBO");
	}

	public static CreateMassnahmeCommand.CreateMassnahmeCommandBuilder withKante(Long id) {
		return defaultValue().netzbezug(NetzbezugCommandTestDataProvider.withKante(id).build());
	}

}
