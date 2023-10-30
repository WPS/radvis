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

package de.wps.radvis.backend.abfrage.netzausschnitt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzMapView;
import de.wps.radvis.backend.netz.domain.entity.NetzklassenStreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;

public class NetzausschnittServiceTest {

	@Mock
	private KantenAbfrageRepository kantenRepositoryMock;

	@Mock
	private KnotenRepository knotenRepositoryMock;

	@Mock
	private StreckeViewCacheRepository<NetzMapView, StreckeVonKanten> radNETZNetzViewCacheRepository;

	@Mock
	private StreckeViewCacheRepository<List<NetzklassenStreckenSignaturView>, NetzklassenStreckeVonKanten> netzklassenStreckenSignaturViewRepository;

	@Mock
	private NetzfehlerRepository netzfehlerRepositoryMock;

	private NetzausschnittService netzService;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		netzService = new NetzausschnittService(netzfehlerRepositoryMock, kantenRepositoryMock, knotenRepositoryMock,
			radNETZNetzViewCacheRepository, netzklassenStreckenSignaturViewRepository);
	}

	@Test
	void testFindNetzAusschnitt_keineKantenImBereich_OptionalEmpty() {
		// arrange
		Mockito.when(kantenRepositoryMock.getKantenMapViewInBereich(any(), any()))
			.thenReturn(Collections.emptySet());

		// act
		NetzMapView result = netzService
			.findNetzAusschnitt(new Envelope(), Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT));

		// assert
		assertThat(result.getKanten()).isEmpty();
	}
}
