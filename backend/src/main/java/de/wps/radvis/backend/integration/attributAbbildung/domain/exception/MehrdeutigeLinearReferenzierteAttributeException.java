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

package de.wps.radvis.backend.integration.attributAbbildung.domain.exception;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import lombok.Getter;
import lombok.Setter;

public class MehrdeutigeLinearReferenzierteAttributeException extends MehrdeutigeProjektionException {

	private static final long serialVersionUID = -2352351645165455640L;

	@Getter
	private final String attributGruppe;

	@Getter
	@Setter
	private LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;

	public MehrdeutigeLinearReferenzierteAttributeException(String attributGruppe) {
		super(String.format(
			"Bei der Attributegruppe %s kam es zu einer mehrdeutigen Abbildung der Attribute auf eine Grundnetzkante.",
			attributGruppe));
		this.attributGruppe = attributGruppe;
	}

	@Override
	public String getMessage() {

		String additionalInfo = linearReferenzierterAbschnitt != null ? String.format(
			" Die Projektion betrifft den Abschnitt %s auf der GrundnetzKante.",
			linearReferenzierterAbschnitt) : "";
		return super.getMessage() + additionalInfo;
	}
}
