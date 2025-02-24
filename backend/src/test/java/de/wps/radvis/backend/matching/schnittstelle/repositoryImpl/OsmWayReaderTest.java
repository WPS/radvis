/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.RAMDirectory;

class OsmWayReaderTest {
	private GraphHopperStorage ghStorage;

	@BeforeEach
	void setUp() {
		ghStorage = new GraphHopperStorage(new RAMDirectory(), (new EncodingManager.Builder()).build(), false);
	}

	@Test
	public void testConstructorWorks() {
		// Testet die Nutzung von Reflection um Tippfehler oder Methoden-Umbenennungen im GraphHopper-Code aufzudecken.
		Assertions.assertDoesNotThrow(() -> new OsmWayReader(ghStorage, Collections.emptyMap(), Collections.emptyMap(),
			Collections.emptyMap()));
	}
}