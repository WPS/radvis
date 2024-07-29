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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.Haendigkeit;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributes;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedFeature;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import lombok.Getter;

public class KantenMapping {
	private static final double DIFFERENZ_IN_HAENDIGKEIT_WAHRSCHEINLICHKEITEN_FUER_SEITENZUORDNUNG = 0.35;
	private static final double DIFFERENZ_IN_HAENDIGKEIT_WAHRSCHEINLICHKEITEN_FUER_SEITENBEZUG = 0.5;

	@Getter
	private final Kante kante;
	private final List<MappedFeature> mappedFeatures;

	// diese Felder dienen als lazy Zwischenspeicher von Berechnungen
	private Optional<List<MappedAttributes>> mappedAttributes;
	private Optional<NormalisierteMappedFeatures> normalisierteMappedAttributes;
	private Optional<List<MappedAttributes>> normalisierteMappedAttributesLinks;
	private Optional<List<MappedAttributes>> normalisierteMappedAttributesRechts;

	private Optional<Boolean> habenFeaturesGeometrischenSeitenbezug;
	private Optional<Boolean> habenFeaturesSeitenAttribut;

	public KantenMapping(Kante kante) {
		require(kante.getQuelle().equals(QuellSystem.DLM) || kante.getQuelle().equals(QuellSystem.RadVis));
		require(kante.getId(), notNullValue());

		this.kante = kante;
		this.mappedFeatures = new ArrayList<>();
		this.mappedAttributes = Optional.empty();
		this.normalisierteMappedAttributes = Optional.empty();
		this.normalisierteMappedAttributesLinks = Optional.empty();
		this.normalisierteMappedAttributesRechts = Optional.empty();
		this.habenFeaturesGeometrischenSeitenbezug = Optional.empty();
		this.habenFeaturesSeitenAttribut = Optional.empty();
	}

	public void add(MappedFeature mappedFeature) {
		require(mappedFeature, notNullValue());
		mappedFeatures.add(mappedFeature);
	}

	public List<MappedAttributes> getMappedAttributes() {
		if (mappedAttributes.isEmpty()) {
			mappedAttributes = Optional.of(
				mappedFeatures.stream().map(mappedFeature -> MappedAttributes.of(mappedFeature,
					unterscheidetSichOrientierungZurKante(mappedFeature.getGeometry())))
					.collect(Collectors.toList()));
		}
		return mappedAttributes.get();
	}

	private NormalisierteMappedFeatures getNormalisierteMappedFeatures() {
		if (normalisierteMappedAttributes.isEmpty()) {
			normalisierteMappedAttributes = Optional.of(NormalisierteMappedFeatures.of(mappedFeatures));
		}
		return normalisierteMappedAttributes.get();
	}

	public List<MappedAttributes> getNormalizedMappedAttributes() {

		List<LinearReferenzierterAbschnitt> lineareReferenzen = getNormalisierteMappedFeatures()
			.getLineareReferenzenNormalizedAndSorted();

		List<MappedAttributes> result = new ArrayList<>();

		for (LinearReferenzierterAbschnitt linearReferenzierterAbschnitt : lineareReferenzen) {
			getNormalisierteMappedFeatures().getFeaturesFor(linearReferenzierterAbschnitt).stream()
				.map(mappedFeature -> MappedAttributes.of(mappedFeature.getProperties(), linearReferenzierterAbschnitt,
					Seitenbezug.BEIDSEITIG,
					unterscheidetSichOrientierungZurKante(mappedFeature.getGeometry())))
				.forEach(result::add);
		}
		return result;
	}

	public List<MappedAttributes> getNormalizedMappedAttributesLinks() {
		require(habenFeaturesSeitenbezug());
		if (normalisierteMappedAttributesLinks.isEmpty()) {
			normalisierteMappedAttributesLinks = Optional.of(
				getNormalizedMappedAttributesFuerSeitenbezug(this.getNormalisierteMappedFeatures(), Seitenbezug.LINKS)
			);
		}

		return normalisierteMappedAttributesLinks.get();
	}

