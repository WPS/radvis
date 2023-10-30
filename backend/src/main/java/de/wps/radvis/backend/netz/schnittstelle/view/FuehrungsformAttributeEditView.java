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

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FuehrungsformAttributeEditView {

	private BelagArt belagArt;
	private Oberflaechenbeschaffenheit oberflaechenbeschaffenheit;
	private Bordstein bordstein;
	private Radverkehrsfuehrung radverkehrsfuehrung;
	private KfzParkenForm parkenForm;
	private KfzParkenTyp parkenTyp;
	private Laenge breite;
	private Benutzungspflicht benutzungspflicht;
	private Laenge trennstreifenBreiteRechts;
	private Laenge trennstreifenBreiteLinks;
	private TrennungZu trennstreifenTrennungZuRechts;
	private TrennungZu trennstreifenTrennungZuLinks;
	private TrennstreifenForm trennstreifenFormRechts;
	private TrennstreifenForm trennstreifenFormLinks;

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

		this.trennstreifenBreiteRechts = fuehrungsformAttribute.getTrennstreifenBreiteRechts().orElse(null);
		this.trennstreifenTrennungZuRechts = fuehrungsformAttribute.getTrennstreifenTrennungZuRechts().orElse(null);
		this.trennstreifenFormRechts = fuehrungsformAttribute.getTrennstreifenFormRechts().orElse(null);

		this.trennstreifenBreiteLinks = fuehrungsformAttribute.getTrennstreifenBreiteLinks().orElse(null);
		this.trennstreifenTrennungZuLinks = fuehrungsformAttribute.getTrennstreifenTrennungZuLinks().orElse(null);
		this.trennstreifenFormLinks = fuehrungsformAttribute.getTrennstreifenFormLinks().orElse(null);
	}
}
