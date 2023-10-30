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

import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StopWatch;

import de.wps.radvis.backend.common.domain.RadVisStopWatchPrinter;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RadVisJobScheduler {
	private final List<AbstractJob> allJobs;

	public RadVisJobScheduler(List<AbstractJob> allJobs) {
		this.allJobs = allJobs;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		runSchedule(new RadVisStartupJobSchedule());
	}

	@Scheduled(cron = "${radVis.schedule.naechtlich}", zone = "Europe/Berlin")
	public void runNaechtlich() {
		runSchedule(new RadVisNaechtlicherJobSchedule());
	}

	private void runSchedule(RadVisJobSchedule schedule) {
		StopWatch stopWatch = new StopWatch();
		log.info("Starting Schedule " + schedule.getClass().getSimpleName() + "...");
		for (Class<? extends AbstractJob> clazz : schedule.jobsToRun()) {
			Optional<AbstractJob> jobToRun = allJobs.stream().filter(job -> clazz.isInstance(job)).findAny();
			if (jobToRun.isEmpty()) {
				log.warn("Job for class " + clazz + " not found.");
			} else {
				stopWatch.start(clazz.getSimpleName());
				log.info("RadVisJobScheduler: Start run von Job " + clazz.getSimpleName());
				jobToRun.get().run(schedule.forceRun());
				stopWatch.stop();
				log.info("RadVisJobScheduler: Stop run von Job " + clazz.getSimpleName());
				log.info("RadVisJobScheduler: Laufzeit der run-Methode: " + stopWatch.getLastTaskTimeMillis() / 1000);
			}
		}
		log.info(RadVisStopWatchPrinter.stringify(stopWatch));
		log.info("Finished Schedule " + schedule.getClass().getSimpleName() + ".");
	}
}
