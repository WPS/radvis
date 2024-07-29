package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity;

import java.util.HashMap;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;

import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service.ManuellerMassnahmenImportService;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;

public class MassnahmenImportZuordnungTestDataProvider {
	public static MassnahmenImportZuordnung neuWithQuellAttribute(Map<String, String> attribute) {
		return new MassnahmenImportZuordnung(
			MassnahmeKonzeptID.of(
				attribute.getOrDefault(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, "id")),
			SimpleFeatureTestDataProvider.withAttributes(attribute),
			null,
			MassnahmenImportZuordnungStatus.NEU);
	}

	public static MassnahmenImportZuordnung gemapptWithQuellAttributeAndMassnahme(Map<String, String> attribute,
		Massnahme massnahme) {
		return new MassnahmenImportZuordnung(
			MassnahmeKonzeptID.of(
				attribute.getOrDefault(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, "id")),
			SimpleFeatureTestDataProvider.withAttributes(attribute),
			massnahme,
			MassnahmenImportZuordnungStatus.ZUGEORDNET);
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

	public static MassnahmenImportZuordnung neuWithFeature(SimpleFeature feature) {
		return new MassnahmenImportZuordnung(
			MassnahmeKonzeptID.of("id"),
			feature,
			null,
			MassnahmenImportZuordnungStatus.NEU);
	}

	public static MassnahmenImportZuordnung neuWithNetzbezug(MassnahmeNetzBezug netzbezug) {
		MassnahmenImportZuordnung zuordnung = neuWithQuellAttribute(new HashMap<>());
		zuordnung.aktualisiereNetzbezug(netzbezug, false);
		return zuordnung;
	}
}