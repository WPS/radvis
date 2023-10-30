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

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
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
public class LinearReferenzierteProfilEigenschaftenCommand {

	@NotNull
	private BelagArt belagArt;

	@NotNull
	private Radverkehrsfuehrung radverkehrsfuehrung;

	@NotNull
	@DecimalMin(value = "0", message = "Von-Wert muss >= 0 sein")
	@DecimalMax(value = "1", inclusive = false, message = "Von-Wert muss < 1 sein")
	private double vonLR;

	@NotNull
	@DecimalMin(value = "0", inclusive = false, message = "Bis-Wert muss > 0 sein")
	@DecimalMax(value = "1", message = "Bis-Wert muss <= 1 sein")
	private double bisLR;

	@AssertTrue(message = "Von-Wert muss < Bis-Wert sein")
	public boolean isVonUndBisValid() {
		return vonLR < bisLR;
	}

}
