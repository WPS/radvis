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

import java.util.Optional;

import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzung;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtKreuzungMusterloesung;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenKommentar;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenTyp;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.LichtsignalAnlageEigenschaften;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.schnittstelle.view.NetzbezugView;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.Getter;

@Getter
public class FurtKreuzungEditView {
	private final VerwaltungseinheitView verantwortlicheOrganisation;
	private final NetzbezugView netzbezug;
	private final FurtenKreuzungenTyp typ;
	private final Boolean radnetzKonform;
	private final FurtenKreuzungenKommentar kommentar;
	private final KnotenForm knotenForm;
	private final Long version;
	private final Optional<FurtKreuzungMusterloesung> furtKreuzungMusterloesung;
	private final Optional<LichtsignalAnlageEigenschaften> lichtsignalAnlageEigenschaften;
	private final boolean benutzerDarfBearbeiten;

	public FurtKreuzungEditView(FurtKreuzung furtKreuzung, boolean benutzerDarfBearbeiten) {
		this.verantwortlicheOrganisation = new VerwaltungseinheitView(furtKreuzung.getVerantwortlicheOrganisation());
		this.netzbezug = new NetzbezugView(furtKreuzung.getNetzbezug());
		this.typ = furtKreuzung.getTyp();
		this.radnetzKonform = furtKreuzung.getRadnetzKonform();
		this.kommentar = furtKreuzung.getKommentar();
		this.version = furtKreuzung.getVersion();
		this.knotenForm = furtKreuzung.getKnotenForm();
		this.furtKreuzungMusterloesung = furtKreuzung.getMusterloesung();
		this.lichtsignalAnlageEigenschaften = furtKreuzung.getLichtsignalAnlageEigenschaften();
		this.benutzerDarfBearbeiten = benutzerDarfBearbeiten;
	}
}
