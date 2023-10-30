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

import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RamUsageUtility {
	public static void logCurrentRamUsage(String beschriftung) {
		log.info(buildMessage(beschriftung, Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory()));
	}

	protected static String buildMessage(String beschriftung, Long freeBytes, Long totalBytes) {
		double gesamtGiB = bytesToGiB(totalBytes);
		double verwendetGiB = gesamtGiB - bytesToGiB(freeBytes);

		String ohneBeschriftung = String.format(
			Locale.GERMANY,
			"Aktuell verwendeter RAM ist %.3f GiB von %.3f GiB (%.2f%%)",
			verwendetGiB,
			gesamtGiB,
			Math.round(verwendetGiB * 10000 / gesamtGiB) / 100.);

		return beschriftung == null || beschriftung.isEmpty()
			? ohneBeschriftung
			: String.format("%s: %s", beschriftung, ohneBeschriftung);
	}

	private static double bytesToGiB(long totalMemory) {
		return Math.round(totalMemory / (1024.0 * 1024.0 * 1024.0) * 1000) / 1000.;
	}
}
