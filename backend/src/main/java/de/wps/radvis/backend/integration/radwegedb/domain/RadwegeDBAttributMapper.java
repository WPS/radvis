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

package de.wps.radvis.backend.integration.radwegedb.domain;

import java.util.HashSet;
import java.util.Set;

import de.wps.radvis.backend.integration.netzbildung.domain.exception.AttributMappingException;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.AttributNichtImportiertException;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import lombok.NonNull;

public class RadwegeDBAttributMapper {

	RadwegeDBNetzbildungProtokollService radwegeDBNetzbildungProtokollService;

	public RadwegeDBAttributMapper(@NonNull RadwegeDBNetzbildungProtokollService radwegeDBNetzbildungProtokollService) {
		this.radwegeDBNetzbildungProtokollService = radwegeDBNetzbildungProtokollService;
	}

	public KantenAttribute mapKantenAttribute(ImportedFeature feature) {
		KantenAttribute.KantenAttributeBuilder kantenattribute = KantenAttribute
			.builder();

		if (feature.hasAttribut("beleuchtun") && feature.getAttribut("beleuchtun") != null) {
			try {
				kantenattribute.beleuchtung(mapBeleuchtung(feature.getAttribut("beleuchtun").toString()));
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		}
		return kantenattribute.build();
	}

	public ZustaendigkeitAttribute mapZustaendigkeitAttribute(ImportedFeature feature) {
		ZustaendigkeitAttribute.ZustaendigkeitAttributeBuilder zustaendigkeitAttribute = ZustaendigkeitAttribute
			.builder();

		if (feature.hasAttribut("vereinbaru") && feature.getAttribut("vereinbaru") != null) {
			zustaendigkeitAttribute
				.vereinbarungsKennung(mapVereinbarungsKennung(feature.getAttribut("vereinbaru").toString()));
		}

		return zustaendigkeitAttribute.build();
	}

	public FuehrungsformAttribute mapFuehrungsformAttribute(ImportedFeature feature) {
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformAttribute = FuehrungsformAttribute
			.builder();

		if (feature.hasAttribut("belag") && feature.getAttribut("belag") != null) {
			try {
				fuehrungsformAttribute.belagArt(mapBelag(feature.getAttribut("belag").toString()));
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		}
		if (feature.hasAttribut("breite") && feature.getAttribut("breite") != null) {
			try {
				fuehrungsformAttribute.breite(mapBreite(feature.getAttribut("breite").toString()));
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		}
		if (feature.hasAttribut("wegart") && feature.getAttribut("wegart") != null) {
			try {
				fuehrungsformAttribute
					.radverkehrsfuehrung(mapRadverkehrsfuehrung(feature.getAttribut("wegart").toString()));
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		}

		return fuehrungsformAttribute.build();
	}

	public Richtung mapRichtungAttribute(ImportedFeature feature) {
		if (feature.hasAttribut("richtung") && feature.getAttribut("richtung") != null) {
			try {
				return mapRichtung(feature.getAttribut("richtung").toString());
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
				return Richtung.defaultWert();
			}
		} else {
			return Richtung.defaultWert();
		}
	}

	public Set<IstStandard> mapIstStandard(ImportedFeature importedFeature) {
		return new HashSet<>();
	}

	public Set<Netzklasse> mapNetzKlassen(ImportedFeature importedFeature) {
		return new HashSet<>();
	}

	private VereinbarungsKennung mapVereinbarungsKennung(String vereinbarung) {
		if (vereinbarung.isEmpty() || vereinbarung.isBlank()) {
			return null;
		} else {
			return VereinbarungsKennung.of(vereinbarung);
		}

	}

	private Richtung mapRichtung(String richtung) throws AttributMappingException {
		switch (richtung) {
		case "0":
			return Richtung.defaultWert();
		case "1":
			return Richtung.BEIDE_RICHTUNGEN;
		case "2":
			return Richtung.IN_RICHTUNG;
		case "3":
			return Richtung.GEGEN_RICHTUNG;
		default:
			throw new AttributMappingException(
				"Der Wert '" + richtung + "' für das Attribut Richtung konnte nicht gemappt werden");
		}
	}

	public GeschwindigkeitAttribute mapGeschwindigkeitAttribute(ImportedFeature importedFeature) {
		return GeschwindigkeitAttribute.builder().build();
	}

	private Beleuchtung mapBeleuchtung(String beleuchtung) throws AttributMappingException {
		switch (beleuchtung) {
		case "1":
			return Beleuchtung.VORHANDEN;
		case "2":
			return Beleuchtung.NICHT_VORHANDEN;
		case "0":
		case "3":
		case "10":
		case "22":
			return Beleuchtung.UNBEKANNT;
		default:
			throw new AttributMappingException(
				"Der Wert '" + beleuchtung + "' für das Attribut Beleuchtung konnte nicht gemappt werden");
		}
	}

	private Radverkehrsfuehrung mapRadverkehrsfuehrung(String wegart) throws AttributMappingException {
		switch (wegart) {
		case "110":
			return Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG;
		case "121":
			return Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_SELBSTSTAENDIG;
		case "122":
			return Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG;
		case "130":
			return Radverkehrsfuehrung.SONSTIGER_BETRIEBSWEG;
		case "210":
			return Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND;
		case "221":
			return Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND;
		case "222":
			return Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND;
		case "230":
			return Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND;
		case "310":
			return Radverkehrsfuehrung.RADFAHRSTREIFEN;
		case "320":
			return Radverkehrsfuehrung.SCHUTZSTREIFEN;
		case "330":
			return Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR;
		case "370":
		case "372":
		case "373":
		case "374":
		case "3737":
			return Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN;
		case "371":
			return Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE;
		case "360":
			return Radverkehrsfuehrung.FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH;
		case "350":
			return Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI;
		case "340":
			return Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE;
		case "500":
		case "510":
		case "520":
			return Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG;
		case "0":
		case "100":
		case "120":
		case "200":
		case "220":
		case "300":
		case "343":
		case "400":
		case "410":
		case "411":
		case "412":
		case "420":
			return Radverkehrsfuehrung.UNBEKANNT;
		default:
			throw new AttributMappingException(
				"Der Wert '" + wegart + "' für das Attribut Radverkehrsfuehrung konnte nicht gemappt werden");
		}
	}

	private Laenge mapBreite(String breite) throws AttributMappingException {
		switch (breite) {
		case "1":
			return Laenge.of(1.49);
		case "2":
			return Laenge.of(1.5);
		case "3":
			return Laenge.of(2.5);
		case "4":
		case "5":
		case "6":
		case "7":
		case "10":
		case "11":
		case "0":
			return null;
		default:
			throw new AttributMappingException(
				"Der Wert '" + breite + "' für das Attribut Breite konnte nicht gemappt werden");
		}
	}

	private BelagArt mapBelag(String belag) throws AttributMappingException {
		switch (belag) {
		case "0":
			return BelagArt.UNBEKANNT;
		case "10":
			return BelagArt.ASPHALT;
		case "20":
			return BelagArt.BETON;
		case "31":
			return BelagArt.NATURSTEINPFLASTER;
		case "30":
		case "32":
		case "33":
			return BelagArt.BETONSTEINPFLASTER_PLATTENBELAG;
		case "40":
			return BelagArt.WASSERGEBUNDENE_DECKE;
		case "80":
			return BelagArt.SONSTIGER_BELAG;
		default:
			if (Integer.parseInt(belag) >= 50 && Integer.parseInt(belag) <= 72) {
				return BelagArt.UNGEBUNDENE_DECKE;
			} else {
				throw new AttributMappingException(
					"Der Wert '" + belag + "' für das Attribut Belagart konnte nicht gemappt werden");
			}
		}
	}

	private void handleAttributeMappingException(ImportedFeature feature, AttributMappingException e) {
		radwegeDBNetzbildungProtokollService.handle(
			new AttributNichtImportiertException(feature.getGeometrie(), e.getMessage()),
			RadwegeDBNetzbildungJob.class.getSimpleName());
	}
}
