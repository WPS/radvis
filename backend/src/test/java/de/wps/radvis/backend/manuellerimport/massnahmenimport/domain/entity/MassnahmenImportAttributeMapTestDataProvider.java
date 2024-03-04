package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportAttribute;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;

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
			massnahme.getBaulastZustaendiger().get().getDisplayText());
		attribute.put(MassnahmenImportAttribute.HANDLUNGSVERANTWORTLICHER.toString(),
			massnahme.getHandlungsverantwortlicher().get().toString());

		// optionale Attribute
		attribute.put(MassnahmenImportAttribute.PRIORITAET.toString(),
			String.valueOf(massnahme.getPrioritaet().get().getValue()));
		attribute.put(MassnahmenImportAttribute.KOSTENANNAHME.toString(),
			massnahme.getKostenannahme().get().getKostenannahme().toString());
		attribute.put(MassnahmenImportAttribute.UNTERHALTSZUSTAENDIGER.toString(),
			massnahme.getunterhaltsZustaendiger().get().getDisplayText());
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
		return attribute;
	}
}