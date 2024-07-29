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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity;

import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedFeature;
import lombok.Getter;

public class NormalisierteMappedFeatures {

	@Getter
	private final List<LinearReferenzierterAbschnitt> lineareReferenzenNormalizedAndSorted;

	private final Map<LinearReferenzierterAbschnitt, List<MappedFeature>> lineareReferenzToMappedFeature;

	private NormalisierteMappedFeatures(List<MappedFeature> features) {
		this.lineareReferenzenNormalizedAndSorted = normalisiereLineareReferenzen(
			features.stream()
				.map(MappedFeature::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));

		this.lineareReferenzToMappedFeature = new HashMap<>();

		for (LinearReferenzierterAbschnitt linearReferenzierterAbschnitt : this.lineareReferenzenNormalizedAndSorted) {
			List<MappedFeature> matchingFeatures = features
				.stream()
				// keine Punktintersection ist an dieser Stelle wichtig
				.filter(
					feature -> linearReferenzierterAbschnitt.intersection(feature.getLinearReferenzierterAbschnitt())
						.isPresent())
				.collect(Collectors.toList());
			this.lineareReferenzToMappedFeature.put(linearReferenzierterAbschnitt, matchingFeatures);
		}
	}

	/**
	 * @param lineareReferenzen
	 *     Liste and Linearen Referenzen die sich potentiell überlappen
	 * @return Konsolidierte List an Linearen Referenzen die sich nicht mehr überlappen
	 */
	private List<LinearReferenzierterAbschnitt> normalisiereLineareReferenzen(
		List<LinearReferenzierterAbschnitt> lineareReferenzen) {
		if (lineareReferenzen.isEmpty()) {
			return Collections.emptyList();
		}

		List<Double> metermarken = new ArrayList<>();

		metermarken.add(.0);
		metermarken.add(1.);

		for (LinearReferenzierterAbschnitt linearReferenzierterAbschnitt : lineareReferenzen) {
			metermarken.add(linearReferenzierterAbschnitt.getVonValue());
			metermarken.add(linearReferenzierterAbschnitt.getBisValue());
		}

		metermarken.sort(Double::compareTo);
		double previousDouble = metermarken.get(0);
		List<LinearReferenzierterAbschnitt> normalisierteLineareReferenzen = new ArrayList<>();
		for (int i = 1; i < metermarken.size(); i++) {
			Double currentDouble = metermarken.get(i);
			if (currentDouble != previousDouble) {
				normalisierteLineareReferenzen.add(LinearReferenzierterAbschnitt.of(previousDouble, currentDouble));
				previousDouble = currentDouble;
			} else if (i == metermarken.size() - 1) {
				int finalIndex = normalisierteLineareReferenzen.size() - 1;
				normalisierteLineareReferenzen
					.set(finalIndex, LinearReferenzierterAbschnitt.of(
						normalisierteLineareReferenzen.get(finalIndex).getVonValue(), 1.));
			}
		}
		return normalisierteLineareReferenzen;
	}

	public static NormalisierteMappedFeatures of(List<MappedFeature> features) {
		return new NormalisierteMappedFeatures(features);
	}

	public List<MappedFeature> getFeaturesFor(LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(this.lineareReferenzToMappedFeature.containsKey(linearReferenzierterAbschnitt),
			"Die LineareReferenz muss in der sortierten und normalisierten Liste vorhanden sein");
		return this.lineareReferenzToMappedFeature.get(linearReferenzierterAbschnitt);
	}
}
