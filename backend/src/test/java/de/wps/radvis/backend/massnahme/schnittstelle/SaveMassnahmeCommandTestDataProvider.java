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

import java.util.List;
import java.util.Set;

import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.LGVFGID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MaViSID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.VerbaID;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.netz.schnittstelle.command.NetzbezugCommandTestDataProvider;

public class SaveMassnahmeCommandTestDataProvider {
	public static SaveMassnahmeCommand.SaveMassnahmeCommandBuilder defaultValue() {
		return SaveMassnahmeCommand.builder()
			.bezeichnung(Bezeichnung.of("Bezeichnung Neu"))
			.massnahmenkategorien(Set.of(Massnahmenkategorie.FURTEN_ERNEUERN))
			.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2022))
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.veroeffentlicht(true)
			.planungErforderlich(true)
			.netzbezug(NetzbezugCommandTestDataProvider.defaultNetzbezug().build())
			.maViSID(MaViSID.of("maViSID"))
			.verbaID(VerbaID.of("verbaID"))
			.lgvfgid(LGVFGID.of("lgvfgid"))
			.massnahmeKonzeptID(MassnahmeKonzeptID.of("ACB123"))
			.prioritaet(Prioritaet.of(1))
			.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG))
			.baulastZustaendigerId(500L)
			.unterhaltsZustaendigerId(2L)
			.zustaendigerId(1L)
			.sollStandard(SollStandard.BASISSTANDARD)
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.version(3l)
			.id(345l)
			.sonstigeKonzeptionsquelle("WAMBO");
	}

	public static SaveMassnahmeCommand.SaveMassnahmeCommandBuilder withKante(Long id) {
		return defaultValue().netzbezug(NetzbezugCommandTestDataProvider.withKante(id).build());
	}

	public static SaveMassnahmeCommand.SaveMassnahmeCommandBuilder withNetzbezuege(List<Long> kanteIds,
		List<Long> knotenIds, List<Long> punktuelleNetzbezugIds) {
		return defaultValue().netzbezug(
			NetzbezugCommandTestDataProvider.withNetzbezuege(kanteIds, knotenIds, punktuelleNetzbezugIds).build());
	}
}
