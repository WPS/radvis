package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportAttribute;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.BegruendungStornierungsanfrage;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;

public class MassnahmenImportAttributeMapTestDataProvider {
	public static Map<String, String> fromMassnahme(Massnahme massnahme) {
		Map<String, String> attribute = new HashMap<>();

		// Pflichtattribute
		attribute.put(MassnahmenImportAttribute.UMSETZUNGSSTATUS.toString(), Umsetzungsstatus.IDEE.toString());
		attribute.put(MassnahmenImportAttribute.BEZEICHNUNG.toString(), massnahme.getBezeichnung().toString());
		attribute.put(MassnahmenImportAttribute.KATEGORIEN.toString(), massnahme.getMassnahmenkategorien().stream()
			.map(Enum::name)
			.collect(Collectors.joining(";")));
		attribute.put(MassnahmenImportAttribute.ZUSTAENDIGER.toString(),
			massnahme.getZustaendiger().get().getDisplayText());
		attribute.put(MassnahmenImportAttribute.SOLL_STANDARD.toString(), massnahme.getSollStandard().toString());

		// Pflichtattribute ab Planung
		attribute.put(MassnahmenImportAttribute.DURCHFUEHRUNGSZEITRAUM.toString(),
			massnahme.getDurchfuehrungszeitraum().get().getGeplanterUmsetzungsstartJahr() == null ? ""
				: massnahme.getDurchfuehrungszeitraum().get().getGeplanterUmsetzungsstartJahr().toString());
		attribute.put(MassnahmenImportAttribute.BAULASTTRAEGER.toString(),
			massnahme.getBaulastZustaendiger().map(v -> v.getDisplayText()).orElse(""));
		attribute.put(MassnahmenImportAttribute.HANDLUNGSVERANTWORTLICHER.toString(),
			massnahme.getHandlungsverantwortlicher().get().toString());

		// optionale Attribute
		attribute.put(MassnahmenImportAttribute.PRIORITAET.toString(),
			String.valueOf(massnahme.getPrioritaet().get().getValue()));
		attribute.put(MassnahmenImportAttribute.KOSTENANNAHME.toString(),
			massnahme.getKostenannahme().get().getKostenannahme().toString());
		attribute.put(MassnahmenImportAttribute.UNTERHALTSZUSTAENDIGER.toString(),
			massnahme.getunterhaltsZustaendiger().map(v -> v.getDisplayText()).orElse(""));
		attribute.put(MassnahmenImportAttribute.MAVIS_ID.toString(), massnahme.getMaViSID().get().toString());
		attribute.put(MassnahmenImportAttribute.VERBA_ID.toString(), massnahme.getVerbaID().get().toString());
		attribute.put(MassnahmenImportAttribute.LGVFG_ID.toString(), massnahme.getLGVFGID().get().toString());
		attribute.put(MassnahmenImportAttribute.REALISIERUNGSHILFE.toString(),
			massnahme.getRealisierungshilfe().get().toString());
		attribute.put(MassnahmenImportAttribute.NETZKLASSEN.toString(), massnahme.getNetzklassen().stream()
			.map(Enum::name)
			.collect(Collectors.joining(";")));
		attribute.put(MassnahmenImportAttribute.PLANUNG_ERFORDERLICH.toString(),
			massnahme.getPlanungErforderlich() ? "Ja" : "Nein");
		attribute.put(MassnahmenImportAttribute.VEROEFFENTLICHT.toString(),
			massnahme.getVeroeffentlicht() ? "Ja" : "Nein");
		attribute.put(MassnahmenImportAttribute.ZURUECKSTELLUNGS_GRUND.toString(),
			massnahme.getZurueckstellungsGrund().map(g -> g.toString()).orElse(""));
		attribute.put(MassnahmenImportAttribute.BEGRUENDUNG_STORNIERUNGSANFRAGE.toString(),
			massnahme.getBegruendungStornierungsanfrage().map(g -> g.toString()).orElse(""));
		attribute.put(MassnahmenImportAttribute.BEGRUENDUNG_ZURUECKSTELLUNG.toString(),
			massnahme.getBegruendungZurueckstellung().map(g -> g.toString()).orElse(""));

		return attribute;
	}

