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

package de.wps.radvis.backend.architektur;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.properties.HasName.AndFullName.Predicates.fullNameMatching;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;

import org.hibernate.envers.RevisionListener;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import de.wps.radvis.backend.abfrage.export.domain.InfrastrukturenExporterFactory;
import de.wps.radvis.backend.abfrage.fehlerprotokoll.schnittstelle.view.FehlerprotokollView;
import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageBRImportJob;
import de.wps.radvis.backend.application.schnittstelle.RequestLoggingInterceptor;
import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoApplier;
import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.entity.RevInfo;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.common.TogglzConfiguration;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.ExportConverterFactory;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteProfilEigenschaften;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Hoehenunterschied;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.FahrradzaehlstellenMobiDataImportJob;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributeAnreicherungsService;
import de.wps.radvis.backend.konsistenz.pruefung.schnittstelle.KonsistenzregelController;
import de.wps.radvis.backend.konsistenz.pruefung.schnittstelle.view.KonsistenzregelView;
import de.wps.radvis.backend.konsistenz.regeln.domain.Konsistenzregel;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilMatchResult;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilRoutingResult;
import de.wps.radvis.backend.matching.domain.valueObject.RoutingResult;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchingRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.GraphhopperRoutingRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.ProfilEigenschaftenCreator;

