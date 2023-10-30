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

package de.wps.radvis.backend.common;

import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import org.postgresql.util.PGobject;

public class PostGisHelper {
	public static PGobject getPGobject(Geometry geometry) throws SQLException {
		return getPGobject(2, geometry);
	}

	public static PGobject getPGobject3D(Geometry geometry) throws SQLException {
		return getPGobject(3, geometry);
	}

	@NotNull
	private static PGobject getPGobject(int outputDimension, Geometry geometry) throws SQLException {
		PGobject pGobject = new PGobject();
		pGobject.setType("geometry");
		String hexValueOfByteRepresentation = WKBWriter.toHex(new WKBWriter(outputDimension, 0, true).write(geometry));
		hexValueOfByteRepresentation = hexValueOfByteRepresentation.charAt(0) + "1"
			+ hexValueOfByteRepresentation.substring(2);
		pGobject.setValue(hexValueOfByteRepresentation);
		return pGobject;
	}
}
