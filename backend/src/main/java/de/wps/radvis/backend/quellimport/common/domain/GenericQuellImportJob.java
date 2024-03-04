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

package de.wps.radvis.backend.quellimport.common.domain;

import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wps.radvis.backend.common.domain.FileBasedInputSummarySupplier;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericQuellImportJob extends AbstractJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenericQuellImportJob.class);

	private FeatureImportRepository featureImportRepository;
	private ImportedFeaturePersistentRepository featureRepository;
	private String name;
	private File file;
	private QuellSystem quellsystem;
	private Art art;
	private String targetGeometry;

	private Predicate<ImportedFeature> filter;

	public GenericQuellImportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		FeatureImportRepository featureImportRepository,
		ImportedFeaturePersistentRepository featureRepository, String name, File file, QuellSystem quellsystem,
		Art art, String targetGeometry) {
		this(jobExecutionDescriptionRepository, featureImportRepository, featureRepository, name, file, quellsystem,
			art,
			targetGeometry, (importedFeature) -> true);
	}

	public GenericQuellImportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		FeatureImportRepository featureImportRepository,
		ImportedFeaturePersistentRepository featureRepository, String name, File file, QuellSystem quellsystem,
		Art art, String targetGeometry, Predicate<ImportedFeature> filter) {
		super(jobExecutionDescriptionRepository);

		this.featureImportRepository = featureImportRepository;
		this.featureRepository = featureRepository;
		this.name = name;
		this.file = file;
		this.quellsystem = quellsystem;
		this.art = art;
		this.targetGeometry = targetGeometry;
		this.filter = filter;

		require(featureImportRepository, Matchers.notNullValue());
		require(featureRepository, Matchers.notNullValue());
		require(name, Matchers.notNullValue());
		require(file, Matchers.notNullValue());
		require(quellsystem, Matchers.notNullValue());
		require(art, Matchers.notNullValue());

		require(file.exists(), "Quelldatei existiert nicht: " + file.getAbsolutePath());

		setInputSummarySupplier(FileBasedInputSummarySupplier.of(file));
	}

	@Override
	public String getName() {
		return getClass().getSimpleName() + " for " + name;
	}

	@Override
	@Transactional
	protected Optional<JobStatistik> doRun() {
		try (Stream<ImportedFeature> featureStream = featureImportRepository
			.getImportedFeaturesFromShapeFiles(targetGeometry, quellsystem, art, file)) {

			AtomicInteger counter = new AtomicInteger();
			AtomicLong stopWatch = new AtomicLong();
			stopWatch.set(System.nanoTime());
			log.info("starting to process features from {}", file.getPath());
			featureStream.filter(filter).forEach(feature -> {
				featureRepository.save(feature);
				counter.getAndIncrement();
				if (counter.get() % 1000 == 0) {
					log.info(" {} Features importiert in {}ms", counter,
						(System.nanoTime() - stopWatch.get()) / 1000000);
					stopWatch.set(System.nanoTime());
				}
			});

		} catch (IOException e) {
			LOGGER.error("Fehler beim Einlesen der Daten für " + name);
			throw new RuntimeException(e);
		}
		featureImportRepository.closeIterators();

		return Optional.empty();
	}
}
