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

package de.wps.radvis.backend.application.schnittstelle;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.wps.radvis.backend.application.domain.InitialImportService;
import de.wps.radvis.backend.common.domain.Job;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import lombok.NonNull;

@RestController
@RequestMapping("jobs")
@DarfJobsAusfuehren
public class JobRestController {
	RadVisJobScheduler radVisJobScheduler;

	private List<Job> jobs;

	private Environment env;

	@Value("${radVis.jobs.jobModus}")
	private Boolean jobModus = false;

	private final InitialImportService initialImportService;

	private Optional<Future<Void>> initialImportResult = Optional.empty();

	public JobRestController(@NonNull InitialImportService initialImportService,
		@NonNull RadVisJobScheduler radVisJobScheduler,
		@NonNull Environment env,
		@Autowired List<Job> jobs) {
		this.jobs = jobs;
		this.initialImportService = initialImportService;
		this.radVisJobScheduler = radVisJobScheduler;
		this.env = env;
	}

	@GetMapping("initial")
	public synchronized String runInitial() {
		checkIfJobModus();
		if (initialImportResult.isPresent()) {
			Future<Void> asyncResult = initialImportResult.get();
			if (asyncResult.isDone()) {
				try {
					asyncResult.get();
					return "Job abgeschlossen.";
				} catch (ExecutionException | InterruptedException e) {
					return String.format("Job beendet mit Ausführungsfehler: %s", e.getMessage());
				}
			} else {
				return "Job wird ausgeführt.";
			}
		} else {
			initialImportResult = Optional.of(initialImportService.runJobs());
			return "Job gestartet.";
		}
	}

	@GetMapping("run")
	public synchronized String runAll() {
		checkIfJobModus();
		if (initialImportResult.isPresent()) {
			Future<Void> asyncResult = initialImportResult.get();
			if (!asyncResult.isDone()) {
				return "Job wird bereits ausgeführt.";
			}
		}

		initialImportResult = Optional.of(initialImportService.runJobs());
		return "Job gestartet.";
	}

	@GetMapping("run/attributprojektionsjobAndPrerequisites")
	public synchronized String runAttributprojektionsjobAndPrerequisites() {
		checkIfJobModus();
		initialImportService.runJobsFuerAttributprojektion();
		return "Job gestartet";
	}

	@GetMapping("run/{name}")
	public synchronized String runJob(@PathVariable("name") String name) {
		checkIfJobModus();
		Optional<Job> optionalJob = jobs
			.stream()
			.filter(job -> job.getName().equals(name))
			.findFirst();

		if (optionalJob.isPresent()) {
			optionalJob.get().run(true);
			return "Job ausgeführt";
		} else {
			throw new IllegalArgumentException(String.format("Job nicht gefunden: %s", name));
		}
	}

	@GetMapping("startNaechtlicherJobSchedule")
	public synchronized String runNaechtlicherJobSchedule() {
		checkIfJobModus();
		radVisJobScheduler.runNaechtlich();
		return "NaechtlicherJobSchedule gestartet";
	}

	@GetMapping(value = "list", produces = "application/json")
	public synchronized List<String> list() {
		checkIfJobModus();
		return jobs.stream().map(Job::getName).collect(Collectors.toList());
	}

	@GetMapping(value = "listDetails", produces = "application/json")
	public synchronized List<JobExecutionDescription> describe() {
		checkIfJobModus();
		return jobs.stream()
			.map(job -> job.findLastExecutionDescription().orElse(null))
			.collect(Collectors.toList());
	}

	@GetMapping(value = "switchJobMode", produces = "application/json")
	public synchronized String switchJobModus() {
		jobModus = !jobModus;
		return String.format("Der JobModus ist nun %s.", jobModus ? "angestellt" : "ausgestellt");
	}

	private void checkIfJobModus() {
		if (!Arrays.asList(env.getActiveProfiles()).contains("dev")) {
			if (!jobModus) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Jobs können nur im Job-Modus ausgeführt werden.");
			}
		}
	}
}
