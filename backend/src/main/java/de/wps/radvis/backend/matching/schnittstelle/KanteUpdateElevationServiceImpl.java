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

import org.geolatte.geom.C2D;
import org.geolatte.geom.PositionSequence;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.data.domain.Slice;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.matching.domain.KanteUpdateElevationService;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.LGLElevationProviderRepository;
import de.wps.radvis.backend.netz.domain.entity.KanteElevationView;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.KanteElevationUpdate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KanteUpdateElevationServiceImpl implements KanteUpdateElevationService {
	private final KantenRepository kantenRepository;

	private final String elevationCacheVerzeichnis;
	private final String tiffTilesVerzeichnis;

	public KanteUpdateElevationServiceImpl(
		KantenRepository kantenRepository,
		String elevationCacheVerzeichnis,
		String tiffTilesVerzeichnis) {
		this.kantenRepository = kantenRepository;
		this.elevationCacheVerzeichnis = elevationCacheVerzeichnis;
		this.tiffTilesVerzeichnis = tiffTilesVerzeichnis;
	}

	@Override
	public void updateElevations() {
		LGLElevationProviderRepository repository = new LGLElevationProviderRepository(
			elevationCacheVerzeichnis,
			tiffTilesVerzeichnis);

		int pageSize = 10000;
		int totalKanten = kantenRepository.countAllByQuelleDLMOrQuelleRadVisAndOutdated3dGeometry();
		int numPages = (totalKanten + pageSize - 1) / pageSize;
		int numKantenVerarbeitet = 0;
		for (int page = 0; page < numPages; page++) {
			log.info("Bearbeite Page {}/{} ", page + 1, numPages);

			// Immer die ersten 10 000 Kanten holen, die diese Bedingung in der Query erfüllen.
			// Am Ende werden sie Angepasst und abgespeichert -> erfüllen dann nicht mehr die Bedingung
			// Und andere 10 000 Kanten werden zurückgegeben.
			Slice<KanteElevationView> kantenOutdatedOrNo3dGeometry = kantenRepository.findFirst10ThousandByQuelleDLMOrQuelleRadVisAndOutdated3dGeometry();
			if (kantenOutdatedOrNo3dGeometry.isEmpty()) {
				log.info("In dieser Page sind keine Kanten mit veralteter oder nicht vorhandener 3d Geometrie");
				continue;
			}
			Slice<KanteElevationUpdate> kanteElevationInserts = kantenOutdatedOrNo3dGeometry.map(kanteElevationView -> {
					PositionSequence<C2D> positions = kanteElevationView.getGeometry().getPositions();
					LineString lineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
						.createLineString(positions.stream()
							.map(
								coordinate -> new Coordinate(coordinate.getX(), coordinate.getY(),
									repository.getEle(new Coordinate(coordinate.getX(), coordinate.getY()))))
							.toArray(Coordinate[]::new));

					return new KanteElevationUpdate(kanteElevationView.getId(), lineString);
				}
			);
			kantenRepository.updateKanteElevation(kanteElevationInserts);
			numKantenVerarbeitet += kanteElevationInserts.getNumberOfElements();
			log.info("{}/{} Kanten verarbeitet", numKantenVerarbeitet, totalKanten);
		}

		// Sanity check:
		int kantenDieNichtGeupdatedWurden = kantenRepository.countAllByQuelleDLMOrQuelleRadVisAndOutdated3dGeometry();
		if (kantenDieNichtGeupdatedWurden != 0) {
			log.error("Es sind noch DLM-/RadVIS-Kanten über, die keine 3D-Geometrie bekommen haben.");
			log.error("Das sollte nie passieren!");
		} else {
			log.info("Update Elevations erfolgreich durchgeführt:");
			log.info("Insgesamt wurden {} Kanten verarbeitet.", numKantenVerarbeitet);
		}
	}
}
