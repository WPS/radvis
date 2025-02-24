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

package de.wps.radvis.backend.netz.schnittstelle.view;

import java.util.Set;

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
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FuehrungsformAttributeEditView {

	private final BelagArt belagArt;
	private final Oberflaechenbeschaffenheit oberflaechenbeschaffenheit;
	private final Bordstein bordstein;
	private final Radverkehrsfuehrung radverkehrsfuehrung;
	private final KfzParkenForm parkenForm;
	private final KfzParkenTyp parkenTyp;
	private final Laenge breite;
	private final Benutzungspflicht benutzungspflicht;
	private final Beschilderung beschilderung;
	private final Laenge trennstreifenBreiteRechts;
	private final Laenge trennstreifenBreiteLinks;
	private final TrennungZu trennstreifenTrennungZuRechts;
	private final TrennungZu trennstreifenTrennungZuLinks;
	private final TrennstreifenForm trennstreifenFormRechts;
	private final TrennstreifenForm trennstreifenFormLinks;
	private final Set<Schadenart> schaeden;
	private final Absenkung absenkung;

	protected LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;

	public FuehrungsformAttributeEditView(FuehrungsformAttribute fuehrungsformAttribute) {
		this.belagArt = fuehrungsformAttribute.getBelagArt();
		this.oberflaechenbeschaffenheit = fuehrungsformAttribute.getOberflaechenbeschaffenheit();
		this.bordstein = fuehrungsformAttribute.getBordstein();
		this.radverkehrsfuehrung = fuehrungsformAttribute.getRadverkehrsfuehrung();
		this.parkenTyp = fuehrungsformAttribute.getParkenTyp();
		this.parkenForm = fuehrungsformAttribute.getParkenForm();
		this.breite = fuehrungsformAttribute.getBreite().orElse(null);
		this.benutzungspflicht = fuehrungsformAttribute.getBenutzungspflicht();
		this.linearReferenzierterAbschnitt = fuehrungsformAttribute.getLinearReferenzierterAbschnitt();
		this.beschilderung = fuehrungsformAttribute.getBeschilderung();
		this.schaeden = fuehrungsformAttribute.getSchaeden();
		this.absenkung = fuehrungsformAttribute.getAbsenkung();

		this.trennstreifenBreiteRechts = fuehrungsformAttribute.getTrennstreifenBreiteRechts().orElse(null);
		this.trennstreifenTrennungZuRechts = fuehrungsformAttribute.getTrennstreifenTrennungZuRechts().orElse(null);
		this.trennstreifenFormRechts = fuehrungsformAttribute.getTrennstreifenFormRechts().orElse(null);

		this.trennstreifenBreiteLinks = fuehrungsformAttribute.getTrennstreifenBreiteLinks().orElse(null);
		this.trennstreifenTrennungZuLinks = fuehrungsformAttribute.getTrennstreifenTrennungZuLinks().orElse(null);
		this.trennstreifenFormLinks = fuehrungsformAttribute.getTrennstreifenFormLinks().orElse(null);
	}
}
