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

package de.wps.radvis.backend.integration.radnetz.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.data.util.Pair;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.netzbildung.domain.AbstractNetzbildungService;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.GeometrieTypNichtUnterstuetztException;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.StartUndEndpunktGleichException;
import de.wps.radvis.backend.integration.radnetz.domain.entity.RadNetzNetzbildungStatistik;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.NonNull;

public class RadNetzNetzbildungService extends AbstractNetzbildungService {

	private RadNetzNetzbildungProtokollService radNetzNetzbildungProtokollService;
	private RadNetzAttributMapper attributMapper;

	private Set<Pair<LineString, KantenAttribute>> kantenIndex;

	public RadNetzNetzbildungService(NetzService netzService,
		@NonNull RadNetzNetzbildungProtokollService radNetzNetzbildungProtokollService,
		RadNetzAttributMapper attributMapper,
		EntityManager entityManager) {
		super(netzService, entityManager);
		this.attributMapper = attributMapper;
		this.radNetzNetzbildungProtokollService = radNetzNetzbildungProtokollService;
	}

	@Override
	protected QuellSystem getQuelle() {
		return QuellSystem.RadNETZ;
	}

	@Transactional
	public void bildeRadNetz(Stream<ImportedFeature> importedFeaturesStrecken,
		Stream<ImportedFeature> importedFeaturesPunkte, RadNetzNetzbildungStatistik statistik)
		throws MissingResourceException {
		require(importedFeaturesStrecken, notNullValue());
		require(importedFeaturesPunkte, notNullValue());

		this.kantenIndex = new HashSet<>();
		initKnotenIndex();

		importedFeaturesStrecken.forEach(feature -> {
			if (isAktuell(feature)) {
				String geometryType = feature.getGeometrie().getGeometryType();
				if (geometryType.equals(Geometry.TYPENAME_LINESTRING)) {
					try {
						addImportedFeatureStrecke(feature);
					} catch (StartUndEndpunktGleichException e) {
						radNetzNetzbildungProtokollService.handle(e, RadNETZNetzbildungJob.class.getSimpleName());
					}
				} else {
					radNetzNetzbildungProtokollService
						.handle(new GeometrieTypNichtUnterstuetztException(feature.getTechnischeId(),
								feature.getGeometrie()),
							RadNETZNetzbildungJob.class.getSimpleName());
				}
			} else {
				statistik.anzahlVeraltet++;
			}
		});

		importedFeaturesPunkte.forEach(feature -> {
			if (isAktuell(feature)) {
				statistik.anzahlKnotenpunkte++;
				String geometryType = feature.getGeometrie().getGeometryType();
				if (geometryType.equals(Geometry.TYPENAME_POINT)) {
					if (isKnotenpunkt(feature))
						try {
							addImportedFeaturePunkt(feature);
							statistik.anzahlAbgebildeterKnotenpunkte++;
						} catch (KnotenMatchingException e) {
							radNetzNetzbildungProtokollService.handle(e);
						}
				} else {
					radNetzNetzbildungProtokollService.handle(
						new GeometrieTypNichtUnterstuetztException(
							"Feature: " + feature.getTechnischeId() + ". Typ: " + geometryType,
							feature.getGeometrie()),
						RadNETZNetzbildungJob.class.getSimpleName());
				}
			} else {
				statistik.anzahlVeraltet++;
			}
		});
	}

	private boolean isKnotenpunkt(ImportedFeature feature) {
		Object aufnTYP = feature.getAttribute().get("AufnTYP");
		return aufnTYP != null && (aufnTYP.equals("Knotenpunkt") || aufnTYP
			.equals("Knotenpunkt derzeit im Umbau"));
	}

