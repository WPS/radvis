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

package de.wps.radvis.backend.netz.domain.valueObject;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class RadverkehrsfuehrungTest {

	@Test
	void anzahlGesamt() {
		assertThat(Radverkehrsfuehrung.values()).hasSize(45);
	}

	@Test
	void anzahlFahrbahnfuehrung() {
		assertThat(Arrays.stream(Radverkehrsfuehrung.values())
			.filter(p -> RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG.equals(p.getRadverkehrsfuehrungKategorie()))
			.collect(Collectors.toList()).size()).isEqualTo(28);
	}

	@Test
	void anzahlSelbststaendig() {
		assertThat(Arrays.stream(Radverkehrsfuehrung.values())
			.filter(p -> RadverkehrsfuehrungKategorie.SELBSTSTAENDIG.equals(p.getRadverkehrsfuehrungKategorie()))
			.collect(Collectors.toList()).size()).isEqualTo(10);
	}

	@Test
	void anzahlStrassenbegleitend() {
		assertThat(Arrays.stream(Radverkehrsfuehrung.values())
			.filter(p -> RadverkehrsfuehrungKategorie.STRASSENBEGLEITEND.equals(p.getRadverkehrsfuehrungKategorie()))
			.collect(Collectors.toList()).size()).isEqualTo(6);
	}

	@Test
	void anzahlUnbekannt() {
		assertThat(Arrays.stream(Radverkehrsfuehrung.values())
			.filter(p -> RadverkehrsfuehrungKategorie.UNBEKANNT.equals(p.getRadverkehrsfuehrungKategorie()))
			.collect(Collectors.toList()).size()).isEqualTo(1);
	}

}
