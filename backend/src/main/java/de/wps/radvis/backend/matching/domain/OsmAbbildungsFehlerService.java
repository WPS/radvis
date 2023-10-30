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

package de.wps.radvis.backend.matching.domain;

import static org.valid4j.Assertive.require;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.common.domain.service.FehlerprotokollService;
import de.wps.radvis.backend.common.domain.valueObject.FehlerprotokollTyp;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.matching.domain.entity.OsmAbbildungsFehler;
import de.wps.radvis.backend.matching.domain.entity.OsmAbbildungsFehlerProtokollEintrag;

public class OsmAbbildungsFehlerService implements FehlerprotokollService {
	private final OsmAbbildungsFehlerRepository osmAbbildungsFehlerRepository;

	public OsmAbbildungsFehlerService(OsmAbbildungsFehlerRepository osmAbbildungsFehlerRepository) {
		this.osmAbbildungsFehlerRepository = osmAbbildungsFehlerRepository;
	}

	@Override
	public List<? extends FehlerprotokollEintrag> getAktuelleFehlerprotokolle(FehlerprotokollTyp fehlerprotokollTyp) {
		requireFehlerprotokollTypIsOsmAbbildung(fehlerprotokollTyp);

		List<OsmAbbildungsFehler> osmAbbildungsFehler = switch (fehlerprotokollTyp) {
			case OSM_ABBILDUNG_RADNETZ -> osmAbbildungsFehlerRepository.findOsmAbbildungsFehlerByRadnetzIsTrue();
			case OSM_ABBILDUNG_KREISNETZ -> osmAbbildungsFehlerRepository.findOsmAbbildungsFehlerByKreisnetzIsTrue();
			case OSM_ABBILDUNG_KOMMUNALNETZ ->
				osmAbbildungsFehlerRepository.findOsmAbbildungsFehlerByKommunalnetzIsTrue();
			case OSM_ABBILDUNG_SONSTIGE ->
				osmAbbildungsFehlerRepository.findOsmAbbildungsFehlerByRadnetzIsFalseAndKreisnetzIsFalseAndKommunalnetzIsFalse();
			default -> throw new RuntimeException("FehlerprotokollTyp nicht implementiert: " + fehlerprotokollTyp);
		};

		return osmAbbildungsFehler.stream()
			.map(OsmAbbildungsFehlerProtokollEintrag::new)
			.collect(Collectors.toList());
	}

	@Override
	public List<? extends FehlerprotokollEintrag> getAktuelleFehlerprotokolleInBereich(
		FehlerprotokollTyp fehlerprotokollTyp, Envelope bereich) {
		requireFehlerprotokollTypIsOsmAbbildung(fehlerprotokollTyp);

		Polygon bereichPolygon = EnvelopeAdapter.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		List<OsmAbbildungsFehler> osmAbbildungsFehler = switch (fehlerprotokollTyp) {
			case OSM_ABBILDUNG_RADNETZ ->
				osmAbbildungsFehlerRepository.findOsmAbbildungsFehlerInBereichRadNETZ(bereichPolygon);
			case OSM_ABBILDUNG_KREISNETZ ->
				osmAbbildungsFehlerRepository.findOsmAbbildungsFehlerInBereichKreisnetz(bereichPolygon);
			case OSM_ABBILDUNG_KOMMUNALNETZ ->
				osmAbbildungsFehlerRepository.findOsmAbbildungsFehlerInBereichKommunalnetz(bereichPolygon);
			case OSM_ABBILDUNG_SONSTIGE ->
				osmAbbildungsFehlerRepository.findOsmAbbildungsFehlerInBereichSonstige(bereichPolygon);
			default -> throw new RuntimeException("FehlerprotokollTyp nicht implementiert: " + fehlerprotokollTyp);
		};

		return osmAbbildungsFehler.stream()
			.map(OsmAbbildungsFehlerProtokollEintrag::new)
			.collect(Collectors.toList());
	}

	private static void requireFehlerprotokollTypIsOsmAbbildung(FehlerprotokollTyp fehlerprotokollTyp) {
		require(fehlerprotokollTyp == FehlerprotokollTyp.OSM_ABBILDUNG_RADNETZ
			|| fehlerprotokollTyp == FehlerprotokollTyp.OSM_ABBILDUNG_KREISNETZ
			|| fehlerprotokollTyp == FehlerprotokollTyp.OSM_ABBILDUNG_KOMMUNALNETZ
			|| fehlerprotokollTyp == FehlerprotokollTyp.OSM_ABBILDUNG_SONSTIGE);
	}
}
