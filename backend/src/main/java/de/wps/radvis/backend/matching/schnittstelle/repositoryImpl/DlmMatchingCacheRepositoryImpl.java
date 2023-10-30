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

import de.wps.radvis.backend.matching.domain.DlmMatchingCacheRepository;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.SeitenbezogeneProfilEigenschaften;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DlmMatchingCacheRepositoryImpl implements DlmMatchingCacheRepository {

	private final String mappingCacheVerzeichnis;
	private final File mappingCacheDateiOsmWays;
	private final File mappingCacheDateiProfilEigenschaften;
	private final ObjectMapper objectMapper;

	public DlmMatchingCacheRepositoryImpl(String mappingCacheVerzeichnis) {
		require(mappingCacheVerzeichnis, notNullValue());
		mappingCacheDateiOsmWays = new File(mappingCacheVerzeichnis + File.separator + "mapping.cache");
		mappingCacheDateiProfilEigenschaften = new File(
			mappingCacheVerzeichnis + File.separator + "mapping.profil.cache");
		objectMapper = new ObjectMapper();

		this.mappingCacheVerzeichnis = mappingCacheVerzeichnis;
	}

	@Override
	public void save(Map<Integer, OsmWayId> mappingCache,
		Map<Integer, SeitenbezogeneProfilEigenschaften> mappingCacheProfil) {
		File directory = new File(mappingCacheVerzeichnis);
		if (!directory.exists()) {
			directory.mkdir();
		}

		try {
			objectMapper.writeValue(mappingCacheDateiOsmWays, mappingCache);
			objectMapper.writeValue(mappingCacheDateiProfilEigenschaften, mappingCacheProfil);
			ensure(hasCache());
		} catch (IOException e) {
			log.error("Fehler beim Dateizugriff auf Datei entweder {} oder {}.", mappingCacheDateiOsmWays,
				mappingCacheDateiProfilEigenschaften);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<Integer, OsmWayId> getWayIds() {
		require(hasCache());
		try {
			return objectMapper.readValue(mappingCacheDateiOsmWays, new TypeReference<>() {
			});
		} catch (IOException e) {
			log.error("Fehler beim Dateizugriff auf Datei entweder {} oder {}.", mappingCacheDateiOsmWays,
				mappingCacheDateiProfilEigenschaften);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<Integer, SeitenbezogeneProfilEigenschaften> getProfilEigenschaften() {
		require(hasCache());
		try {
			return objectMapper.readValue(mappingCacheDateiProfilEigenschaften, new TypeReference<>() {
			});
		} catch (IOException e) {
			log.error("Fehler beim Dateizugriff auf Datei entweder {} oder {}.", mappingCacheDateiOsmWays,
				mappingCacheDateiProfilEigenschaften);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasCache() {
		return (mappingCacheDateiOsmWays.exists()
			&& mappingCacheDateiOsmWays.length() > 0
			&& mappingCacheDateiProfilEigenschaften.exists()
			&& mappingCacheDateiProfilEigenschaften.length() > 0);
	}

	@Override
	public void deleteAll() {
		try {
			Files.deleteIfExists(mappingCacheDateiOsmWays.toPath());
			Files.deleteIfExists(mappingCacheDateiProfilEigenschaften.toPath());
			ensure(!hasCache());
		} catch (IOException e) {
			log.error("Fehler beim Löschen von Datei entweder {} oder {}.", mappingCacheDateiOsmWays,
				mappingCacheDateiProfilEigenschaften);
			throw new RuntimeException(e);
		}
	}

	@Override
	public LocalDateTime getTimestamp() {
		require(hasCache());
		// Hier ignorieren wir den Timestamp von weiteren Cache-Dateien.
		// Koennte das bei der Migration problematisch sein?
		// Methode findet nur in Tests verwendung.
		Instant epochMilli = Instant.ofEpochMilli(mappingCacheDateiOsmWays.lastModified());
		return LocalDateTime.ofInstant(epochMilli, ZoneId.systemDefault());
	}
}
