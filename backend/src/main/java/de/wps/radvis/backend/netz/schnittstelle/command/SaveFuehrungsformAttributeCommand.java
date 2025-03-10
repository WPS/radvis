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

package de.wps.radvis.backend.netz.schnittstelle.command;

import java.util.Set;

import org.springframework.validation.annotation.Validated;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.Absenkung;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Beschilderung;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Schadenart;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Validated
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaveFuehrungsformAttributeCommand {

	@NotNull
	private BelagArt belagArt;

	@NotNull
	private Oberflaechenbeschaffenheit oberflaechenbeschaffenheit;

	@NotNull
	private Bordstein bordstein;

	@NotNull
	private Radverkehrsfuehrung radverkehrsfuehrung;

	@NotNull
	private KfzParkenTyp parkenTyp;

	@NotNull
	private KfzParkenForm parkenForm;

	@NotNull
	private Beschilderung beschilderung;

	@NotNull
	private Absenkung absenkung;

	@NotNull
	private Set<Schadenart> schaeden;

	private Laenge breite;

	private Laenge trennstreifenBreiteRechts;

	private Laenge trennstreifenBreiteLinks;

	private TrennungZu trennstreifenTrennungZuRechts;

	private TrennungZu trennstreifenTrennungZuLinks;

	private TrennstreifenForm trennstreifenFormRechts;

	private TrennstreifenForm trennstreifenFormLinks;

	@NotNull
	private LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;

	@NotNull
	private Benutzungspflicht benutzungspflicht;

	@AssertTrue(message = "Trennstreifen passt nicht zur Radverkehrsführung oder enthält fehlerhafte Werte")
	public boolean isTrennstreifenValid() {
		return FuehrungsformAttribute.isTrennstreifenCorrect(radverkehrsfuehrung, trennstreifenFormLinks,
			trennstreifenBreiteLinks, trennstreifenTrennungZuLinks)
			&& FuehrungsformAttribute.isTrennstreifenCorrect(radverkehrsfuehrung, trennstreifenFormRechts,
				trennstreifenBreiteRechts, trennstreifenTrennungZuRechts);
	}

	@AssertTrue(message = "Gewählte Beschilderung passt nicht zu Radverkehrsführung: nur für Betriebswege erlaubt")
	public boolean isBeschilderungValid() {
		// diese Validierung kann ggf. vor der @NotNull aufgerufen werden. Damit die Fehlermeldung aber korrekt ist,
		// geben wir hier true zurück.
		if (radverkehrsfuehrung == null || beschilderung == null) {
			return true;
		}
		return beschilderung.isValidForRadverkehrsfuehrung(radverkehrsfuehrung);
	}
}
