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

package de.wps.radvis.backend.netz.domain.valueObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@AllArgsConstructor(staticName = "of")
@Getter
public class SeitenbezogeneProfilEigenschaften {

	private BelagArt belagArtLinks;

	private BelagArt belagArtRechts;

	private Radverkehrsfuehrung radverkehrsfuehrungLinks;

	private Radverkehrsfuehrung radverkehrsfuehrungRechts;

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static SeitenbezogeneProfilEigenschaften deserialize(Object value) {
		String[] values = value.toString().split(";");
		return SeitenbezogeneProfilEigenschaften.of(BelagArt.valueOf(values[0]), BelagArt.valueOf(values[1]),
			Radverkehrsfuehrung.valueOf(values[2]), Radverkehrsfuehrung.valueOf(values[3]));
	}

	@JsonValue
	public String serialize() {
		return String.format("%s;%s;%s;%s", belagArtLinks.name(), belagArtRechts.name(),
			radverkehrsfuehrungLinks.name(), radverkehrsfuehrungRechts.name());
	}
}
