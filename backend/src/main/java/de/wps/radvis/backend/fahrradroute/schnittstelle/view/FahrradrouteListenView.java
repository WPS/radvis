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

import java.util.Locale;

import de.wps.radvis.backend.fahrradroute.domain.dbView.FahrradrouteListenDbOhneGeomView;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import jakarta.annotation.Nullable;
import lombok.Getter;

@Getter
public class FahrradrouteListenView {
	private final Long id;
	private final FahrradrouteName name;
	private final Kategorie fahrradrouteKategorie;
	private final FahrradrouteTyp fahrradrouteTyp;
	@Nullable
	private final String verantwortlicheOrganisation;

	@Nullable
	private String anstiegAbstieg;

	public FahrradrouteListenView(FahrradrouteListenDbOhneGeomView dbView) {
		this.id = dbView.getId();
		this.name = dbView.getName();
		this.fahrradrouteKategorie = dbView.getKategorie();
		this.fahrradrouteTyp = dbView.getFahrradrouteTyp();
		this.verantwortlicheOrganisation = dbView.getVerantwortlicheOrganisationName();

		this.anstiegAbstieg = getAnstiegAbstiegText(dbView.getAnstieg(), dbView.getAbstieg());
	}

	private String getAnstiegAbstiegText(Double anstieg, Double abstieg) {
		return String.format("%s m / %s m",
			getNullableDoubleAsIntegerString(anstieg),
			getNullableDoubleAsIntegerString(abstieg));
	}

	private String getNullableDoubleAsIntegerString(Double d) {
		return d == null ? "-" : String.format(Locale.GERMANY, "%d", d.intValue());
	}
}
