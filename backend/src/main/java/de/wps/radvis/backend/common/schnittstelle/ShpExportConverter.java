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

package de.wps.radvis.backend.common.schnittstelle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import de.wps.radvis.backend.common.domain.SimpleFeatureTypeFactory;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShpExportConverter implements ExportConverter {

	private final ShapeFileRepository shapeFileRepository;

	private final ShapeZipService shapeZipService;

	public ShpExportConverter(ShapeFileRepository shapeFileRepository, ShapeZipService shapeZipService) {
		this.shapeFileRepository = shapeFileRepository;
		this.shapeZipService = shapeZipService;
	}

	@Override
	public byte[] convert(List<ExportData> data) {
		if (data.isEmpty()) {
			return new byte[0];
		}

		ByteArrayOutputStream result = new ByteArrayOutputStream();
		File exportDir = null;
		try {
			File finalExportDir = java.nio.file.Files.createTempDirectory("export").toFile();
			finalExportDir.deleteOnExit();
			exportDir = finalExportDir;

			Map<String, List<ExportData>> dataGroupedByGeometryType = data
				.stream()
				.flatMap(ShpExportConverter::cleanUpGeometries)
				.collect(Collectors.groupingBy(d -> d.getGeometry().getGeometryType()));

			dataGroupedByGeometryType.forEach((geometryType, exportData) -> {
				try {
					log.debug("Erstelle Shapefile für Geometrietyp " + geometryType);
					exportAsShpToDir(exportData, finalExportDir, geometryType);
				} catch (IOException e) {
					log.warn("SHP-Export von Geometrietyp {} nach {} fehlgeschlagen: {}", geometryType, finalExportDir,
						e.getMessage());
					log.warn(e.getMessage(), e);
				}
			});

			shapeZipService.zip(result, exportDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (exportDir != null) {
				exportDir.delete();
			}
		}
		return result.toByteArray();
	}

	/**
	 * Zum einen wollen wir Points und LineStrings in die jeweilige Multi-Form konvertieren, zum anderen machen aber
	 * auch GeometryCollections als Geometrie Probleme. Diese spalten wir auf und verpacken die Einzelteile wieder in
	 * die passenden Multi-Geometrien.
	 * Datei werden nur Points, LineStrings und GeometryCollections so behandelt, alle anderen Typen (z.B. Polygone)
	 * bleiben unverändert.
	 */
	private static Stream<ExportData> cleanUpGeometries(ExportData exportData) {
		GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
		Geometry geometry;
		List<ExportData> newExportdata = new ArrayList<>();

		switch (exportData.getGeometry().getGeometryType()) {
		case Geometry.TYPENAME_LINESTRING:
			geometry = geometryFactory.createMultiLineString(
				new LineString[] { (LineString) exportData.getGeometry() });
			newExportdata.add(new ExportData(geometry, exportData.getProperties()));
			break;
		case Geometry.TYPENAME_POINT:
			geometry = geometryFactory.createMultiPoint(
				new Point[] { (Point) exportData.getGeometry() });
			newExportdata.add(new ExportData(geometry, exportData.getProperties()));
			break;
		case Geometry.TYPENAME_GEOMETRYCOLLECTION:
			List<LineString> lineStrings = new LinkedList<>();
			List<Point> points = new LinkedList<>();
			List<Geometry> resltGeometries = new LinkedList<>();

			// Geometrien einzelnd aufsammeln um sie danach zusammen zu fassen.
			GeometryCollection geometries = ((GeometryCollection) exportData.getGeometry());
			for (int i = 0; i < geometries.getNumGeometries(); i++) {
				Geometry geom = geometries.getGeometryN(i);
				switch (geom.getGeometryType()) {
				case Geometry.TYPENAME_LINESTRING:
					lineStrings.add((LineString) geom);
					break;
				case Geometry.TYPENAME_POINT:
					points.add((Point) geom);
					break;
				default:
					resltGeometries.add(geom);
				}
			}

			// Wandle alle LineStirngs und Points in jeweils eine Multi-Geometrie um.
			if (!lineStrings.isEmpty()) {
				resltGeometries.add(geometryFactory.createMultiLineString(lineStrings.toArray(LineString[]::new)));
			}
			if (!points.isEmpty()) {
				resltGeometries.add(geometryFactory.createMultiPoint(points.toArray(Point[]::new)));
			}

			resltGeometries.forEach(g -> newExportdata.add(new ExportData(g, exportData.getProperties())));

			break;
		default:
			newExportdata.add(exportData);
		}

		return newExportdata.stream();
	}

	private void exportAsShpToDir(List<ExportData> data, File exportDir, String fileSuffix) throws IOException {
		File exportShpFile = null;

		data = data.stream().map(exportData -> {
				HashMap<String, String> newProperties = new HashMap<>();
				exportData.getProperties().forEach((key, value) -> newProperties.put(escapeSpecialCharacters(key), value));
				return new ExportData(exportData.getGeometry(), newProperties);
			})
			.collect(Collectors.toList());

		try {
			List<ExportData> trimmedData = this.trimAttributeKeys(data);
			SimpleFeatureType simpleFeatureType = SimpleFeatureTypeFactory.createSimpleFeatureType(
				trimmedData.get(0).getProperties(),
				toFeatureType(trimmedData.get(0).getGeometry().getGeometryType()),
				SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_THE_GEOM);

			List<SimpleFeature> simpleFeatures = new ArrayList<>();
			for (int i = 0; i < trimmedData.size(); i++) {
				ExportData singleData = trimmedData.get(i);
				SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureType);
				simpleFeatureBuilder.set(SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_THE_GEOM,
					singleData.getGeometry());
				singleData.getProperties().forEach(simpleFeatureBuilder::set);
				simpleFeatures.add(simpleFeatureBuilder.buildFeature(String.valueOf(i)));
			}

			exportShpFile = new File(exportDir, "export-" + fileSuffix + ".shp");
			exportShpFile.deleteOnExit();
			shapeFileRepository.writeShape(exportDir, exportShpFile, simpleFeatures);
		} catch (IOException e) {
			if (exportShpFile != null) {
				exportShpFile.delete();
			}
			throw e;
		}
	}

	private static String escapeSpecialCharacters(String name) {
		return name
			.replaceAll("-", "")
			.replaceAll("/", "")
			.replaceAll("ä", "ae")
			.replaceAll("Ä", "Ae")
			.replaceAll("ö", "oe")
			.replaceAll("Ö", "Oe")
			.replaceAll("ü", "ue")
			.replaceAll("Ü", "Ue")
			.replaceAll("ß", "ss");
	}

	// Shapefiles unterstuetzen nur Attribut-Keys mit maximal 10 Zeichen Laenge. Das automatische Abschneiden durch
	// GeoTools bewirkt, dass die Values nicht wiedergefunden werden und fuer die betroffenen Attribute nur NULL-Werte
	// ins Shapefile geschrieben werden (Bug?). Daher hier ein manuelles Abschneiden der Attribute-Keys unter Vermeidung
	// von Duplikaten
	private List<ExportData> trimAttributeKeys(List<ExportData> data) {
		List<ExportData> trimmedData = new ArrayList<>(data.size());
		data.forEach(singleData -> {
			Map<String, String> trimmedProperties = new HashMap<>();
			singleData.getProperties().forEach((key, value) -> {
				String trimmedKey = key.substring(0, Math.min(key.length(), 10));
				if (trimmedProperties.containsKey(trimmedKey)) {
					// Annahme: Nur maximal 10 Attribute mit denselben 10 Anfangsbuchstaben
					String potentialExistingSuffix = trimmedKey.substring(trimmedKey.length() - 1);
					int suffixNumber;
					try {
						suffixNumber = Integer.parseInt(potentialExistingSuffix);
						suffixNumber++;
					} catch (NumberFormatException e) {
						// Noch kein Zahlensuffix vorhanden
						suffixNumber = 2;
					}
					trimmedKey = trimmedKey.substring(0, trimmedKey.length() - 1) + suffixNumber;
				}
				trimmedProperties.put(trimmedKey, value);
			});
			trimmedData.add(new ExportData(singleData.getGeometry(), trimmedProperties));
		});
		return trimmedData;
	}

	private Class<? extends Geometry> toFeatureType(String geometryType) {
		switch (geometryType) {
		case Geometry.TYPENAME_GEOMETRYCOLLECTION:
			return GeometryCollection.class;
		case Geometry.TYPENAME_MULTILINESTRING:
			return MultiLineString.class;
		case Geometry.TYPENAME_LINESTRING:
			return LineString.class;
		case Geometry.TYPENAME_MULTIPOINT:
			return MultiPoint.class;
		case Geometry.TYPENAME_LINEARRING:
			return LinearRing.class;
		case Geometry.TYPENAME_POLYGON:
			return Polygon.class;
		case Geometry.TYPENAME_MULTIPOLYGON:
			return MultiPolygon.class;
		}
		throw new IllegalArgumentException("GeometryType " + geometryType + " unbekannt");
	}

	@Override
	public String getDateinamenSuffix() {
		return "_shp_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".zip";
	}
}
