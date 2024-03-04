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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.BetreiberEigeneId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleBezeichnung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleGebietskoerperschaft;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Seriennummer;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlintervall;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zeitstempel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Fahrradzaehlstelle extends VersionierteEntity {

	private Point geometrie;
	private BetreiberEigeneId betreiberEigeneId;

	private FahrradzaehlstelleGebietskoerperschaft fahrradzaehlstelleGebietskoerperschaft;
	private FahrradzaehlstelleBezeichnung fahrradzaehlstelleBezeichnung;
	private Seriennummer seriennummer;
	private Zaehlintervall zaehlintervall;
	private Zeitstempel neusterZeitstempel;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "fahrradzaehlstelle_id")
	private List<Channel> channels;

	@Builder()
	private Fahrradzaehlstelle(
		Long id,
		Long version,
		Point geometrie,
		FahrradzaehlstelleGebietskoerperschaft fahrradzaehlstelleGebietskoerperschaft,
		FahrradzaehlstelleBezeichnung fahrradzaehlstelleBezeichnung,
		BetreiberEigeneId betreiberEigeneId,
		Seriennummer seriennummer,
		Zaehlintervall zaehlintervall,
		Zeitstempel neusterZeitstempel,
		List<Channel> channels) {
		super(id, version);

		require(geometrie, notNullValue());
		require(betreiberEigeneId, notNullValue());
		require(neusterZeitstempel, notNullValue());
		require(channels, notNullValue());
		this.geometrie = geometrie;
		this.betreiberEigeneId = betreiberEigeneId;
		this.channels = channels;

		this.fahrradzaehlstelleGebietskoerperschaft = fahrradzaehlstelleGebietskoerperschaft;
		this.fahrradzaehlstelleBezeichnung = fahrradzaehlstelleBezeichnung;
		this.seriennummer = seriennummer;
		this.zaehlintervall = zaehlintervall;
		this.neusterZeitstempel = neusterZeitstempel;
	}

	public void merge(Fahrradzaehlstelle otherFahrradzaehlstelle) {
		require(this.betreiberEigeneId.equals(otherFahrradzaehlstelle.getBetreiberEigeneId()));
		this.geometrie = otherFahrradzaehlstelle.getGeometrie();

		this.fahrradzaehlstelleGebietskoerperschaft = otherFahrradzaehlstelle.getFahrradzaehlstelleGebietskoerperschaft()
			.orElse(null);
		this.fahrradzaehlstelleBezeichnung = otherFahrradzaehlstelle.getFahrradzaehlstelleBezeichnung().orElse(null);
		this.seriennummer = otherFahrradzaehlstelle.getSeriennummer().orElse(null);
		this.zaehlintervall = otherFahrradzaehlstelle.getZaehlintervall().orElse(null);
		this.neusterZeitstempel = otherFahrradzaehlstelle.getNeusterZeitstempel();

		Map<ChannelId, Channel> otherChannelMap = otherFahrradzaehlstelle.getChannels().stream()
			.collect(Collectors.toMap(Channel::getChannelId, Function.identity()));

		this.getChannels().stream()
			.filter(channel -> otherChannelMap.containsKey(channel.getChannelId()))
			.forEach(channel -> {
				channel.merge(otherChannelMap.get(channel.getChannelId()));
				otherChannelMap.remove(channel.getChannelId());
			});

		this.channels.addAll(otherChannelMap.values());
	}

	public Point getGeometrie() {
		return this.geometrie;
	}

	public BetreiberEigeneId getBetreiberEigeneId() {
		return this.betreiberEigeneId;
	}

	public Zeitstempel getNeusterZeitstempel() {
		return this.neusterZeitstempel;
	}

	public List<Channel> getChannels() {
		return this.channels;
	}

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

}

