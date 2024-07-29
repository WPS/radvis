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

package de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.KantenKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.view.KonfliktView;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;

public class KonfliktToGeoJsonConverter {
	public FeatureCollection convert(List<KantenKonfliktProtokoll> konfliktprotokolle) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		konfliktprotokolle.forEach(konfliktprotokoll -> {
			Feature feature = GeoJsonConverter.createFeature(konfliktprotokoll.getKantenGeometrie());
			feature.setProperty("konflikte", konfliktprotokoll.getKonflikte().stream()
				.map(konflikt -> this.convertKonfliktToKonfliktView(konflikt, konfliktprotokoll.getKantenGeometrie()))
				.collect(Collectors.toSet()));
			feature.setId(Long.toString(konfliktprotokoll.getKanteId()));

			featureCollection.add(feature);
		});

		return featureCollection;
	}

	private KonfliktView convertKonfliktToKonfliktView(Konflikt konflikt, LineString lineString) {
		String nichtUebernommeneWerte = String.join(", ", konflikt.getNichtUebernommeneWerte());
		String betroffenerAbschnitt = "";

		if (konflikt.getLinearReferenzierterAbschnitt().getVonValue() != 0.
			|| konflikt.getLinearReferenzierterAbschnitt().getBisValue() != 1.) {
			double metermarkeVon = konflikt.getLinearReferenzierterAbschnitt().getVonValue() * lineString.getLength();
			double metermarkeBis = konflikt.getLinearReferenzierterAbschnitt().getBisValue() * lineString.getLength();
			betroffenerAbschnitt = new DecimalFormat("#.##").format(metermarkeVon) + " m bis "
				+ new DecimalFormat("#.##").format(metermarkeBis) + " m";
		}

		String seitenbezugName = konflikt.getSeitenbezug() != null ? konflikt.getSeitenbezug().name().toLowerCase()
			: "";

		return new KonfliktView(konflikt.getAttributName(), betroffenerAbschnitt,
			konflikt.getUebernommenerWert(), nichtUebernommeneWerte, seitenbezugName,
			konflikt.getBemerkung());
	}
}
