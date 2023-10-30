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

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "de.wps.radvis.backend", importOptions = ImportOption.OnlyIncludeTests.class)
public class TestNamingConventionTest {

    @ArchTest
    static final ArchRule INTEGRATION_TESTS_HAVE_SUFFIX_IT = classes()
            .that().areAssignableTo(annotatedWith(DataJpaTest.class)).or()
            .areAssignableTo(annotatedWith(WebMvcTest.class)).or()
            .areAssignableTo(annotatedWith(SpringBootTest.class))
            .should().haveNameMatching(".*IT");

    @ArchTest
    static final ArchRule TEST_WITH_SUFFIX_IT_ARE_INTEGRATION_TEST = classes()
            .that().haveNameMatching(".*IT")
            .should().beAssignableTo(annotatedWith(DataJpaTest.class)).orShould()
            .beAssignableTo(annotatedWith(WebMvcTest.class)).orShould()
            .beAssignableTo(annotatedWith(SpringBootTest.class));
}
