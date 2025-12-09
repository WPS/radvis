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

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import de.wps.radvis.backend.abfrage.netzausschnitt.AbfrageNetzausschnittConfiguration;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.StreckeViewCacheRepository;
import de.wps.radvis.backend.abfrage.signatur.SignaturConfiguration;
import de.wps.radvis.backend.abfrage.statistik.domain.StatistikRepository;
import de.wps.radvis.backend.application.JobConfiguration;
import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoApplier;
import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.basicAuthentication.BasicAuthenticationConfiguration;
import de.wps.radvis.backend.basicAuthentication.domain.BasicAuthPasswortService;
import de.wps.radvis.backend.basicAuthentication.domain.BenutzerBasicAuthenticationToken;
import de.wps.radvis.backend.benutzer.domain.BenutzerAktivitaetsService;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.fahrradroute.FahrradrouteConfiguration;
import de.wps.radvis.backend.fahrradzaehlstelle.FahrradzaehlstelleConfiguration;
import de.wps.radvis.backend.karte.KarteConfiguration;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepository;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.RandomMassnahmenGenerierenJob;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.ortssuche.OrtsSucheConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.ImportsGrundnetzConfiguration;
import de.wps.radvis.backend.weitereKartenebenen.WeitereKartenebenenConfiguration;

@AnalyzeClasses(packages = { "de.wps.radvis.backend" }, importOptions = ImportOption.DoNotIncludeTests.class)
public class MusterTest {

	@ArchTest
	static final ArchRule noClassesShouldUseJavaUtilLogging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

	@ArchTest
	static final ArchRule noClassesShouldUseStdStreamsForLogging = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

	@ArchTest
	static final ArchRule CONTROLLER_METHODS_ACCEPTING_COMMANDS_SHOULD_HAVE_POST_OR_DELETE_MAPPING = methods()
		.that().haveRawParameterTypes(containing(JavaClass.Predicates.simpleNameEndingWith("Command")))
		.and().areDeclaredInClassesThat(JavaClass.Predicates.simpleNameEndingWith("Controller"))
		.should().beAnnotatedWith(PostMapping.class)
		.orShould().beAnnotatedWith(DeleteMapping.class);

	@ArchTest
	static final ArchRule REPOSITORIES_SHOULD_HAVE_INTERFACE = classes().that()
		.resideInAnyPackage("..domain..").and().haveSimpleNameEndingWith("Repository")
		// TODO: auftrennen in RepositoryImpl und Interface!
		.and().areNotAssignableTo(StreckeViewCacheRepository.class)
		.and().areNotAssignableTo(StatistikRepository.class)
		.and().areNotAssignableTo(InMemoryKantenRepository.class)
		.should().beInterfaces();

	@ArchTest
	static final ArchRule NO_SPRINGSECURITY_CLASSES_IN_DOMAIN = noClasses().that().resideInAnyPackage("..domain..")
		// ist in folgenden Fällen korrekt:
		.and().doNotBelongToAnyOf(RadVisAuthentication.class, RadVisUserDetails.class,
			BenutzerBasicAuthenticationToken.class, AdditionalRevInfoApplier.class, BasicAuthPasswortService.class,
			BenutzerResolver.class, BenutzerAktivitaetsService.class)
		// FIXME: violations
		.and().doNotBelongToAnyOf(RandomMassnahmenGenerierenJob.class)
		.should()
		.dependOnClassesThat(JavaClass.Predicates.resideInAnyPackage("..org.springframework.security..")
			.and(not(JavaClass.Predicates.belongToAnyOf(AccessDeniedException.class))))
		.because("das weist auf eine schlechte Trennung zwischen Schnittstelle und Domain hin");

