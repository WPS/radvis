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

package de.wps.radvis.backend.quellimport.radnetz.domain;

import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wps.radvis.backend.common.domain.FileBasedInputSummarySupplier;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.quellimport.common.domain.FeatureImportRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;

public class RadNETZQuellImportJob extends AbstractJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(RadNETZQuellImportJob.class);

	private final ImportedFeaturePersistentRepository featureRepository;
	private final FeatureImportRepository featureImportRepository;

	private final List<File> punktFiles;
	private final List<File> streckenFiles;
	private final List<File> massnahmenPunktFiles;
	private final List<File> massnahmenStreckenFiles;

	public RadNETZQuellImportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		FeatureImportRepository featureImportRepository,
		ImportedFeaturePersistentRepository featuresRepository, File radNETZQuellVerzeichnis,
		File radNETZStreckenQuellVerzeichnis) {

		super(jobExecutionDescriptionRepository);

		require(featureImportRepository, Matchers.notNullValue());
		require(featuresRepository, Matchers.notNullValue());

		require(radNETZQuellVerzeichnis.exists(),
			"RadNETZ-Quellverzeichnis existiert nicht: " + radNETZQuellVerzeichnis.getAbsolutePath());
		require(radNETZQuellVerzeichnis.isDirectory(),
			"RadNETZ-Quellverzeichnis ist kein Verzeichnis: " + radNETZQuellVerzeichnis.getAbsolutePath());
		require(radNETZStreckenQuellVerzeichnis.exists(),
			"RadNETZ-Streckenquellverzeichnis existiert nicht: " + radNETZStreckenQuellVerzeichnis.getAbsolutePath());
		require(radNETZStreckenQuellVerzeichnis.isDirectory(),
			"RadNETZ-Streckenquellverzeichnis ist kein Verzeichnis: " + radNETZStreckenQuellVerzeichnis
				.getAbsolutePath());

		this.featureImportRepository = featureImportRepository;
		this.featureRepository = featuresRepository;

		this.punktFiles = getRadnetzInfrastrukturFilesPunkte(radNETZQuellVerzeichnis);
		this.streckenFiles = getRadnetzInfrastrukturFilesStrecken(radNETZStreckenQuellVerzeichnis);
		this.massnahmenPunktFiles = getRadnetzMassnahmenFilesPunkte(radNETZQuellVerzeichnis);
		this.massnahmenStreckenFiles = getRadnetzMassnahmenFilesStrecken(radNETZQuellVerzeichnis);

		Stream<File> files = Stream.of(punktFiles, streckenFiles, massnahmenPunktFiles, massnahmenStreckenFiles)
			.flatMap(Collection::stream);
		setInputSummarySupplier(FileBasedInputSummarySupplier.of(files.collect(Collectors.toList())));
	}

	@Override
	protected Optional<JobStatistik> doRun() {

		try (
			Stream<ImportedFeature> radnetzPunkteFeatures = featureImportRepository
				.getImportedFeaturesFromShapeFiles(Geometry.TYPENAME_POINT, QuellSystem.RadNETZ, Art.Strecke,
					punktFiles.toArray(new File[0]));

			Stream<ImportedFeature> radnetzStreckenFeatures = featureImportRepository
				.getImportedFeaturesFromShapeFiles(Geometry.TYPENAME_LINESTRING, QuellSystem.RadNETZ, Art.Strecke,
					streckenFiles.toArray(new File[0]));

			Stream<ImportedFeature> radnetzFeatureStream = Stream.concat(radnetzPunkteFeatures,
				radnetzStreckenFeatures);) {
			radnetzFeatureStream.forEach(featureRepository::save);
			LOGGER.info("RadNETZ-Features wurden erfolgreich in die DB geschrieben.");
			featureImportRepository.closeIterators();
		} catch (IOException e) {
			LOGGER.error("Fehler beim Einlesen der Radnetz-Daten");
			throw new RuntimeException(e);
		}

		try (
			Stream<ImportedFeature> massnahmenPunkteFeatures = featureImportRepository
				.getImportedFeaturesFromShapeFiles(Geometry.TYPENAME_POINT, QuellSystem.RadNETZ, Art.Massnahme,
					massnahmenPunktFiles.toArray(new File[0]));

			Stream<ImportedFeature> massnahmenStreckenFeatures = featureImportRepository
				.getImportedFeaturesFromShapeFiles(Geometry.TYPENAME_MULTILINESTRING, QuellSystem.RadNETZ,
					Art.Massnahme,
					massnahmenStreckenFiles.toArray(new File[0]));

			Stream<ImportedFeature> massnahmenFeatureStream = Stream.concat(massnahmenPunkteFeatures,
				massnahmenStreckenFeatures);) {
			massnahmenFeatureStream.forEach(featureRepository::save);
			featureImportRepository.closeIterators();
		} catch (IOException e) {
			LOGGER.error("Fehler beim Einlesen der Radnetz-Maßnahmen-Daten");
			throw new RuntimeException(e);
		}
		return Optional.empty();
	}

	private List<File> getRadnetzInfrastrukturFilesPunkte(File radNetzShapeFileRoot) {
		require(radNetzShapeFileRoot.listFiles().length > 0,
			String.format("RadNETZShapefile-Root %s muss Unterordner enthalten: ",
				radNetzShapeFileRoot.getPath()));
		List<File> result = new ArrayList<>();
		for (File radNetzRegionDirectory : radNetzShapeFileRoot.listFiles()) {
			require(radNetzRegionDirectory.listFiles().length > 0,
				String.format("Unterordner %s enthält keine Shapefiles: ",
					radNetzShapeFileRoot.getPath() + radNetzRegionDirectory.getPath()));
			for (File radNetzRegionFile : radNetzRegionDirectory
				.listFiles((dir, name) -> name.contains("Punktdaten") && name.endsWith(".shp"))) {
				result.add(radNetzRegionFile);
			}
		}
		return result;
	}

	private List<File> getRadnetzInfrastrukturFilesStrecken(File radNetzShapeFileRoot) {
		require(radNetzShapeFileRoot.listFiles().length > 0,
			String.format("RadNETZShapefile-Root %s muss Unterordner enthalten: ",
				radNetzShapeFileRoot.getPath()));
		List<File> result = new ArrayList<>();
		for (File radNetzRegionDirectory : radNetzShapeFileRoot.listFiles()) {
			require(radNetzRegionDirectory.listFiles().length > 0,
				String.format("Unterordner %s enthält keine Shapefiles: ",
					radNetzShapeFileRoot.getPath() + radNetzRegionDirectory.getPath()));

			for (File radNetzRegionFile : radNetzRegionDirectory
				.listFiles((dir, name) -> name.contains("Streckendaten") && name.endsWith(".shp"))) {
				result.add(radNetzRegionFile);
			}
		}
		return result;
	}

	private List<File> getRadnetzMassnahmenFilesPunkte(File radNetzShapeFileRoot) {
		require(radNetzShapeFileRoot.listFiles().length > 0,
			String.format("RadNETZShapefile-Root %s muss Unterordner enthalten: ",
				radNetzShapeFileRoot.getPath()));
		List<File> result = new ArrayList<>();
		for (File radNetzRegionDirectory : radNetzShapeFileRoot.listFiles()) {
			require(radNetzRegionDirectory.listFiles().length > 0,
				String.format("Unterordner %s enthält keine Shapefiles: ",
					radNetzShapeFileRoot.getPath() + radNetzRegionDirectory.getPath()));
			for (File radNetzRegionFile : radNetzRegionDirectory.listFiles()) {
				if (radNetzRegionFile.getPath().endsWith("MassnBI_Pkt_2020.shp")
					|| radNetzRegionFile.getPath().endsWith("MassnBl_Pkt_2020.shp")) {
					result.add(radNetzRegionFile);
				}
			}
		}
		return result;
	}

	private List<File> getRadnetzMassnahmenFilesStrecken(File radNetzShapeFileRoot) {
		require(radNetzShapeFileRoot.listFiles().length > 0,
			String.format("RadNETZShapefile-Root %s muss Unterordner enthalten: ",
				radNetzShapeFileRoot.getPath()));
		List<File> result = new ArrayList<>();
		for (File radNetzRegionDirectory : radNetzShapeFileRoot.listFiles()) {
			require(radNetzRegionDirectory.listFiles().length > 0,
				String.format("Unterordner %s enthält keine Shapefiles: ",
					radNetzShapeFileRoot.getPath() + radNetzRegionDirectory.getPath()));
			for (File radNetzRegionFile : radNetzRegionDirectory.listFiles()) {
				if (radNetzRegionFile.getPath().endsWith("MassnBI_Strecken_2020.shp")
					|| radNetzRegionFile.getPath().endsWith("MassnBl_Strecken_2020.shp")) {
					result.add(radNetzRegionFile);
				}
			}
		}
		return result;
	}
}