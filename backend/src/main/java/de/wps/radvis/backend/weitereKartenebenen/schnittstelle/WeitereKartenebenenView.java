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

import de.wps.radvis.backend.weitereKartenebenen.domain.entity.WeitereKartenebene;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Deckkraft;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.HexColor;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Name;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Quellangabe;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.WeitereKartenebeneTyp;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Zindex;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Zoomstufe;
import lombok.Getter;

@Getter
public class WeitereKartenebenenView {
	private final Long id;
	private final Name name;
	private final String url;
	private final WeitereKartenebeneTyp weitereKartenebeneTyp;
	private final Deckkraft deckkraft;
	private final Zoomstufe zoomstufe;
	private final Zindex zindex; // muss durchgehend kleingeschrieben werden. zIndex wird nicht korrekt vom BE ans FE uebergeben.
	private final HexColor farbe;
	private final Quellangabe quellangabe;

	public WeitereKartenebenenView(WeitereKartenebene weitereKartenebene) {
		this.id = weitereKartenebene.getId();
		this.name = weitereKartenebene.getName();
		this.url = weitereKartenebene.getUrl();
		this.farbe = weitereKartenebene.getFarbe();
		this.deckkraft = weitereKartenebene.getDeckkraft();
		this.zoomstufe = weitereKartenebene.getZoomstufe();
		this.zindex = weitereKartenebene.getZindex();
		this.weitereKartenebeneTyp = weitereKartenebene.getWeitereKartenebeneTyp();
		this.quellangabe = weitereKartenebene.getQuellangabe();
	}
}
