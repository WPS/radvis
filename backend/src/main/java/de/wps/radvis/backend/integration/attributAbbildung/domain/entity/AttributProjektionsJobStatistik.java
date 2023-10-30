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

package de.wps.radvis.backend.integration.attributAbbildung.domain.entity;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import lombok.Getter;

@Getter
public class AttributProjektionsJobStatistik extends JobStatistik {

	// Dubletten
	public int dubletten;
	public int grundnetzKantenInDubletten;
	public int dlmKantenAbgearbeitet;
	public double laengeDerDlmUebereinstimmungInMeter;

	// Attributprojektionsbeschreibungen
	public int projektionen;
	public int projektionenOhneSegmente;
	public int projektionenMitNurEinemSegment;
	public int projektionenMitMehrAls2Segmenten;
	public int projektionenMitAnteilGleicherKantenAttributeKleiner50;
	public int projektionenMitAnteilGleicherKantenAttributeGroesser50;
	public int projektionenMitAnteilGleicherKantenAttributeGroesser80;
	public int projektionenMitUniformenKantenAttributen;
	public double laengeDesDLMAufDasProjiziertWurdeInMeter;

	public int rueckProjektionsLaengenVerhaeltnisZuUnterschiedlich;
	public int rueckProjektionsLaengeZuKurz;

	// Reichere GrundnetzKanten Mit Attributen An
	public int mehrdeutigeKantenAttribute;
	public int mehrdeutigeGeschwindigkeitsAttribute;
	public int mehrdeutigeRichtungsattribute;
	public int mehrdeutigeFuehrungsformAttribute;
	public int mehrdeutigeZustaendigkeitAttribute;
	public int mehrdeutigeNetzklassen;
	public int mehrdeutigeIstStandards;
	public double laengeDesDLMAufDasNetzklassenErfolgreichProjiziertWurden;
	public double laengeDesDLMAufDasKantenattributeErfolgreichProjiziertWurden;

	public double laengeDerUebernommenenQuelleDieProjiziertWurde;
	public double laengeKantenIsoliertInmeter;

	public void addMehrdeutigeLinearReferenzierteAttribute(String attributGruppe) {
		switch (attributGruppe) {
		case "ZustaendigkeitAttribute":
			mehrdeutigeZustaendigkeitAttribute++;
			break;
		case "FuehrungsformAttribute":
			mehrdeutigeFuehrungsformAttribute++;
			break;
		case "GeschwindigkeitAttribute":
			mehrdeutigeFuehrungsformAttribute++;
			break;
		default:
			throw new RuntimeException("AttributGruppe nicht unterstützt");
		}
	}

	public void addProjektionMitAnteilGleicherKantenAttribute(double anteil) {
		if (anteil == 1) {
			projektionenMitUniformenKantenAttributen++;
		} else if (anteil >= 0.80) {
			projektionenMitAnteilGleicherKantenAttributeGroesser80++;
		} else if (anteil >= 0.50) {
			projektionenMitAnteilGleicherKantenAttributeGroesser50++;
		} else {
			projektionenMitAnteilGleicherKantenAttributeKleiner50++;
		}
	}

	public void reset() {
		dubletten = 0;
		grundnetzKantenInDubletten = 0;
		dlmKantenAbgearbeitet = 0;
		laengeDerDlmUebereinstimmungInMeter = 0;

		projektionen = 0;
		projektionenOhneSegmente = 0;
		projektionenMitNurEinemSegment = 0;
		projektionenMitMehrAls2Segmenten = 0;
		projektionenMitAnteilGleicherKantenAttributeKleiner50 = 0;
		projektionenMitAnteilGleicherKantenAttributeGroesser50 = 0;
		projektionenMitAnteilGleicherKantenAttributeGroesser80 = 0;
		projektionenMitUniformenKantenAttributen = 0;
		laengeDesDLMAufDasProjiziertWurdeInMeter = 0;

		rueckProjektionsLaengenVerhaeltnisZuUnterschiedlich = 0;
		rueckProjektionsLaengeZuKurz = 0;

		mehrdeutigeKantenAttribute = 0;
		mehrdeutigeGeschwindigkeitsAttribute = 0;
		mehrdeutigeRichtungsattribute = 0;
		mehrdeutigeFuehrungsformAttribute = 0;
		mehrdeutigeZustaendigkeitAttribute = 0;
		mehrdeutigeNetzklassen = 0;
		mehrdeutigeIstStandards = 0;

		laengeDesDLMAufDasKantenattributeErfolgreichProjiziertWurden = 0;
		laengeDesDLMAufDasNetzklassenErfolgreichProjiziertWurden = 0;

		laengeDerUebernommenenQuelleDieProjiziertWurde = 0;
		laengeKantenIsoliertInmeter = 0;
	}

	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
