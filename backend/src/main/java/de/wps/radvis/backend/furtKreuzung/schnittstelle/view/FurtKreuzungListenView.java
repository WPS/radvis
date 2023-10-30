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

package de.wps.radvis.backend.furtKreuzung.schnittstelle.view;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzung;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenKommentar;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.Getter;

@Getter
public class FurtKreuzungListenView {
	private final long id;
	private final VerwaltungseinheitView verantwortlich;
	private final FurtenKreuzungenTyp typ;
	private final boolean radnetzKonform;
	private final FurtenKreuzungenKommentar kommentar;
	private final KnotenForm knotenForm;
	private final Point iconPosition;

	public FurtKreuzungListenView(FurtKreuzung furtKreuzung) {
		this.id = furtKreuzung.getId();
		this.verantwortlich = new VerwaltungseinheitView(furtKreuzung.getVerantwortlicheOrganisation());
		this.typ = furtKreuzung.getTyp();
		this.radnetzKonform = furtKreuzung.getRadnetzKonform();
		this.kommentar = furtKreuzung.getKommentar();
		this.knotenForm = furtKreuzung.getKnotenForm();
		this.iconPosition = furtKreuzung.getNetzbezug().getDisplayGeometry().orElse(null);
	}
}
