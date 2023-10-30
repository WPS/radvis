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

package de.wps.radvis.backend.quellimport.common.schnittstelle.repositoryImpl;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import de.wps.radvis.backend.common.domain.KoordinateAusserhalbDesUnterstuetztenBereichsException;
import de.wps.radvis.backend.common.domain.SimpleFeatureTypeFactory;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.quellimport.common.domain.FeatureImportRepository;
import de.wps.radvis.backend.quellimport.common.domain.GeometrieNormalisierungsException;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeatureImportRepositoryImpl implements FeatureImportRepository {

	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;

	private final Envelope performanceFilterEnvelope;

	private final List<SimpleFeatureIterator> featureIterators;

	public FeatureImportRepositoryImpl(CoordinateReferenceSystemConverter coordinateReferenceSystemConverter,
		Envelope performanceFilterEnvelope) {
		require(coordinateReferenceSystemConverter, notNullValue());

		this.coordinateReferenceSystemConverter = coordinateReferenceSystemConverter;
		this.performanceFilterEnvelope = performanceFilterEnvelope;
		featureIterators = new ArrayList<>();
	}

	/**
	 * WARNUNG: Diese Methode ist NICHT für parallele Nutzung geeignet. In diesem Fall bitte Methode
	 * List<ImportedFeature> getImportedLinestringFeaturesFromShapeFile nutzen
	 */
	@SuppressWarnings("resource")
	@Override
	public Stream<ImportedFeature> getImportedFeaturesFromShapeFiles(String zielGeometryType, QuellSystem quelle,
		Art art,
		File... shapeFiles)
		throws IOException {
		require(shapeFiles.length != 0, "Es muss mindestens eine ShapeFile angegeben werden");
		require(Arrays.asList(Geometry.TYPENAME_LINESTRING, Geometry.TYPENAME_POINT, Geometry.TYPENAME_MULTILINESTRING)
			.contains(zielGeometryType), "geometrieType nicht unterstützt");

		log.info("Importiere Features mit Ziel-GeometrieTyp " + zielGeometryType + "...");

		Stream<ImportedFeature> importedFeatureResultStream = null;
		for (File shapeFile : shapeFiles) {
			Stream<ImportedFeature> importedFeatureStream = generateImportedFeatureStreamFromShapefile(zielGeometryType,
				quelle, art, shapeFile);
			log.info("Shape file: {} loaded", shapeFile.getPath());
			if (importedFeatureResultStream == null) {
				importedFeatureResultStream = importedFeatureStream;
			} else {
				importedFeatureResultStream = Stream.concat(importedFeatureResultStream, importedFeatureStream);
			}
		}
		return importedFeatureResultStream;
	}

	@Override
	public void closeIterators() {
		featureIterators.forEach(SimpleFeatureIterator::close);
	}

	private Stream<ImportedFeature> generateImportedFeatureStreamFromShapefile(String zielGeometryType,
		QuellSystem quelle, Art art, File shapeFile)
		throws IOException {
		log.info("Reading shape file: {}", shapeFile.getPath());

		SimpleFeatureCollection simpleFeatureCollection = readShapeFile(shapeFile);
		CoordinateReferenceSystem sourceCRS = simpleFeatureCollection.getSchema().getCoordinateReferenceSystem();

		KoordinatenReferenzSystem sourceReferenzSystem = null;
		try {
			sourceReferenzSystem = KoordinatenReferenzSystem.of(
				sourceCRS);
		} catch (FactoryException e) {
			log.error("Ermittlung der SRID fehlgeschlagen.", e);
			return Stream.empty();
		}

		boolean flipAxes;
		try {
			flipAxes = checkObAchsenZuTauschen(simpleFeatureCollection, sourceReferenzSystem);
		} catch (KoordinateAusserhalbDesUnterstuetztenBereichsException e) {
			log.error(e.getMessage());
			return Stream.empty();
		}

		try {
			checkObGeometrienNormalisiertWerdenKoennen(simpleFeatureCollection, zielGeometryType);
		} catch (GeometrieNormalisierungsException e) {
			log.error(e.getMessage());
			return Stream.empty();
		}

		SimpleFeatureIterator featureIterator = simpleFeatureCollection.features();
		featureIterators.add(featureIterator);

		return generateImportedFeatureStream(zielGeometryType, quelle, art, featureIterator,
			sourceReferenzSystem, flipAxes);
	}

	private Stream<ImportedFeature> generateImportedFeatureStream(String zielGeometryType, QuellSystem quelle, Art art,
		SimpleFeatureIterator featureIterator, KoordinatenReferenzSystem sourceReferenzSystem,
		boolean flipAxes) {

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new CustomFeatureIterator(featureIterator),
				Spliterator.ORDERED), false)
			.filter(simpleFeature -> simpleFeature.getDefaultGeometry() != null)
			.map(simpleFeature -> getImportedFeatureFromSimpleFeature(zielGeometryType, quelle, art,
				sourceReferenzSystem, flipAxes, simpleFeature))
			.filter(importedFeature -> importedFeature.getGeometrie() != null)
			.filter(importedFeature -> performanceFilterEnvelope == null
				|| performanceFilterEnvelope
				.contains(importedFeature.getGeometrie().getEnvelopeInternal()));
	}

	private ImportedFeature getImportedFeatureFromSimpleFeature(String zielGeometryType, QuellSystem quelle, Art art,
		KoordinatenReferenzSystem sourceReferenzSystem, boolean flipAxes, SimpleFeature simpleFeature) {
		try {
			return createImportedFeature(sourceReferenzSystem, simpleFeature,
				zielGeometryType, quelle, art, flipAxes);
		} catch (KoordinateAusserhalbDesUnterstuetztenBereichsException exception) {
			log.error(exception.getMessage() + " Achsen wurden {}getauscht! Geometrie: {}",
				flipAxes ? "" : "NICHT ",
				simpleFeature.getDefaultGeometry());
			return new ImportedFeature();
		} catch (GeometrieNormalisierungsException exception) {
			log.error(exception.getMessage());
			return new ImportedFeature();
		}
	}

	private void checkObGeometrienNormalisiertWerdenKoennen(SimpleFeatureCollection simpleFeatureCollection,
		String zielGeometryType)
		throws GeometrieNormalisierungsException {
		if (simpleFeatureCollection.isEmpty()) {
			return;
		}
		try (SimpleFeatureIterator featureIterator = simpleFeatureCollection.features()) {
			SimpleFeature simpleFeature = featureIterator.next();

			Geometry sourceGeometry = (Geometry) simpleFeature.getDefaultGeometry();
			checkObGeometrieNormalisiertWerdenKann(sourceGeometry, zielGeometryType);
		}
	}

	private void checkObGeometrieNormalisiertWerdenKann(Geometry geometry, String zielGeometryType)
		throws GeometrieNormalisierungsException {
		String quellGeometryType = geometry.getGeometryType();
		if (!(quellGeometryType.equals(Geometry.TYPENAME_MULTILINESTRING)
			&& zielGeometryType.equals(Geometry.TYPENAME_LINESTRING))
			// Für z.B. Maßnahmen sind MultiLineStrings mit mehreren LSs erlaubt
			&& !(quellGeometryType.equals(Geometry.TYPENAME_MULTILINESTRING)
			&& zielGeometryType.equals(Geometry.TYPENAME_MULTILINESTRING))

			&& !(quellGeometryType.equals(Geometry.TYPENAME_POINT)
			&& zielGeometryType.equals(Geometry.TYPENAME_POINT))) {
			throw new GeometrieNormalisierungsException(
				"Geometrietyp '" + quellGeometryType + "' kann nicht nach '" + zielGeometryType + "' überführt werden");
		}
	}

	/**
	 * Die Reihenfolge der Achsen in den Shape-Files ist für die Geotools nicht erkennbar. Bei unterschiedlichen
	 * Datenquellen können die Achsen getauscht sein, d.h. Northing ist x und Easting ist y oder umegekehrt. Dies kann
	 * bei der Transformation zu Koordinaten führen, die inkorrekt sind.
	 * <p>
	 * Ansatz: Um dies zu prüfen, wird geschaut, ob die konvertierten Koordinaten einer Geometrie innerhalb von
	 * Baden-Württemberg ist. Hierbei wird so lange über die enthaltenen Geometrien iteriert bis die konvertierte
	 * Geometrie entweder vor oder nach Achsentausch in BW liegt. Falls es keine solche Koordinate gibt, wird eine
	 * KoordinateAusserhalbDesUnterstuetztenBereichsException geworfen.
	 */
	private boolean checkObAchsenZuTauschen(SimpleFeatureCollection simpleFeatureCollection,
		KoordinatenReferenzSystem sourceReferenzSystem) throws KoordinateAusserhalbDesUnterstuetztenBereichsException {
		if (simpleFeatureCollection.isEmpty()) {
			return false;
		}
		try (SimpleFeatureIterator featureIterator = simpleFeatureCollection.features()) {
			while (featureIterator.hasNext()) {
				SimpleFeature simpleFeature = featureIterator.next();
				Geometry sourceGeometry = (Geometry) simpleFeature.getDefaultGeometry();

				if (sourceGeometry != null) {
					sourceGeometry.setSRID(sourceReferenzSystem.getSrid());
					Geometry transformedNonFlippedGeometry = coordinateReferenceSystemConverter
						.transformGeometry(sourceGeometry, KoordinatenReferenzSystem.ETRS89_UTM32_N);
					if (coordinateReferenceSystemConverter.sindKoordinatenPlausibel(transformedNonFlippedGeometry)) {
						return false;
					}

					normalizeCoordinates(sourceGeometry, true);
					Geometry transformedFlippedGeometry = coordinateReferenceSystemConverter
						.transformGeometry(sourceGeometry, KoordinatenReferenzSystem.ETRS89_UTM32_N);

					if (coordinateReferenceSystemConverter.sindKoordinatenPlausibel(transformedFlippedGeometry)) {
						return true;
					}
				}
			}
		}
		throw new KoordinateAusserhalbDesUnterstuetztenBereichsException(
			"Die Importkoordinaten keiner Geometrie konnten im Bereich von Baden-Württemberg verortet werden.");
	}

	private void normalizeCoordinates(Geometry geometry, boolean flipAxes) {
		for (Coordinate c : geometry.getCoordinates()) {
			if (flipAxes) {
				c.setCoordinate(new Coordinate(c.y, c.x));
			} else {
				// Wir setzen die Coordinate erneut mit denselben Werten für x & y,
				// um ggf. vorhandene Werte für m & z zu entfernen!
				c.setCoordinate(new Coordinate(c.x, c.y));
			}
		}
	}

	private ImportedFeature createImportedFeature(KoordinatenReferenzSystem sourceCRS, SimpleFeature simpleFeature,
		String zielGeometryType, QuellSystem quelle, Art art, boolean flipAxes)
		throws KoordinateAusserhalbDesUnterstuetztenBereichsException, GeometrieNormalisierungsException {
		require(simpleFeature.getDefaultGeometry() != null);
		String fachId = simpleFeature.getID();
		Geometry sourceGeometry = (Geometry) simpleFeature.getDefaultGeometry();

		normalizeCoordinates(sourceGeometry, flipAxes);
		sourceGeometry.setSRID(sourceCRS.getSrid());
		Geometry transformedGeometry = coordinateReferenceSystemConverter
			.transformGeometry(sourceGeometry,
				KoordinatenReferenzSystem.ETRS89_UTM32_N);

		if (!coordinateReferenceSystemConverter.sindKoordinatenPlausibel(transformedGeometry)) {
			throw new KoordinateAusserhalbDesUnterstuetztenBereichsException(
				"Die Importkoordinaten der Geometrie konnten nicht im Bereich von Baden-Württemberg verortet werden.");
		}

		if (transformedGeometry.getGeometryType().equals(Geometry.TYPENAME_MULTILINESTRING)
			&& zielGeometryType.equals(Geometry.TYPENAME_LINESTRING)) {
			transformedGeometry = normalizeMultiLineStringToLineString(transformedGeometry);
		}

		Map<String, Object> attribute = new HashMap<>();
		simpleFeature.getProperties().forEach(property -> {
			// Die Geometrie einer Shapefile wird unter dem Attribut 'the_geom' abgelegt.
			// Wir wollen die Geometrie allerdings nicht als gewöhnliches Attribut behandeln, daher überspringen wir das
			// Attribut.
			if (!property.getName().toString().equals(SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_THE_GEOM)) {
				attribute.put(property.getName().toString(), property.getValue());
			} else {
				attribute.put(property.getName().toString(), property.getValue().toString());
			}
		});
		return new ImportedFeature(fachId, transformedGeometry, attribute, LocalDateTime.now(), quelle, art);
	}

	private Geometry normalizeMultiLineStringToLineString(Geometry geometry) throws GeometrieNormalisierungsException {
		MultiLineString multiLineString = (MultiLineString) geometry;
		if (multiLineString.getNumGeometries() > 1) {
			throw new GeometrieNormalisierungsException(
				"Es werden keine Multilinestrings mit mehr als einem Linestring erwartet für Ziel-GeometrieTyp "
					+ Geometry.TYPENAME_LINESTRING);
		}
		LineString geometryN = (LineString) multiLineString.getGeometryN(0);
		geometryN.setSRID(geometry.getSRID());

		return geometryN;
	}

	private SimpleFeatureCollection readShapeFile(File shpFile) throws IOException {
		ShapefileDataStore dataStore = new ShapefileDataStore(shpFile.toURI().toURL());
		try {
			dataStore.setCharset(StandardCharsets.UTF_8);
			return dataStore.getFeatureSource().getFeatures();
		} finally {
			dataStore.dispose();
		}
	}

	private class CustomFeatureIterator implements Iterator<SimpleFeature> {

		private SimpleFeatureIterator simpleFeatureIterator;

		public CustomFeatureIterator(SimpleFeatureIterator simpleFeatureIterator) {

			this.simpleFeatureIterator = simpleFeatureIterator;
		}

		@Override
		public boolean hasNext() {
			return simpleFeatureIterator.hasNext();
		}

		@Override
		public SimpleFeature next() {
			return simpleFeatureIterator.next();
		}
	}
}
