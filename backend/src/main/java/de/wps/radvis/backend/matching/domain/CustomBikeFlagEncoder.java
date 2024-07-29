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

package de.wps.radvis.backend.matching.domain;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.util.BikeFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.IntsRef;

import lombok.NoArgsConstructor;

/**
 * Nur OSM-Ways, die mit dem Fahrrad befahrbar sind, sollen mit RadVIS-Attributen angereichert werden.
 * Die Reihenfolge dafür ist folgende:
 * - Der MatchNetzAufOSMJob wird angestoßen und erstellt den Cache des Graphhoppers.
 * - Dabei wird durch diesen CustomBikeFlagEncoder hier festgelegt, ob eine Kante mit dem Fahrrad befahrbar ist.
 * - Wenn später der OsmAuszeichnungsJob versucht, über die Klasse OsmMapMatching auf diese Kanten zu matchen,
 * wird für nicht mit dem Fahrrad befahrbare Kanten kein Match gefunden.
 * - Der Test zu dieser Funktionalität findet sich in dem OsmMatchingRepositoryImplTest.
 */
@NoArgsConstructor
public class CustomBikeFlagEncoder extends BikeFlagEncoder {

	@Override
	public IntsRef handleWayTags(IntsRef edgeFlags, ReaderWay way, EncodingManager.Access access) {
		super.handleWayTags(edgeFlags, way, access);

		String cyclewayToAvoid = "separate";
		if (
			way.hasTag("cycleway", cyclewayToAvoid) ||
				way.hasTag("cycleway:both", cyclewayToAvoid) ||
				way.hasTag("conveying", "yes") ||
				way.hasTag("indoor", "yes") ||
				!way.getTag("railway", "").equals("")
		) {
			// beide Fahrtrichtungen
			this.accessEnc.setBool(false, edgeFlags, false);
			this.accessEnc.setBool(true, edgeFlags, false);
		}
		if (way.hasTag("cycleway:left", cyclewayToAvoid)) {
			// entgegen Fahrtrichtung
			this.accessEnc.setBool(true, edgeFlags, false);
		}
		if (way.hasTag("cycleway:right", cyclewayToAvoid)) {
			// in Fahrtrichtung
			this.accessEnc.setBool(false, edgeFlags, false);
		}

		return edgeFlags;
	}
}
