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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "de.wps.radvis.backend", importOptions = ImportOption.DoNotIncludeTests.class)
public class SpringFrameworkUsageTest {

	@ArchTest
	static final ArchRule SPRING_SECURITY_ANNOTATIONS_SHOULD_BE_ON_PUBLIC_METHODS = methods()
		.that().areMetaAnnotatedWith(PreAuthorize.class)
		.or().areMetaAnnotatedWith(PostAuthorize.class)
		.or().areMetaAnnotatedWith(PreFilter.class)
		.or().areMetaAnnotatedWith(PostFilter.class)
		.should().bePublic().because("Sicherheitslücke in Spring Security: https://spring.io/security/cve-2025-41232");
}
