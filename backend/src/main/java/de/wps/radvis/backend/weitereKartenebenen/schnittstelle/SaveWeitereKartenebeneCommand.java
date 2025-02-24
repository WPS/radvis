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

import static de.wps.radvis.backend.common.domain.Validators.isValidURL;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.weitereKartenebenen.domain.entity.WeitereKartenebene;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Deckkraft;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.HexColor;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Name;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Quellangabe;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.WeitereKartenebeneTyp;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Zindex;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Zoomstufe;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@ToString
@Validated
@AllArgsConstructor
@Builder
public class SaveWeitereKartenebeneCommand {
	// Neue Layer haben noch keine ID
	@Nullable
	private Long id;

	@NotNull
	private Name name;

	@NotNull
	private String url;

	@NotNull
	private WeitereKartenebeneTyp weitereKartenebeneTyp;

	@NotNull
	private Deckkraft deckkraft;

	@NotNull
	private Zoomstufe zoomstufe;

	@NotNull
	private Zindex zindex; // muss durchgehend kleingeschrieben werden. zIndex wird nicht korrekt vom BE ans FE
						  // uebergeben.

	@Nullable
	private HexColor farbe;

	@NotNull
	private Quellangabe quellangabe;

	@Nullable
	private Long dateiLayerId;

	private boolean defaultLayer;

	@AssertTrue(message = "Es muss eine gültige URL sein.")
	public boolean isUrlValid() {
		return isValidURL(url);
	}

	@AssertTrue(message = "WMS-Layer dürfen keine Farbe haben und WFS-Layer müssen eine Farbe haben")
	public boolean isFarbeValid() {
		return WeitereKartenebene.isFarbeValid(weitereKartenebeneTyp, farbe);
	}
}
