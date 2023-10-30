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

package de.wps.radvis.backend.fahrradroute.schnittstelle.view;

import java.time.LocalDateTime;
import java.util.List;

import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.schnittstelle.view.AbstractImportprotokollView;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class FahrradrouteImportprotokollView extends AbstractImportprotokollView {
	private final FahrradrouteTyp fahrradrouteTyp;
	private final List<String> geloescht;
	private final List<String> erstellt;

	public FahrradrouteImportprotokollView(JobExecutionDescription jobExecutionDescription, List<String> geloescht,
		List<String> erstellt) {
		super(jobExecutionDescription);
		fahrradrouteTyp = jobExecutionDescription.getName().toLowerCase().contains("tfis") ? FahrradrouteTyp.TFIS_ROUTE
			: FahrradrouteTyp.TOUBIZ_ROUTE;
		this.geloescht = geloescht;
		this.erstellt = erstellt;
	}

	@Builder
	private FahrradrouteImportprotokollView(Long id, LocalDateTime startZeit, LocalDateTime endZeit, String statistik,
		FahrradrouteTyp fahrradrouteTyp, List<String> geloescht, List<String> erstellt) {
		super(id, startZeit, endZeit, statistik);
		this.fahrradrouteTyp = fahrradrouteTyp;
		this.geloescht = geloescht;
		this.erstellt = erstellt;
	}
}
