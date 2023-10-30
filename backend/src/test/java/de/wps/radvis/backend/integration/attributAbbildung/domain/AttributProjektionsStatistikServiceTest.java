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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.AttributProjektionsJobStatistik;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.ProjizierteKanteIstIsoliertException;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungService;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungService;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;

class AttributProjektionsStatistikServiceTest {

	private AttributProjektionsStatistikService statistikService;
	@Mock
	private AttributeProjektionsProtokollService protokollService;
	@Mock
	private NetzService netzService;
	@Mock
	private ImportedFeaturePersistentRepository importedFeatureRepository;
	@Mock
	private RadNetzNetzbildungService radNetzNetzbildungService;
	@Mock
	private RadwegeDBNetzbildungService radwegeDBNetzbildungService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		this.statistikService = new AttributProjektionsStatistikService(protokollService, netzService,
			importedFeatureRepository, radNetzNetzbildungService, radwegeDBNetzbildungService);
	}

	@Test
	void testeUeberpruefeTopologieDerNetzklassen_KanteInternIsoliertAberVerbundenMitKantenAusserhalbEnvelope() {
		// arrange
		Envelope partition = new Envelope(10000, 20000, 10000, 20000);
		Envelope erweiterterEnvelopeFuerKonsistenzCheck = partition.copy();
		erweiterterEnvelopeFuerKonsistenzCheck.expandBy(2000);

		Set<Long> idsBereitsAbgearbeitet = new HashSet<>();
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();

		Set<Kante> kantenInPartition = new HashSet<>();
		Kante kanteAmRandVonPartition = radnetzKante(9900, 9900, 10030, 10030)
			.id(1L)
			.vonKnoten(KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(9900, 9900), QuellSystem.DLM).id(10L)
				.build())
			.nachKnoten(KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10030, 10030), QuellSystem.DLM).id(9L)
				.build())
			.build();
		kantenInPartition.add(kanteAmRandVonPartition);

		Set<Kante> kantenImUmfeldDerPartition = new HashSet<>();
		Kante kanteAusserhalbVerbundenMitInnerhalb1 = radnetzKante(9500, 9500, 9899, 9899)
			.nachKnoten(kanteAmRandVonPartition.getVonKnoten())
			.vonKnoten(KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(9500, 9500), QuellSystem.DLM).id(11L)
				.build())
			.id(2L)
			.build();
		kantenImUmfeldDerPartition.add(kanteAusserhalbVerbundenMitInnerhalb1);
		Kante kanteAusserhalbVerbundenMitInnerhalb2 = radnetzKante(9300, 9300, 9500, 9500)
			.nachKnoten(kanteAusserhalbVerbundenMitInnerhalb1.getVonKnoten())
			.vonKnoten(KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(9300, 9300), QuellSystem.DLM).id(12L)
				.build())
			.id(3L)
			.build();
		kantenImUmfeldDerPartition.add(kanteAusserhalbVerbundenMitInnerhalb2);
		Kante kanteAusserhalbVerbundenMitInnerhalb3 = radnetzKante(9100, 9100, 9300, 9300)
			.nachKnoten(kanteAusserhalbVerbundenMitInnerhalb2.getVonKnoten())
			.vonKnoten(KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(9100, 9100), QuellSystem.DLM).id(13L)
				.build())
			.id(4L)
			.build();
		kantenImUmfeldDerPartition.add(kanteAusserhalbVerbundenMitInnerhalb3);

		Set<Kante> alleRelevantenKanten = new HashSet<>(kantenInPartition);
		alleRelevantenKanten.addAll(kantenImUmfeldDerPartition);

		when(netzService
			.getKantenInBereichMitNetzklassen(erweiterterEnvelopeFuerKonsistenzCheck, Set.of(NetzklasseFilter.RADNETZ),
				false))
			.thenReturn(alleRelevantenKanten);

		// act
		statistikService.ueberpruefeTopologieDerNetzklassen(partition, idsBereitsAbgearbeitet, statistik, "");

		verify(protokollService, never()).handle(any(ProjizierteKanteIstIsoliertException.class), anyString());
		assertThat(statistik.laengeKantenIsoliertInmeter).isCloseTo(0., Offset.offset(0.));
		assertThat(idsBereitsAbgearbeitet).containsExactly(kanteAmRandVonPartition.getId());
	}

	@Test
	void testeUeberpruefeTopologieDerNetzklassen_IgnoriereIsolationVonKantenAusserhalb() {
		// arrange
		Envelope partition = new Envelope(10000, 20000, 10000, 20000);
		Envelope erweiterterEnvelopeFuerKonsistenzCheck = partition.copy();
		erweiterterEnvelopeFuerKonsistenzCheck.expandBy(2000);

		Set<Long> idsBereitsAbgearbeitet = new HashSet<>();
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();

		Set<Kante> kantenInPartition = new HashSet<>();
		Kante kanteAmRandVonPartition = radnetzKante(9900, 9900, 9950, 9950)
			.id(1L)
			.vonKnoten(KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(9900, 9900), QuellSystem.DLM).id(10L)
				.build())
			.nachKnoten(KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(9950, 9950), QuellSystem.DLM).id(9L)
				.build())
			.build();
		kantenInPartition.add(kanteAmRandVonPartition);

		Set<Kante> alleRelevantenKanten = new HashSet<>(kantenInPartition);

		when(netzService
			.getKantenInBereichMitNetzklassen(erweiterterEnvelopeFuerKonsistenzCheck, Set.of(NetzklasseFilter.RADNETZ),
				false))
			.thenReturn(alleRelevantenKanten);

		// act
		statistikService.ueberpruefeTopologieDerNetzklassen(partition, idsBereitsAbgearbeitet, statistik, "");

		verify(protokollService, never()).handle(any(ProjizierteKanteIstIsoliertException.class), anyString());
		assertThat(statistik.laengeKantenIsoliertInmeter).isCloseTo(0., Offset.offset(0.));
		assertThat(idsBereitsAbgearbeitet).isEmpty();
	}

	@Test
	void testeUeberpruefeTopologieDerNetzklassen_IgnoriertBereitsabgearbeiteteKante() {
		// arrange
		Envelope partition = new Envelope(10000, 20000, 10000, 20000);
		Envelope erweiterterEnvelopeFuerKonsistenzCheck = partition.copy();
		erweiterterEnvelopeFuerKonsistenzCheck.expandBy(2000);

		Set<Long> idsBereitsAbgearbeitet = new HashSet<>();
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();

		Set<Kante> kantenInPartition = new HashSet<>();
		Kante kanteAmRandVonPartition = radnetzKante(9900, 9900, 10300, 10300)
			.id(1L)
			.vonKnoten(KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(9900, 9900), QuellSystem.DLM).id(10L)
				.build())
			.nachKnoten(KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10300, 10300), QuellSystem.DLM).id(9L)
				.build())
			.build();
		kantenInPartition.add(kanteAmRandVonPartition);

		Set<Kante> alleRelevantenKanten = new HashSet<>(kantenInPartition);

		when(netzService
			.getKantenInBereichMitNetzklassen(erweiterterEnvelopeFuerKonsistenzCheck, Set.of(NetzklasseFilter.RADNETZ),
				false))
			.thenReturn(alleRelevantenKanten);

		idsBereitsAbgearbeitet.add(kanteAmRandVonPartition.getId());

		// act
		statistikService.ueberpruefeTopologieDerNetzklassen(partition, idsBereitsAbgearbeitet, statistik, "");

		verify(protokollService, never()).handle(any(ProjizierteKanteIstIsoliertException.class), anyString());
		assertThat(statistik.laengeKantenIsoliertInmeter).isCloseTo(0., Offset.offset(0.));
		assertThat(idsBereitsAbgearbeitet).containsExactly(kanteAmRandVonPartition.getId());
	}

	@Test
	void testeUeberpruefeTopologieDerNetzklassen_ruftProtokollserviceAufFuerIsolierteKante() {
		// arrange
		Envelope partition = new Envelope(10000, 20000, 10000, 20000);
		Envelope erweiterterEnvelopeFuerKonsistenzCheck = partition.copy();
		erweiterterEnvelopeFuerKonsistenzCheck.expandBy(2000);

		Set<Long> idsBereitsAbgearbeitet = new HashSet<>();
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();

		Set<Kante> kantenInPartition = new HashSet<>();
		Kante kanteAmRandVonPartition = radnetzKante(9900, 9900, 10300, 10300)
			.id(1L)
			.vonKnoten(KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(9900, 9900), QuellSystem.DLM).id(10L)
				.build())
			.nachKnoten(KnotenTestDataProvider
				.withCoordinateAndQuelle(new Coordinate(10300, 10300), QuellSystem.DLM).id(9L)
				.build())
			.build();
		kantenInPartition.add(kanteAmRandVonPartition);

		Set<Kante> alleRelevantenKanten = new HashSet<>(kantenInPartition);

		when(netzService
			.getKantenInBereichMitNetzklassen(erweiterterEnvelopeFuerKonsistenzCheck, Set.of(NetzklasseFilter.RADNETZ),
				false))
			.thenReturn(alleRelevantenKanten);

		// act
		statistikService.ueberpruefeTopologieDerNetzklassen(partition, idsBereitsAbgearbeitet, statistik, "");

		verify(protokollService, times(1)).handle(any(ProjizierteKanteIstIsoliertException.class), anyString());
		assertThat(statistik.laengeKantenIsoliertInmeter)
			.isCloseTo(kanteAmRandVonPartition.getGeometry().getLength(), Offset.offset(0.01));
		assertThat(idsBereitsAbgearbeitet).containsExactly(kanteAmRandVonPartition.getId());
	}

	public Kante.KanteBuilder radnetzKante(double x1, double y1, double x2, double y2) {
		return KanteTestDataProvider
			.withCoordinatesAndQuelle(x1, y1, x2, y2, QuellSystem.DLM)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(Set.of(
				Netzklasse.RADNETZ_ALLTAG)).build());
	}
}
