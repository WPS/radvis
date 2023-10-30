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

package de.wps.radvis.backend.matching.schnittstelle.repositoryImpl;

import org.locationtech.jts.geom.Coordinate;

import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.storage.DAType;

import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.matching.domain.entity.ElevationDataFileNotFoundException;
import de.wps.radvis.backend.matching.domain.entity.ElevationTileDirectory;
import lombok.NonNull;

public class LGLElevationProviderRepository implements ElevationProvider {

	private ElevationDataSpatialIndexRepository cacheIndex;
	private final ElevationTileDirectory elevationTileDirectory;

	public LGLElevationProviderRepository(@NonNull String cacheDirPfad, @NonNull String tiffTilesVerzeichnis) {
		this.cacheIndex = new ElevationDataSpatialIndexRepository(tiffTilesVerzeichnis, cacheDirPfad);
		this.elevationTileDirectory = new ElevationTileDirectory(cacheDirPfad, DAType.MMAP);
	}

	@Override
	public double getEle(double lat, double lon) {
		// Umrechnen von WGS84 zu UTM
		final var coordinateInUtm32 = transformCoordinate(new Coordinate(lat, lon));
		final var latGerundet = (int) (coordinateInUtm32.x);
		final var lonGerundet = (int) (coordinateInUtm32.y);

		// ElevationTile aus dem Index holen
		try {
			final var elevationTile = cacheIndex.findeTileZuKoordinaten(coordinateInUtm32);
			// Höhe auslesen und zurückgeben
			return elevationTile.getHeight(latGerundet, lonGerundet, elevationTileDirectory);
		} catch (ElevationDataFileNotFoundException e) {
			return 0;
		}
	}

	public double getEle(Coordinate coordinateInUtm32) {
		final var latGerundet = (int) (coordinateInUtm32.x);
		final var lonGerundet = (int) (coordinateInUtm32.y);
		// ElevationTile aus dem Index holen
		try {
			final var elevationTile = cacheIndex.findeTileZuKoordinaten(coordinateInUtm32);
			// Höhe auslesen und zurückgeben
			return elevationTile.getHeight(latGerundet, lonGerundet, elevationTileDirectory);
		} catch (ElevationDataFileNotFoundException e) {
			return 0;
		}
	}

	@Override
	public boolean canInterpolate() {
		return false;
	}

	@Override
	public void release() {
		cacheIndex = null;
	}

	private Coordinate transformCoordinate(Coordinate coordinate) {
		return CoordinateReferenceSystemConverterUtility.transformCoordinateUnsafe(coordinate,
			KoordinatenReferenzSystem.WGS84,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);
	}
}
