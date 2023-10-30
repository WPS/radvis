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

import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.GHDirectory;

public class ElevationTileDirectory extends GHDirectory {
	private static final Long MAX_OPEN_DATA_ACCESS = 100L;

	public ElevationTileDirectory(String location, DAType daType) {
		super(location, daType);
	}

	@Override
	public DataAccess find(String name) {
		final var dataAccessExists = this.map.containsKey(name);
		final var dataAccess = super.find(name, DAType.MMAP);
		if (!dataAccessExists) {
			dataAccess.loadExisting();
		}
		return dataAccess;
	}

	public void flush() {
		if (this.map.size() > MAX_OPEN_DATA_ACCESS) {
			for (var dataAccess : this.map.values()) {
				dataAccess.close();
			}
			this.map.clear();
		}
	}
}
