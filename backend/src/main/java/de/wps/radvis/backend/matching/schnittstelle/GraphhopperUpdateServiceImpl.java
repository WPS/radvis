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

import de.wps.radvis.backend.matching.domain.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.service.GraphhopperUpdateService;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GraphhopperUpdateServiceImpl implements GraphhopperUpdateService {
	final DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory;
	final GraphhopperRoutingRepository graphhopperRoutingRepository;
	final DlmMatchingRepository dlmMatchingRepository;

	public GraphhopperUpdateServiceImpl(
		DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory,
		GraphhopperRoutingRepository graphhopperRoutingRepository,
		DlmMatchingRepository dlmMatchingRepository) {

		this.dlmMatchedGraphHopperFactory = dlmMatchedGraphHopperFactory;
		this.graphhopperRoutingRepository = graphhopperRoutingRepository;
		this.dlmMatchingRepository = dlmMatchingRepository;
	}

	@Override
	public void update() {
		log.info("Der aktuell genutzte Graphhopper wird aktualisiert...");
		dlmMatchedGraphHopperFactory.updateDlmGraphHopper();
		dlmMatchingRepository.swapGraphHopper();
		graphhopperRoutingRepository.swapGraphHopper();
	}
}
