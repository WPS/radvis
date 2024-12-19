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

package de.wps.radvis.backend.wegweisendeBeschilderung.domain;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.AttributeType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.data.util.Lazy;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.SimpleFeatureTypeFactory;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.entity.WegweisendeBeschilderung;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.entity.WegweisendeBeschilderungImportJobStatistik;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.repository.WegweisendeBeschilderungRepository;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Defizit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Gemeinde;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Kreis;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Land;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.PfostenNr;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.PfostenTyp;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Pfostendefizit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Pfostenzustand;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.WegweiserTyp;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Zustandsbewertung;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithFehlercode(Fehlercode.WEGWEISENDE_BESCHILDERUNG_IMPORT)
public class WegweisendeBeschilderungImportJob extends AbstractJob {

	public static final String JOB_NAME = "WegweisendeBeschilderungImportJob";

	private final String importGeoJsonUrl;
	private final GeoJsonImportRepository geoJsonImportRepository;
	private final WegweisendeBeschilderungRepository wegweisendeBeschilderungRepository;
	private final Lazy<Gebietskoerperschaft> obersteGebietskoerperschaft;

	public WegweisendeBeschilderungImportJob(
		String wegweisendeBeschilderungImportGeoJsonUrl,
		GeoJsonImportRepository geoJsonImportRepository,
		WegweisendeBeschilderungRepository wegweisendeBeschilderungRepository,
		Lazy<Gebietskoerperschaft> obersteGebietskoerperschaft,
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository) {
		super(jobExecutionDescriptionRepository);
		this.importGeoJsonUrl = wegweisendeBeschilderungImportGeoJsonUrl;
		this.geoJsonImportRepository = geoJsonImportRepository;
		this.wegweisendeBeschilderungRepository = wegweisendeBeschilderungRepository;
		this.obersteGebietskoerperschaft = obersteGebietskoerperschaft;
	}

