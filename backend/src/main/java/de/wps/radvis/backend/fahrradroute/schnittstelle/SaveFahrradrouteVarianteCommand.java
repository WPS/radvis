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

import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.fahrradroute.domain.valueObject.VarianteKategorie;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@ToString
@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveFahrradrouteVarianteCommand {

	private Long id;

	@NotNull
	private VarianteKategorie kategorie;

	@NotNull
	@NotEmpty
	private List<Long> kantenIDs;

	@NotNull
	private Geometry stuetzpunkte;

	@NotNull
	private org.geojson.LineString geometrie;

	@NotNull
	private List<LinearReferenzierteProfilEigenschaftenCommand> profilEigenschaften;

	Long customProfileId;
}
