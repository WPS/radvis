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

import static org.valid4j.Assertive.require;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.hamcrest.Matchers;

/**
 * Provides an input summary based on the last modification date of the given files
 */
public class FileBasedInputSummarySupplier implements Supplier<String> {

	private final List<File> files;

	private FileBasedInputSummarySupplier(List<File> files) {
		require(files, Matchers.not(Matchers.empty()));
		this.files = files;
	}

	@Override
	public String get() {
		if (files.size() == 1) {
			var file = files.get(0);
			return file.getName() + " last modified " + file.lastModified();
		} else {
			long lastModified = files.stream().map(File::lastModified).max(Long::compare).orElseGet(() -> 0L);
			return "multiple files. last modified: " + lastModified;
		}
	}

	public static Supplier<String> of(List<File> files) {
		return new FileBasedInputSummarySupplier(files);
	}

	public static Supplier<String> of(File... files) {
		return new FileBasedInputSummarySupplier(Arrays.asList(files));
	}

}