	private void addImportedFeatureStrecke(ImportedFeature importedFeature) throws StartUndEndpunktGleichException {
		require(importedFeature, notNullValue());

		LineString lineString = (LineString) importedFeature.getGeometrie();

		KantenAttribute kantenAttribute = attributMapper.mapKantenAttribute(importedFeature);
		ZustaendigkeitAttribute zustaendigkeitAttribute = attributMapper
			.mapZustaendigkeitAttribute();
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = attributMapper.mapFahrtrichtungAttributGruppe(
			importedFeature);
		GeschwindigkeitAttribute geschwindigkeitAttribute = attributMapper.mapGeschwindigkeitAttribute(importedFeature);
		FuehrungsformAttribute fuehrungsformAttribute = attributMapper.mapFuehrungsformAttribute(importedFeature);
		Set<Netzklasse> netzKlassen = attributMapper.mapNetzKlassen(importedFeature);
		Set<IstStandard> istStandards = attributMapper.mapIstStandards(importedFeature);

		if (!isKanteInIndex(lineString, kantenAttribute)) {
			addKante(lineString, new KantenAttributGruppe(kantenAttribute, netzKlassen, istStandards),
				fahrtrichtungAttributGruppe,
				new ZustaendigkeitAttributGruppe(new ArrayList<>(List.of(zustaendigkeitAttribute))),
				GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(List.of(geschwindigkeitAttribute))
					.build(),
				new FuehrungsformAttributGruppe(new ArrayList<>(List.of(fuehrungsformAttribute)), false), null,
				importedFeature.getTechnischeId());
			addToIndex(lineString, kantenAttribute);
		}
	}

	private void addImportedFeaturePunkt(ImportedFeature importedFeature) throws KnotenMatchingException {
		require(importedFeature, notNullValue());

		Point point = (Point) importedFeature.getGeometrie();
		Optional<Knoten> knotenOptional = findeKnoten(point);
		if (knotenOptional.isPresent()) {
			Knoten knoten = knotenOptional.get();
			KnotenAttribute knotenAttribute = attributMapper
				.uebersetzePunktAttributeNachRadVis(importedFeature);
			if (!knoten.getKnotenAttribute().istLeer()) {
				radNetzNetzbildungProtokollService.handle(new KnotenDuplikatException(knoten.getPoint()));
			}
			knoten.setKnotenAttribute(knotenAttribute);
		} else {
			throw new KnotenMatchingException(importedFeature.getTechnischeId(), importedFeature.getGeometrie());
		}
	}

	public boolean isAktuell(ImportedFeature feature) {
		Object veraltetProperty = feature.getAttribute().get("Status_WW");
		if (veraltetProperty == null) {
			veraltetProperty = feature.getAttribute().get("STATUS_WW"); // es gibt beide Schreibweisen...
		}
		/*
		 * Status_WW kann folgende Werte haben: 1: Ist ein altes Feature aber noch aktuell und wird übernommen 2: Ist
		 * ein veraltetes Feature und wird nicht übernommen 3: Ist ein neues Feature und soll übernommen werden. Daher
		 * soll alles was nicht 2 ist übenrommen werden
		 */
		if (veraltetProperty == null) {
			radNetzNetzbildungProtokollService.handle(
				new VeraltetStatusMappingException(
					"Feature mit der technischen ID" + feature.getTechnischeId() + " hat kein 'Status_WW'",
					feature.getGeometrie()));
			return true;
		}
		if (veraltetProperty instanceof Double) {
			return !veraltetProperty.equals(2.0);
		} else if (veraltetProperty instanceof Integer) {
			return !veraltetProperty.equals(2);
		} else if (veraltetProperty instanceof String) {
			return !veraltetProperty.equals("2");
		} else {
			radNetzNetzbildungProtokollService.handle(
				new VeraltetStatusMappingException(
					"Feature mit der technischen ID" + feature.getTechnischeId()
						+ " hat einen Typ von 'Status_WW', der nicht unterstützt wird:"
						+ veraltetProperty.getClass().toString(),
					feature.getGeometrie()));
			return true;
		}
	}

	private void addToIndex(LineString lineString, KantenAttribute kantenAttribute) {
		kantenIndex.add(Pair.of(lineString, kantenAttribute));
	}

	private boolean isKanteInIndex(LineString lineString, KantenAttribute kantenAttribute) {
		return kantenIndex.contains(Pair.of(lineString, kantenAttribute));
	}
}
