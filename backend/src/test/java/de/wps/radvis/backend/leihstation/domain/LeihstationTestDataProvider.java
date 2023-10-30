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

package de.wps.radvis.backend.leihstation.domain;

import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation.LeihstationBuilder;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationStatus;

public class LeihstationTestDataProvider {
	public static LeihstationBuilder defaultLeihstation() {
		return Leihstation.builder().betreiber("Mein Betreiber")
			.geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 100)))
			.status(LeihstationStatus.AKTIV)
			.quellSystem(LeihstationQuellSystem.RADVIS);
	}

	public static LeihstationBuilder defaultAusMobiData() {
		return Leihstation.builder().betreiber("Mobi")
			.geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 100)))
			.freiesAbstellen(false)
			.status(LeihstationStatus.AKTIV)
			.quellSystem(LeihstationQuellSystem.MOBIDATABW);
	}
}
