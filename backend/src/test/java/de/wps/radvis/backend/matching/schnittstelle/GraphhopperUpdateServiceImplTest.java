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

package de.wps.radvis.backend.matching.schnittstelle;

import static org.mockito.Mockito.times;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.matching.domain.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.service.GraphhopperUpdateService;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;

class GraphhopperUpdateServiceImplTest {
	@Mock
	DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory;
	@Mock
	GraphhopperRoutingRepository graphhopperRoutingRepository;
	@Mock
	DlmMatchingRepository dlmMatchingRepository;

	GraphhopperUpdateService graphhopperUpdateService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		this.graphhopperUpdateService = new GraphhopperUpdateServiceImpl(dlmMatchedGraphHopperFactory,
			graphhopperRoutingRepository, dlmMatchingRepository);
	}

	@Test
	void testeUpdate() {
		graphhopperUpdateService.update();
		InOrder inOrder1 = Mockito.inOrder(dlmMatchedGraphHopperFactory, graphhopperRoutingRepository);
		InOrder inOrder2 = Mockito.inOrder(dlmMatchedGraphHopperFactory, dlmMatchingRepository);

		inOrder1.verify(dlmMatchedGraphHopperFactory, times(1)).updateDlmGraphHopper();
		inOrder1.verify(graphhopperRoutingRepository, times(1)).swapGraphHopper();
		inOrder2.verify(dlmMatchedGraphHopperFactory, times(1)).updateDlmGraphHopper();
		inOrder2.verify(dlmMatchingRepository, times(1)).swapGraphHopper();
	}
}