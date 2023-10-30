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

import static com.graphhopper.util.Helper.createFormatter;
import static com.graphhopper.util.Helper.getMemInfo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.graphhopper.GraphHopper;

import de.wps.radvis.backend.matching.domain.OsmMatchingCacheRepository;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OsmMatchedGraphHopper extends GraphHopper {

	@Getter
	private final Map<Integer, LinearReferenzierteOsmWayId> graphHopperEdgesAufLinRefOsmWaysIds;

	private final OsmMatchingCacheRepository osmMatchingCacheRepository;

	public OsmMatchedGraphHopper(OsmMatchingCacheRepository osmMatchingCacheRepository) {
		super();
		require(osmMatchingCacheRepository, notNullValue());

		this.graphHopperEdgesAufLinRefOsmWaysIds = new HashMap<>();
		this.osmMatchingCacheRepository = osmMatchingCacheRepository;

	}

	@Override
	protected void importOSM() {
		if (getOSMFile() == null)
			throw new IllegalStateException("Couldn't load from existing folder: " + getGraphHopperLocation()
				+ " but also cannot use file for DataReader as it wasn't specified!");

		log.info("start creating graph from {}.", getOSMFile());
		OsmWayReader reader = new OsmWayReader(getGraphHopperStorage(), null, null,
			graphHopperEdgesAufLinRefOsmWaysIds)
			.setFile(_getOSMFile())
			.setWorkerThreads(getWorkerThreads())
			// Verhindert Simplification; wird standardmäßig über getWayPointMaxDistance gesetzt.
			// https://discuss.graphhopper.com/t/map-matching-pillow-nodes-missing-in-matchresult/3142/2
			// TODO dann brauchen wir das aber auch im Graphhopper, oder? siehe
			// https://discuss.graphhopper.com/t/map-matching-pillow-nodes-missing-in-matchresult/3142/6
			.setWayPointMaxDistance(0)
			.setLongEdgeSamplingDistance(Double.MAX_VALUE);

		log.info("using " + getGraphHopperStorage().toString() + ", memory:" + getMemInfo());
		try {
			reader.readGraph();
		} catch (IOException ex) {
			throw new RuntimeException("Cannot read file " + getOSMFile(), ex);
		}
		DateFormat f = createFormatter();
		getGraphHopperStorage().getProperties().put("datareader.import.date", f.format(new Date()));
		if (reader.getDataDate() != null)
			getGraphHopperStorage().getProperties().put("datareader.data.date", f.format(reader.getDataDate()));
		osmMatchingCacheRepository.save(graphHopperEdgesAufLinRefOsmWaysIds);
		log.info("Speichere {} Edge-Mappings", graphHopperEdgesAufLinRefOsmWaysIds.size());
	}

	@Override
	public boolean load(String graphHopperFolder) {
		boolean hasLoaded = super.load(graphHopperFolder);
		if (hasLoaded) {
			if (!osmMatchingCacheRepository.hasCache()) {
				throw new RuntimeException(
					"Die Datei für den OsmMatchingCache ist nicht vorhanden. Bitte den GrapHopper-Cache löschen unter "
						+ getGraphHopperLocation());
			}
			graphHopperEdgesAufLinRefOsmWaysIds.putAll(osmMatchingCacheRepository.get());
		}
		return hasLoaded;
	}
}
