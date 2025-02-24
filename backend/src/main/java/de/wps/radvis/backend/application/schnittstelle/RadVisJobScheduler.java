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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StopWatch;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.RadVisStopWatchPrinter;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RadVisJobScheduler {
	private final List<AbstractJob> allJobs;
	private final List<String> radVisStartupJobSchedule;
	private final List<String> radVisNaechtlicherJobSchedule;

	public RadVisJobScheduler(
		List<AbstractJob> allJobs,
		List<String> radVisStartupJobSchedule,
		List<String> radVisNaechtlicherJobSchedule) {
		this.allJobs = allJobs;
		this.radVisStartupJobSchedule = radVisStartupJobSchedule;
		this.radVisNaechtlicherJobSchedule = radVisNaechtlicherJobSchedule;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		runSchedule(new RadVisStartupJobSchedule(radVisStartupJobSchedule));
	}

	@Scheduled(cron = "${radVis.schedule.naechtlich}", zone = "Europe/Berlin")
	public void runNaechtlich() {
		runSchedule(new RadVisNaechtlicherJobSchedule(radVisNaechtlicherJobSchedule));
	}

	private void runSchedule(RadVisJobSchedule schedule) {
		StopWatch stopWatch = new StopWatch();
		log.info("Starte Schedule " + schedule.getClass().getSimpleName() + "...");
		Map<String, String> failedJobs = new HashMap<>();
		for (String jobName : schedule.jobsToRun()) {
			Optional<AbstractJob> jobToRun = allJobs.stream().filter(job -> jobName.equals(job.getName())).findAny();
			if (jobToRun.isEmpty()) {
				log.error("Job der Klasse " + jobName + " nicht gefunden.");
			} else {
				stopWatch.start(jobName);
				log.info("RadVisJobScheduler: Starte run-Methode von Job " + jobName);
				try {
					jobToRun.get().run(schedule.forceRun());
				} catch (Exception | RequireViolation e) {
					if (schedule.verhindereWeitereJobAusfuehrungBeiFehler()) {
						throw e;
					}

					String failureType = e.getClass().getName();
					log.error("Job " + jobName + " fehlgeschlagen durch " + failureType, e);
					failedJobs.put(jobName, failureType);
				}

				stopWatch.stop();
				log.info("RadVisJobScheduler: Stoppe run-Methode von Job " + jobName);
				log.info("RadVisJobScheduler: Laufzeit der run-Methode: " + RadVisStopWatchPrinter.stringify(
					stopWatch));
			}
		}
		log.info(RadVisStopWatchPrinter.stringify(stopWatch, failedJobs));
		log.info("Beende Schedule " + schedule.getClass().getSimpleName() + ".");
	}
}