@AnalyzeClasses(packages = { "de.wps.radvis.backend" }, importOptions = ImportOption.DoNotIncludeTests.class)
public class SchichtungTest {
	@ArchTest
	public static final ArchRule MUSTER = Architectures.layeredArchitecture()
		.consideringAllDependencies()
		.layer("config").definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("Configuration"))
		.layer("controller")
		.definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("Controller")
			.or(simpleNameEndingWithIncludingGeneratedInnerClasses("ControllerAdvice")))
		// ArS: Jobs vorläufig wie Services eingeordnet. Muss noch geschärft werden. mw: command auch.
		.layer("service")
		.definedBy(
			simpleNameEndingWithIncludingGeneratedInnerClasses("Service")
				.or(fullNameMatching(InfrastrukturenExporterFactory.class.getName()))
				.or(simpleNameEndingWithIncludingGeneratedInnerClasses("ServiceImpl"))
				.or(fullNameMatching(AttributeAnreicherungsService.MergeFunction.class.getCanonicalName()))
				.or(simpleNameEndingWithIncludingGeneratedInnerClasses("Job"))
				.or(simpleNameEndingWithIncludingGeneratedInnerClasses("Scheduler"))
				.or(simpleNameEndingWithIncludingGeneratedInnerClasses("Schedule"))
				.or(simpleNameIncludingGeneratedInnerClasses("JobExecutionInputSummarySupplier"))
				.or(simpleNameEndingWithIncludingGeneratedInnerClasses("Reporter"))
				.or(simpleNameEndingWithIncludingGeneratedInnerClasses("Mapper"))
				.or(simpleNameEndingWithIncludingGeneratedInnerClasses("MapperFactory"))
				.or(simpleNameEndingWithIncludingGeneratedInnerClasses("ServiceFactory"))
				.or(fullNameMatching(AdditionalRevInfoHolder.class.getName()))
				.or(JavaClass.Predicates.assignableTo(RevisionListener.class))
				.or(simpleNameEndingWithIncludingGeneratedInnerClasses("Aspect"))
				.or(simpleNameEndingWithIncludingGeneratedInnerClasses("Konsistenzregel"))
				.or(simpleNameEndingWithIncludingGeneratedInnerClasses("AuthenticationProvider"))
				.or(simpleName("WithAuditing"))
				.or(fullNameMatching(RequestLoggingInterceptor.class.getName())))
		.layer("converter")
		.definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("Converter")
			.and(DescribedPredicate.not(simpleNameEndingWith("ExportConverter")
				.or(nameMatching("^.*" + "ExportConverter" + "\\$\\d+$"))))
			.and(DescribedPredicate.not(simpleNameEndingWith("AttributeConverter")
				.or(nameMatching("^.*" + "AttributeConverter" + "\\$\\d+$")))))
		.layer("exportConverter")
		.definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("ExportConverter").or(
			fullNameMatching(ExportConverterFactory.class.getName())))
		.layer("attributeConverter")
		.definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("AttributeConverter"))
		.layer("resolver").definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("Resolver")
			.or(simpleNameEndingWithIncludingGeneratedInnerClasses("ResolverImpl")))
		.layer("repository")
		.definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("Repository")
			.or(simpleNameIncludingGeneratedInnerClasses("DLMImportedFeatureXMLIterator"))
			.or(simpleNameIncludingGeneratedInnerClasses("LeihstationWFSXMLIterator"))
			.or(JavaClass.Predicates.simpleNameContaining("RepositoryImpl"))
			.or(JavaClass.Predicates.resideInAPackage("..repository.."))
			.or(JavaClass.Predicates.resideInAPackage("..repositoryImpl..")))
		.layer("entity")
		.definedBy(JavaClass.Predicates.resideInAPackage("..entity..")
			.or(simpleNameIncludingGeneratedInnerClasses("BenutzerBasicAuthenticationToken"))
			.or(simpleNameIncludingGeneratedInnerClasses("RadVisAuthentication"))
			.or(simpleNameEndingWithIncludingGeneratedInnerClasses("BenutzerHolder"))
			.or(simpleNameIncludingGeneratedInnerClasses("RadVisUserDetails"))
			.or(simpleNameIncludingGeneratedInnerClasses("RevInfo"))
			.or(simpleNameIncludingGeneratedInnerClasses("KantenViewMitPotentiellAbweichenderTopologie")))
		.layer("exception").definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("Exception"))
		.layer("dbView").definedBy(JavaClass.Predicates.resideInAnyPackage("..dbView.."))
		.layer("view")
		.definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("View")
			.and(JavaClass.Predicates.resideOutsideOfPackage("..dbView..")))
		.layer("command")
		.definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("Command")
			.or(simpleNameEndingWithIncludingGeneratedInnerClasses("CommandBuilder")))
		.layer("bezug").definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("Bezug"))
		.layer("valueObject")
		.definedBy(JavaClass.Predicates.resideInAnyPackage("..valueObject..", "..valueobject..")
			.and(DescribedPredicate.not(simpleName("KoordinatenReferenzSystem"))))
		.layer("guard").definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("Guard"))
		.layer("event").definedBy(JavaClass.Predicates.resideInAnyPackage("..event.."))
		.layer("liquibaseMigration").definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("LiquibaseMigration"))
		.layer("encodedValues").definedBy(JavaClass.Predicates.resideInAnyPackage("..encodedValues.."))
		.whereLayer("config").mayNotBeAccessedByAnyLayer()
		.whereLayer("controller").mayNotBeAccessedByAnyLayer()
		.whereLayer("guard").mayOnlyBeAccessedByLayers("controller", "config")
		.whereLayer("service").mayOnlyBeAccessedByLayers("controller", "config", "guard", "exportConverter")
		.whereLayer("encodedValues")
		.mayOnlyBeAccessedByLayers("repository")
		.whereLayer("repository")
		.mayOnlyBeAccessedByLayers("service", "controller", "config", "resolver", "exportConverter", "guard")
		.whereLayer("converter")
		.mayOnlyBeAccessedByLayers("controller", "guard", "repository", "config", "exportConverter",
			"liquibaseMigration")
		.whereLayer("resolver").mayOnlyBeAccessedByLayers("controller", "converter", "config", "service", "guard")
		.whereLayer("view")
		.mayOnlyBeAccessedByLayers("controller", "converter", "service", "repository", "config")
		.whereLayer("command").mayOnlyBeAccessedByLayers("controller", "converter", "guard")
		.whereLayer("bezug")
		.mayOnlyBeAccessedByLayers("entity", "view", "service", "converter", "guard", "controller", "repository")
		.whereLayer("entity")
		.mayOnlyBeAccessedByLayers("config", "service", "repository", "controller", "view", "converter",
			"resolver", "command", "guard", "bezug", "dbView", "event", "exportConverter", "liquibaseMigration")
		.whereLayer("exception")
		.mayOnlyBeAccessedByLayers("config", "service", "controller", "exportConverter", "entity", "repository",
			"view", "converter", "command", "guard")
		.whereLayer("valueObject")
		.mayOnlyBeAccessedByLayers("config", "service", "controller", "exportConverter", "entity", "bezug",
			"repository", "view", "converter", "command", "exception", "repository", "guard", "dbView", "event",
			"liquibaseMigration", "encodedValues")
		.whereLayer("event").mayOnlyBeAccessedByLayers("service", "entity")
		.whereLayer("dbView")
		.mayOnlyBeAccessedByLayers("repository", "service", "controller", "view", "guard", "entity")
		.whereLayer("exportConverter").mayOnlyBeAccessedByLayers("controller", "config")
		.whereLayer("attributeConverter").mayOnlyBeAccessedByLayers("valueObject", "entity", "liquibaseMigration")
		.whereLayer("liquibaseMigration").mayNotBeAccessedByAnyLayer()
		// Die RevInfo Entity is speziell. Sie benötigt Zugriff auf den AdditionalRevInfoApplier, der hier unter das
		// Muster "service" fällt.
		.ignoreDependency(RevInfo.class, AdditionalRevInfoApplier.class)
		// Konsistenzregeln machen so eine Art Spagat aus Service und Entity; wir ordnen es als Service ein und ignoren
		// das hier
		.ignoreDependency(KonsistenzregelView.class, Konsistenzregel.class)
		// TODO: Der Job ist laut dieser Einstufung ein Service und darf damit nicht auf einen Converter zugreifen
		.ignoreDependency(AbstellanlageBRImportJob.class, CoordinateReferenceSystemConverter.class)
		.ignoreDependency(FahrradzaehlstellenMobiDataImportJob.class, CoordinateReferenceSystemConverter.class);

	@ArchTest
	public static final ArchRule TECHNISCHE_SCHICHTUNG = Architectures.layeredArchitecture()
		.consideringAllDependencies()
		.layer("schnittstelle").definedBy("..schnittstelle..")
		.layer("domain").definedBy("..domain..")
		.layer("configuration").definedBy(simpleNameEndingWithIncludingGeneratedInnerClasses("Configuration"))
		.whereLayer("domain").mayOnlyBeAccessedByLayers("schnittstelle", "configuration")
		.whereLayer("schnittstelle").mayOnlyBeAccessedByLayers("configuration")
		// TODO: Der Job liegt in domain und darf somit nicht auf den Converter in Schnittstelle zugreifen
		.ignoreDependency(AbstellanlageBRImportJob.class, CoordinateReferenceSystemConverter.class)
		.ignoreDependency(FahrradzaehlstellenMobiDataImportJob.class, CoordinateReferenceSystemConverter.class);

	@ArchTest
	public static final ArchRule FACHLICHE_SCHICHTUNG = Architectures.layeredArchitecture()
		.consideringOnlyDependenciesInLayers()
		.layer("Application").definedBy("..application..")
		.layer("Administration").definedBy("..administration..")
		.layer("AbfrageSignatur").definedBy("..abfrage.signatur..")
		.layer("AbfrageNetzausschnitt").definedBy("..abfrage.netzausschnitt..")
		.layer("AbfrageStatistik").definedBy("..abfrage.statistik..")
		.layer("AbfrageAuswertung").definedBy("..abfrage.auswertung..")
		.layer("AbfrageExtern").definedBy("..abfrage.extern..")
		.layer("AbfrageFehlerprotokoll").definedBy("..abfrage.fehlerprotokoll..")
		.layer("AbfrageExport").definedBy("..abfrage.export..")
		.layer("Authentication").definedBy("..authentication..")
		.layer("BasicAuthentication").definedBy("..basicAuthentication..")
		.layer("Konsistenzregeln").definedBy("..konsistenz.regeln..")
		.layer("KonsistenzPruefung").definedBy("..konsistenz.pruefung..")
		.layer("ManuellerImportMassnahmenImport").definedBy("..manuellerimport.massnahmenimport..")
		.layer("ManuellerImportNetzzugehoerigkeit").definedBy("..manuellerimport.netzzugehoerigkeit..")
		.layer("ManuellerImportAttributeImport").definedBy("..manuellerimport.attributeimport..")
		.layer("ManuellerImportCommon").definedBy("..manuellerimport.common..")
		.layer("IntegrationGrundnetzReimport").definedBy("..integration.grundnetzReimport..")
		.layer("IntegrationGrundnetz").definedBy("..integration.grundnetz..")
		.layer("IntegrationRadnetz").definedBy("..integration.radnetz..")
		.layer("IntegrationRadwegeDB").definedBy("..integration.radwegedb..")
		.layer("IntegrationAttributAbbildung").definedBy("..integration.attributAbbildung..")
		.layer("IntegrationNetzbildung").definedBy("..integration.netzbildung..")
		.layer("Massnahme").definedBy("..massnahme..")
		.layer("Barriere").definedBy("..barriere..")
		.layer("FurtKreuzung").definedBy("..furtKreuzung..")
		.layer("Fahrradroute").definedBy("..fahrradroute..")
		.layer("WegweisendeBeschilderung").definedBy("..wegweisendeBeschilderung..")
		.layer("Matching").definedBy("..matching..")
		.layer("Netz").definedBy("..netz..")
		.layer("Netzfehler").definedBy("..netzfehler..")
		.layer("ImportGrundnetz").definedBy("..quellimport.grundnetz..")
		.layer("ImportRadnetz").definedBy("..quellimport.radnetz..")
		.layer("ImportTtsib").definedBy("..quellimport.ttsib..")
		.layer("ImportCommon").definedBy("..quellimport.common..")
		.layer("Organisation").definedBy("..organisation..")
		.layer("Common").definedBy("..backend.common..")
		.layer("Ortssuche").definedBy("..ortssuche..")
		.layer("Karte").definedBy("..karte..")
		.layer("Benutzer").definedBy("..benutzer..")
		.layer("Kommentar").definedBy("..kommentar..")
		.layer("Dokument").definedBy("..dokument..")
		.layer("Auditing").definedBy("..auditing..")
		.layer("WeitereKartenebenen").definedBy("..weitereKartenebenen..")
		.layer("Abstellanlage").definedBy("..abstellanlage..")
		.layer("Servicestation").definedBy("..servicestation..")
		.layer("Leihstation").definedBy("..leihstation..")
		.layer("Fahrradzaehlstelle").definedBy("..fahrradzaehlstelle..")

		.whereLayer("Application").mayNotBeAccessedByAnyLayer()
		.whereLayer("Administration").mayNotBeAccessedByAnyLayer()
		.whereLayer("AbfrageSignatur").mayOnlyBeAccessedByLayers("Application")
		.whereLayer("AbfrageNetzausschnitt").mayOnlyBeAccessedByLayers("AbfrageSignatur", "Application")
		.whereLayer("ManuellerImportMassnahmenImport").mayNotBeAccessedByAnyLayer()
		.whereLayer("ManuellerImportAttributeImport").mayNotBeAccessedByAnyLayer()
		.whereLayer("ManuellerImportNetzzugehoerigkeit").mayNotBeAccessedByAnyLayer()
		.whereLayer("ManuellerImportCommon")
		.mayOnlyBeAccessedByLayers("ManuellerImportAttributeImport", "ManuellerImportNetzzugehoerigkeit",
			"ManuellerImportMassnahmenImport", "AbfrageFehlerprotokoll")
		.whereLayer("Massnahme")
		.mayOnlyBeAccessedByLayers("Application", "AbfrageFehlerprotokoll", "AbfrageExport", "Konsistenzregeln",
			"ManuellerImportMassnahmenImport")
		.whereLayer("Konsistenzregeln").mayOnlyBeAccessedByLayers("KonsistenzPruefung")
		.whereLayer("KonsistenzPruefung").mayOnlyBeAccessedByLayers("Application", "Netzfehler")
		.whereLayer("IntegrationGrundnetz")
		.mayOnlyBeAccessedByLayers("IntegrationAttributAbbildung", "IntegrationGrundnetzReimport", "Application")
		.whereLayer("IntegrationAttributAbbildung")
		.mayOnlyBeAccessedByLayers("AbfrageNetzausschnitt", "IntegrationRadnetz", "IntegrationRadwegeDB",
			"IntegrationGrundnetzReimport",
			"Application")
		.whereLayer("IntegrationRadnetz").mayOnlyBeAccessedByLayers("Application", "IntegrationAttributAbbildung")
		.whereLayer("IntegrationRadwegeDB").mayOnlyBeAccessedByLayers("Application", "IntegrationAttributAbbildung")
		.whereLayer("IntegrationNetzbildung")
		.mayOnlyBeAccessedByLayers("IntegrationAttributAbbildung", "IntegrationRadnetz",
			"IntegrationRadwegeDB", "IntegrationGrundnetz", "IntegrationGrundnetzReimport", "Application")
		.whereLayer("Matching")
		.mayOnlyBeAccessedByLayers("IntegrationNetzbildung", "IntegrationAttributAbbildung", "IntegrationRadnetz",
			"IntegrationRadwegeDB", "ManuellerImportNetzzugehoerigkeit", "ManuellerImportAttributeImport",
			"IntegrationGrundnetzReimport", "Massnahme",
			"IntegrationGrundnetz", "Application", "Fahrradroute",
			"AbfrageFehlerprotokoll")
		.whereLayer("Netz")
		.mayOnlyBeAccessedByLayers("Matching", "IntegrationNetzbildung", "IntegrationAttributAbbildung",
			"IntegrationRadnetz", "IntegrationRadwegeDB", "IntegrationGrundnetz", "IntegrationGrundnetzReimport",
			"ManuellerImportNetzzugehoerigkeit", "ManuellerImportAttributeImport", "ManuellerImportCommon",
			"ManuellerImportMassnahmenImport", "AbfrageNetzausschnitt", "AbfrageSignatur", "AbfrageAuswertung",
			"Massnahme", "Barriere", "FurtKreuzung", "AbfrageFehlerprotokoll", "Fahrradroute", "AbfrageStatistik",
			"Application", "Konsistenzregeln", "KonsistenzPruefung", "Abstellanlage", "Servicestation", "Leihstation")
		.whereLayer("Netzfehler")
		.mayOnlyBeAccessedByLayers("Matching", "IntegrationNetzbildung", "IntegrationAttributAbbildung",
			"IntegrationRadnetz", "IntegrationRadwegeDB", "IntegrationGrundnetz", "AbfrageNetzausschnitt",
			"AbfrageExtern", "Application")
		.whereLayer("ImportGrundnetz")
		.mayOnlyBeAccessedByLayers("Matching", "IntegrationAttributAbbildung", "IntegrationRadnetz",
			"IntegrationRadwegeDB", "IntegrationGrundnetz", "IntegrationGrundnetzReimport", "AbfrageNetzausschnitt",
			"AbfrageSignatur", "Application")
		.whereLayer("ImportRadnetz")
		.mayOnlyBeAccessedByLayers("Matching", "IntegrationAttributAbbildung", "IntegrationRadnetz",
			"IntegrationRadwegeDB",
			"IntegrationGrundnetz", "Application")
		.whereLayer("ImportTtsib")
		.mayOnlyBeAccessedByLayers("Matching", "IntegrationAttributAbbildung", "IntegrationRadnetz",
			"IntegrationRadwegeDB",
			"IntegrationGrundnetz", "Application")
		.whereLayer("ImportCommon")
		.mayOnlyBeAccessedByLayers("ImportTtsib", "ImportRadnetz", "ImportGrundnetz", "Matching",
			"IntegrationAttributAbbildung", "IntegrationRadnetz", "IntegrationRadwegeDB",
			"IntegrationGrundnetz", "IntegrationGrundnetzReimport", "ManuellerImportNetzzugehoerigkeit",
			"ManuellerImportAttributeImport", "ManuellerImportCommon",
			"Application")
		.whereLayer("Organisation")
		.mayOnlyBeAccessedByLayers("Netz", "Matching", "IntegrationAttributAbbildung", "IntegrationRadnetz",
			"IntegrationRadwegeDB", "IntegrationGrundnetz", "ManuellerImportNetzzugehoerigkeit",
			"ManuellerImportAttributeImport", "ManuellerImportMassnahmenImport", "ManuellerImportCommon",
			"AbfrageNetzausschnitt", "AbfrageSignatur", "Application", "Benutzer", "AbfrageAuswertung",
			"Authentication", "BasicAuthentication", "Massnahme", "Barriere", "FurtKreuzung", "Fahrradroute",
			"AbfrageFehlerprotokoll",
			"Netzfehler", "Administration", "WegweisendeBeschilderung", "Leihstation", "Servicestation",
			"Abstellanlage", "Fahrradzaehlstelle")
		.whereLayer("Benutzer")
		.mayOnlyBeAccessedByLayers("Application", "Administration", "Authentication", "BasicAuthentication", "Netz",
			"AbfrageNetzausschnitt",
			"Fahrradroute", "Kommentar", "Dokument", "ManuellerImportCommon", "ManuellerImportAttributeImport",
			"ManuellerImportMassnahmenImport", "ManuellerImportNetzzugehoerigkeit", "Massnahme", "WeitereKartenebenen",
			"IntegrationRadnetz", "Netzfehler", "Auditing", "Common", "Barriere", "FurtKreuzung", "Servicestation",
			"Abstellanlage", "Leihstation", "Matching")
		.whereLayer("Ortssuche").mayNotBeAccessedByAnyLayer()
		.whereLayer("Karte").mayNotBeAccessedByAnyLayer()
		.whereLayer("Kommentar").mayOnlyBeAccessedByLayers("Massnahme", "Netzfehler")
		.whereLayer("Dokument").mayOnlyBeAccessedByLayers("Massnahme", "Servicestation", "Abstellanlage")
		.whereLayer("Barriere").mayOnlyBeAccessedByLayers("Matching")
		.whereLayer("FurtKreuzung").mayNotBeAccessedByAnyLayer()
		.whereLayer("WegweisendeBeschilderung").mayOnlyBeAccessedByLayers("Application", "Konsistenzregeln")
		.whereLayer("AbfrageStatistik").mayNotBeAccessedByAnyLayer()
		.whereLayer("AbfrageAuswertung").mayNotBeAccessedByAnyLayer()
		.whereLayer("AbfrageExtern").mayNotBeAccessedByAnyLayer()
		.whereLayer("AbfrageFehlerprotokoll").mayOnlyBeAccessedByLayers("Application")
		.whereLayer("WeitereKartenebenen").mayNotBeAccessedByAnyLayer()
		.whereLayer("Leihstation").mayOnlyBeAccessedByLayers("AbfrageExport", "Application")
		.whereLayer("Servicestation").mayOnlyBeAccessedByLayers("AbfrageExport", "Application")
		.whereLayer("Abstellanlage").mayOnlyBeAccessedByLayers("AbfrageExport", "Application")
		.whereLayer("Fahrradzaehlstelle").mayOnlyBeAccessedByLayers("Application")
		.whereLayer("AbfrageExport").mayNotBeAccessedByAnyLayer()
		.whereLayer("IntegrationGrundnetzReimport")
		.mayOnlyBeAccessedByLayers("Application", "AbfrageNetzausschnitt", "ManuellerImportCommon")
		.whereLayer("Fahrradroute").mayOnlyBeAccessedByLayers("Application", "AbfrageExport", "AbfrageFehlerprotokoll")
		.whereLayer("Common").mayNotAccessAnyLayer() // right?
		.whereLayer("BasicAuthentication").mayOnlyBeAccessedByLayers("Application")
		.whereLayer("Authentication").mayOnlyBeAccessedByLayers("Application", "Auditing", "Benutzer")

		// TODO fix this!
		.ignoreDependency(ProfilMatchResult.class, LinearReferenzierteProfilEigenschaften.class)
		.ignoreDependency(ProfilRoutingResult.class, LinearReferenzierteProfilEigenschaften.class)
		.ignoreDependency(ProfilRoutingResult.class, Hoehenunterschied.class)
		.ignoreDependency(RoutingResult.class, Hoehenunterschied.class)
		.ignoreDependency(DlmMatchingRepositoryImpl.class, LinearReferenzierteProfilEigenschaften.class)
		.ignoreDependency(GraphhopperRoutingRepositoryImpl.class, LinearReferenzierteProfilEigenschaften.class)
		.ignoreDependency(GraphhopperRoutingRepositoryImpl.class, Hoehenunterschied.class)
		.ignoreDependency(ProfilEigenschaftenCreator.class, LinearReferenzierteProfilEigenschaften.class)
		.ignoreDependency(ProfilEigenschaftenCreator.class, FahrradrouteProfilEigenschaften.class)
		// TODO fix this!
		.ignoreDependency(KonsistenzregelController.class, FehlerprotokollView.class)
		// TODO fix this!
		.ignoreDependency(TogglzConfiguration.class, Recht.class)
		.ignoreDependency(AbstractJob.class, AdditionalRevInfoHolder.class);

	// TODO integrate those!
	private final Architectures.LayeredArchitecture missingLayers = Architectures.layeredArchitecture()
		.consideringOnlyDependenciesInLayers()
		.whereLayer("Auditing").mayNotBeAccessedByAnyLayer();

	private static DescribedPredicate<JavaClass> simpleNameEndingWithIncludingGeneratedInnerClasses(String suffix) {
		return JavaClass.Predicates.belongTo(simpleNameEndingWith(suffix));
	}

	private static DescribedPredicate<JavaClass> simpleNameIncludingGeneratedInnerClasses(String name) {
		return JavaClass.Predicates.belongTo(simpleName(name));
	}
}