	@ArchTest
	static final ArchRule MODIFYING_CONTROLLER_METHODS_SHOULD_CALL_CORRESPONDING_GUARD = methods()
		.that(DescribedPredicate.and(annotatedWith(PostMapping.class),
			rawParameterTypes(containing(JavaClass.Predicates.simpleNameEndingWith("Command")))))
		.or().areAnnotatedWith(DeleteMapping.class)
		.and().areDeclaredInClassesThat(JavaClass.Predicates.simpleNameEndingWith("Controller"))
		// Violations! TODO: Fix me!
		.and().doNotHaveFullName(
			"de.wps.radvis.backend.barriere.schnittstelle.BarriereController.updateBarriere(java.lang.Long, de.wps.radvis.backend.barriere.schnittstelle.SaveBarriereCommand, org.springframework.security.core.Authentication)")
		.and().doNotHaveFullName(
			"de.wps.radvis.backend.barriere.schnittstelle.BarriereController.createBarriere(de.wps.radvis.backend.barriere.schnittstelle.SaveBarriereCommand, org.springframework.security.core.Authentication)")
		.and().doNotHaveFullName(
			"de.wps.radvis.backend.fahrradroute.schnittstelle.FahrradrouteController.deleteFahrradroute(java.lang.Long, org.springframework.security.core.Authentication, de.wps.radvis.backend.fahrradroute.schnittstelle.DeleteFahrradrouteCommand)")
		.and().doNotHaveFullName(
			"de.wps.radvis.backend.fahrradroute.schnittstelle.FahrradrouteController.saveFahrradroute(org.springframework.security.core.Authentication, de.wps.radvis.backend.fahrradroute.schnittstelle.SaveFahrradrouteCommand)")
		.and().doNotHaveFullName(
			"de.wps.radvis.backend.furtKreuzung.schnittstelle.FurtKreuzungController.createFurtKreuzung(de.wps.radvis.backend.furtKreuzung.schnittstelle.SaveFurtKreuzungCommand, org.springframework.security.core.Authentication)")
		.and().doNotHaveFullName(
			"de.wps.radvis.backend.furtKreuzung.schnittstelle.FurtKreuzungController.updateFurtKreuzung(java.lang.Long, de.wps.radvis.backend.furtKreuzung.schnittstelle.SaveFurtKreuzungCommand, org.springframework.security.core.Authentication)")
		.and().doNotHaveFullName(
			"de.wps.radvis.backend.massnahme.schnittstelle.MassnahmeController.deleteDatei(org.springframework.security.core.Authentication, java.lang.Long, java.lang.Long)")
		.and().doNotHaveFullName(
			"de.wps.radvis.backend.massnahme.schnittstelle.MassnahmeController.deleteMassnahme(java.lang.Long, org.springframework.security.core.Authentication, de.wps.radvis.backend.massnahme.schnittstelle.DeleteMassnahmeCommand)")
		.and().doNotHaveFullName(
			"de.wps.radvis.backend.massnahme.schnittstelle.MassnahmeController.uploadDatei(java.lang.Long, de.wps.radvis.backend.dokument.schnittstelle.AddDokumentCommand, org.springframework.security.core.Authentication)")
		.and().doNotHaveFullName(
			"de.wps.radvis.backend.netz.schnittstelle.NetzController.createKante(de.wps.radvis.backend.netz.schnittstelle.command.CreateKanteCommand, org.springframework.security.core.Authentication)")
		.should(callCorrespondingGuard());

	@ArchTest
	static final ArchRule PROPERTIES_SHOULD_BE_UNPACKED_IN_CONFIG = classes().that()
		.haveSimpleNameEndingWith("Configuration")
		// FIXME
		.and().areNotAssignableTo(AbfrageNetzausschnittConfiguration.class)
		.and().areNotAssignableTo(SignaturConfiguration.class)
		.and().areNotAssignableTo(JobConfiguration.class)
		.and().areNotAssignableTo(BasicAuthenticationConfiguration.class)
		.and().areNotAssignableTo(FahrradrouteConfiguration.class)
		.and().areNotAssignableTo(FahrradzaehlstelleConfiguration.class)
		.and().areNotAssignableTo(KarteConfiguration.class)
		.and().areNotAssignableTo(MassnahmeConfiguration.class)
		.and().areNotAssignableTo(MatchingConfiguration.class)
		.and().areNotAssignableTo(NetzConfiguration.class)
		.and().areNotAssignableTo(OrtsSucheConfiguration.class)
		.and().areNotAssignableTo(ImportsGrundnetzConfiguration.class)
		.and().areNotAssignableTo(WeitereKartenebenenConfiguration.class)
		.should(notPassConfigurationPropertiesClassesToBeans())
		.because("Information Hiding is important & ConfigurationProperties shouldn't be all over the place");

