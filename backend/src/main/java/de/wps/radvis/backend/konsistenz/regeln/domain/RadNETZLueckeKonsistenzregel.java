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

package de.wps.radvis.backend.konsistenz.regeln.domain;

import java.util.Set;

import de.wps.radvis.backend.netz.domain.service.NetzklassenSackgassenService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;

public class RadNETZLueckeKonsistenzregel extends AbstractNetzklassenLueckeKonsistenzregel {

	public static String VERLETZUNGS_TYP = "RADNETZ_LUECKE";

	public RadNETZLueckeKonsistenzregel(
		NetzklassenSackgassenService netzklassenSackgassenService) {
		super(netzklassenSackgassenService);
	}

	@Override
	public String getVerletzungsTyp() {
		return VERLETZUNGS_TYP;
	}

	@Override
	public String getTitel() {
		return "RadNETZ-Lücke";
	}

	@Override
	protected Set<Netzklasse> getNetzklassen() {
		return Netzklasse.RADNETZ_NETZKLASSEN;
	}

	@Override
	protected String getBeschreibung() {
		return "Das RadNETZ enthält an dieser Stelle eine Lücke bzw. eine Sackgasse.";
	}
}
