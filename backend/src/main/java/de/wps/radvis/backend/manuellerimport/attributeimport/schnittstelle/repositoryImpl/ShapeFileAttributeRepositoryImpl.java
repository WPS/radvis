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

package de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.repositoryImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFiles;

import de.wps.radvis.backend.manuellerimport.attributeimport.domain.repository.ShapeFileAttributeRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShapeFileAttributeRepositoryImpl implements ShapeFileAttributeRepository {

	public static Charset ENCODING_UTF8 = StandardCharsets.UTF_8;

	@Override
	public Set<String> getAttributnamen(File shpFile) throws IOException {
		Set<String> attributNames = new HashSet<>();
		ShpFiles shp = new ShpFiles(shpFile);

		ShapefileDataStore store;
		try {
			store = new ShapefileDataStore(shpFile.toURI().toURL());
			store.setCharset(ENCODING_UTF8);
		} catch (IOException e) {
			shp.dispose();
			throw e;
		}

		DbaseFileReader dbf;
		try {
			dbf = new DbaseFileReader(shp, store.isMemoryMapped(), store.getCharset());
		} catch (IOException e) {
			shp.dispose();
			store.dispose();
			throw e;
		}

		DbaseFileHeader header = dbf.getHeader();
		for (int i = 0; i < header.getNumFields(); i++) {
			attributNames.add(header.getFieldName(i));
		}

		try {
			dbf.close();
		} catch (IOException e) {
			log.error("Kann DBF Datei nicht schliessen.");
		}
		shp.dispose();
		store.dispose();

		return attributNames;
	}

	@Override
	public Stream<String> getAttributWerte(File shpFile, String attributName) throws IOException {
		ShpFiles shp = new ShpFiles(shpFile);

		ShapefileDataStore store;
		try {
			store = new ShapefileDataStore(shpFile.toURI().toURL());
			store.setCharset(ENCODING_UTF8);
		} catch (IOException e) {
			shp.dispose();
			throw e;
		}

		DbaseFileReader dbf;
		try {
			dbf = new DbaseFileReader(shp, store.isMemoryMapped(), store.getCharset());
		} catch (IOException e) {
			shp.dispose();
			store.dispose();

			throw e;
		}

		int fieldNum = 0;
		DbaseFileHeader header = dbf.getHeader();
		for (int i = 0; i < header.getNumFields(); i++) {
			if (header.getFieldName(i).equals(attributName)) {
				fieldNum = i;
			}
		}

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new CustomAttributeIterator(dbf, fieldNum),
			Spliterator.ORDERED), false).onClose(() -> {
				try {
					dbf.close();
					shp.dispose();
					store.dispose();
				} catch (IOException e) {
					log.error("Kann DBF Datei nicht schliessen nach schliessen des Streams.");
				}
			});
	}

	private class CustomAttributeIterator implements Iterator<String> {
		private DbaseFileReader reader;
		private int fieldNum;

		public CustomAttributeIterator(DbaseFileReader reader, int fieldNum) {
			this.reader = reader;
			this.fieldNum = fieldNum;
		}

		@Override
		public boolean hasNext() {
			return reader.hasNext();
		}

		@Override
		public String next() {
			String attribute = null;
			try {
				// Da wir nur ein Feld auslesen muss read aufgerufen werden um auf die erste bzw. nächste Zeile zu
				// kommen
				reader.read();
				Object value = reader.readField(fieldNum);
				if (value != null) {
					attribute = value.toString();
				}
			} catch (IOException e) {
				log.error("Kann Attributwert nicht lesen");
			}
			return attribute;
		}
	}

}
