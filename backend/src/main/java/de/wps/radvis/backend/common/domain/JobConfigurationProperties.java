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

import static de.wps.radvis.backend.common.domain.Validators.isValidURL;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.not;
import static org.valid4j.Assertive.require;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import lombok.Getter;

@ConfigurationProperties("radvis.jobs")
@Getter
public class JobConfigurationProperties {

	private final String radwegeLglTuttlingenShpFilePath;

	private final String radNetzShapeFilesPath;

	private final String radNetzStreckenShapeFilesPath;

	private final String gisGoeppingenShapeFilesPath1;

	private final String gisGoeppingenShapeFilesPath2;

	private final String radWegeDBShapeFilePath;

	private final String rvkEsslingenShapeFilePath;

	private final String bietigheimBissingenShapeFilePath;

	private final String ttSibFilesPath;

	private final String verwaltungsgrenzenShapeFilesPath;

	private final String dlmBasisStrassenXmlFilePath;

	private final String dlmBasisWegeXmlFilePath;

	private final String radnetzMassnahmenImportPath;

	private final String massnahmenBlaetterImportPath;

	private final String umsetzungsstandabfragenCsvImportFilePath;

	private final String tfisRadwegePath;

	private final String dRoutenPath;

	private final List<String> abstellanlageBRImportUrlList;

	private final String leihstationImportUrl;

	private final String fahrradzaehlstellenMobiDataImportBaseUrl;

	private final String fahrradzaehlstellenMobiDataImportStartDate;