	private static ArchCondition<JavaClass> notPassConfigurationPropertiesClassesToBeans() {
		return new ArchCondition<>("not pass ConfigurationProperties-Classes to Beans") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
				Set<JavaCall<?>> ctorCallsWithPropertiesPassed = javaClass.getConstructorCallsFromSelf().stream()
					.filter(javaCall -> javaCall.getTarget().getRawParameterTypes().stream().anyMatch(
						parameter -> parameter.getName().endsWith("Properties")))
					.collect(Collectors.toSet());

				if (ctorCallsWithPropertiesPassed.isEmpty()) {
					conditionEvents.add(new SimpleConditionEvent(
						javaClass,
						true,
						javaClass.getFullName() + " doesn't pass Properties to any Bean: "
							+ javaClass.getSourceCodeLocation()
					));
				} else {
					String listOfViolations = ctorCallsWithPropertiesPassed.stream()
						.map(ctorCall -> "\t" + ctorCall.getTargetOwner().getSimpleName() + " "
							+ ctorCall.getSourceCodeLocation())
						.collect(Collectors
							.joining("\n"));
					conditionEvents.add(new SimpleConditionEvent(
						javaClass,
						false,
						javaClass.getFullName() + " passes Properties to following Beans:\n"
							+ listOfViolations
					));
				}

			}
		};
	}

	private static DescribedPredicate<List<JavaClass>> containing(DescribedPredicate<JavaClass> clazz) {
		return new DescribedPredicate<>("containing " + clazz.getDescription()) {
			@Override
			public boolean test(List<JavaClass> javaClasses) {
				return javaClasses.stream().anyMatch(clazz);
			}
		};
	}

	private static ArchCondition<JavaMethod> callCorrespondingGuard() {
		return new ArchCondition<>("call guard-Method with same signature first, if any") {
			@Override
			public void check(JavaMethod javaMethod, ConditionEvents conditionEvents) {
				Set<JavaCall<?>> guardCalls = javaMethod.getCallsFromSelf().stream()
					.filter(javaCall -> javaCall.getTarget().getOwner().getName().endsWith("Guard"))
					.collect(Collectors.toSet());

				if (guardCalls.isEmpty()) {
					conditionEvents.add(new SimpleConditionEvent(javaMethod, true,
						javaMethod.getFullName() + " doesn't call any guard: "
							+ javaMethod.getSourceCodeLocation()));
					return;
				}

				Optional<JavaCall<?>> callWithCorrespondingMethodName = guardCalls.stream()
					.filter(c -> c.getTarget().getName().equals(javaMethod.getName()))
					.findAny();

				conditionEvents.add(new SimpleConditionEvent(javaMethod, callWithCorrespondingMethodName.isPresent(),
					javaMethod.getFullName() + " calls guard with different method name: "
						+ javaMethod.getSourceCodeLocation()));

				List<String> callerParameterTypes = javaMethod.getParameters()
					.stream()
					.map(JavaParameter::getType)
					.map(JavaType::getName)
					.toList();

				List<String> guardMethodParameterTypes = callWithCorrespondingMethodName.get().getTarget()
					.resolveMember().get()
					.getParameters()
					.stream()
					.map(JavaParameter::getType)
					.map(JavaType::getName)
					.toList();

				boolean methodSignaturesAreEqual = callerParameterTypes.equals(guardMethodParameterTypes);

				conditionEvents.add(new SimpleConditionEvent(javaMethod, methodSignaturesAreEqual,
					String.format("%s calls guard with different parameters: %s\n  Expected: %s\n  Actual: %s",
						javaMethod.getFullName(),
						javaMethod.getSourceCodeLocation(),
						callerParameterTypes,
						guardMethodParameterTypes)));

			}
		};
	}
}
