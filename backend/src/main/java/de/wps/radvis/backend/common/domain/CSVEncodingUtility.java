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

package de.wps.radvis.backend.common.domain;

import java.io.IOException;
import java.io.OutputStream;

public class CSVEncodingUtility {

	// UTF-8 without BOM encoding
	// (https://stackoverflow.com/questions/10136343/opencsv-csvwriter-using-utf-8-doesnt-seem-to-work-for-multiple-languages)
	public static void writeBOMEncoding(OutputStream outputStream) throws IOException {
		outputStream.write(0xef);
		outputStream.write(0xbb);
		outputStream.write(0xbf);
	}
}
