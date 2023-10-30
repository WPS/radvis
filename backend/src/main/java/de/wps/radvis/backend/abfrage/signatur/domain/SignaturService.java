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

package de.wps.radvis.backend.abfrage.signatur.domain;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import de.wps.radvis.backend.abfrage.signatur.domain.valueObject.SignaturTyp;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;

public class SignaturService {

	private final String basisPfad;
	private final String signaturenPfad;
	private final String massnahmenSignaturenPfad;
	private final FilenameFilter filter;

	public SignaturService(@NotNull CommonConfigurationProperties commonConfigurationProperties,
		@NotNull SignaturConfigurationProperties signaturConfigurationProperties) {

		this.basisPfad = commonConfigurationProperties.getExterneResourcenBasisPfad();
		this.signaturenPfad = signaturConfigurationProperties.getSignaturStylePath();
		this.massnahmenSignaturenPfad = signaturConfigurationProperties.getMassnahmenSignaturenStylePath();

		filter = (f, name) -> name.endsWith(".sld");
	}

	public String readSldFileFromSignaturName(SignaturTyp signaturTyp, String signaturname) throws IOException {
		Path path = Paths.get(basisPfad + getPathForSignaturTyp(signaturTyp) + signaturname + ".sld");
		return Files.readString(path);
	}

	public List<String> getAvailableSignaturen() {
		File signaturStylePfad = new File(basisPfad + signaturenPfad);

		return getSldFiles(signaturStylePfad);
	}

	public List<String> getAvailableMassnahmenSignaturen() {
		File signaturStylePfad = new File(basisPfad + massnahmenSignaturenPfad);
		return getSldFiles(signaturStylePfad);
	}

	private List<String> getSldFiles(File signaturStylePfad) {
		if (!signaturStylePfad.exists()) {
			return List.of();
		}
		return Arrays.stream(signaturStylePfad.list(filter))
			.map(file -> file.split("[.]")[0])
			.collect(Collectors.toList());
	}

	private String getPathForSignaturTyp(SignaturTyp signaturTyp) {
		switch (signaturTyp) {
		case NETZ:
			return signaturenPfad;
		case MASSNAHME:
			return massnahmenSignaturenPfad;
		default:
			throw new RuntimeException("Unbekannter Signaturtyp " + signaturTyp.name());
		}
	}
}