	private static Map<String, String> allCommonValue(String value) {
		Map<String, String> attribute = new HashMap<>();

		// Pflichtattribute
		attribute.put(MassnahmenImportAttribute.UMSETZUNGSSTATUS.toString(), value);
		attribute.put(MassnahmenImportAttribute.BEZEICHNUNG.toString(), value);
		attribute.put(MassnahmenImportAttribute.KATEGORIEN.toString(), value);
		attribute.put(MassnahmenImportAttribute.ZUSTAENDIGER.toString(), value);
		attribute.put(MassnahmenImportAttribute.SOLL_STANDARD.toString(), value);

		// Pflichtattribute ab Planung
		attribute.put(MassnahmenImportAttribute.DURCHFUEHRUNGSZEITRAUM.toString(), value);
		attribute.put(MassnahmenImportAttribute.BAULASTTRAEGER.toString(), value);
		attribute.put(MassnahmenImportAttribute.HANDLUNGSVERANTWORTLICHER.toString(), value);

		// optionale Attribute
		attribute.put(MassnahmenImportAttribute.PRIORITAET.toString(), value);
		attribute.put(MassnahmenImportAttribute.KOSTENANNAHME.toString(), value);
		attribute.put(MassnahmenImportAttribute.UNTERHALTSZUSTAENDIGER.toString(), value);
		attribute.put(MassnahmenImportAttribute.MAVIS_ID.toString(), value);
		attribute.put(MassnahmenImportAttribute.VERBA_ID.toString(), value);
		attribute.put(MassnahmenImportAttribute.LGVFG_ID.toString(), value);
		attribute.put(MassnahmenImportAttribute.REALISIERUNGSHILFE.toString(), value);
		attribute.put(MassnahmenImportAttribute.NETZKLASSEN.toString(), value);
		attribute.put(MassnahmenImportAttribute.PLANUNG_ERFORDERLICH.toString(), value);
		attribute.put(MassnahmenImportAttribute.VEROEFFENTLICHT.toString(), value);
		attribute.put(MassnahmenImportAttribute.BEGRUENDUNG_STORNIERUNGSANFRAGE.toString(), value);
		attribute.put(MassnahmenImportAttribute.BEGRUENDUNG_ZURUECKSTELLUNG.toString(), value);
		attribute.put(MassnahmenImportAttribute.ZURUECKSTELLUNGS_GRUND.toString(), value);

		return attribute;
	}

	public static Map<String, String> withBlankValues() {
		Map<String, String> attribute = allCommonValue("");
		attribute.put(MassnahmenImportAttribute.PLANUNG_ERFORDERLICH.toString(), "Nein");
		attribute.put(MassnahmenImportAttribute.VEROEFFENTLICHT.toString(), "Nein");
		return attribute;
	}

	public static Map<String, String> withIncorrectValues() {
		Map<String, String> attribute = allCommonValue("Ungültiger Wert");
		String stringOverMaxLength = "X".repeat(256);
		attribute.put(MassnahmenImportAttribute.BEZEICHNUNG.toString(), stringOverMaxLength);
		attribute.put(MassnahmenImportAttribute.MAVIS_ID.toString(), stringOverMaxLength);
		attribute.put(MassnahmenImportAttribute.VERBA_ID.toString(), stringOverMaxLength);
		attribute.put(MassnahmenImportAttribute.LGVFG_ID.toString(), stringOverMaxLength);
		attribute.put(MassnahmenImportAttribute.BEGRUENDUNG_STORNIERUNGSANFRAGE.toString(),
			"X".repeat(BegruendungStornierungsanfrage.MAX_LENGTH + 1));
		attribute.put(MassnahmenImportAttribute.BEGRUENDUNG_ZURUECKSTELLUNG.toString(),
			"X".repeat(BegruendungStornierungsanfrage.MAX_LENGTH + 1));
		return attribute;
	}

	public static Map<String, String> dummyPflichtattribute() {
		Map<String, String> attribute = new HashMap<>();

		attribute.put(MassnahmenImportAttribute.UMSETZUNGSSTATUS.toString(), Umsetzungsstatus.IDEE.toString());
		attribute.put(MassnahmenImportAttribute.BEZEICHNUNG.toString(), "Bezeichnende Bezeichnung");
		attribute.put(MassnahmenImportAttribute.KATEGORIEN.toString(),
			List.of(Massnahmenkategorie.FURTEN_ERNEUERN.name(), Massnahmenkategorie.EINRICHTUNG_FAHRRADSTRASSE.name())
				.stream()
				.collect(Collectors.joining(";")));
		attribute.put(MassnahmenImportAttribute.ZUSTAENDIGER.toString(), "Großoberkleinmitteluntenbach (Gemeinde)");
		attribute.put(MassnahmenImportAttribute.SOLL_STANDARD.toString(),
			SollStandard.STARTSTANDARD_RADNETZ.toString());

		return attribute;
	}
}