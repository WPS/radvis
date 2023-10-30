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

package de.wps.radvis.backend.fahrradzaehlstelle.schnittstelle.view;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.Fahrradzaehlstelle;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.BetreiberEigeneId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleBezeichnung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleGebietskoerperschaft;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Seriennummer;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlintervall;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zeitstempel;
import lombok.Getter;

@Getter
public class FahrradzaehlstelleListView {

	private final Long id;
	private final Point geometrie;
	private final BetreiberEigeneId betreiberEigeneId;
	private final FahrradzaehlstelleGebietskoerperschaft fahrradzaehlstelleGebietskoerperschaft;
	private final FahrradzaehlstelleBezeichnung fahrradzaehlstelleBezeichnung;
	private final Seriennummer seriennummer;
	private final Zaehlintervall zaehlintervall;
	private final Zeitstempel neusterZeitstempel;
	private final Long neusterZeitstempelEpochSecs;

	public FahrradzaehlstelleListView(Fahrradzaehlstelle fahrradzaehlstelle) {
		this.id = fahrradzaehlstelle.getId();
		this.geometrie = fahrradzaehlstelle.getGeometrie();
		this.betreiberEigeneId = fahrradzaehlstelle.getBetreiberEigeneId();
		this.fahrradzaehlstelleGebietskoerperschaft = fahrradzaehlstelle.getFahrradzaehlstelleGebietskoerperschaft()
			.orElse(null);
		this.fahrradzaehlstelleBezeichnung = fahrradzaehlstelle.getFahrradzaehlstelleBezeichnung().orElse(null);
		this.seriennummer = fahrradzaehlstelle.getSeriennummer().orElse(null);
		this.zaehlintervall = fahrradzaehlstelle.getZaehlintervall().orElse(null);
		this.neusterZeitstempel = fahrradzaehlstelle.getNeusterZeitstempel();
		this.neusterZeitstempelEpochSecs = fahrradzaehlstelle.getNeusterZeitstempel().getValue();
	}
}
