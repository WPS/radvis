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

package de.wps.radvis.backend.integration.grundnetzReimport.domain;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.grundnetz.domain.DLMAttributMapper;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.AbstractDLMImportStatistik;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.KnotenTupel;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.StartUndEndpunktGleichException;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateKantenService {

	private final DLMAttributMapper dlmAttributMapper;
	private final NetzService netzService;
	private final FindKnotenFromIndexService findKnotenFromIndexService;

	public CreateKantenService(DLMAttributMapper dlmAttributMapper, NetzService netzService,
		FindKnotenFromIndexService findKnotenFromIndexService) {
		this.dlmAttributMapper = dlmAttributMapper;
		this.netzService = netzService;
		this.findKnotenFromIndexService = findKnotenFromIndexService;
	}

	public void createNewDLMKante(ImportedFeature importedFeature, AbstractDLMImportStatistik dlmImportJobStatistik,
		KnotenIndex knotenIndex) {
		Geometry geometry = importedFeature.getGeometrie();
		if (!geometry.getGeometryType().equals(Geometry.TYPENAME_LINESTRING)) {
			dlmImportJobStatistik.nichtunterstuetzterGeometrietyp++;
			return;
		}
		LineString lineString = (LineString) geometry;

		KantenAttributGruppe kantenAttributGruppe = dlmAttributMapper.mapKantenAttributGruppe(importedFeature);
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = dlmAttributMapper.mapFahrtrichtungAttributGruppe(
			importedFeature);
		ZustaendigkeitAttribute zustaendigkeitAttribute = dlmAttributMapper.mapZustaendigkeitAttribute(importedFeature);
		FuehrungsformAttribute fuehrungsformAttribute = dlmAttributMapper.mapFuehrungsformAttribute(importedFeature);
		GeschwindigkeitAttribute geschwindigkeitAttribute = dlmAttributMapper.mapGeschindigkeitAttribute(
			importedFeature);

		Point vonPoint = lineString.getStartPoint();
		Point nachPoint = lineString.getEndPoint();

		try {
			KnotenTupel knotenTupel = this.findKnotenFromIndexService.findOrCreateKnotenTupel(
				vonPoint, nachPoint, knotenIndex);

			Kante kante = new Kante(DlmId.of(importedFeature.getTechnischeId()),
				importedFeature.getTechnischeId(),
				knotenTupel.vonKnoten,
				knotenTupel.nachKnoten,
				lineString,
				false,
				QuellSystem.DLM,
				kantenAttributGruppe,
				fahrtrichtungAttributGruppe,
				new ZustaendigkeitAttributGruppe(new ArrayList<>(List.of(zustaendigkeitAttribute))),
				GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(List.of(geschwindigkeitAttribute))
					.build(),
				new FuehrungsformAttributGruppe(new ArrayList<>(List.of(fuehrungsformAttribute)), false));
			kante.setGrundnetz(true);
			netzService.saveKante(kante);
			dlmImportJobStatistik.neueKanteHinzugefuegt++;
			log.debug("Kante zwischen Punkten {}, {} hinzugefügt", knotenTupel.vonKnoten.getPoint(),
				knotenTupel.vonKnoten.getPoint());
		} catch (StartUndEndpunktGleichException e) {
			dlmImportJobStatistik.startUndEndpunktGleich++;
		}

	}

}
