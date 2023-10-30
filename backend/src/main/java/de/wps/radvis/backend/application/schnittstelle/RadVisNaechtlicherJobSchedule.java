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
import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageBRImportJob;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.fahrradroute.domain.DRouteMatchingJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenTfisUpdateJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenToubizImportJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenVariantenTfisUpdateJob;
import de.wps.radvis.backend.fahrradroute.domain.ProfilInformationenUpdateJob;
import de.wps.radvis.backend.fahrradroute.domain.RecreateFahrradrouteImportDiffViewJob;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.FahrradzaehlstellenMobiDataImportJob;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.DLMReimportJob;
import de.wps.radvis.backend.konsistenz.pruefung.domain.KonsistenzregelPruefJob;
import de.wps.radvis.backend.leihstation.domain.LeihstationMobiDataImportJob;
import de.wps.radvis.backend.matching.domain.DlmPbfErstellungsJob;
import de.wps.radvis.backend.matching.domain.MatchNetzAufOSMJob;
import de.wps.radvis.backend.matching.domain.OsmAuszeichnungsJob;
import de.wps.radvis.backend.matching.domain.OsmPbfDownloadJob;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.WegweisendeBeschilderungImportJob;

public class RadVisNaechtlicherJobSchedule implements RadVisJobSchedule {
	@Override
	public List<Class<? extends AbstractJob>> jobsToRun() {
		List<Class<? extends AbstractJob>> naechtlicheJobs = new ArrayList<>();

		if (FeatureTogglz.DLM_REIMPORT_NAECHTLICH_AUSFUEHREN.isActive()) {
			naechtlicheJobs.add(DLMReimportJob.class);
		}

		naechtlicheJobs.add(OsmPbfDownloadJob.class);

		if (FeatureTogglz.NETZ_AUF_OSM_MATCHING_NAECHTLICH_AUSFUEHREN.isActive()) {
			naechtlicheJobs.add(MatchNetzAufOSMJob.class);
		}

		naechtlicheJobs.add(DlmPbfErstellungsJob.class);
		naechtlicheJobs.add(OsmAuszeichnungsJob.class);
		naechtlicheJobs.add(FahrradroutenToubizImportJob.class);

		naechtlicheJobs.add(FahrradroutenTfisUpdateJob.class);
		naechtlicheJobs.add(FahrradroutenVariantenTfisUpdateJob.class);
		if (FeatureTogglz.D_ROUTEN_NAECHTLICH_NETZBEZUG_MATCHVERSUCH.isActive()) {
			naechtlicheJobs.add(DRouteMatchingJob.class);
		}
		naechtlicheJobs.add(ProfilInformationenUpdateJob.class);
		naechtlicheJobs.add(RecreateFahrradrouteImportDiffViewJob.class);

		naechtlicheJobs.add(WegweisendeBeschilderungImportJob.class);
		naechtlicheJobs.add(AbstellanlageBRImportJob.class);
		naechtlicheJobs.add(LeihstationMobiDataImportJob.class);

		naechtlicheJobs.add(GeoserverFehlerprotokolleUpdateJob.class);

		if (FeatureTogglz.KONSISTENZREGELN.isActive()) {
			naechtlicheJobs.add(KonsistenzregelPruefJob.class);
		}

		naechtlicheJobs.add(FahrradzaehlstellenMobiDataImportJob.class);

		return naechtlicheJobs;
	}

	public boolean forceRun() {
		return true;
	}
}
