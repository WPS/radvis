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

package de.wps.radvis.backend.weitereKartenebenen.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Deckkraft;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.HexColor;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Name;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Quellangabe;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.WeitereKartenebeneTyp;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Zindex;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Zoomstufe;
import jakarta.annotation.Nullable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
@Builder
public class WeitereKartenebene extends AbstractEntity {

	private Name name;

	private String url;

	@Enumerated(EnumType.STRING)
	private WeitereKartenebeneTyp weitereKartenebeneTyp;

	private Deckkraft deckkraft;

	@Embedded
	private Zoomstufe zoomstufe;

	@Embedded
	private Zindex zindex;

	@Nullable
	private HexColor farbe;

	@ManyToOne
	private Benutzer benutzer;

	private Quellangabe quellangabe;

	private Long dateiLayerId;

	public WeitereKartenebene(
		Name name,
		String url,
		WeitereKartenebeneTyp weitereKartenebeneTyp,
		Deckkraft deckkraft,
		Zoomstufe zoomstufe,
		Zindex zindex,
		HexColor farbe,
		Benutzer benutzer,
		Quellangabe quellangabe,
		Long dateiLayerId) {
		require(name, notNullValue());
		require(url, notNullValue());
		require(!url.isEmpty(), "Url darf nicht leer sein.");
		require(weitereKartenebeneTyp, notNullValue());
		require(deckkraft, notNullValue());
		require(zoomstufe, notNullValue());
		require(zindex, notNullValue());
		require(benutzer, notNullValue());
		require(isFarbeValid(weitereKartenebeneTyp, farbe));
		require(quellangabe, notNullValue());
		require(quellangabe.getValue().length() <= 1000);

		this.name = name;
		this.url = url;
		this.weitereKartenebeneTyp = weitereKartenebeneTyp;
		this.deckkraft = deckkraft;
		this.zoomstufe = zoomstufe;
		this.zindex = zindex;
		this.farbe = farbe;
		this.quellangabe = quellangabe;
		this.benutzer = benutzer;
		this.dateiLayerId = dateiLayerId;
	}

	public void update(
		Name name,
		String url,
		WeitereKartenebeneTyp weitereKartenebeneTyp,
		Deckkraft deckkraft,
		Zoomstufe zoomstufe,
		Zindex zindex,
		HexColor farbe,
		Quellangabe quellangabe) {
		require(name, notNullValue());
		require(url, notNullValue());
		require(!url.isEmpty(), "Url darf nicht leer sein.");
		require(weitereKartenebeneTyp, notNullValue());
		require(deckkraft, notNullValue());
		require(zoomstufe, notNullValue());
		require(zindex, notNullValue());
		require(isFarbeValid(weitereKartenebeneTyp, farbe));
		require(quellangabe, notNullValue());
		require(quellangabe.getValue().length() <= 1000);

		this.name = name;
		this.url = url;
		this.weitereKartenebeneTyp = weitereKartenebeneTyp;
		this.deckkraft = deckkraft;
		this.zoomstufe = zoomstufe;
		this.zindex = zindex;
		this.farbe = farbe;
		this.quellangabe = quellangabe;
	}

	public static boolean isFarbeValid(WeitereKartenebeneTyp weitereKartenebeneTyp, HexColor farbe) {
		if (weitereKartenebeneTyp == WeitereKartenebeneTyp.WMS) {
			return farbe == null;
		}
		return farbe != null;
	}
}
