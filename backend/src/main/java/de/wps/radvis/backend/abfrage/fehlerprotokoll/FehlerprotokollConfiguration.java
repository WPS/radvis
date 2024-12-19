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

package de.wps.radvis.backend.abfrage.fehlerprotokoll;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.abfrage.fehlerprotokoll.domain.GeoserverFehlerprotokolleUpdateJob;
import de.wps.radvis.backend.abfrage.fehlerprotokoll.domain.repository.GeoserverFehlerprotokollRepository;
import de.wps.radvis.backend.abfrage.fehlerprotokoll.domain.service.FehlerprotokollAbfrageService;
import de.wps.radvis.backend.abfrage.fehlerprotokoll.domain.service.FehlerprotokollServiceFactory;
import de.wps.radvis.backend.barriere.domain.BarriereService;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.fahrradroute.domain.FahrradrouteService;
import de.wps.radvis.backend.furtKreuzung.domain.FurtKreuzungService;
import de.wps.radvis.backend.integration.dlm.domain.AttributlueckenService;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.matching.domain.service.OsmAbbildungsFehlerService;

@Configuration
@EnableJpaRepositories
@EntityScan
public class FehlerprotokollConfiguration {

	@Autowired
	private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;

	@Autowired
	private AttributlueckenService attributlueckenService;

	@Autowired
	private FahrradrouteService fahrradrouteService;

	@Autowired
	private BarriereService barriereService;

	@Autowired
	private FurtKreuzungService furtKreuzungService;

	@Autowired
	private GeoserverFehlerprotokollRepository geoserverFehlerprotokollRepository;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Autowired
	private OsmAbbildungsFehlerService osmAbbildungsFehlerService;

	@Bean
	public FehlerprotokollServiceFactory fehlerprotokollServiceFactory() {
		return new FehlerprotokollServiceFactory(massnahmeNetzbezugAenderungProtokollierungsService,
			attributlueckenService, fahrradrouteService, barriereService, furtKreuzungService,
			osmAbbildungsFehlerService);
	}

	@Bean
	public FehlerprotokollAbfrageService fehlerprotokollAbfrageService() {
		return new FehlerprotokollAbfrageService(fehlerprotokollServiceFactory());
	}

	@Bean
	public GeoserverFehlerprotokolleUpdateJob geoserverFehlerprotokolleUpdateJob() {
		return new GeoserverFehlerprotokolleUpdateJob(jobExecutionDescriptionRepository,
			fehlerprotokollAbfrageService(), geoserverFehlerprotokollRepository);
	}
}
