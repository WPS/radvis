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

import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.GHDirectory;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.matching.domain.entity.ElevationDataFileNotFoundException;
import de.wps.radvis.backend.matching.domain.entity.ElevationTile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.comparator.DefaultFileComparator;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;

import jakarta.validation.constraints.NotNull;
import java.awt.image.Raster;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ElevationDataSpatialIndexRepository {

	private static final int WIDTH = 1000;
	private static final int HEIGHT = 1000;

	private final File tiffTilesVerzeichnis;
	private final File cacheDir;

	@Getter
	private final STRtree index;

	public ElevationDataSpatialIndexRepository(@NotNull String tiffTilesVerzeichnisPfad,
		@NotNull String cacheDirPfad) {
		this.tiffTilesVerzeichnis = new File(tiffTilesVerzeichnisPfad);
		this.cacheDir = new File(cacheDirPfad);
		if (!this.tiffTilesVerzeichnis.exists()) {
			throw new RuntimeException(
				"Das TIFF-Tiles-Verzeichnis " + tiffTilesVerzeichnisPfad + " existiert nicht.");
		}

		this.index = new STRtree(8);

		if (!cacheDir.exists()) {
			try {
				Files.createDirectories(cacheDir.toPath());
			} catch (IOException e) {
				throw new RuntimeException("Konnte CacheDirectory " + cacheDir + " nicht erstellen.");
			}
		}

		if (cacheDir.listFiles().length == 0) {
			log.info("Importiere ElevationData aus Dateien.");
			buildCache();
		}
		importiereAusCache();
		index.build();
	}

	public ElevationTile findeTileZuKoordinaten(Coordinate coordinate) throws ElevationDataFileNotFoundException {
		List<?> results = index.query(new Envelope(coordinate));

		if (results.isEmpty()) {
			throw new ElevationDataFileNotFoundException("Es wurden keine Höhendaten gefunden zur Koordinate");
		}
		if (results.size() > 1) {
			log.warn("Bei der Höhendatensuche wurde mehr als eine passende Datei gefunden!");
		}

		return (ElevationTile) results.get(0);
	}

	private void buildCache() {
		final var ghCacheDir = new GHDirectory(cacheDir.getAbsolutePath(), DAType.MMAP);

		final FilenameFilter tifFileNameFilter = (dir, name) -> name.endsWith("bw.tif");
		final FilenameFilter tifDirectoryNameFilter = (dir, name) -> name.startsWith("s32");
		final var listFiles = tiffTilesVerzeichnis.listFiles(tifDirectoryNameFilter);

		final var directoryIndex = new AtomicInteger(1);
		Arrays.stream(listFiles)
			.sorted(DefaultFileComparator.DEFAULT_COMPARATOR).forEach(directory -> {
				log.info("Verzeichnis {} wird verarbeitet ({} von {}).", directory.getName(), directoryIndex,
					listFiles.length);
				Arrays.stream(directory.listFiles(tifFileNameFilter))
					.parallel()
					.forEach(file -> {
						final var fileNameCoords = file.getName().split("_");
						final var x = Integer.parseInt(fileNameCoords[2]);
						final var y = Integer.parseInt(fileNameCoords[3]);
						final var cacheFileName = getCacheFileName(x, y);

						try {
							final var heights = ghCacheDir.find(cacheFileName, DAType.MMAP);
							final var raster = readTiffFile(file);

							// short = 2 bytes
							heights.create(2 * WIDTH * HEIGHT);

							fillDataAccessWithElevationData(raster, heights);
						} catch (IOException e) {
							log.error(e.getMessage());
						}
					});
				directoryIndex.getAndIncrement();
			});
		ghCacheDir.close();
	}

	private void importiereAusCache() {
		log.info("Starte Import der Höhendaten.");
		Arrays.stream(cacheDir.listFiles()).forEach(file -> {
			final var fileNameCoords = file.getName().split("_");
			final var x = Integer.parseInt(fileNameCoords[0]);
			final var y = Integer.parseInt(fileNameCoords[1]);

			addToIndex(x, y, file.getName());
		});
		log.info("Höhendaten Import beendet.");
	}

	private void addToIndex(int x, int y, String cacheFileName) {
		final var realMinX = x * 1000;
		final var realMaxX = realMinX + 1000;
		final var realMinY = y * 1000;
		final var realMaxY = realMinY + 1000;

		final var tile = new ElevationTile(realMinX, realMinY, WIDTH, HEIGHT, cacheFileName);
		final var envelope = new Envelope(realMinX, realMaxX, realMinY, realMaxY);
		index.insert(envelope, tile);
	}

	private Raster readTiffFile(File file) throws IOException {
		final var hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeotoolsCRS());
		final var reader = new GeoTiffReader(file, hints);
		final var coverage = reader.read(null);
		final var raster = coverage.getRenderedImage().getData();
		reader.dispose();
		coverage.dispose(true);
		return raster;
	}

	private String getCacheFileName(int x, int y) {
		return x + "_" + y;
	}

	private void fillDataAccessWithElevationData(Raster raster, DataAccess heights) {
		final var height = raster.getHeight();
		final var width = raster.getWidth();
		var x = 0;
		var y = 0;

		try (heights) {
			for (y = 0; y < height; y++) {
				for (x = 0; x < width; x++) {
					// Wir müssen hier den y-Wert von der HEIGHT abziehen, da die
					// ElevationTiles ansonsten horizontal gespiegelt sind
					short val = (short) raster.getPixel(x, height - 1 - y, (int[]) null)[0];
					if (val < -1000 || val > 12000) {
						val = Short.MIN_VALUE;
					}
					heights.setShort(2 * ((long) y * WIDTH + x), val);
				}
			}
			heights.flush();
		} catch (Exception ex) {
			throw new RuntimeException("Problem at x:" + x + ", y:" + y, ex);
		}
	}

}

