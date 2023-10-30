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

import static de.wps.radvis.backend.common.domain.Validators.isValidDateipfad;
import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import lombok.Getter;

@ConfigurationProperties("radvis.common")
@Getter
public class CommonConfigurationProperties {

	private final Envelope badenWuerttembergEnvelope = new Envelope(
		new Coordinate(378073.54, 5255657.09),
		new Coordinate(633191.12, 5534702.95));
	private static final String BASIS_URL_PATTERN = "^https?://[A-zäöüÄÖÜ\\d_\\-/.]*:?[0-9]{0,5}/?[-a-zA-ZäöüÄÖÜ\\d()!@:%_+.~#?&/=]*$";

	private final String externeResourcenBasisPfad;

	private final Integer anzahlTageImportprotokolleVorhalten;

	private final ExtentProperty extentProperty;

	private String proxyAdress;

	private int proxyPort;

	private final String version;

	private final String basisUrl;

	@ConstructorBinding
	public CommonConfigurationProperties(String externeResourcenBasisPfad,
		Integer anzahlTageImportprotokolleVorhalten,
		ExtentProperty extent,
		String proxy,
		String version,
		String basisUrl) {
		require(externeResourcenBasisPfad, notNullValue());
		require(externeResourcenBasisPfad.length() > 1, "externeResourcenBasisPfad muss länge größer 1 haben");
		require(anzahlTageImportprotokolleVorhalten, notNullValue());
		require(isValidDateipfad(externeResourcenBasisPfad), "externeResourcenBasisPfad muss Dateipfadstruktur haben");
		require(extent, notNullValue());
		require(isValidBasisURL(basisUrl), "basisUrl muss URL-Struktur haben");
		this.externeResourcenBasisPfad = externeResourcenBasisPfad;
		this.anzahlTageImportprotokolleVorhalten = anzahlTageImportprotokolleVorhalten;
		this.extentProperty = extent;
		this.version = version;
		this.basisUrl = basisUrl;

		if (proxy != null && !proxy.isEmpty()) {
			String[] proxyparts = proxy.split(":");
			this.proxyAdress = proxyparts[0];
			this.proxyPort = Integer.parseInt(proxyparts[1]);
		}
	}

	/**
	 * Wir können hier nicht den Validator aus Validators nehmen, da wir auch localhost erlauben müssen
	 */
	public static boolean isValidBasisURL(String value) {
		return value != null && value.matches(BASIS_URL_PATTERN);
	}

}