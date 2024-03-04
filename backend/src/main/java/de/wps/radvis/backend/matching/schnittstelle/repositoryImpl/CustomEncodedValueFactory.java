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

package de.wps.radvis.backend.matching.schnittstelle.repositoryImpl;

import com.graphhopper.routing.ev.DefaultEncodedValueFactory;
import com.graphhopper.routing.ev.EncodedValue;

import de.wps.radvis.backend.matching.schnittstelle.encodedValues.BarriereFormEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.BelagArtEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.BeleuchtungEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.BreiteEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.DtvPkwEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KommunalnetzAlltagEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KommunalnetzFreizeitEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KreisnetzAlltagEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KreisnetzFreizeitEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.OberflaechenbeschaffenheitEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadNetzAlltagEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadNetzFreizeitEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadNetzZielnetzEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadschnellverbindungEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadverkehrsfuehrungEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadvorrangroutenEncodedValue;

/**
 * Wird vom Graphhopper benutzt, wenn dieser einen Cache lädt. Aus diesem Cache werden die encoded-value-strings
 * gelesen, welche hier ins "create()" gegeben werden.
 * <p>
 * Beim Bauen des Caches wird diese Factory *nicht* benötigt, da beim Bauen der CustomTagParser benutzt wird.
 */
public class CustomEncodedValueFactory extends DefaultEncodedValueFactory {
	@Override
	public EncodedValue create(String string) {
		String name = string.split("\\|")[0];

		// RadNETZ
		if (name.equals(RadNetzAlltagEncodedValue.getEncodedValueKey())) {
			return RadNetzAlltagEncodedValue.create();
		}
		if (name.equals(RadNetzFreizeitEncodedValue.getEncodedValueKey())) {
			return RadNetzFreizeitEncodedValue.create();
		}
		if (name.equals(RadNetzZielnetzEncodedValue.getEncodedValueKey())) {
			return RadNetzZielnetzEncodedValue.create();
		}

		// Kreisnetz
		if (name.equals(KreisnetzAlltagEncodedValue.getEncodedValueKey())) {
			return KreisnetzAlltagEncodedValue.create();
		}
		if (name.equals(KreisnetzFreizeitEncodedValue.getEncodedValueKey())) {
			return KreisnetzFreizeitEncodedValue.create();
		}

		// Kommunalnetz
		if (name.equals(KommunalnetzAlltagEncodedValue.getEncodedValueKey())) {
			return KommunalnetzAlltagEncodedValue.create();
		}
		if (name.equals(KommunalnetzFreizeitEncodedValue.getEncodedValueKey())) {
			return KommunalnetzFreizeitEncodedValue.create();
		}

		// Radrouten
		if (name.equals(RadschnellverbindungEncodedValue.getEncodedValueKey())) {
			return RadschnellverbindungEncodedValue.create();
		}
		if (name.equals(RadvorrangroutenEncodedValue.getEncodedValueKey())) {
			return RadvorrangroutenEncodedValue.create();
		}

		// Führungsform
		if (name.equals(RadverkehrsfuehrungEncodedValue.getEncodedValueKey())) {
			return RadverkehrsfuehrungEncodedValue.create();
		}

		// BarriereForm
		if (name.equals(BarriereFormEncodedValue.getEncodedValueKey())) {
			return BarriereFormEncodedValue.create();
		}

		// Belagart
		if (name.equals(BelagArtEncodedValue.getEncodedValueKey())) {
			return BelagArtEncodedValue.create();
		}

		// Oberflächenbeschaffenheit
		if (name.equals(OberflaechenbeschaffenheitEncodedValue.getEncodedValueKey())) {
			return OberflaechenbeschaffenheitEncodedValue.create();
		}

		// Beleuchtung
		if (name.equals(BeleuchtungEncodedValue.getEncodedValueKey())) {
			return BeleuchtungEncodedValue.create();
		}

		// DTV Pkw
		if (name.equals(DtvPkwEncodedValue.getEncodedValueKey())) {
			return DtvPkwEncodedValue.create();
		}

		// Breite
		if (name.equals(BreiteEncodedValue.getEncodedValueKey())) {
			return BreiteEncodedValue.create();
		}

		return super.create(string);
	}
}
