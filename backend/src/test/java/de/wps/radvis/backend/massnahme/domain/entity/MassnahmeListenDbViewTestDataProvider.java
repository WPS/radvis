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

package de.wps.radvis.backend.massnahme.domain.entity;

import java.util.stream.Collectors;

import de.wps.radvis.backend.massnahme.domain.dbView.MassnahmeListenDbView;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class MassnahmeListenDbViewTestDataProvider {
	public static MassnahmeListenDbView.MassnahmeListenDbViewBuilder withMassnahme(Massnahme massnahme) {
		// Pflichtfelder
		MassnahmeListenDbView.MassnahmeListenDbViewBuilder builder = MassnahmeListenDbView.builder()
			.id(massnahme.getId())
			.bezeichnung(massnahme.getBezeichnung())
			.massnahmenkategorien(massnahme.getMassnahmenkategorien()
				.stream()
				.map(Massnahmenkategorie::name)
				.collect(Collectors.joining(";")))
			.netzklassen(massnahme.getNetzklassen()
				.stream()
				.map(Netzklasse::name)
				.collect(Collectors.joining(";")))
			.umsetzungsstatus(massnahme.getUmsetzungsstatus())
			.letzteAenderung(massnahme.getLetzteAenderung())
			.benutzerLetzteAenderungVorname(massnahme.getBenutzerLetzteAenderung().getVorname())
			.benutzerLetzteAenderungNachname(massnahme.getBenutzerLetzteAenderung().getNachname())
			.veroeffentlicht(massnahme.getVeroeffentlicht())
			.planungErforderlich(massnahme.getPlanungErforderlich())
			.sollStandard(massnahme.getSollStandard())
			.konzeptionsquelle(massnahme.getKonzeptionsquelle())
			.archiviert(massnahme.isArchiviert())
			.geometry(massnahme.getNetzbezug().getGeometrie());

		// optionale Felder
		massnahme.getMassnahmeKonzeptID()
			.ifPresent(builder::massnahmeKonzeptId);
		massnahme.getDurchfuehrungszeitraum()
			.ifPresent(builder::durchfuehrungszeitraum);
		massnahme.getBaulastZustaendiger()
			.map(Verwaltungseinheit::getName)
			.ifPresent(builder::baulastName);
		massnahme.getBaulastZustaendiger()
			.map(Verwaltungseinheit::getOrganisationsArt)
			.ifPresent(builder::baulastOrganisationsArt);
		massnahme.getZustaendiger()
			.map(Verwaltungseinheit::getName)
			.ifPresent(builder::zustaendigName);
		massnahme.getZustaendiger()
			.map(Verwaltungseinheit::getOrganisationsArt)
			.ifPresent(builder::zustaendigOrganisationsArt);
		massnahme.getunterhaltsZustaendiger()
			.map(Verwaltungseinheit::getName)
			.ifPresent(builder::unterhaltName);
		massnahme.getunterhaltsZustaendiger()
			.map(Verwaltungseinheit::getOrganisationsArt)
			.ifPresent(builder::unterhaltOrganisationsArt);
		massnahme.getPrioritaet()
			.ifPresent(builder::prioritaet);
		massnahme.getHandlungsverantwortlicher()
			.ifPresent(builder::handlungsverantwortlicher);
		massnahme.getUmsetzungsstand()
			.map(Umsetzungsstand::getUmsetzungsstandStatus)
			.ifPresent(builder::umsetzungsstandStatus);
		massnahme.getRealisierungshilfe()
			.ifPresent(builder::realisierungshilfe);
		massnahme.getKostenannahme()
			.ifPresent(builder::kostenannahme);
		massnahme.getMaViSID()
			.ifPresent(builder::maViSID);
		massnahme.getVerbaID()
			.ifPresent(builder::verbaID);
		massnahme.getLGVFGID()
			.ifPresent(builder::lgvfgID);

		if (massnahme.isArchiviert()) {
			builder.netzbezugSnapshotLines(massnahme.getNetzbezugSnapshotLines())
				.netzbezugSnapshotPoints(massnahme.getNetzbezugSnapshotPoints());
		}

		return builder;
	}
}