	@ConstructorBinding
	public JobConfigurationProperties(
		String radwegeLglTuttlingenShpFilePath,
		String radNetzShapeFilesPath,
		String radNetzStreckenShapeFilesPath,
		String gisGoeppingenShapeFilesPath1,
		String gisGoeppingenShapeFilesPath2,
		String radWegeDBShapeFilePath,
		String rvkEsslingenShapeFilePath,
		String bietigheimBissingenShapeFilePath,
		String ttSibFilesPath,
		String verwaltungsgrenzenShapeFilesPath,
		String dlmBasisStrassenXmlFilePath,
		String dlmBasisWegeXmlFilePath,
		String umsetzungsstandabfragenCsvImportFilePath,
		String radnetzMassnahmenImportPath,
		String massnahmenBlaetterImportPath,
		String tfisRadwegePath,
		String dRoutenPath,
		List<String> abstellanlageBRImportUrlList,
		String leihstationImportUrl,
		String fahrradzaehlstellenMobiDataImportBaseUrl,
		String fahrradzaehlstellenMobiDataImportStartDate) {

		require(isValidDateipfad(radwegeLglTuttlingenShpFilePath),
			"radwegeLglTuttlingenShpFilePath muss Dateipfadstruktur haben");
		require(isValidDateipfad(radNetzShapeFilesPath), "radNetzShapeFilesPath muss Dateipfadstruktur haben");
		require(isValidDateipfad(radNetzStreckenShapeFilesPath),
			"radNetzStreckenShapeFilesPath muss Dateipfadstruktur haben");
		require(isValidDateipfad(gisGoeppingenShapeFilesPath1),
			"gisGoeppingenShapeFilesPath1 muss Dateipfadstruktur haben");
		require(isValidDateipfad(gisGoeppingenShapeFilesPath2),
			"gisGoeppingenShapeFilesPath2 muss Dateipfadstruktur haben");
		require(isValidDateipfad(radWegeDBShapeFilePath), "radWegeDBShapeFilePath muss Dateipfadstruktur haben");
		require(isValidDateipfad(rvkEsslingenShapeFilePath), "rvkEsslingenShapeFilePath muss Dateipfadstruktur haben");
		require(isValidDateipfad(bietigheimBissingenShapeFilePath),
			"bietigheimBissingenShapeFilePath muss Dateipfadstruktur haben");
		require(isValidDateipfad(ttSibFilesPath), "ttSibFilesPath muss Dateipfadstruktur haben");
		require(isValidDateipfad(verwaltungsgrenzenShapeFilesPath),
			"verwaltungsgrenzenShapeFilesPath muss Dateipfadstruktur haben");
		require(isValidDateipfad(dlmBasisStrassenXmlFilePath),
			"dlmBasisStrassenXmlFilePath muss Dateipfadstruktur haben");
		require(isValidDateipfad(dlmBasisWegeXmlFilePath), "dlmBasisWegeXmlFilePath muss Dateipfadstruktur haben");
		require(isValidDateipfad(radnetzMassnahmenImportPath),
			"radnetzMassnahmenImportPath muss Dateipfadstruktur haben");
		require(isValidDateipfad(umsetzungsstandabfragenCsvImportFilePath),
			"umsetzungsstandabfragenCsvImportFilePath muss Dateipfadstruktur haben");
		require(isValidDateipfad(massnahmenBlaetterImportPath),
			"massnahmenBlaetterImportPath muss Dateipfadstruktur haben");
		require(isValidDateipfad(tfisRadwegePath),
			"tfisRadwegePath muss Dateipfadstruktur haben");
		require(isValidDateipfad(dRoutenPath),
			"dRoutenPath muss Dateipfadstruktur haben");
		require(abstellanlageBRImportUrlList, notNullValue());
		require(abstellanlageBRImportUrlList, not(empty()));
		abstellanlageBRImportUrlList.forEach(abstellanlageBRImportUrl -> {
			require(isValidURL(abstellanlageBRImportUrl), "Die abstellanlageBRImportUrl muss URL-Struktur haben");
		});
		require(isValidURL(leihstationImportUrl), "Die LeistationImportURL muss URL-Struktur haben");
		require(isValidURL(fahrradzaehlstellenMobiDataImportBaseUrl),
			"Die FahrradzaehlstellenMobiDataImportURL muss URL-Struktur haben");
		require(fahrradzaehlstellenMobiDataImportStartDate, hasLength(6));

		this.radwegeLglTuttlingenShpFilePath = radwegeLglTuttlingenShpFilePath;
		this.radNetzShapeFilesPath = radNetzShapeFilesPath;
		this.radNetzStreckenShapeFilesPath = radNetzStreckenShapeFilesPath;
		this.gisGoeppingenShapeFilesPath1 = gisGoeppingenShapeFilesPath1;
		this.gisGoeppingenShapeFilesPath2 = gisGoeppingenShapeFilesPath2;
		this.radWegeDBShapeFilePath = radWegeDBShapeFilePath;
		this.rvkEsslingenShapeFilePath = rvkEsslingenShapeFilePath;
		this.bietigheimBissingenShapeFilePath = bietigheimBissingenShapeFilePath;
		this.ttSibFilesPath = ttSibFilesPath;
		this.verwaltungsgrenzenShapeFilesPath = verwaltungsgrenzenShapeFilesPath;
		this.dlmBasisStrassenXmlFilePath = dlmBasisStrassenXmlFilePath;
		this.dlmBasisWegeXmlFilePath = dlmBasisWegeXmlFilePath;
		this.radnetzMassnahmenImportPath = radnetzMassnahmenImportPath;
		this.umsetzungsstandabfragenCsvImportFilePath = umsetzungsstandabfragenCsvImportFilePath;
		this.massnahmenBlaetterImportPath = massnahmenBlaetterImportPath;
		this.tfisRadwegePath = tfisRadwegePath;
		this.abstellanlageBRImportUrlList = abstellanlageBRImportUrlList;
		this.leihstationImportUrl = leihstationImportUrl;
		this.dRoutenPath = dRoutenPath;
		this.fahrradzaehlstellenMobiDataImportBaseUrl = fahrradzaehlstellenMobiDataImportBaseUrl;
		this.fahrradzaehlstellenMobiDataImportStartDate = fahrradzaehlstellenMobiDataImportStartDate;
	}

	public static boolean isValidDateipfad(String value) {
		try {
			Paths.get(value);
			return true;
		} catch (InvalidPathException e) {
			return false;
		}
	}
}
