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

package de.wps.radvis.backend.application.schnittstelle;

import java.util.ArrayList;
import java.util.List;

import de.wps.radvis.backend.abfrage.fehlerprotokoll.domain.GeoserverFehlerprotokolleUpdateJob;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.BuildNetzklassenStreckenSignaturViewJob;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.BuildRadNETZNetzViewCacheJob;
import de.wps.radvis.backend.benutzer.domain.InitialBenutzerImportJob;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.fahrradroute.domain.RecreateFahrradrouteImportDiffViewJob;
import de.wps.radvis.backend.matching.domain.DlmPbfErstellungsJob;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportJob;

public class RadVisStartupJobSchedule implements RadVisJobSchedule {

	@Override
	public List<Class<? extends AbstractJob>> jobsToRun() {
		List<Class<? extends AbstractJob>> startupJobs = new ArrayList<>();

		if (FeatureTogglz.RUN_DLM_PBF_ON_STARTUP.isActive()) {
			startupJobs.add(DlmPbfErstellungsJob.class);
		}
		startupJobs.add(VerwaltungseinheitImportJob.class);
		startupJobs.add(InitialBenutzerImportJob.class);

		if (FeatureTogglz.RADNETZ_STRECKEN.isActive()) {
			startupJobs.add(GeoserverFehlerprotokolleUpdateJob.class);
			startupJobs.add(BuildRadNETZNetzViewCacheJob.class);
			startupJobs.add(BuildNetzklassenStreckenSignaturViewJob.class);
		}

		startupJobs.add(RecreateFahrradrouteImportDiffViewJob.class);

		return startupJobs;
	}

	public boolean forceRun() {
		return false;
	}
}
