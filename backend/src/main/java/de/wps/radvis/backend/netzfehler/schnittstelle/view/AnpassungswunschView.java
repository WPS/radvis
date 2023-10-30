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

package de.wps.radvis.backend.netzfehler.schnittstelle.view;

import java.time.LocalDateTime;

import de.wps.radvis.backend.benutzer.schnittstelle.BenutzerView;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelVerletzung;
import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;
import lombok.Getter;

@Getter
public class AnpassungswunschView extends AnpassungswunschListenView {
	private final LocalDateTime erstellung;
	private final LocalDateTime aenderung;
	private final BenutzerView benutzerLetzteAenderung;

	private final boolean canEdit;
	private final KonsistenzregelVerletzung ursaechlicheKonsistenzregelVerletzung;

	private final boolean basiertAufKonsistenzregelVerletzung;

	public AnpassungswunschView(Anpassungswunsch anpassungswunsch,
		KonsistenzregelVerletzung ursaechlicheKonsistenzregelVerletzung,
		boolean canEdit) {
		super(anpassungswunsch);
		this.erstellung = anpassungswunsch.getErstellung();
		this.aenderung = anpassungswunsch.getAenderung();
		this.benutzerLetzteAenderung = new BenutzerView(anpassungswunsch.getBenutzerLetzteAenderung());
		this.canEdit = canEdit;
		this.ursaechlicheKonsistenzregelVerletzung = ursaechlicheKonsistenzregelVerletzung;
		this.basiertAufKonsistenzregelVerletzung = anpassungswunsch.getKonsistenzregelVerletzungReferenz().isPresent();
	}
}
