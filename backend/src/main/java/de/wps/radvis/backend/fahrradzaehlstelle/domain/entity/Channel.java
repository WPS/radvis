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

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderBy;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelBezeichnung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlstand;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zeitstempel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Channel extends VersionierteEntity {

	private ChannelId channelId;

	private ChannelBezeichnung channelBezeichnung;

	@ElementCollection(fetch = FetchType.LAZY)
	@MapKeyColumn(name = "zeitstempel")
	@OrderBy("zeitstempel")
	@CollectionTable(name = "channel_fahrradzaehl_daten_eintrag")
	private Map<Zeitstempel, FahrradzaehlDatenEintrag> fahrradzaehlDaten;

	@Builder
	private Channel(ChannelBezeichnung channelBezeichnung, ChannelId channelId,
		Map<Zeitstempel, FahrradzaehlDatenEintrag> fahrradzaehlDaten) {

		require(channelId, notNullValue());
		require(fahrradzaehlDaten, notNullValue());
		this.channelId = channelId;
		this.fahrradzaehlDaten = fahrradzaehlDaten;

		this.channelBezeichnung = channelBezeichnung;
	}

	public void merge(Channel otherChannel) {
		require(this.channelId.equals(otherChannel.getChannelId()));
		this.channelBezeichnung = otherChannel.getChannelBezeichnung().orElse(null);

		// Die neuen Zaehldaten werden zu der eigenen fahrradzaehlDaten-Map hinzugefuegt.
		// So wird sie um die noch nicht importierten Daten erweitert und die schon vorhandenen Key-Value-Paare
		// werden durch die neuen (gleichen) Werte ueberschrieben.
		this.fahrradzaehlDaten.putAll(otherChannel.getFahrradzaehlDaten());
	}

	public ChannelId getChannelId() {
		return this.channelId;
	}

	public Map<Zeitstempel, FahrradzaehlDatenEintrag> getFahrradzaehlDaten() {
		return this.fahrradzaehlDaten;
	}

	public Map<Zeitstempel, Zaehlstand> getZaehlstaendeInZeitraum(Instant startDate,
		Instant endDate) {
		return this.fahrradzaehlDaten.entrySet().stream()
			.filter(e -> {
				long epochSecond = e.getKey().toZonedDateTime().toEpochSecond();
				return epochSecond >= startDate.getEpochSecond() && epochSecond < endDate.plus(1, ChronoUnit.DAYS)
					.getEpochSecond();
			})
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getZaehlstand()));
	}

	public Optional<ChannelBezeichnung> getChannelBezeichnung() {
		return Optional.ofNullable(this.channelBezeichnung);
	}

}