	public List<MappedAttributes> getNormalizedMappedAttributesRechts() {
		require(habenFeaturesSeitenbezug());
		if (normalisierteMappedAttributesRechts.isEmpty()) {
			normalisierteMappedAttributesRechts = Optional.of(
				getNormalizedMappedAttributesFuerSeitenbezug(this.getNormalisierteMappedFeatures(), Seitenbezug.RECHTS)
			);
		}

		return normalisierteMappedAttributesRechts.get();
	}

	public boolean habenFeaturesSeitenbezug() {
		return habenFeaturesGeometrischenSeitenbezug() || habenFeaturesAttributiellenSeitenbezug();
	}

	public boolean habenFeaturesGeometrischenSeitenbezug() {
		if (habenFeaturesGeometrischenSeitenbezug.isEmpty()) {
			habenFeaturesGeometrischenSeitenbezug = Optional.of(berechneMuessenFeaturesSeitenbezogenGemapptWerden());
		}

		return habenFeaturesGeometrischenSeitenbezug.get();
	}

	private boolean berechneMuessenFeaturesSeitenbezogenGemapptWerden() {
		NormalisierteMappedFeatures normalisierteMappedFeatures = getNormalisierteMappedFeatures();

		List<LinearReferenzierterAbschnitt> lineareReferenzen = normalisierteMappedFeatures
			.getLineareReferenzenNormalizedAndSorted();
		double confidenceInSeitenbezug = 0.;
		for (LinearReferenzierterAbschnitt linearReferenzierterAbschnitt : lineareReferenzen) {
			List<MappedFeature> matchingAttribute = normalisierteMappedFeatures.getFeaturesFor(
				linearReferenzierterAbschnitt);
			if (matchingAttribute.isEmpty()) {
				continue;
			}

			matchingAttribute.sort(Comparator.comparing(MappedFeature::getHaendigkeit,
				Haendigkeit.vonLinksNachRechts));

			MappedFeature leftMost = matchingAttribute.get(0);
			MappedFeature rightMost = matchingAttribute.get(matchingAttribute.size() - 1);

			double differenz = Math.abs(
				leftMost.getHaendigkeit().getVorzeichenbehafteteWahrscheinlichkeit() - rightMost.getHaendigkeit()
					.getVorzeichenbehafteteWahrscheinlichkeit());

			confidenceInSeitenbezug += differenz * linearReferenzierterAbschnitt.relativeLaenge();
		}
		return confidenceInSeitenbezug > DIFFERENZ_IN_HAENDIGKEIT_WAHRSCHEINLICHKEITEN_FUER_SEITENBEZUG;
	}

	public boolean habenFeaturesAttributiellenSeitenbezug() {
		if (habenFeaturesSeitenAttribut.isEmpty()) {
			habenFeaturesSeitenAttribut = Optional.of(habenFeaturesEinSeitenAttribut());
		}

		return habenFeaturesSeitenAttribut.get();
	}

	private boolean habenFeaturesEinSeitenAttribut() {
		return mappedFeatures.stream()
			.map(MappedFeature::getAttributierterSeitenbezug)
			.allMatch(seitenbezug -> seitenbezug.isPresent() &&
				(seitenbezug.get().equals(Seitenbezug.LINKS) ||
					seitenbezug.get().equals(Seitenbezug.RECHTS))
			);
	}

