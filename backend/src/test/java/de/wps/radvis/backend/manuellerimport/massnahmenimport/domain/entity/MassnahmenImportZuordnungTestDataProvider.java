package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity;

import java.util.Map;

import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service.ManuellerMassnahmenImportService;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;

public class MassnahmenImportZuordnungTestDataProvider {
	public static MassnahmenImportZuordnung neuWithQuellAttribute(Map<String, String> attribute) {
		return new MassnahmenImportZuordnung(
			MassnahmeKonzeptID.of(
				attribute.getOrDefault(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, "id")),
			SimpleFeatureTestDataProvider.withAttributes(attribute),
			null,
			MassnahmenImportZuordnungStatus.NEU);
	}

	public static MassnahmenImportZuordnung gemapptWithQuellAttributesAndMassnahme(Map<String, String> attribute,
		Massnahme massnahme) {
		return new MassnahmenImportZuordnung(
			MassnahmeKonzeptID.of(
				attribute.getOrDefault(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, "id")),
			SimpleFeatureTestDataProvider.withAttributes(attribute),
			massnahme,
			MassnahmenImportZuordnungStatus.GEMAPPT);
	}

	public static MassnahmenImportZuordnung geloeschtWithMassnahme(Massnahme massnahme) {
		return new MassnahmenImportZuordnung(
			MassnahmeKonzeptID.of("id"),
			SimpleFeatureTestDataProvider.withAttributes(
				Map.of(ManuellerMassnahmenImportService.GELOESCHT_ATTRIBUTENAME, "ja")),
			massnahme,
			MassnahmenImportZuordnungStatus.GELOESCHT);
	}

	public static MassnahmenImportZuordnung fehlerhaft() {
		return new MassnahmenImportZuordnung(
			null,
			SimpleFeatureTestDataProvider.defaultFeature(),
			null,
			MassnahmenImportZuordnungStatus.FEHLERHAFT);
	}
}