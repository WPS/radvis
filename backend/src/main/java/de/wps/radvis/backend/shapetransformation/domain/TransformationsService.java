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

package de.wps.radvis.backend.shapetransformation.domain;

import java.util.stream.Stream;

import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import de.wps.radvis.backend.shapetransformation.domain.valueObject.TransformationsKonfiguration;

public class TransformationsService {

	public Stream<SimpleFeature> transformiere(Stream<SimpleFeature> zuTransformierendeFeatures,
		TransformationsKonfiguration konfiguration) {
		return zuTransformierendeFeatures.map(
			simpleFeature -> transformiereFeature(simpleFeature, konfiguration));
	}

	private SimpleFeature transformiereFeature(SimpleFeature quellFeature,
		TransformationsKonfiguration konfiguration) {
		SimpleFeatureType newType = createDerivedFeatureType(quellFeature, konfiguration);

		SimpleFeature resultFeature = DataUtilities.reType(newType, quellFeature);

		konfiguration.getQuellAttributNamen().forEach(quellAttributName -> {
			String zielAttributName = konfiguration.getZielAttributName(quellAttributName);
			Object featureAttributWert = quellFeature.getAttribute(quellAttributName);
			String quellAttributWert = featureAttributWert != null ? featureAttributWert.toString() : null;
			if (konfiguration.hasQuellAttributWert(quellAttributName, quellAttributWert)) {
				String zielAttributWert = konfiguration.getZielAttributWert(quellAttributName, quellAttributWert);
				resultFeature.setAttribute(zielAttributName, zielAttributWert);
			} else {
				resultFeature.setAttribute(zielAttributName, quellAttributWert);
			}
		});

		return resultFeature;

	}

	private SimpleFeatureType createDerivedFeatureType(SimpleFeature quellFeature,
		TransformationsKonfiguration konfiguration) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.init(quellFeature.getFeatureType());
		for (String quellAttributName : konfiguration.getQuellAttributNamen()) {
			if (builder.get(quellAttributName) != null) {
				builder.remove(quellAttributName);
			}
		}
		for (String zielAttributName : konfiguration.getZielAttributNamen()) {
			builder.add(zielAttributName, String.class);
		}
		SimpleFeatureType newType = builder.buildFeatureType();
		return newType;
	}

}
