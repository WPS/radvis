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

package de.wps.radvis.backend.common.domain.entity;

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.common.domain.ExtentProperty;
import de.wps.radvis.backend.common.domain.Job;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionInputSummarySupplier;
import de.wps.radvis.backend.common.domain.RadVisStopWatchPrinter;
import de.wps.radvis.backend.common.domain.RamUsageUtility;
import jakarta.transaction.Transactional;

public abstract class AbstractJob implements Job {

	/**
	 * Custom impl. to use logger of concrete class for jobs
	 */
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final JobExecutionDescriptionRepository repository;
	private Supplier<String> inputSummarySupplier = () -> LocalDateTime.now().toString();

	public AbstractJob(JobExecutionDescriptionRepository repository) {
		this.repository = repository;

		require(repository, notNullValue());
	}

	@Override
	@Transactional
	public JobExecutionDescription run() {
		return run(false);
	}

	@Override
	@Transactional
	public JobExecutionDescription run(boolean force) {
		String name = getName();
		RamUsageUtility.logCurrentRamUsage(String.format("Vor Start des Jobs %s", name));

		String inputSummary = inputSummarySupplier.get();

		Optional<JobExecutionDescription> lastExecution = repository
			.findFirstByNameEqualsOrderByExecutionStartDesc(name);
		if (lastExecution.isPresent()) {
			if (lastExecution.get().getInputSummary().equals(inputSummary)) {
				if (force && isRepeatable()) {
					log.info("Starting job execution for {}. Forced.", name);
				} else {
					log.info("Skipping job execution for {}", name);
					return lastExecution.get();
				}
			} else if (isRepeatable()) {
				log.info("Starting job execution for {}. Input has changed.", name);
			} else {
				log.info("Skipping job execution for {}", name);
				return lastExecution.get();
			}
		} else {
			log.info("Starting first job execution for {}.", name);
		}

		// Schon vor der Job-Ausführung die Description setzen, damit diese (wenn auch größtenteils leer) innerhalb
		// der Jobs einsehbar ist. Z.B. für den Start-Zeitpunkt relevant.
		LocalDateTime startTime = LocalDateTime.now();
		var descriptionBuilder = JobExecutionDescription
			.builder()
			.name(name)
			.inputSummary(inputSummary)
			.executionStart(startTime)
			.executionEnd(startTime);
		JobExecutionDescription jobExecutionDescription = repository.save(descriptionBuilder.build());
		AdditionalRevInfoHolder.setJobExecutionDescription(jobExecutionDescription);

		Optional<JobStatistik> jobstatistic = doRun();
		LocalDateTime endTime = LocalDateTime.now();
		jobExecutionDescription.setExecutionEnd(endTime);

		if (jobstatistic.isPresent()) {
			try {
				jobExecutionDescription.setStatistic(jobstatistic.get().toJSON());
			} catch (JsonProcessingException e) {
				log.error("JobStatistic für " + name + " konnte nicht erzeugt werden.", e);
			}
		}

		log.info("Completed job execution for {}. Duration: {}", name, RadVisStopWatchPrinter.stringify(
			jobExecutionDescription.getDuration()));

		RamUsageUtility.logCurrentRamUsage(String.format("Am Ende des Jobs %s", name));

		return jobExecutionDescription;
	}

	@Override
	public Optional<JobExecutionDescription> findLastExecutionDescription() {
		return repository.findFirstByNameEqualsOrderByExecutionStartDesc(getName());
	}

	public JobExecutionInputSummarySupplier asJobExecutionInputSummarySupplier() {
		return JobExecutionInputSummarySupplier.of(() -> findLastExecutionDescription());
	}

	public void setInputSummarySupplier(Supplier<String> inputSummarySupplier) {
		this.inputSummarySupplier = inputSummarySupplier;
	}

	public boolean isRepeatable() {
		return true;
	}

	protected abstract Optional<JobStatistik> doRun();

	/**
	 * Provides the unique name of the job.
	 * <p>
	 * This method is intended to be overridden.
	 */
	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public JobDescription getDescription() {
		return null;
	}

	public static List<Envelope> getPartitionen(ExtentProperty extentProperty, int anzahlPartitionenX) {

		double minX = extentProperty.getMinX();
		double maxX = extentProperty.getMaxX();
		double minY = extentProperty.getMinY();
		double maxY = extentProperty.getMaxY();

		return getPartitionInternal(anzahlPartitionenX, minX, maxX, minY, maxY);
	}

	private static List<Envelope> getPartitionInternal(int anzahlPartitionenX, double minX, double maxX, double minY,
		double maxY) {
		double partitionWidth = (maxX - minX) / anzahlPartitionenX;
		List<Envelope> partitions = new ArrayList<>();

		for (int x = 0; x < anzahlPartitionenX; x++) {
			partitions.add(new Envelope(minX + (x * partitionWidth), minX + ((x + 1) * partitionWidth),
				minY, maxY));
		}
		return partitions;
	}

	protected void logProgressInPercent(int size, AtomicInteger count, int fortschrittsrate) {
		if (size >= fortschrittsrate
			&& count.incrementAndGet() % (size / fortschrittsrate) == 0) {
			log.info("Fortschritt {}%",
				(int) ((count.get() / (double) size) * 100000) / 1000);
		}
	}

	protected void logProgress(AtomicInteger count, int updateModulo, String entityKind) {
		if (count.incrementAndGet() % updateModulo == 0) {
			log.info("Fortschritt: {} " + entityKind, count);
		}
	}
}
