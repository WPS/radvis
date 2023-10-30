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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.wps.radvis.backend.matching.domain.OsmMatchingCacheRepository;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OsmMatchingCacheRepositoryImpl implements OsmMatchingCacheRepository {

	private final String mappingCacheVerzeichnis;
	private final File mappingCacheDateiOsmWays;
	private final ObjectMapper objectMapper;

	public OsmMatchingCacheRepositoryImpl(String mappingCacheVerzeichnis) {
		require(mappingCacheVerzeichnis, notNullValue());
		mappingCacheDateiOsmWays = new File(mappingCacheVerzeichnis + File.separator + "mapping.cache");
		objectMapper = new ObjectMapper();

		this.mappingCacheVerzeichnis = mappingCacheVerzeichnis;
	}

	@Override
	public void save(Map<Integer, LinearReferenzierteOsmWayId> mappingCache) {
		File directory = new File(mappingCacheVerzeichnis);
		if (!directory.exists()) {
			directory.mkdir();
		}

		try {
			objectMapper.writeValue(mappingCacheDateiOsmWays, mappingCache);
			ensure(hasCache());
		} catch (IOException e) {
			log.error("Fehler beim Dateizugriff auf Datei {}", mappingCacheDateiOsmWays);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<Integer, LinearReferenzierteOsmWayId> get() {
		require(hasCache());
		try {
			return objectMapper.readValue(mappingCacheDateiOsmWays,
				new TypeReference<>() {
				});
		} catch (IOException e) {
			log.error("Fehler beim Dateizugriff auf Datei {}.", mappingCacheDateiOsmWays);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasCache() {
		return (mappingCacheDateiOsmWays.exists()
			&& mappingCacheDateiOsmWays.length() > 0);
	}

	@Override
	public void deleteAll() {
		try {
			Files.deleteIfExists(mappingCacheDateiOsmWays.toPath());
			ensure(!hasCache());
		} catch (IOException e) {
			log.error("Fehler beim Löschen von Datei {}.", mappingCacheDateiOsmWays);
			throw new RuntimeException(e);
		}
	}

	@Override
	public LocalDateTime getTimestamp() {
		require(hasCache());

		Instant epochMilli = Instant.ofEpochMilli(mappingCacheDateiOsmWays.lastModified());
		return LocalDateTime.ofInstant(epochMilli, ZoneId.systemDefault());
	}
}
