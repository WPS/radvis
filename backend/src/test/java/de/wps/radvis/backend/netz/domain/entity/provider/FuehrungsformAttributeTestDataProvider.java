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

package de.wps.radvis.backend.netz.domain.entity.provider;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;

public class FuehrungsformAttributeTestDataProvider {
	public static FuehrungsformAttribute.FuehrungsformAttributeBuilder withGrundnetzDefaultwerte() {
		return FuehrungsformAttribute.builder()
			.bordstein(Bordstein.UNBEKANNT)
			.belagArt(BelagArt.UNBEKANNT)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.UNBEKANNT)
			.breite(null)
			.radverkehrsfuehrung(Radverkehrsfuehrung.UNBEKANNT)
			.parkenTyp(KfzParkenTyp.UNBEKANNT)
			.parkenForm(KfzParkenForm.UNBEKANNT)
			.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 1.))
			.trennstreifenBreiteRechts(null)
			.trennstreifenBreiteLinks(null)
			.trennstreifenTrennungZuRechts(null)
			.trennstreifenTrennungZuLinks(null)
			.trennstreifenFormRechts(null)
			.trennstreifenFormLinks(null);
	}

	public static FuehrungsformAttribute.FuehrungsformAttributeBuilder withLineareReferenz(double von, double bis) {
		return withGrundnetzDefaultwerte()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(von, bis));
	}
}