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

package de.wps.radvis.backend.matching.domain.entity;

import com.graphhopper.storage.DataAccess;

import lombok.Getter;

@Getter
public class ElevationTile {
	private static final double MIN_ELEVATION_METERS = -12_000;
	private static final double MAX_ELEVATION_METERS = 9_000;

	private final int minX;
	private final int minY;
	private final int width;
	private final int height;
	private final String cacheFileName;

	public ElevationTile(int minX, int minY, int width, int height, String cacheFileName) {
		this.minX = minX;
		this.minY = minY;
		this.width = width;
		this.height = height;
		this.cacheFileName = cacheFileName;
	}

	public double getHeight(int x, int y, ElevationTileDirectory elevationTileDirectory) {
		final var deltaX = Math.abs(x - minX);
		final var deltaY = Math.abs(y - minY);

		final var heights = elevationTileDirectory.find(cacheFileName);
		var elevation = (double) getHeightSample(heights, deltaX, deltaY);
		elevationTileDirectory.flush();

		if (!isValidElevation(elevation)) {
			elevation = Double.NaN;
		}

		return elevation;
	}

	private short getHeightSample(DataAccess heights, int x, int y) {
		return heights.getShort(2 * ((long) y * width + x));
	}

	private boolean isValidElevation(double elevation) {
		return elevation > MIN_ELEVATION_METERS && elevation < MAX_ELEVATION_METERS;
	}
}
