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

package de.wps.radvis.backend.barriere.schnittstelle.view;

import java.util.Optional;

import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.valueObject.BarriereBegruendung;
import de.wps.radvis.backend.barriere.domain.valueObject.BarriereFormDetails;
import de.wps.radvis.backend.barriere.domain.valueObject.BarrierenForm;
import de.wps.radvis.backend.barriere.domain.valueObject.Markierung;
import de.wps.radvis.backend.barriere.domain.valueObject.Sicherung;
import de.wps.radvis.backend.barriere.domain.valueObject.VerbleibendeDurchfahrtsbreite;
import de.wps.radvis.backend.netz.schnittstelle.view.NetzbezugView;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.Getter;

@Getter
public class BarriereEditView {
	private final Long version;
	private final VerwaltungseinheitView verantwortlicheOrganisation;
	private final NetzbezugView netzbezug;

	private final BarrierenForm barrierenForm;
	private final Optional<BarriereFormDetails> barriereFormDetails;
	private final Optional<VerbleibendeDurchfahrtsbreite> verbleibendeDurchfahrtsbreite;
	private final Optional<Sicherung> sicherung;
	private final Optional<Markierung> markierung;
	private final Optional<BarriereBegruendung> begruendung;
	private final boolean darfBenutzerBearbeiten;

	public BarriereEditView(Barriere barriere, boolean darfBenutzerBearbeiten) {
		this.verantwortlicheOrganisation = new VerwaltungseinheitView(barriere.getVerantwortlich());
		this.netzbezug = new NetzbezugView(barriere.getNetzbezug());
		this.version = barriere.getVersion();
		this.barrierenForm = barriere.getBarrierenForm();
		this.verbleibendeDurchfahrtsbreite = barriere.getVerbleibendeDurchfahrtsbreite();
		this.sicherung = barriere.getSicherung();
		this.markierung = barriere.getMarkierung();
		this.begruendung = barriere.getBegruendung();
		this.darfBenutzerBearbeiten = darfBenutzerBearbeiten;
		this.barriereFormDetails = barriere.getBarriereFormDetails();
	}
}
