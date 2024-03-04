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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.validation.constraints.AssertTrue;

@AnalyzeClasses(packages = "de.wps.radvis.backend", importOptions = ImportOption.DoNotIncludeTests.class)
public class NamingConventionTest {

	// An einigen Stellen werden durch Lambdas oder ein Switch auf Enums Inner Classes erzeugt.
	// Beispiel: neben NetzBefuellerService.class noch eine NetzBefuellerService$1.class inner class.
	static final String OR_INNER_CLASS = "(\\$*.+)?$";

	@ArchTest
	static final ArchRule CONTROLLER_SOLLTEN_MIT_CONTROLLER_ENDEN = classes()
		.that().areAnnotatedWith(RestController.class)
		.should().haveNameMatching(".*Controller" + OR_INNER_CLASS);

	// Es handelt sich hier um Java-BEAN-Validation. Daher müssen die Methodennamen
	// mit "is" beginnen oder direkt felder annotiert werden. Es muss eine entsprechende
	// Property geben und wenn die Methode nicht mit "is" anfaengt, findet Spring
	// die property nicht
	@ArchTest
	static final ArchRule VALIDATION_ASSERTS_IN_COMMANDS_SHOULD_HAVE_CORRECT_METHOD_NAME = methods()
		.that().areAnnotatedWith(AssertTrue.class)
		.should().haveNameMatching("is.*");

	// TODO move into seperate test-suite? (MusterTest o.ä.)
	@ArchTest
	static final ArchRule VALIDATION_ASSERTS_SHOULD_ONLY_BE_USED_IN_COMMANDS = methods()
		.that().areAnnotatedWith(AssertTrue.class)
		.should().beDeclaredInClassesThat(JavaClass.Predicates.simpleNameEndingWith("Command"));
}
