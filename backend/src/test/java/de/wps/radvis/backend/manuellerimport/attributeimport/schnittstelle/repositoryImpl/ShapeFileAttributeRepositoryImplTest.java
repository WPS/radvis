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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

public class ShapeFileAttributeRepositoryImplTest {

	private static final String TEST_DBF = "./shp/Bodenseekreis_Kreisnetz.dbf";

	private static final String TEST_SHP = "./shp/test_attribute/test_attribute.shp";

	private ShapeFileAttributeRepositoryImpl shpAttributeRepository;

	@BeforeEach
	public void setup() {
		shpAttributeRepository = new ShapeFileAttributeRepositoryImpl();
	}

	@Test
	public void testGetAttributnamen() throws IOException {
		// arrange
		File dbfFile = new ClassPathResource(TEST_DBF).getFile();
		Set<String> sollAttribute = Set.of("FURT3", "FURT1", "AUDIO1", "FURT2", "ABSENK", "VORSCHL2", "STRASSE",
			"SCHADEN", "VORSCHL1", "WEGEART", "Landkreis", "BreitNA1", "BreitNA2", "BELAGART", "LRVN_ID", "BORD",
			"AufnTYP", "LICHT", "BREITST2", "BreitePR", "AUDIO10", "BreiteMF", "MASSN_P_K", "BreitNA", "Netz",
			"WEGETYP", "ANM", "BELAGMGL", "MASSN_P", "AUDIO111", "BENUTZPF", "FOTO3", "FOTO2", "FOTO1", "Randmark",
			"FOTO12", "MASSNFIN", "FOTO11", "Furtmark", "BreiteGW", "BreiteRW", "ST", "BREITEVA", "RICHTUNG", "VZ1",
			"LRVN_KAT", "VZ2", "ST2", "BREITST", "RWANF", "RWENDE", "ORTSLAGE", "BreiteWG");

		// act
		Set<String> attributnamen = shpAttributeRepository.getAttributnamen(dbfFile);

		// assert
		assertThat(attributnamen).isEqualTo(sollAttribute);
	}

	@Test
	public void testGetAttributwerte() throws IOException {
		// arrange
		File dbfFile = new ClassPathResource(TEST_DBF).getFile();

		// act
		Stream<String> attributWerte = shpAttributeRepository.getAttributWerte(dbfFile, "LICHT");

		// assert
		assertThat(attributWerte.collect(Collectors.toSet())).containsExactlyInAnyOrder(
			"vorhanden", "nicht vorhanden", "");
	}

	@Test
	void testGetAttributwerte_ganzeZahl() throws IOException {
		// arrange
		File shp = new ClassPathResource(TEST_SHP).getFile();

		// act
		Stream<String> attributWerte = shpAttributeRepository.getAttributWerte(shp, "ganzezahl");

		// assert
		assertThat(attributWerte.collect(Collectors.toSet())).containsExactlyInAnyOrder("1", null);
	}

	@Test
	void testGetAttributwerte_dezimalzahl() throws IOException {
		// arrange
		File shp = new ClassPathResource(TEST_SHP).getFile();

		// act
		Stream<String> attributWerte = shpAttributeRepository.getAttributWerte(shp, "dezimalzah");

		// assert
		assertThat(attributWerte.collect(Collectors.toSet())).containsExactlyInAnyOrder("1.2323", "-12.3", null);
	}

	@Test
	void testGetAttributwerte_datum() throws IOException {
		// arrange
		File shp = new ClassPathResource(TEST_SHP).getFile();

		// act
		Stream<String> attributWerte = shpAttributeRepository.getAttributWerte(shp, "datum");

		// assert
		Calendar calendar = Calendar.getInstance();
		calendar.set(2022, 1, 14, 0, 0, 0);
		assertThat(attributWerte.collect(Collectors.toSet())).containsExactlyInAnyOrder(
			new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy", Locale.US).format(calendar.getTime()),
			null);
	}

	@Test
	void testGetAttributwerte_id() throws IOException {
		// arrange
		File shp = new ClassPathResource(TEST_SHP).getFile();

		// act
		Stream<String> attributWerte = shpAttributeRepository.getAttributWerte(shp, "id");

		// assert
		assertThat(attributWerte.collect(Collectors.toSet())).containsExactlyInAnyOrder("1", "3", null);
	}

}
