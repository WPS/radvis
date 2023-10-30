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

package de.wps.radvis.backend.integration.attributAbbildung.domain.exception;

import org.locationtech.jts.geom.Geometry;

import lombok.Getter;
import lombok.Setter;

public class ProjektionsLaengenVerhaeltnisException extends Exception {

	private static final long serialVersionUID = -6223525208058375073L;

	@Getter
	private Geometry dlmUeberschneidung;

	@Getter
	private double laengeProjektion;

	@Getter
	@Setter
	private Long kanteMitFehlerhafterRueckprojektionId;

	@Getter
	@Setter
	private Long kanteMitZuProjizierendenAttributenId;

	@Getter
	@Setter
	private Long grundNetzKanteId;

	public ProjektionsLaengenVerhaeltnisException(Geometry dlmUeberschneidung, double laengeProjektion) {
		super();
		this.dlmUeberschneidung = dlmUeberschneidung;
		this.laengeProjektion = laengeProjektion;
	}

	@Override
	public String getMessage() {
		boolean kanteMitFehlerIstGrundnetzKante = this.kanteMitFehlerhafterRueckprojektionId
			.equals(this.grundNetzKanteId);

		return String.format(
			"Diese Überschneidung der dlm-gematchten Geometrie hat durch Rückprojektion auf die zugehörige %s %d " +
				"eine zu große Längenveränderung erfahren: %f m vs. %f m. Diese Kante formt mit Kante %d eine Dublette.",
			kanteMitFehlerIstGrundnetzKante ? "GrundnetzKante" : "Kante",
			this.kanteMitFehlerhafterRueckprojektionId,
			laengeProjektion,
			this.dlmUeberschneidung.getLength(),
			kanteMitFehlerIstGrundnetzKante ? this.kanteMitZuProjizierendenAttributenId : this.grundNetzKanteId);
	}

}