	private List<MappedAttributes> getNormalizedMappedAttributesFuerSeitenbezug(
		NormalisierteMappedFeatures normalisierteMappedFeatures,
		Seitenbezug seitenbezug) {
		require(!seitenbezug.equals(Seitenbezug.BEIDSEITIG),
			"Features für beidseitigen Seitenbezug zu ermitteln ist Quatsch");

		List<LinearReferenzierterAbschnitt> lineareReferenzen = normalisierteMappedFeatures
			.getLineareReferenzenNormalizedAndSorted();

		List<MappedAttributes> result = new ArrayList<>();

		for (LinearReferenzierterAbschnitt linearReferenzierterAbschnitt : lineareReferenzen) {
			List<MappedFeature> matchingFeatures = normalisierteMappedFeatures.getFeaturesFor(
				linearReferenzierterAbschnitt);
			if (matchingFeatures.isEmpty()) {
				continue;
			}

			matchingFeatures.sort(Comparator.comparing(MappedFeature::getHaendigkeit,
				Haendigkeit.vonLinksNachRechts));

			MappedFeature leftMost = matchingFeatures.get(0);
			MappedFeature rightMost = matchingFeatures.get(matchingFeatures.size() - 1);

			for (MappedFeature mappedFeature : matchingFeatures) {
				if (mappedFeature.getAttributierterSeitenbezug().isPresent()) {
					// Wenn es ein Attribut gibt, was den Seitenbezug des Features spezifiziert, dann verwende nur das,
					// um das Feature auf die entsprechende Seite zu mappen.
					if (mappedFeature.getAttributierterSeitenbezug().get().equals(seitenbezug)) {
						result.add(
							MappedAttributes.of(mappedFeature.getProperties(), linearReferenzierterAbschnitt,
								seitenbezug,
								unterscheidetSichOrientierungZurKante(mappedFeature.getGeometry()))
						);
					}
				} else {
					// Wenn nicht, verwende die geometrische Haendigkeit für die Zuordnung des Features
					// zu einer Seite der Kante
					double differenzLinks = mappedFeature.getHaendigkeit()
						.getVorzeichenbehafteteWahrscheinlichkeit()
						- leftMost.getHaendigkeit().getVorzeichenbehafteteWahrscheinlichkeit();

					double differenzRechts = mappedFeature.getHaendigkeit()
						.getVorzeichenbehafteteWahrscheinlichkeit()
						- rightMost.getHaendigkeit().getVorzeichenbehafteteWahrscheinlichkeit();

					double groessteDifferenz = Math.abs(differenzLinks) > Math.abs(differenzRechts) ? differenzLinks
						: differenzRechts;

					if (Math.abs(groessteDifferenz)
						> DIFFERENZ_IN_HAENDIGKEIT_WAHRSCHEINLICHKEITEN_FUER_SEITENZUORDNUNG) {
						if (seitenbezug.equals(Seitenbezug.LINKS) && groessteDifferenz > 0) {
							result.add(
								MappedAttributes.of(mappedFeature.getProperties(), linearReferenzierterAbschnitt,
									seitenbezug,
									unterscheidetSichOrientierungZurKante(mappedFeature.getGeometry()))
							);
						} else if (seitenbezug.equals(Seitenbezug.RECHTS) && groessteDifferenz < 0) {
							result.add(
								MappedAttributes.of(mappedFeature.getProperties(), linearReferenzierterAbschnitt,
									seitenbezug,
									unterscheidetSichOrientierungZurKante(mappedFeature.getGeometry()))
							);
						}
					} else {
						result.add(
							MappedAttributes.of(mappedFeature.getProperties(), linearReferenzierterAbschnitt,
								seitenbezug,
								unterscheidetSichOrientierungZurKante(mappedFeature.getGeometry()))
						);
					}
				}
			}
		}
		return result;
	}

	private boolean unterscheidetSichOrientierungZurKante(LineString linestring) {
		LocationIndexedLine locationIndexedGrundnetzGeometrie = new LocationIndexedLine(kante.getGeometry());

		final LinearLocation projektionDesAnfangs = locationIndexedGrundnetzGeometrie
			.project(linestring.getStartPoint().getCoordinate());
		final LinearLocation projektionDesEndes = locationIndexedGrundnetzGeometrie
			.project(linestring.getEndPoint().getCoordinate());

		List<LinearLocation> projektionen = Arrays.asList(projektionDesAnfangs, projektionDesEndes);
		projektionen.sort(Comparator.comparing(LinearLocation::getSegmentIndex)
			.thenComparing(LinearLocation::getSegmentFraction));

		final LinearLocation ueberschneidungsAnfang = projektionen.get(0);

		return !ueberschneidungsAnfang.equals(projektionDesAnfangs);
	}
}
