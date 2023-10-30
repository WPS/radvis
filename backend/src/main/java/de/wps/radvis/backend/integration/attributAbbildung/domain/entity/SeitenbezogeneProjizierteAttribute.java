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

package de.wps.radvis.backend.integration.attributAbbildung.domain.entity;

import java.util.List;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import lombok.Getter;
import lombok.NonNull;

public class SeitenbezogeneProjizierteAttribute {
	@Getter
	@NonNull
	private LinearReferenzierterAbschnitt linearReferenzierterAbschnittAufZielnetzkante;

	@NonNull
	@Getter
	private Haendigkeit haendigkeit;

	@NonNull
	@Getter
	private final Richtung richtung;

	@Getter
	@NonNull
	private final List<FuehrungsformAttribute> fuehrungsformAttribute;

	SeitenbezogeneProjizierteAttribute(LinearReferenzierterAbschnitt linearReferenzierterAbschnittAufZielnetzkante, Haendigkeit haendigkeit,
		Richtung richtung, List<FuehrungsformAttribute> fuehrungsformAttribute) {
		this.linearReferenzierterAbschnittAufZielnetzkante = linearReferenzierterAbschnittAufZielnetzkante;
		this.haendigkeit = haendigkeit;
		this.richtung = richtung;
		this.fuehrungsformAttribute = fuehrungsformAttribute;
	}
}