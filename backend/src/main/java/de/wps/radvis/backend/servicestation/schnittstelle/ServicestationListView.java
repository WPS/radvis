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

package de.wps.radvis.backend.servicestation.schnittstelle;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import de.wps.radvis.backend.servicestation.domain.valueObject.Betreiber;
import de.wps.radvis.backend.servicestation.domain.valueObject.Fahrradhalterung;
import de.wps.radvis.backend.servicestation.domain.valueObject.Gebuehren;
import de.wps.radvis.backend.servicestation.domain.valueObject.Kettenwerkzeug;
import de.wps.radvis.backend.servicestation.domain.valueObject.Luftpumpe;
import de.wps.radvis.backend.servicestation.domain.valueObject.Marke;
import de.wps.radvis.backend.servicestation.domain.valueObject.Radkultur;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationBeschreibung;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationName;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationStatus;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationTyp;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationenQuellSystem;
import de.wps.radvis.backend.servicestation.domain.valueObject.Werkzeug;
import lombok.Getter;

@Getter
public class ServicestationListView {
	private final Point geometrie;
	private final ServicestationName name;
	private final Long id;
	private final Gebuehren gebuehren;
	private final Betreiber betreiber;
	private final Marke marke;
	private final Luftpumpe luftpumpe;
	private final Kettenwerkzeug kettenwerkzeug;
	private final Werkzeug werkzeug;
	private final Fahrradhalterung fahrradhalterung;
	private final ServicestationBeschreibung beschreibung;
	private final VerwaltungseinheitView organisation;
	private final ServicestationTyp typ;
	private final ServicestationStatus status;
	private final ServicestationenQuellSystem quellSystem;
	private final Radkultur radkultur;

	public ServicestationListView(Servicestation servicestation) {
		geometrie = servicestation.getGeometrie();
		name = servicestation.getName();
		id = servicestation.getId();
		quellSystem = servicestation.getQuellSystem();
		gebuehren = servicestation.getGebuehren();
		betreiber = servicestation.getBetreiber();
		marke = servicestation.getMarke().orElse(null);
		luftpumpe = servicestation.getLuftpumpe();
		kettenwerkzeug = servicestation.getKettenwerkzeug();
		werkzeug = servicestation.getWerkzeug();
		fahrradhalterung = servicestation.getFahrradhalterung();
		beschreibung = servicestation.getBeschreibung().orElse(null);
		organisation = new VerwaltungseinheitView(servicestation.getOrganisation());
		typ = servicestation.getTyp();
		status = servicestation.getStatus();
		radkultur = servicestation.getRadkultur();
	}
}
