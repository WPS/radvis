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

package de.wps.radvis.backend.integration.dlm.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.netzbildung.domain.AbstractNetzbildungService;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.GeometrieTypNichtUnterstuetztException;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.StartUndEndpunktGleichException;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.domain.entity.DLMNetzbildungJobStatistik;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DLMNetzbildungService extends AbstractNetzbildungService {

	private DLMNetzbildungProtokollService dlmNetzbildungProtokollService;
	private DLMAttributMapper attributMapper;
	private int counter;

	public DLMNetzbildungService(DLMNetzbildungProtokollService dlmNetzbildungProtokollService, NetzService netzService,
		EntityManager entityManager) {
		super(netzService, entityManager);
		this.dlmNetzbildungProtokollService = dlmNetzbildungProtokollService;
		this.attributMapper = new DLMAttributMapper();
	}

	@Override
	protected QuellSystem getQuelle() {
		return QuellSystem.DLM;
	}

	@Transactional
	public void bildeDLMBasisNetz(Stream<ImportedFeature> importedFeatures) {
		counter = 0;
		DLMNetzbildungJobStatistik statistik = new DLMNetzbildungJobStatistik();

		initKnotenIndex();

		importedFeatures.forEach(feature -> {
			try {
				addImportedFeature(feature);
			} catch (GeometrieTypNichtUnterstuetztException e) {
				statistik.nichtunterstuetzterGeometrietyp++;
				dlmNetzbildungProtokollService.handle(e, DLMNetzbildungJob.class.getSimpleName());
			} catch (StartUndEndpunktGleichException e) {
				statistik.startUndEndpunktGleich++;
				dlmNetzbildungProtokollService.handle(e, DLMNetzbildungJob.class.getSimpleName());
			}

			counter++;
			if (counter % 1000 == 0) {
				log.info(counter + " Features in Netz eingelesen");
			}
			if (counter % 10000 == 0) {
				entityManager.flush();
				entityManager.clear();
			}
		});
		statistik.verarbeiteteFeatures = counter;
	}

	private void addImportedFeature(ImportedFeature importedFeature)
		throws GeometrieTypNichtUnterstuetztException, StartUndEndpunktGleichException {
		Geometry geometry = importedFeature.getGeometrie();
		if (!geometry.getGeometryType().equals(Geometry.TYPENAME_LINESTRING)) {
			throw new GeometrieTypNichtUnterstuetztException(importedFeature.getTechnischeId(),
				geometry);
		}
		KantenAttributGruppe kantenAttributGruppe = attributMapper.mapKantenAttributGruppe(importedFeature);

		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = attributMapper.mapFahrtrichtungAttributGruppe(
			importedFeature);
		ZustaendigkeitAttribute zustaendigkeitAttribute = attributMapper.mapZustaendigkeitAttribute(importedFeature);
		FuehrungsformAttribute fuehrungsformAttribute = attributMapper.mapFuehrungsformAttribute(importedFeature);
		GeschwindigkeitAttribute geschwindigkeitAttribute = attributMapper.mapGeschindigkeitAttribute(importedFeature);

		addKante((LineString) geometry, kantenAttributGruppe,
			fahrtrichtungAttributGruppe,
			new ZustaendigkeitAttributGruppe(new ArrayList<>(List.of(zustaendigkeitAttribute))),
			GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(List.of(geschwindigkeitAttribute)).build(),
			new FuehrungsformAttributGruppe(new ArrayList<>(List.of(fuehrungsformAttribute)), false),
			DlmId.of(importedFeature.getTechnischeId()),
			importedFeature.getTechnischeId(), true);
	}

}
