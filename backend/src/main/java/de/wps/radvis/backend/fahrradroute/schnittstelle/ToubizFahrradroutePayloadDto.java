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

package de.wps.radvis.backend.fahrradroute.schnittstelle;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ToubizFahrradroutePayloadDto {

	@Setter
	@Getter
	private String id;

	@Setter
	@Getter
	private String name;

	@Setter
	@Getter
	private String description;

	@JsonProperty("abstract")
	@Setter
	@Getter
	private String _abstract;

	@Getter
	@Setter
	private ToubizFahrradroutePayloadTourDto tour;

	@Setter
	@Getter
	private PrimaryCategoryDTO primaryCategory;

	@Setter
	@Getter
	private String sourceInformationLink;

	@Setter
	@Getter
	private String author;

	@Setter
	@Getter
	private String license;

	@Setter
	@Getter
	private LocalDateTime updatedAt;

	@Getter
	@Setter
	private WebMediaLinksDto webMediaLinks;

	@Getter
	@Setter
	private String copyright;

	public List<List<Float>> getPoints() {
		if (tour != null) {
			List<List<Float>> points = tour.getPoints();
			if (!points.isEmpty()) {
				return points;
			}
		}

		return new ArrayList<>();
	}
}
