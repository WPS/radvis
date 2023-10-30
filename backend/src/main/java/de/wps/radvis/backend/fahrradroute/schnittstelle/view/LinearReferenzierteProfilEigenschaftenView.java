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

package de.wps.radvis.backend.fahrradroute.schnittstelle.view;

import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public class LinearReferenzierteProfilEigenschaftenView {

	private BelagArt belagArt;

	private Radverkehrsfuehrung radverkehrsfuehrung;

	private double vonLR;

	private double bisLR;

	public LinearReferenzierteProfilEigenschaftenView(LinearReferenzierteProfilEigenschaften eigenschaften) {
		this.belagArt = eigenschaften.getProfilEigenschaften().getBelagArt();
		this.radverkehrsfuehrung = eigenschaften.getProfilEigenschaften().getRadverkehrsfuehrung();

		this.vonLR = eigenschaften.getLinearReferenzierterAbschnitt().getVonValue();

		this.bisLR = eigenschaften.getLinearReferenzierterAbschnitt().getBisValue();
	}
}
