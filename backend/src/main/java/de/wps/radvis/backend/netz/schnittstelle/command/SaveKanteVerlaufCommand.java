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

package de.wps.radvis.backend.netz.schnittstelle.command;

import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class SaveKanteVerlaufCommand extends SaveKantenAttributGruppeCommand {
	@NotNull
	private Geometry geometry;
	private Geometry verlaufLinks;
	private Geometry verlaufRechts;

	@Builder
	private SaveKanteVerlaufCommand(@NotNull Long id, @NotNull Long kantenVersion, @NotNull Geometry geometry,
		Geometry verlaufLinks,
		Geometry verlaufRechts) {
		super(id, kantenVersion);
		this.geometry = geometry;
		this.verlaufLinks = verlaufLinks;
		this.verlaufRechts = verlaufRechts;
	}

	@AssertTrue(message = "Geometrien müssen vom Typ Linestring sein")
	public boolean isGeometryTypeValid() {
		boolean verlaufLinksValid = verlaufLinks == null
			|| verlaufLinks.getGeometryType().equals(Geometry.TYPENAME_LINESTRING);
		boolean verlaufRechtsValid = verlaufRechts == null
			|| verlaufRechts.getGeometryType().equals(Geometry.TYPENAME_LINESTRING);
		boolean geometryValid = geometry.getGeometryType().equals(Geometry.TYPENAME_LINESTRING);
		return verlaufLinksValid && verlaufRechtsValid && geometryValid;
	}

	@AssertTrue(message = "Geometrien müssen das Koordinatenreferenz-System (CRS) ETRS89 (UTM32_N) gesetzt haben")
	public boolean isGeometryCrsValid() {
		boolean verlaufLinksValid = verlaufLinks == null
			|| verlaufRechts.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid();
		boolean verlaufRechtsValid = verlaufRechts == null
			|| verlaufRechts.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid();
		boolean geometryValid = geometry.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid();
		return verlaufLinksValid && verlaufRechtsValid && geometryValid;
	}
}
