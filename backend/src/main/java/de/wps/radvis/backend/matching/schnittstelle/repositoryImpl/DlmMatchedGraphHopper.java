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

import de.wps.radvis.backend.matching.domain.repository.DlmMatchingCacheRepository;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.SeitenbezogeneProfilEigenschaften;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DlmMatchedGraphHopper extends GraphHopper {

	@Getter
	private final Map<Integer, OsmWayId> graphHopperEdgesAufOsmWays;

	@Getter
	private final Map<Integer, SeitenbezogeneProfilEigenschaften> graphHopperEdgesAufProfilEigenschaften;

	private final DlmMatchingCacheRepository dlmMatchingCacheRepository;

	public DlmMatchedGraphHopper(DlmMatchingCacheRepository dlmMatchingCacheRepository) {
		super();
		require(dlmMatchingCacheRepository, notNullValue());

		this.graphHopperEdgesAufOsmWays = new HashMap<>();
		this.graphHopperEdgesAufProfilEigenschaften = new HashMap<>();

		this.dlmMatchingCacheRepository = dlmMatchingCacheRepository;

		this.setElevation(true);
		this.setEncodedValueFactory(new CustomEncodedValueFactory());
	}

	@Override
	protected void importOSM() {
		if (getOSMFile() == null)
			throw new IllegalStateException("Couldn't load from existing folder: " + getGraphHopperLocation()
				+ " but also cannot use file for DataReader as it wasn't specified!");

		log.info("start creating graph from {}.", getOSMFile());
		OsmWayReader reader = new OsmWayReader(
			getGraphHopperStorage(),
			graphHopperEdgesAufOsmWays,
			graphHopperEdgesAufProfilEigenschaften,
			// wir benötigen bislang keine lin. Ref. beim Dlm-Matching. Wir übergeben null damit diese nicht unnötig berechnet werden.
			null
		)
			.setFile(_getOSMFile())
			.setWorkerThreads(getWorkerThreads())
			// Verhindert Simplification; wird standardmäßig über getWayPointMaxDistance gesetzt.
			// https://discuss.graphhopper.com/t/map-matching-pillow-nodes-missing-in-matchresult/3142/2
			.setWayPointMaxDistance(0)
			.setWayPointElevationMaxDistance(getRouterConfig().getElevationWayPointMaxDistance())
			.setElevationProvider(this.getElevationProvider())
			.setSmoothElevation(false)
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
		dlmMatchingCacheRepository.save(graphHopperEdgesAufOsmWays, graphHopperEdgesAufProfilEigenschaften);

		/*
		 Es kann sein, dass Teile der Geometrien außerhalb des Graphen (also dem "GraphHopperStorage") vom Graphhopper
		 liegen. Das liegt daran, dass dieser Graph nur Tower-Nodes enthält, also Keuzungspunkte. Die ganzen anderen
		 Nodes eines OSM-Ways werden hier nicht direkt gespeichert. Wenn man also eine PBF mit nur einem U-förmigen
		 Way hat, dann liegt der untere Teil vom "U" außerhalb des Graphen. Das führt beim Matching zu Problemen, da
		 der GraphHopper viel auf die BBox des Graphen prüft. Als Fix erweitern wir hier die BBox in jede Richtung um
		 >100km (in BW sind 1° Lat ca. 110 km und 1° Lon ca. 70 km), sodass sichergestellt ist, dass alle Teile der
		 Geometrien auch innerhalb der BBox liegen.
		*/
		getGraphHopperStorage().getBounds().minLat -= 1.0;
		getGraphHopperStorage().getBounds().minLon -= 2.0;
		getGraphHopperStorage().getBounds().maxLat += 1.0;
		getGraphHopperStorage().getBounds().maxLon += 2.0;
	}

	@Override
	public boolean load(String graphHopperFolder) {
		boolean hasLoaded = super.load(graphHopperFolder);
		if (hasLoaded) {
			if (!dlmMatchingCacheRepository.hasCache()) {
				throw new RuntimeException(
					"Die Datei für den OsmMatchingCache ist nicht vorhanden. Bitte den GrapHopper-Cache löschen unter "
						+ getGraphHopperLocation());
			}
			// Die Verwendung von clear hilft bei der Einführung der neuen zusätzlichen Cache-Datei.
			graphHopperEdgesAufOsmWays.clear();
			graphHopperEdgesAufOsmWays.putAll(dlmMatchingCacheRepository.getWayIds());
			graphHopperEdgesAufProfilEigenschaften.clear();
			graphHopperEdgesAufProfilEigenschaften.putAll(dlmMatchingCacheRepository.getProfilEigenschaften());
		}
		return hasLoaded;
	}

	@Override
	public void clean() {
		super.clean();
		dlmMatchingCacheRepository.deleteAll();
	}
}
