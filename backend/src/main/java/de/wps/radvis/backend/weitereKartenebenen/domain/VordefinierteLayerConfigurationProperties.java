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

package de.wps.radvis.backend.weitereKartenebenen.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;

import org.springframework.boot.context.properties.bind.ConstructorBinding;

import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Deckkraft;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.HexColor;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Quellangabe;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.VordefinierteLayerQuelle;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.WeitereKartenebeneTyp;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Zoomstufe;
import lombok.Getter;

@Getter
public class VordefinierteLayerConfigurationProperties {
	private String name;
	private VordefinierteLayerQuelle quelle;
	private String url;
	private WeitereKartenebeneTyp typ;
	private String farbe;
	private Double deckkraft;
	private Double zoomstufe;
	private String quellangabe;
	private List<String> path;

	@ConstructorBinding
	public VordefinierteLayerConfigurationProperties(String name, VordefinierteLayerQuelle quelle, String url,
		WeitereKartenebeneTyp typ, String farbe, Double deckkraft, Double zoomstufe, String quellangabe,
		List<String> path) {
		require(name, notNullValue());
		require(quelle, notNullValue());
		require(url, notNullValue());
		require(typ, notNullValue());
		require(deckkraft, notNullValue());
		require(zoomstufe, notNullValue());
		require(quellangabe, notNullValue());

		require(Deckkraft.isValid(deckkraft));
		require(Zoomstufe.isValid(zoomstufe));
		require(Quellangabe.isValid(quellangabe));

		if (farbe != null) {
			require(HexColor.isValid(farbe));
		}

		if (quelle == VordefinierteLayerQuelle.RADVIS) {
			require(url.startsWith("/"));
			require(quellangabe.startsWith("/"));
		}

		this.path = path;
		this.name = name;
		this.quelle = quelle;
		this.url = url;
		this.typ = typ;
		this.farbe = farbe;
		this.deckkraft = deckkraft;
		this.zoomstufe = zoomstufe;
		this.quellangabe = quellangabe;
	}

}
