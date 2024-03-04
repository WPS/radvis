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

package de.wps.radvis.backend.common.domain;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.Map;

import org.springframework.util.StopWatch;
import org.springframework.util.StopWatch.TaskInfo;

public class RadVisStopWatchPrinter {
	public static String stringify(StopWatch stopWatch, Map<String, String> failedJobs) {
		StringBuilder sb = new StringBuilder("\nTotal Time [hh:mm:ss]: ");
		sb.append(printDuration(Duration.ofMillis(stopWatch.getTotalTimeMillis())));
		sb.append('\n');
		sb.append("-----------------------------------------------------------------------\n");
		sb.append("[hh:mm:ss]     %     Status      Task name\n");
		sb.append("-----------------------------------------------------------------------\n");
		NumberFormat percentFormat = NumberFormat.getPercentInstance();
		percentFormat.setMinimumIntegerDigits(2);
		for (TaskInfo task : stopWatch.getTaskInfo()) {
			sb.append(" ");
			sb.append(printDuration(Duration.ofMillis(task.getTimeMillis())));
			sb.append("   ");
			sb.append(percentFormat.format((double) task.getTimeNanos() / stopWatch.getTotalTimeNanos()));
			sb.append("     ");
			String status = failedJobs.containsKey(task.getTaskName())
				? "FAILURE: " + failedJobs.get(task.getTaskName())
				: "SUCCESS";
			sb.append(status);
			sb.append("     ");
			sb.append(task.getTaskName());
			sb.append('\n');
		}
		return sb.toString();
	}

	private static String printDuration(Duration duration) {
		return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
	}
}
