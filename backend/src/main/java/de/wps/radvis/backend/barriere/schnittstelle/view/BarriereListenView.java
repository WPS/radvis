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

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.valueObject.BarrierenForm;
import de.wps.radvis.backend.barriere.domain.valueObject.Markierung;
import de.wps.radvis.backend.barriere.domain.valueObject.Sicherung;
import de.wps.radvis.backend.barriere.domain.valueObject.VerbleibendeDurchfahrtsbreite;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.Getter;

@Getter
public class BarriereListenView {
	private final long id;
	private final VerwaltungseinheitView verantwortlich;
	private final BarrierenForm barrierenForm;
	private final VerbleibendeDurchfahrtsbreite verbleibendeDurchfahrtsbreite;
	private final Sicherung sicherung;
	private final Markierung markierung;
	private final Point iconPosition;

	public BarriereListenView(Barriere barriere) {
		this.id = barriere.getId();
		this.verantwortlich = new VerwaltungseinheitView(barriere.getVerantwortlich());
		this.barrierenForm = barriere.getBarrierenForm();
		this.verbleibendeDurchfahrtsbreite = barriere.getVerbleibendeDurchfahrtsbreite().orElse(null);
		this.sicherung = barriere.getSicherung().orElse(null);
		this.markierung = barriere.getMarkierung().orElse(null);
		this.iconPosition = barriere.getNetzbezug().getDisplayGeometry().orElse(null);
	}
}
