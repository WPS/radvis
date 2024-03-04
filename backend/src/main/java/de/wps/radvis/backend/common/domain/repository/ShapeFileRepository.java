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

package de.wps.radvis.backend.common.domain.repository;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeEncodingException;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeUnreadableException;
import lombok.NonNull;

public interface ShapeFileRepository {
	/**
	 * Liest Features aus einem Shape-File.
	 * <p>
	 * Unbedingt Stream.close() aufrufen
	 *
	 * @param shpFile
	 * @return
	 * @throws IOException
	 */
	public Stream<SimpleFeature> readShape(File shpFile) throws IOException, ShapeProjectionException;

	/**
	 * @param shpDirectory
	 * @param shpFile
	 * @param features
	 * @return true, wenn das Shape-File geschrieben wurde, sonst false
	 * @throws IOException
	 */
	public boolean writeShape(File shpDirectory, File shpFile, List<SimpleFeature> features) throws IOException;

	/**
	 * Prüft, ob die ShpFile folgende Kriterien erfüllt:
	 * - encoding ist als UTF-8 deklariert
	 * - Geometrien müssen von Geotools lesbar sein
	 * - die deklarierte Projection muss UTM32N sein
	 *
	 * @param shpFile
	 * @return true
	 */
	void validate(File shpFile)
		throws ShapeEncodingException, ShapeUnreadableException, IOException, ShapeProjectionException;

	@NonNull Consumer<SimpleFeature> setzeGeometryFactoryAufGeometrie(
		KoordinatenReferenzSystem koordinatenReferenzSystem);

	SimpleFeature transformGeometryToUTM32(SimpleFeature simpleFeature);
}
