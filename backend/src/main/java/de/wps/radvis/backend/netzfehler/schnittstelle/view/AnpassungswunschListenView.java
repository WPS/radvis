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

import java.util.Optional;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AnpassungswunschListenView {
	private final Long id;
	private final String beschreibung;
	private final AnpassungswunschStatus status;
	private final Point geometrie;
	private final AnpassungswunschKategorie kategorie;
	private final Optional<VerwaltungseinheitView> verantwortlicheOrganisation;

	public AnpassungswunschListenView(Anpassungswunsch anpassungswunsch) {
		this.id = anpassungswunsch.getId();
		this.beschreibung = anpassungswunsch.getBeschreibung();
		this.status = anpassungswunsch.getStatus();
		this.geometrie = anpassungswunsch.getGeometrie();
		this.kategorie = anpassungswunsch.getKategorie();
		this.verantwortlicheOrganisation = anpassungswunsch.getVerantwortlicheOrganisation().map(
			VerwaltungseinheitView::new);
	}
}
