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

package de.wps.radvis.backend.konsistenz;

import java.time.LocalDateTime;

import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelVerletzung;
import de.wps.radvis.backend.konsistenz.regeln.domain.RadNETZLueckeKonsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;

public class KonsistenzregelVerletzungTestdataProvider {
	public static KonsistenzregelVerletzung.KonsistenzregelVerletzungBuilder defaultVerletzung(String identity) {
		return KonsistenzregelVerletzung.testbuilder()
			.details(getDefaultKonsistenzregelVerletzungsDetails(identity))
			.datum(LocalDateTime.of(2022, 11, 16, 0, 0))
			.titel("Lücke")
			.typ(RadNETZLueckeKonsistenzregel.VERLETZUNGS_TYP);
	}

	public static KonsistenzregelVerletzungsDetails getDefaultKonsistenzregelVerletzungsDetails(String identity) {
		return getDefaultKonsistenzregelVerletzungsDetails(identity, "Mut zur Lücke!");
	}

	public static KonsistenzregelVerletzungsDetails getDefaultKonsistenzregelVerletzungsDetails(String identity,
		String beschreibung) {
		return new KonsistenzregelVerletzungsDetails(GeometryTestdataProvider.createPoint(new Coordinate(10, 10)),
			GeometryTestdataProvider.createPoint(new Coordinate(10, 10)),
			beschreibung, identity);
	}

	public static KonsistenzregelVerletzung.KonsistenzregelVerletzungBuilder defaultVerletzung() {
		return defaultVerletzung("id");
	}
}
