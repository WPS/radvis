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

package de.wps.radvis.backend.matching.schnittstelle.repositoryImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.graphhopper.config.Profile;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.reader.dem.SRTMProvider;
import com.graphhopper.routing.util.FlagEncoder;

import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.matching.domain.DlmMatchingCacheRepository;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.BarriereFormEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.BelagArtEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.BeleuchtungEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.BreiteEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.DtvPkwEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KommunalnetzAlltagEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KommunalnetzFreizeitEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KreisnetzAlltagEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KreisnetzFreizeitEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.OberflaechenbeschaffenheitEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadNetzAlltagEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadNetzFreizeitEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadNetzZielnetzEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadschnellverbindungEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadverkehrsfuehrungEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadvorrangroutenEncodedValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DlmMatchedGraphHopperFactory {

	public interface FlagEncoderFactory {
		FlagEncoder build();
	}

	private final String dlmPbfPath;
	private final String routingCacheVerzeichnis;
	private final String mappingCacheVerzeichnis;
	private final List<Profile> profiles;
	private final int minNetworkSize;
	private final String elevationCacheVerzeichnis;
	private final String tiffTilesVerzeichnis;
	private DlmMatchingCacheRepository dlmMatchingCacheRepository = null;
	private DlmMatchedGraphHopper currentGraphhopper = null;

	public DlmMatchedGraphHopperFactory(
		String dlmPbfPath, String routingCacheVerzeichnis,
		String mappingCacheVerzeichnis, List<Profile> profiles, int minNetworkSize, String elevationCacheVerzeichnis,
		String tiffTilesVerzeichnis) {
		this.dlmPbfPath = dlmPbfPath;
		this.routingCacheVerzeichnis = routingCacheVerzeichnis;
		this.mappingCacheVerzeichnis = mappingCacheVerzeichnis;
		this.profiles = profiles;
		this.minNetworkSize = minNetworkSize;
		this.elevationCacheVerzeichnis = elevationCacheVerzeichnis;
		this.tiffTilesVerzeichnis = tiffTilesVerzeichnis;
	}

	public void updateDlmGraphHopper() {
		log.info("Initialisiere oder update den Graphhopper Cache");
		log.info("Genutztes Dlm-Pbf-File: {}", dlmPbfPath);
		File tempRoutingCache;
		File tempMappingCache;
		try {
			tempRoutingCache = Files.createTempDirectory("graphhopper-routing-cache").toFile();
			tempMappingCache = Files.createTempDirectory("graphhopper-mapping-cache").toFile();
		} catch (IOException e) {
			log.warn(
				"Graphhopper konnte nicht geupdated werden, da kein temporäres Cache-Verzeichnis angelegt werden konnte.");
			throw new RuntimeException(e);
		}

		File configuredRoutingCache = new File(routingCacheVerzeichnis);
		File configuredMappingCache = new File(mappingCacheVerzeichnis);

		ElevationProvider elevationProvider;
		if (FeatureTogglz.USE_LGL_HOEHENDATEN.isActive()) {
			elevationProvider = new LGLElevationProviderRepository(
				elevationCacheVerzeichnis,
				tiffTilesVerzeichnis);
		} else {
			elevationProvider = new SRTMProvider();
		}

		// Erstmal einen Tmp Graphhopper erstellen um einen neuen Cache zu bauen. Spaeter wird wieder ein neuer
		// verwendet
		// damit nichts mehr ueberbleibt
		log.info("Temporärer Graphhopper wird für die Cacheerstellung erzeugt");
		DlmMatchingCacheRepository tmpDlmMatchingCacheRepository = new DlmMatchingCacheRepositoryImpl(
			tempMappingCache.getAbsolutePath());
		DlmMatchedGraphHopper tmpGraphHopper = new DlmMatchedGraphHopper(tmpDlmMatchingCacheRepository);
		tmpGraphHopper.setOSMFile(dlmPbfPath);
		tmpGraphHopper.setGraphHopperLocation(tempRoutingCache.toString());
		tmpGraphHopper.setElevationProvider(elevationProvider);

		if (!tempMappingCache.exists()) {
			tempMappingCache.mkdirs();
		}

		addCustomTagParsers(tmpGraphHopper);

		tmpGraphHopper.setProfiles(profiles);
		tmpGraphHopper.setMinNetworkSize(minNetworkSize);
		tmpGraphHopper.importOrLoad(); // hier sollte der tmp-Cache neu erstellt werden (tmpCache-Ordner ist vorher
		// leer)

		log.info("Alte Caches werden gelöscht und Dateien aus den zuvor erstellten Tmp-Caches nach {} verschoben",
			dlmPbfPath);

		// Beide Graphhopper kurz herunterfahren (erzeugt eine kurze downtime, aber den Cache nur zu laden dauert nicht
		// so lang, als wuerden wir den Cache neu erzeugen)
		tmpGraphHopper.close();
		if (this.currentGraphhopper != null) {
			this.currentGraphhopper.close();
			this.currentGraphhopper.clean();
		}

		// alte Caches loeschen
		try {
			if (configuredRoutingCache.exists()) {
				FileUtils.deleteDirectory(configuredRoutingCache);
			}
			if (configuredMappingCache.exists()) {
				FileUtils.deleteDirectory(configuredMappingCache);
			}
		} catch (IOException e) {
			log.warn(
				"Graphhopper konnte nicht geupdated werden, da nicht alle alten Cache-Verzeichnisse geleert werden konnten.");
			throw new RuntimeException(e);
		}

		// Die neuen Caches aus den tmp Ordnern an die richtige Stelle verschieben
		try {
			FileUtils.moveDirectory(tempMappingCache, configuredMappingCache);
			FileUtils.moveDirectory(tempRoutingCache, configuredRoutingCache);
		} catch (IOException e) {
			log.warn(
				"Graphhopper konnte nicht geupdated werden, da nicht alle Tmp-Cache-Verzeichnisse verschoben werden konnten.");
			throw new RuntimeException(e);
		}

		tmpGraphHopper.clean();

		log.info("Neuer Graphhopper wird mit dem zuvor erstellten Cache hochgefahren");
		dlmMatchingCacheRepository = new DlmMatchingCacheRepositoryImpl(configuredMappingCache.getAbsolutePath());
		currentGraphhopper = new DlmMatchedGraphHopper(dlmMatchingCacheRepository);
		currentGraphhopper.setOSMFile(dlmPbfPath);
		currentGraphhopper.setGraphHopperLocation(configuredRoutingCache.getAbsolutePath());
		currentGraphhopper.setProfiles(profiles);
		currentGraphhopper.setElevationProvider(elevationProvider);
		currentGraphhopper.setMinNetworkSize(minNetworkSize);
		currentGraphhopper.importOrLoad(); // hier sollte der zuvor verschobene Cache geladen werden

		log.info("DLM-Graphopper ist (re-)initialisiert.");
	}

	private static void addCustomTagParsers(DlmMatchedGraphHopper graphhopper) {
		// RadNETZ
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				RadNetzAlltagEncodedValue.getEncodedValueKey(),
				RadNetzAlltagEncodedValue.getCreator(),
				RadNetzAlltagEncodedValue.getApplier()
			));
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				RadNetzFreizeitEncodedValue.getEncodedValueKey(),
				RadNetzFreizeitEncodedValue.getCreator(),
				RadNetzFreizeitEncodedValue.getApplier()
			));
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				RadNetzZielnetzEncodedValue.getEncodedValueKey(),
				RadNetzZielnetzEncodedValue.getCreator(),
				RadNetzZielnetzEncodedValue.getApplier()
			));

		// Kreisnetz
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				KreisnetzAlltagEncodedValue.getEncodedValueKey(),
				KreisnetzAlltagEncodedValue.getCreator(),
				KreisnetzAlltagEncodedValue.getApplier()
			));
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				KreisnetzFreizeitEncodedValue.getEncodedValueKey(),
				KreisnetzFreizeitEncodedValue.getCreator(),
				KreisnetzFreizeitEncodedValue.getApplier()
			));

		// Kommunalnetz
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				KommunalnetzAlltagEncodedValue.getEncodedValueKey(),
				KommunalnetzAlltagEncodedValue.getCreator(),
				KommunalnetzAlltagEncodedValue.getApplier()
			));
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				KommunalnetzFreizeitEncodedValue.getEncodedValueKey(),
				KommunalnetzFreizeitEncodedValue.getCreator(),
				KommunalnetzFreizeitEncodedValue.getApplier()
			));

		// Radrouten
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				RadschnellverbindungEncodedValue.getEncodedValueKey(),
				RadschnellverbindungEncodedValue.getCreator(),
				RadschnellverbindungEncodedValue.getApplier()
			));
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				RadvorrangroutenEncodedValue.getEncodedValueKey(),
				RadvorrangroutenEncodedValue.getCreator(),
				RadvorrangroutenEncodedValue.getApplier()
			));

		// Führungsform
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				RadverkehrsfuehrungEncodedValue.getEncodedValueKey(),
				RadverkehrsfuehrungEncodedValue.getCreator(),
				RadverkehrsfuehrungEncodedValue.getApplier()
			));

		// BarriereForm
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				BarriereFormEncodedValue.getEncodedValueKey(),
				BarriereFormEncodedValue.getCreator(),
				BarriereFormEncodedValue.getApplier()
			));

		// Belagart
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				BelagArtEncodedValue.getEncodedValueKey(),
				BelagArtEncodedValue.getCreator(),
				BelagArtEncodedValue.getApplier()
			));

		// Oberflächenbeschaffenheit
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				OberflaechenbeschaffenheitEncodedValue.getEncodedValueKey(),
				OberflaechenbeschaffenheitEncodedValue.getCreator(),
				OberflaechenbeschaffenheitEncodedValue.getApplier()
			));

		// Beleuchtung
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				BeleuchtungEncodedValue.getEncodedValueKey(),
				BeleuchtungEncodedValue.getCreator(),
				BeleuchtungEncodedValue.getApplier()
			));

		// DTV Pkw
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				DtvPkwEncodedValue.getEncodedValueKey(),
				DtvPkwEncodedValue.getCreator(),
				DtvPkwEncodedValue.getApplier()
			));

		// Breite
		graphhopper.getEncodingManagerBuilder().add(
			new CustomTagParser<>(
				BreiteEncodedValue.getEncodedValueKey(),
				BreiteEncodedValue.getCreator(),
				BreiteEncodedValue.getApplier()
			));
	}

	public DlmMatchedGraphHopper getDlmGraphHopper() {
		if (this.currentGraphhopper == null) {
			log.info("Graphhopper wurde seid Anwendungsstart noch nicht initialisiert...");
			this.updateDlmGraphHopper();
		}
		return this.currentGraphhopper;
	}

	public DlmMatchingCacheRepository getDlmMatchingCacheRepository() {
		if (this.currentGraphhopper == null) {
			log.info("DlmMatchingCacheRepository wurde seid Anwendungsstart noch nicht initialisiert...");
			this.updateDlmGraphHopper();
		}
		return this.dlmMatchingCacheRepository;
	}
}
