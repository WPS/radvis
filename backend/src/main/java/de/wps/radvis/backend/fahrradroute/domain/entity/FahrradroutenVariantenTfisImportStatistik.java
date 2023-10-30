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

package de.wps.radvis.backend.fahrradroute.domain.entity;

import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.fahrradroute.domain.AbbildungAufKantenStatistik;

public class FahrradroutenVariantenTfisImportStatistik extends JobStatistik {
	public int anzahlFahrradroutenMitVarianten = 0;
	public int anzahlVariantenIgnoriert = 0;
	public int anzahlVariantenAusserhalbBW = 0;
	public int anzahlVariantenMitFahrradrouteNichtBetrachtet = 0;

	public int anzahlVariantenBetrachtet = 0;
	public int anzahlVariantenAktualisiert = 0;
	public int anzahlVariantenHinzugefuegt = 0;
	public int anzahlVariantenGeloescht = 0;

	public FilterFeaturesStatistik filterFeaturesStatistik = new FilterFeaturesStatistik();
	public AbbildungAufKantenStatistik abbildungAufKantenStatistik = new AbbildungAufKantenStatistik();
	public ErstellungZusammenhaengenderLinestringStatistik erstellungZusammenhaengenderLinestringStatistik = new ErstellungZusammenhaengenderLinestringStatistik();

	@JsonIgnore
	public HashSet<String> varianteIdsOhneFahrradroute = new HashSet<>();

	public class FilterFeaturesStatistik {
		public int anzahlFeaturesGesamt = 0;
		public int anzahlFeaturesIsVarianteMitFahrradrouteInRadvis = 0;
		public int anzahlFeaturesAusserhalbBW = 0;
	}

	public class ErstellungZusammenhaengenderLinestringStatistik {
		public int anzahlVariantenNichtZusammengesetzterLinestring = 0;
		public int anzahlVariantenNichtRoutebarerLineString = 0;
	}

	public void varianteFound(String variantenId) {
		if (!varianteIdsOhneFahrradroute.contains(variantenId)) {
			varianteIdsOhneFahrradroute.add(variantenId);
			anzahlVariantenMitFahrradrouteNichtBetrachtet = varianteIdsOhneFahrradroute.size();
		}
	}

	public void varianteHasFahrradroute(String variantenId) {
		varianteIdsOhneFahrradroute.remove(variantenId);
		anzahlVariantenMitFahrradrouteNichtBetrachtet = varianteIdsOhneFahrradroute.size();
	}

	@Override
	public String toString() {
		DefaultPrettyPrinter p = new DefaultPrettyPrinter();
		DefaultPrettyPrinter.Indenter i = new DefaultIndenter("  ", "\n");
		p.indentArraysWith(i);
		p.indentObjectsWith(i);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(p);
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
	}
}
