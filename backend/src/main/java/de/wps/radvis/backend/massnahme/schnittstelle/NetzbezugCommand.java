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

import de.wps.radvis.backend.netz.schnittstelle.command.KnotenNetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.PunktuellerKantenSeitenBezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SeitenabschnittsKantenBezugCommand;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(access = AccessLevel.PACKAGE)
public class NetzbezugCommand {

	private List<SeitenabschnittsKantenBezugCommand> kantenBezug;
	private List<PunktuellerKantenSeitenBezugCommand> punktuellerKantenBezug;
	private List<KnotenNetzbezugCommand> knotenBezug;
}
