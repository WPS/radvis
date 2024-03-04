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

package de.wps.radvis.backend.abstellanlage.domain.entity.provider;

import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBetreiber;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenOrt;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenStatus;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlStellplaetze;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Stellplatzart;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberdacht;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberwacht;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;

public class AbstellanlageTestDataProvider {
	public static Abstellanlage.AbstellanlageBuilder withDefaultValues() {
		return Abstellanlage.builder()
			.geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 100)))

			.anzahlStellplaetze(AnzahlStellplaetze.of(30))
			.betreiber(AbstellanlagenBetreiber.of("Default Betreiber"))
			.quellSystem(AbstellanlagenQuellSystem.RADVIS)
			.ueberwacht(Ueberwacht.VIDEO)
			.abstellanlagenOrt(AbstellanlagenOrt.BIKE_AND_RIDE)
			.stellplatzart(Stellplatzart.SONSTIGE)
			.ueberdacht(Ueberdacht.of(false))
			.status(AbstellanlagenStatus.AKTIV)
			.dokumentListe(new DokumentListe());
	}
}
