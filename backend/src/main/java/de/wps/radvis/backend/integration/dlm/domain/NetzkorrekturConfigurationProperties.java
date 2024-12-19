/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.integration.dlm.domain;

import static org.valid4j.Assertive.require;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;

@ConfigurationProperties("radvis.netzkorrektur")
public class NetzkorrekturConfigurationProperties {

	@Getter
	private final double vernetzungKorrekturPartitionBreiteInM;

	@Getter
	private final int attributlueckenMaximaleLaengeInM;

	@Getter
	private final int attributlueckenMaximaleKantenanzahl;

	@Getter
	private final int maximaleAnzahlAdjazenterAttribuierterKanten;

	@Getter
	private final boolean attributlueckeninGpkgSchreiben;

	public NetzkorrekturConfigurationProperties(double vernetzungKorrekturPartitionBreiteInM,
		int attributlueckenMaximaleLaengeInM, int attributlueckenMaximaleKantenanzahl,
		int maximaleAnzahlAdjazenterAttribuierterKanten,
		boolean attributlueckeninGpkgSchreiben) {
		require(vernetzungKorrekturPartitionBreiteInM > 0);
		require(attributlueckenMaximaleLaengeInM > 0);
		require(attributlueckenMaximaleKantenanzahl > 0);
		require(maximaleAnzahlAdjazenterAttribuierterKanten > 0);
		this.vernetzungKorrekturPartitionBreiteInM = vernetzungKorrekturPartitionBreiteInM;
		this.attributlueckenMaximaleLaengeInM = attributlueckenMaximaleLaengeInM;
		this.attributlueckenMaximaleKantenanzahl = attributlueckenMaximaleKantenanzahl;
		this.attributlueckeninGpkgSchreiben = attributlueckeninGpkgSchreiben;
		this.maximaleAnzahlAdjazenterAttribuierterKanten = maximaleAnzahlAdjazenterAttribuierterKanten;
	}
}