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

package de.wps.radvis.backend.abstellanlage.domain.valueObject;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public enum Groessenklasse {

	BASISANGEBOT_XXS("B+R Basisangebot (XXS)"),
	BASISANGEBOT_XS("B+R Basisangebot (XS)"),
	BASISANGEBOT_S("B+R Basisangebot (S)"),
	STANDARDANGEBOT_M("B+R Standardangebot (M)"),
	SCHWERPUNKT_L("B+R Schwerpunkt (L)"),
	HOTSPOT_XL("B+R Hotspot (XL)"),
	GROSSANLAGE_XXL("B+R Großanlage (XXL)");

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public static Groessenklasse fromString(String displayText) {
		switch (displayText) {
		case "B+R Basisangebot (XXS)":
			return Groessenklasse.BASISANGEBOT_XXS;
		case "B+R Basisangebot (XS)":
			return Groessenklasse.BASISANGEBOT_XS;
		case "B+R Basisangebot (S)":
			return Groessenklasse.BASISANGEBOT_S;
		case "B+R Standardangebot (M)":
			return Groessenklasse.STANDARDANGEBOT_M;
		case "B+R Schwerpunkt (L)":
			return Groessenklasse.SCHWERPUNKT_L;
		case "B+R Hotspot (XL)":
			return Groessenklasse.HOTSPOT_XL;
		case "B+R Großanlage (XXL)":
			return Groessenklasse.GROSSANLAGE_XXL;
		}
		throw new RuntimeException("Größenklasse " + displayText + " kann nicht gelesen werden");
	}
}
