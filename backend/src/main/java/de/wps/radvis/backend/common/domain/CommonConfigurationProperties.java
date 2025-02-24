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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import de.wps.radvis.backend.common.domain.valueObject.BasisnetzImportSource;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import lombok.Getter;

@ConfigurationProperties("radvis.common")
@Getter
public class CommonConfigurationProperties {
	private static final String BASIS_URL_PATTERN = "^https?://[A-zäöüÄÖÜ\\d_\\-/.]*:?[0-9]{0,5}/?[-a-zA-ZäöüÄÖÜ\\d()!@:%_+.~#?&/=]*$";

	private final String externeResourcenBasisPfad;

	private final int anzahlTageImportprotokolleVorhalten;

	private final ExtentProperty extentProperty;

	private String proxyAdress;

	private int proxyPort;

	private final String version;

	private final String basisUrl;

	private final BasisnetzImportSource basisnetzImportSource;

	private final Envelope obersteGebietskoerperschaftEnvelope;

	private final String obersteGebietskoerperschaftName;

	private final OrganisationsArt obersteGebietskoerperschaftOrganisationsArt;

	private final String staticResourcesPath;

	private final double erlaubteAbweichungFuerKnotenNetzbezugRematch;

	private final double erlaubteAbweichungFuerKantenNetzbezugRematch;

	@ConstructorBinding
	public CommonConfigurationProperties(String externeResourcenBasisPfad,
		Integer anzahlTageImportprotokolleVorhalten,
		ExtentProperty extent,
		String proxy,
		String version,
		String basisUrl,
		String basisnetzImportSource,
		String obersteGebietskoerperschaftName,
		OrganisationsArt obersteGebietskoerperschaftOrganisationsArt, String staticResourcesPath,
		Double erlaubteAbweichungFuerKnotenNetzbezugRematch, Double erlaubteAbweichungFuerKantenNetzbezugRematch) {
		require(externeResourcenBasisPfad, notNullValue());
		require(externeResourcenBasisPfad.length() > 1, "externeResourcenBasisPfad muss Länge größer 1 haben");
		require(anzahlTageImportprotokolleVorhalten, notNullValue());
		require(isValidDateipfad(externeResourcenBasisPfad), "externeResourcenBasisPfad muss Dateipfadstruktur haben");
		require(extent, notNullValue());
		require(isValidBasisURL(basisUrl), "basisUrl muss URL-Struktur haben");
		require(BasisnetzImportSource.isValid(basisnetzImportSource),
			"basisnetzImportSource muss einen der folgenden Wert enthalten: " + Arrays.stream(
				BasisnetzImportSource.values()).map(v -> v.name()).collect(Collectors.joining(", ")));
		require(staticResourcesPath, notNullValue());
		require(erlaubteAbweichungFuerKantenNetzbezugRematch, notNullValue());
		require(erlaubteAbweichungFuerKnotenNetzbezugRematch, notNullValue());
		require(anzahlTageImportprotokolleVorhalten, notNullValue());

		this.externeResourcenBasisPfad = externeResourcenBasisPfad;
		this.anzahlTageImportprotokolleVorhalten = anzahlTageImportprotokolleVorhalten;
		this.extentProperty = extent;
		this.version = version;
		this.basisUrl = basisUrl;
		this.basisnetzImportSource = BasisnetzImportSource.valueOf(basisnetzImportSource);
		this.staticResourcesPath = staticResourcesPath;

		if (proxy != null && !proxy.isEmpty()) {
			String[] proxyparts = proxy.split(":");
			this.proxyAdress = proxyparts[0];
			this.proxyPort = Integer.parseInt(proxyparts[1]);
		}

		obersteGebietskoerperschaftEnvelope = new Envelope(
			new Coordinate(extent.getMinX(), extent.getMinY()),
			new Coordinate(extent.getMaxX(), extent.getMaxY()));
		this.obersteGebietskoerperschaftName = obersteGebietskoerperschaftName;
		this.obersteGebietskoerperschaftOrganisationsArt = obersteGebietskoerperschaftOrganisationsArt;
		this.erlaubteAbweichungFuerKnotenNetzbezugRematch = erlaubteAbweichungFuerKnotenNetzbezugRematch;
		this.erlaubteAbweichungFuerKantenNetzbezugRematch = erlaubteAbweichungFuerKantenNetzbezugRematch;

	}

	/**
	 * Wir können hier nicht den Validator aus Validators nehmen, da wir auch localhost erlauben müssen
	 */
	public static boolean isValidBasisURL(String value) {
		return value != null && value.matches(BASIS_URL_PATTERN);
	}
}
