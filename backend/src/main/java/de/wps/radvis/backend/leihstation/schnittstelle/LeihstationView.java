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

package de.wps.radvis.backend.leihstation.schnittstelle;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.leihstation.domain.valueObject.Anzahl;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationStatus;
import de.wps.radvis.backend.leihstation.domain.valueObject.UrlAdresse;
import lombok.Getter;

@Getter
public class LeihstationView {
	private final Point geometrie;
	private final String betreiber;
	private final Anzahl anzahlFahrraeder;
	private final Anzahl anzahlPedelecs;
	private final Anzahl anzahlAbstellmoeglichkeiten;
	private final boolean freiesAbstellen;
	private final UrlAdresse buchungsUrl;
	private final LeihstationStatus status;

	private LeihstationQuellSystem quellSystem;

	private final Long id;
	private final Long version;
	private final boolean darfBenutzerBearbeiten;

	public LeihstationView(Leihstation leihstation, boolean darfBenutzerBearbeiten) {
		geometrie = leihstation.getGeometrie();
		betreiber = leihstation.getBetreiber();
		id = leihstation.getId();
		version = leihstation.getVersion();
		this.darfBenutzerBearbeiten = darfBenutzerBearbeiten;

		anzahlFahrraeder = leihstation.getAnzahlFahrraeder().orElse(null);
		anzahlPedelecs = leihstation.getAnzahlPedelecs().orElse(null);
		anzahlAbstellmoeglichkeiten = leihstation.getAnzahlAbstellmoeglichkeiten().orElse(null);
		freiesAbstellen = leihstation.isFreiesAbstellen();
		buchungsUrl = leihstation.getBuchungsUrl().orElse(null);
		status = leihstation.getStatus();
		quellSystem = leihstation.getQuellSystem();
	}
}
