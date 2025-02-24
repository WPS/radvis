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

package de.wps.radvis.backend.weitereKartenebenen.schnittstelle;

import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Deckkraft;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.HexColor;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Name;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Quellangabe;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.WeitereKartenebeneTyp;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Zindex;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Zoomstufe;
import de.wps.radvis.backend.weitereKartenebenen.schnittstelle.SaveWeitereKartenebeneCommand.SaveWeitereKartenebeneCommandBuilder;

public class SaveWeitereKartenebenenCommandTestDataProvider {
	public static SaveWeitereKartenebeneCommandBuilder defaultValue() {
		return SaveWeitereKartenebeneCommand.builder()
			.name(Name.of("Dienst A"))
			.url("localhost")
			.weitereKartenebeneTyp(WeitereKartenebeneTyp.WFS)
			.deckkraft(Deckkraft.of(1.0))
			.zoomstufe(Zoomstufe.of(8.7))
			.zindex(Zindex.of(1011))
			.farbe(HexColor.of("#000000"))
			.quellangabe(Quellangabe.of("Quellangabe"));
	}
}
