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

package de.wps.radvis.backend.servicestation.domain.entity.provider;

import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
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
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationenQuellSystem;
import de.wps.radvis.backend.servicestation.domain.valueObject.Werkzeug;

public class ServicestationTestDataProvider {
	public static Servicestation.ServicestationBuilder withDefaultValues() {
		return Servicestation.builder()
			.geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 100)))
			.name(ServicestationName.of("Teststation1"))
			.gebuehren(Gebuehren.of(true))
			.oeffnungszeiten(Oeffnungszeiten.of("8-16"))
			.betreiber(Betreiber.of("Testbetreiber"))
			.marke(Marke.of("Testmarke"))
			.luftpumpe(Luftpumpe.of(true))
			.kettenwerkzeug(Kettenwerkzeug.of(false))
			.werkzeug(Werkzeug.of(true))
			.fahrradhalterung(Fahrradhalterung.of(true))
			.beschreibung(ServicestationBeschreibung.of("Testbeschreibung"))
			.organisation(VerwaltungseinheitTestDataProvider.defaultOrganisation().build())
			.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
			.status(ServicestationStatus.AKTIV)
			.dokumentListe(new DokumentListe())
			.quellSystem(ServicestationenQuellSystem.RADVIS);
	}

	public static Servicestation.ServicestationBuilder withDefaultMobiDataValues() {
		return Servicestation.builder()
			.geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 100)))
			.status(ServicestationStatus.AKTIV)
			.oeffnungszeiten(null)
			.marke(null)
			.beschreibung(null)
			.luftpumpe(Luftpumpe.of(false))
			.gebuehren(Gebuehren.of(false))
			.werkzeug(Werkzeug.of(false))
			.kettenwerkzeug(Kettenwerkzeug.of(false))
			.fahrradhalterung(Fahrradhalterung.of(false))
			.betreiber(Betreiber.of(""))
			.dokumentListe(new DokumentListe())
			.quellSystem(ServicestationenQuellSystem.MOBIDATABW);
	}
}
