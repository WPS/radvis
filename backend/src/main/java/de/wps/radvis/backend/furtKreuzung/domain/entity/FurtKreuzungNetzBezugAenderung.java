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

package de.wps.radvis.backend.furtKreuzung.domain.entity;

import java.time.LocalDateTime;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.netz.domain.entity.NetzBezugAenderung;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzBezugAenderungsArt;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class FurtKreuzungNetzBezugAenderung extends NetzBezugAenderung implements FehlerprotokollEintrag {

	@OneToOne(optional = false)
	private FurtKreuzung furtKreuzung;

	public FurtKreuzungNetzBezugAenderung(NetzBezugAenderungsArt netzBezugAenderungsArt, Long kantenKnotenId,
		FurtKreuzung furtKreuzung, Benutzer technischerBenutzer, LocalDateTime datum, NetzAenderungAusloeser ausloeser,
		Geometry geometry) {
		super(netzBezugAenderungsArt, kantenKnotenId, technischerBenutzer, datum, ausloeser, geometry);
		this.furtKreuzung = furtKreuzung;
	}

	@Override
	public String getTitel() {
		return "Netzbezug-Änderung (Furten und Kreuzungen)";
	}

	@Override
	public String getEntityLink() {
		return FrontendLinks.furtKreuzungDetailView(furtKreuzung.getId());
	}
}