	@Override
	public String getName() {
		return JOB_NAME;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.WEGWEISENDE_BESCHILDERUNGEN_JOB)
	@SuppressChangedEvents
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.WEGWEISENDE_BESCHILDERUNGEN_JOB)
	@SuppressChangedEvents
	protected Optional<JobStatistik> doRun() {
		log.info("WegweisendeBeschilderungImportJob gestartet.");

		URL url = null;
		try {
			url = new URL(importGeoJsonUrl);
		} catch (MalformedURLException e) {
			log.error("Die URL ({}) für das Importieren der wegweisenden Beschilderung ist nicht richtig.",
				importGeoJsonUrl);
			return Optional.empty();
		}

		String geoJsonString = null;
		try {
			geoJsonString = geoJsonImportRepository.getFileContentAsString(url);
		} catch (IOException e) {
			log.error("Die GeoJSON Datei konnte nicht heruntergeladen werden: URL ({}) ", url);
			return Optional.empty();
		}

		List<SimpleFeature> features = null;
		try {
			features = geoJsonImportRepository.getSimpleFeatures(geoJsonString);
		} catch (ReadGeoJSONException e) {
			log.error("Die GeoJSON Datei von URL " + url + " konnte nicht eingelesen werden:", e);
			return Optional.empty();
		}

		List<AttributeType> expectedFeatureTypeTypes = SimpleFeatureTypeFactory.createSimpleFeatureType(
			Set.of(
				"PfostenNr", // --> PfostenNr
				"WWTyp_Tx", // --> WegweiserTyp
				"PfTyp_Tx", // --> PfostenTyp
				"GesZus", // --> Zustandsbewertung
				"GesMangel", // --> Defizit
				"PfZus", // --> Pfostenzustand
				"PfMangel", // --> Pfostendefizit
				"GE_Gem", // --> Gemeinde
				"GE_Kreis", // --> Kreis
				"GE_Land" // --> Land
			),
			Point.class,
			SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_GEOMETRY).getTypes();

		WegweisendeBeschilderungImportJobStatistik wegweisendeBeschilderungImportJobStatistik = new WegweisendeBeschilderungImportJobStatistik();
		wegweisendeBeschilderungImportJobStatistik.anzahlFeatures = features.size();

		Map<PfostenNr, WegweisendeBeschilderung> beschilderungInMemRepo = new HashMap<>();
		wegweisendeBeschilderungRepository.findAll().forEach(wegweisendeBeschilderung -> beschilderungInMemRepo
			.put(wegweisendeBeschilderung.getPfostenNr(), wegweisendeBeschilderung));

		List<SimpleFeature> featuresWithUniquePfostenNr = removeFeaturesThatHaveRedundantPostenNr(features,
			wegweisendeBeschilderungImportJobStatistik);

		updateOrCreateWegbeschilderungenFromFeatures(wegweisendeBeschilderungImportJobStatistik,
			expectedFeatureTypeTypes, featuresWithUniquePfostenNr, beschilderungInMemRepo);

		deleteWegbeschilderungenWhichAreNotInFeatures(wegweisendeBeschilderungImportJobStatistik,
			featuresWithUniquePfostenNr, beschilderungInMemRepo);

		log.info("WegweisendeBeschilderungImportJobStatistik:\n" + wegweisendeBeschilderungImportJobStatistik);
		return Optional.of(wegweisendeBeschilderungImportJobStatistik);
	}

	private List<SimpleFeature> removeFeaturesThatHaveRedundantPostenNr(List<SimpleFeature> features,
		WegweisendeBeschilderungImportJobStatistik wegweisendeBeschilderungImportJobStatistik) {
		Set<String> allePfostenNr = new HashSet<>();
		Set<String> redundantPfostenNr = new HashSet<>();

		features.forEach(f -> {
			String pfostenNr = (String) f.getAttribute("PfostenNr");
			if (allePfostenNr.contains(pfostenNr)) {
				redundantPfostenNr.add(pfostenNr);
			} else {
				allePfostenNr.add(pfostenNr);
			}
		});

		wegweisendeBeschilderungImportJobStatistik.doppeltePfostenNummern.addAll(redundantPfostenNr);

		return features.stream()
			.filter(f -> {
				String pfostenNr = (String) f.getAttribute("PfostenNr");
				if (redundantPfostenNr.contains(pfostenNr)) {
					log.warn(
						"Wegweisende Beschilderung mit PfostenNr {} ist redundant! Das ist ein Datenfehler! Alle Features mit dieser PfostenNr werden NICHT importiert!",
						pfostenNr);
					wegweisendeBeschilderungImportJobStatistik.anzahlIgnoriert += 1;
					return false;
				} else {
					return true;
				}
			})
			.collect(Collectors.toList());
	}

	private void updateOrCreateWegbeschilderungenFromFeatures(
		WegweisendeBeschilderungImportJobStatistik wegweisendeBeschilderungImportJobStatistik,
		List<AttributeType> expectedFeatureTypeTypes,
		List<SimpleFeature> features,
		Map<PfostenNr, WegweisendeBeschilderung> beschilderungInMemRepo) {
		AtomicInteger countIgnoriert = new AtomicInteger(0);
		AtomicInteger countNeuErstellt = new AtomicInteger(0);
		AtomicInteger countAktualisiert = new AtomicInteger(0);
		AtomicInteger countProgress = new AtomicInteger(0);

		wegweisendeBeschilderungRepository.saveAll(
			features.stream()
				.filter((SimpleFeature f) -> hatFeatureRichtigesFeatureTypeUndHabenDieEntsprechendeFelderAuchWerte(
					expectedFeatureTypeTypes, countIgnoriert, f))
				.map(this::erstelleWegweisendeBeschilderungAusFeature)
				.peek(f -> logProgressInPercent(features.size(), countProgress, 5))
				.map(zuImportierendeBeschilderung -> ermittleZuUebernehmendeBeschilderung(
					countNeuErstellt, countAktualisiert, countIgnoriert, zuImportierendeBeschilderung,
					beschilderungInMemRepo))
				.filter(beschilderung -> beschilderung.isPresent())
				.map(beschilderung -> beschilderung.get())
				.collect(Collectors.toList()));

		wegweisendeBeschilderungImportJobStatistik.anzahlIgnoriert += countIgnoriert.get();
		wegweisendeBeschilderungImportJobStatistik.anzahlNeuErstellt += countNeuErstellt.get();
		wegweisendeBeschilderungImportJobStatistik.anzahlAktualisiert += countAktualisiert.get();
	}

	private boolean hatFeatureRichtigesFeatureTypeUndHabenDieEntsprechendeFelderAuchWerte(
		List<AttributeType> expectedFeatureTypeTypes, AtomicInteger countIgnoriert,
		SimpleFeature f) {
		if (!f.getFeatureType().getTypes().containsAll(expectedFeatureTypeTypes)) {
			log.warn(
				"In einem Feature sind nicht alle benötigten Attribute vorhanden und wird nicht importiert: {}",
				f);
			countIgnoriert.incrementAndGet();
			return false;
		}
		if (f.getValue().stream().anyMatch(v -> v.getValue() == null)) {
			log.warn("In einem Feature ist ein Attribute nicht gesetzt und wird nicht importiert: {}", f);
			countIgnoriert.incrementAndGet();
			return false;
		}
		return true;
	}

	@NotNull
	private WegweisendeBeschilderung erstelleWegweisendeBeschilderungAusFeature(SimpleFeature f) {
		return new WegweisendeBeschilderung(
			PfostenNr.of(f.getAttribute("PfostenNr").toString()),
			(Geometry) f.getDefaultGeometry(),
			WegweiserTyp.of(f.getAttribute("WWTyp_Tx").toString()),
			PfostenTyp.of(f.getAttribute("PfTyp_Tx").toString()),
			Zustandsbewertung.of(f.getAttribute("GesZus").toString()),
			Defizit.of(f.getAttribute("GesMangel").toString()),
			Pfostenzustand.of(f.getAttribute("PfZus").toString()),
			Pfostendefizit.of(f.getAttribute("PfMangel").toString()),
			Gemeinde.of(f.getAttribute("GE_Gem").toString()),
			Kreis.of(f.getAttribute("GE_Kreis").toString()),
			Land.of(f.getAttribute("GE_Land").toString()),
			obersteGebietskoerperschaft.get());
	}

	@NotNull
	private Optional<WegweisendeBeschilderung> ermittleZuUebernehmendeBeschilderung(AtomicInteger countNeuErstellt,
		AtomicInteger countAktualisiert, AtomicInteger countIgnoriert,
		WegweisendeBeschilderung zuImportierendeBeschilderung,
		Map<PfostenNr, WegweisendeBeschilderung> beschilderungInMemRepo) {

		Optional<WegweisendeBeschilderung> potentiellBestehende = Optional.ofNullable(
			beschilderungInMemRepo.get(zuImportierendeBeschilderung.getPfostenNr()));

		if (potentiellBestehende.isPresent()) {
			if (!potentiellBestehende.get().isDifferentTo(zuImportierendeBeschilderung)) {
				countIgnoriert.incrementAndGet();
				return Optional.empty();
			}

			countAktualisiert.incrementAndGet();
		} else {
			countNeuErstellt.incrementAndGet();
		}

		return Optional.of(
			potentiellBestehende
				.map(p -> p.update(zuImportierendeBeschilderung))
				.orElse(zuImportierendeBeschilderung));
	}

	private void deleteWegbeschilderungenWhichAreNotInFeatures(
		WegweisendeBeschilderungImportJobStatistik wegweisendeBeschilderungImportJobStatistik,
		List<SimpleFeature> features,
		Map<PfostenNr, WegweisendeBeschilderung> beschilderungInMemRepo) {
		log.info("Prüfe ob wegweisende Beschilderungen entfernt werden...");

		Set<String> pfostenNrsOfFeatures = features.stream()
			.filter(feature -> feature.getAttribute("PfostenNr") != null)
			.map(feature -> feature.getAttribute("PfostenNr").toString())
			.collect(Collectors.toCollection(HashSet::new));

		List<WegweisendeBeschilderung> toDelete = beschilderungInMemRepo.values().stream()
			.filter((WegweisendeBeschilderung beschilderung) -> !pfostenNrsOfFeatures
				.contains(beschilderung.getPfostenNr().getValue()))
			.collect(Collectors.toList());

		int anzahlToDelete = toDelete.size();

		log.info("Anzahl zu entfernen: {}", anzahlToDelete);
		wegweisendeBeschilderungRepository.deleteAll(toDelete);
		wegweisendeBeschilderungImportJobStatistik.anzahlEntfernt = anzahlToDelete;
	}
}
