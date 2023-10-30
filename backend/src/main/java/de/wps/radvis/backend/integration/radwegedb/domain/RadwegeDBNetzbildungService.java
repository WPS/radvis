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

package de.wps.radvis.backend.integration.radwegedb.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.netzbildung.domain.AbstractNetzbildungService;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.GeometrieTypNichtUnterstuetztException;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.StartUndEndpunktGleichException;
import de.wps.radvis.backend.integration.radwegedb.domain.entity.RadwegeDBNetzbildungStatistik;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import lombok.NonNull;

public class RadwegeDBNetzbildungService extends AbstractNetzbildungService {

	private RadwegeDBNetzbildungProtokollService radwegeDBNetzbildungProtokollService;
	private RadwegeDBAttributMapper attributMapper;

	public RadwegeDBNetzbildungService(NetzService netzService,
		@NonNull RadwegeDBNetzbildungProtokollService radwegeDBNetzbildungProtokollService,
		RadwegeDBAttributMapper attributMapper,
		EntityManager entityManager) {
		super(netzService, entityManager);
		this.attributMapper = attributMapper;
		this.radwegeDBNetzbildungProtokollService = radwegeDBNetzbildungProtokollService;
	}

	@Override
	protected QuellSystem getQuelle() {
		return QuellSystem.RadwegeDB;
	}

	public RadwegeDBNetzbildungStatistik bildeRadwegeDBNetz(Stream<ImportedFeature> importedFeaturesStrecken)
		throws MissingResourceException {
		require(importedFeaturesStrecken, notNullValue());

		initKnotenIndex();
		RadwegeDBNetzbildungStatistik statistik = new RadwegeDBNetzbildungStatistik();
		importedFeaturesStrecken.forEach(feature -> {
			verarbeiteFeature(statistik, feature);
		});
		return statistik;
	}

	private void verarbeiteFeature(RadwegeDBNetzbildungStatistik statistik, ImportedFeature feature) {
		if (sollFeatureUebernommenWerden(feature)) {
			String geometryType = feature.getGeometrie().getGeometryType();
			if (geometryType.equals(Geometry.TYPENAME_MULTILINESTRING)) {
				try {
					if (feature.getGeometrie().getNumGeometries() == 1
						&& feature.getGeometrie().getGeometryN(0).getGeometryType()
						.equals(Geometry.TYPENAME_LINESTRING)) {
						addImportedFeatureStrecke(feature);
						statistik.anzahlKantenErstellt++;
					} else {
						radwegeDBNetzbildungProtokollService
							.handle(new MultilinestringHatMehrereLinestringsException(feature.getTechnischeId(),
								feature.getGeometrie()), RadwegeDBNetzbildungJob.class.getSimpleName());
						statistik.multilinestringHatMehrereLinestrings++;
					}
				} catch (StartUndEndpunktGleichException e) {
					radwegeDBNetzbildungProtokollService.handle(e, RadwegeDBNetzbildungJob.class.getSimpleName());
				}
			} else {
				radwegeDBNetzbildungProtokollService.handle(new GeometrieTypNichtUnterstuetztException(feature.getTechnischeId(),
					feature.getGeometrie()), RadwegeDBNetzbildungJob.class.getSimpleName());
				statistik.geometrieTypNichtUnterstuetzt++;
			}
		} else {
			statistik.anzahlSollenNichtUbernommenWerden++;
		}
	}

	private void addImportedFeatureStrecke(ImportedFeature importedFeature) throws StartUndEndpunktGleichException {
		require(importedFeature, notNullValue());

		LineString lineString = (LineString) importedFeature.getGeometrie().getGeometryN(0);

		KantenAttribute kantenAttribute = attributMapper.mapKantenAttribute(importedFeature);
		ZustaendigkeitAttribute zustaendigkeitAttribute = attributMapper
			.mapZustaendigkeitAttribute(importedFeature);
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = new FahrtrichtungAttributGruppe(
			attributMapper.mapRichtungAttribute(importedFeature), false);
		GeschwindigkeitAttribute geschwindigkeitAttribute = attributMapper.mapGeschwindigkeitAttribute(importedFeature);
		FuehrungsformAttribute fuehrungsformAttribute = attributMapper.mapFuehrungsformAttribute(importedFeature);

		addKante(lineString,
			new KantenAttributGruppe(
				kantenAttribute,
				attributMapper.mapNetzKlassen(importedFeature),
				attributMapper.mapIstStandard(importedFeature)
			),
			fahrtrichtungAttributGruppe,
			new ZustaendigkeitAttributGruppe(new ArrayList<>(List.of(zustaendigkeitAttribute))),
			GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(List.of(geschwindigkeitAttribute)).build(),
			new FuehrungsformAttributGruppe(new ArrayList<>(List.of(fuehrungsformAttribute)), false), null,
			importedFeature.getTechnischeId());

	}

	public boolean sollFeatureUebernommenWerden(ImportedFeature feature) {
		if (!feature.hasAttribut("radverkehr")) {
			return false;
		}
		return feature.getAttribut("radverkehr").toString().equals("1");
	}

}
