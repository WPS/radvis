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

public class ProjektionsLaengeZuKurzException extends Exception {

	private static final long serialVersionUID = 6654582184663765598L;

	@Getter
	private final Geometry dlmUeberschneidung;

	@Getter
	private final double laengeProjektion;

	@Getter
	@Setter
	private Long kanteMitFehlerhafterRueckprojektionId;

	@Getter
	@Setter
	private Long kanteMitZuProjizierendenAttributenId;

	@Getter
	@Setter
	private Long grundNetzKanteId;

	public ProjektionsLaengeZuKurzException(Geometry dlmUeberschneidung, double laengeProjektion) {
		super();
		this.dlmUeberschneidung = dlmUeberschneidung;
		this.laengeProjektion = laengeProjektion;
	}

	@Override
	public String getMessage() {
		boolean kanteMitFehlerIstGrundnetzKante = this.kanteMitFehlerhafterRueckprojektionId
			.equals(this.grundNetzKanteId);

		return String.format(
			"Diese Überschneidung der dlm-gematchten Geometrie hat nach Rückprojektion auf die zugehörige %s %d "
				+ "die zu kurze Länge %f und wurde deshalb nicht weiter berücksichtigt. "
				+ "Diese Kante formt mit Kante %d eine Dublette.",
			kanteMitFehlerIstGrundnetzKante ? "GrundnetzKante" : "Kante",
			this.kanteMitFehlerhafterRueckprojektionId,
			this.laengeProjektion,
			kanteMitFehlerIstGrundnetzKante ? this.kanteMitZuProjizierendenAttributenId : this.grundNetzKanteId);
	}
}
