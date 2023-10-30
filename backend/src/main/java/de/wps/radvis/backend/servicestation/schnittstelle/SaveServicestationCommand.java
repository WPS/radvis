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

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.servicestation.domain.valueObject.Betreiber;
import de.wps.radvis.backend.servicestation.domain.valueObject.Fahrradhalterung;
import de.wps.radvis.backend.servicestation.domain.valueObject.Gebuehren;
import de.wps.radvis.backend.servicestation.domain.valueObject.Kettenwerkzeug;
import de.wps.radvis.backend.servicestation.domain.valueObject.Luftpumpe;
import de.wps.radvis.backend.servicestation.domain.valueObject.Marke;
import de.wps.radvis.backend.servicestation.domain.valueObject.Oeffnungszeiten;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationBeschreibung;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationName;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationStatus;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationTyp;
import de.wps.radvis.backend.servicestation.domain.valueObject.Werkzeug;
import lombok.Getter;

@Getter
public class SaveServicestationCommand {
	@NotNull
	private Geometry geometrie;
	@NotNull
	private ServicestationName name;
	private Long version;

	@NotNull
	private Gebuehren gebuehren;

	private Oeffnungszeiten oeffnungszeiten; // kein Pflichtfeld

	@NotNull
	private Betreiber betreiber;

	private Marke marke; //  kein Pflichtfeld

	@NotNull
	private Luftpumpe luftpumpe;

	@NotNull
	private Kettenwerkzeug kettenwerkzeug;

	@NotNull
	private Werkzeug werkzeug;

	@NotNull
	private Fahrradhalterung fahrradhalterung;

	private ServicestationBeschreibung beschreibung; // keine Pflicht

	@NotNull
	private Long organisationId;

	@NotNull
	private ServicestationTyp typ;

	@NotNull
	private ServicestationStatus status;

	@AssertTrue
	public boolean isGeometrieValid() {
		return geometrie.getGeometryType().equals(Geometry.TYPENAME_POINT);
	}
}
