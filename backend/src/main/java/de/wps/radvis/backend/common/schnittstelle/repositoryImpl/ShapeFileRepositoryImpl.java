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

package de.wps.radvis.backend.common.schnittstelle.repositoryImpl;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.IndexFile;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeEncodingException;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeUnreadableException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShapeFileRepositoryImpl implements ShapeFileRepository {

	public static Charset ENCODING_UTF8 = StandardCharsets.UTF_8;

	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;

	public ShapeFileRepositoryImpl(CoordinateReferenceSystemConverter coordinateReferenceSystemConverter) {
		this.coordinateReferenceSystemConverter = coordinateReferenceSystemConverter;
	}

	@Override
	public Stream<SimpleFeature> readShape(File shpFile) throws IOException, ShapeProjectionException {
		ShapefileDataStore store = new ShapefileDataStore(shpFile.toURI().toURL());

		store.setCharset(ENCODING_UTF8);
		KoordinatenReferenzSystem koordinatenReferenzSystem;
		try {
			koordinatenReferenzSystem = KoordinatenReferenzSystem.of(
				store.getSchema().getCoordinateReferenceSystem());
		} catch (FactoryException e) {
			throw new ShapeProjectionException(
				"Koordinatenreferenzsystem nicht ermittelbar. Bitte überprüfen sie die .prj-Datei in der Shape-Zip.");
		}

		SimpleFeatureCollection features;
		try {
			features = store.getFeatureSource().getFeatures();
		} catch (IOException e) {
			store.dispose();
			throw e;
		}

		SimpleFeatureIterator featureIterator = features.features();
		return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(new CustomFeatureIterator(featureIterator), Spliterator.ORDERED),
			false)
			.onClose(() -> {
				featureIterator.close();
				store.dispose();
			})
			.peek(setzeGeometryFactoryAufGeometrie(koordinatenReferenzSystem));
	}

	@Override
	public boolean writeShape(File shpDirectory, File shpFile, List<SimpleFeature> features) throws IOException {
		require(shpDirectory.isDirectory());
		if (features.size() == 0) {
			return false;
		}

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<>();
		URL url = shpFile.toURI().toURL();

		params.put("url", url);
		params.put("charset", ENCODING_UTF8.toString());
		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

		Transaction transaction = new DefaultTransaction("create");
		try {
			SimpleFeatureType featureType = features.get(0).getFeatureType();
			newDataStore.createSchema(featureType);

			String typeName = newDataStore.getTypeNames()[0];
			SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
				SimpleFeatureCollection collection = new ListFeatureCollection(featureType, features);
				featureStore.setTransaction(transaction);
				featureStore.addFeatures(collection);

				// write CPG File (Optionale file die das Encoding angibt)
				File cpgFile = new File(shpDirectory, FilenameUtils.removeExtension(shpFile.getName()) + ".cpg");
				cpgFile.createNewFile();
				BufferedWriter writer = new BufferedWriter(new FileWriter(cpgFile));
				writer.write(ENCODING_UTF8.toString());
				writer.close();

				transaction.commit();
				return true;
			}
			return false;
		} finally {
			try {
				transaction.close();
			} catch (IOException e) {
				log.error("Kann Transaction nicht schließen nach Schließen des Streams");
			}
			newDataStore.dispose();
		}
	}

	@Override
	public void validate(File shpDirectory)
		throws ShapeEncodingException, ShapeUnreadableException, IOException, ShapeProjectionException {
		Optional<File> shpFile = Arrays.stream(shpDirectory.listFiles()).filter(file -> file.getName().endsWith(".shp"))
			.findFirst();

		if (shpFile.isEmpty()) {
			throw new ShapeUnreadableException();
		}

		// validate Encoding
		validateEncodingIsUTF8(shpFile.get());

		// validate Readability via geotools
		SimpleFeatureCollection simpleFeatureCollection = validateAndRead(shpFile.get());

		// validate Projection
		validateProjection(simpleFeatureCollection);
	}

	private void validateEncodingIsUTF8(File shpFile) throws ShapeEncodingException {
		Optional<Charset> charset;
		try {
			charset = readCharsetFromCpg(shpFile);
		} catch (IOException e) {
			throw new ShapeEncodingException();
		}

		if (charset.isPresent() && !charset.get().equals(ENCODING_UTF8)) {
			throw new ShapeEncodingException(charset.get().name());
		}
		// Wenn charset nicht present ist werfe keine Exception sondern probiere den Import mit der Annahme, dass es
		// sich um UTF-8 handelt
	}

	private Optional<Charset> readCharsetFromCpg(File shpFile) throws IOException, ShapeEncodingException {
		File shpDirectory = shpFile.getParentFile();
		require(shpDirectory.isDirectory());

		File cpgFile = new File(shpDirectory, FilenameUtils.removeExtension(shpFile.getName()) + ".cpg");

		if (!cpgFile.exists()) {
			return Optional.empty();
		}

		String charsetName = FileUtils.readFileToString(cpgFile, "UTF-8");
		Charset charset;
		try {
			charset = Charset.forName(charsetName);
		} catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
			throw new ShapeEncodingException(charsetName);
		}
		return Optional.of(charset);
	}

	private SimpleFeatureCollection validateAndRead(File shpFile) throws ShapeUnreadableException, IOException {
		// -> zunächst wird nur geprüft, ob Geotools ohne Fehler auf die Dateien zugreifen kann,
		// da ansonsten Ressourcen nicht ordentlich geschlossen werden.
		IndexFile file = null;
		ShpFiles shpFiles = null;
		try {
			shpFiles = new ShpFiles(shpFile.toURI().toURL());
			file = new IndexFile(shpFiles, false);

		} catch (Exception e) {
			throw new ShapeUnreadableException();
		} finally {
			if (file != null) {
				file.close();
			}
			if (shpFiles != null) {
				shpFiles.dispose();
			}
		}

		// Validieren, dass Features mittels Geotools aus der ShapeFile eingelesen werden können
		ShapefileDataStore dataStore = new ShapefileDataStore(shpFile.toURI().toURL());
		SimpleFeatureCollection simpleFeatureCollection;
		try {
			dataStore.setCharset(StandardCharsets.UTF_8);
			simpleFeatureCollection = dataStore.getFeatureSource().getFeatures();
		} catch (Exception e) {
			throw new ShapeUnreadableException();
		} finally {
			dataStore.dispose();
		}

		return simpleFeatureCollection;
	}

	private void validateProjection(SimpleFeatureCollection simpleFeatureCollection) throws ShapeProjectionException {
		CoordinateReferenceSystem coordinateReferenceSystem = simpleFeatureCollection.getSchema()
			.getCoordinateReferenceSystem();

		KoordinatenReferenzSystem koordinatenReferenzSystem;
		try {
			koordinatenReferenzSystem = KoordinatenReferenzSystem.of(
				coordinateReferenceSystem);
		} catch (FactoryException e) {
			throw new ShapeProjectionException();
		}

		if (!koordinatenReferenzSystem.equals(KoordinatenReferenzSystem.ETRS89_UTM32_N)
			&& !koordinatenReferenzSystem.equals(KoordinatenReferenzSystem.DHDN_3_Degree_Gauss_Zone_3)) {
			throw new ShapeProjectionException(koordinatenReferenzSystem);
		}
	}

	@NonNull
	@Override
	public Consumer<SimpleFeature> setzeGeometryFactoryAufGeometrie(
		KoordinatenReferenzSystem koordinatenReferenzSystem) {
		return feature -> {
			Geometry defaultGeometry = (Geometry) feature.getDefaultGeometry();
			if (defaultGeometry != null) {
				feature.setDefaultGeometry(koordinatenReferenzSystem.getGeometryFactory()
					.createGeometry(defaultGeometry));
			}
		};
	}

	@Override
	public SimpleFeature transformGeometryToUTM32(SimpleFeature simpleFeature) {
		require(simpleFeature.getDefaultGeometry(), notNullValue());

		Geometry UTM32Geometry = coordinateReferenceSystemConverter.transformGeometry(
			(Geometry) simpleFeature.getDefaultGeometry(),
			KoordinatenReferenzSystem.ETRS89_UTM32_N);

		simpleFeature.setDefaultGeometry(UTM32Geometry);
		setzeGeometryFactoryAufGeometrie(KoordinatenReferenzSystem.ETRS89_UTM32_N)
			.accept(simpleFeature);
		return simpleFeature;
	}

	private class CustomFeatureIterator implements Iterator<SimpleFeature> {

		private final SimpleFeatureIterator simpleFeatureIterator;

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
