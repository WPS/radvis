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

package de.wps.radvis.backend.fahrradzaehlstelle.domain.entity;

import java.util.Optional;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.BetreiberEigeneId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelBezeichnung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleBezeichnung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleGebietskoerperschaft;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Seriennummer;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlintervall;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlstand;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlstatus;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zeitstempel;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MessDatenEintrag {
	private BetreiberEigeneId betreiberEigeneId;
	private Point geometrie;
	private ChannelId channelId;
	private Zaehlstand zaehlstand;

	private FahrradzaehlstelleGebietskoerperschaft fahrradzaehlstelleGebietskoerperschaft;
	private FahrradzaehlstelleBezeichnung fahrradzaehlstelleBezeichnung;
	private Seriennummer seriennummer;
	private Zaehlintervall zaehlintervall;
	private ChannelBezeichnung channelBezeichnung;
	private Zeitstempel zeitstempel;
	private Zaehlstatus zaehlstatus;

	public Optional<FahrradzaehlstelleGebietskoerperschaft> getFahrradzaehlstelleGebietskoerperschaft() {
		return Optional.ofNullable(this.fahrradzaehlstelleGebietskoerperschaft);
	}

	public Optional<FahrradzaehlstelleBezeichnung> getFahrradzaehlstelleBezeichnung() {
		return Optional.ofNullable(this.fahrradzaehlstelleBezeichnung);
	}

	public Optional<Seriennummer> getSeriennummer() {
		return Optional.ofNullable(this.seriennummer);
	}

	public Optional<Zaehlintervall> getZaehlintervall() {
		return Optional.ofNullable(this.zaehlintervall);
	}

	public Optional<ChannelBezeichnung> getChannelBezeichnung() {
		return Optional.ofNullable(this.channelBezeichnung);
	}

	public Optional<Zaehlstatus> getZaehlstatus() {
		return Optional.ofNullable(this.zaehlstatus);
	}
}
