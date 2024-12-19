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

package de.wps.radvis.backend.netz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.data.history.RevisionSort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.entity.RevInfo;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.AuditingTestIT;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.FuehrungsformAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.GeschwindigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.ZustaendigkeitAttributGruppeRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import jakarta.transaction.Transactional;

@Tag("group7")
@ContextConfiguration(classes = {
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
	CommonConfiguration.class,
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
class LineareReferenzenDefragmentierungJobAuditingTestIT extends AuditingTestIT {

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private LineareReferenzenDefragmentierungJob lineareReferenzenDefragmentierungJob;

	@Autowired
	private FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppeRepository;

	@Autowired
	private ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppeRepository;

	@Autowired
	private GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository;

	@Test
	@Transactional
	void run() {
		// arrange
		TestTransaction.end();
		TestTransaction.start();
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);
		Kante kanteWithMiniSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.005).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.005, 1).build()),
					false))
				.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder()
					.geschwindigkeitAttribute(
						List.of(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.005).build(),
							GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.005, 1).build()))
					.build())
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.005).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.005, 1).build()))
					.build())
				.build());
		Kante kanteWithOneSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).build()), false))
				.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder()
					.geschwindigkeitAttribute(
						List.of(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1).build()))
					.build())
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1).build()))
					.build())
				.build());
		testEntityManager.flush();
		testEntityManager.clear();
		TestTransaction.flagForCommit();
		TestTransaction.end();

		// act
		TestTransaction.start();
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.LINEARE_REFERENZEN_DEFRAGMENTIERUNG_JOB);
		lineareReferenzenDefragmentierungJob.doRun();
		testEntityManager.flush();
		testEntityManager.clear();
		TestTransaction.flagForCommit();
		TestTransaction.end();

		// assert
		TestTransaction.start();
		Kante kanteWithMiniSegmentAfter = kantenRepository.findById(kanteWithMiniSegment.getId()).get();
		assertThat(kanteWithMiniSegmentAfter.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.hasSize(1);
		assertThat(kanteWithMiniSegmentAfter.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute())
			.hasSize(1);
		assertThat(kanteWithMiniSegmentAfter.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.hasSize(1);

		PageRequest pageRequest = PageRequest.of(0, 2, RevisionSort.desc());
		checkRevisions(
			fuehrungsformAttributGruppeRepository
				.findRevisions(kanteWithOneSegment.getFuehrungsformAttributGruppe().getId(), pageRequest),
			fuehrungsformAttributGruppeRepository
				.findRevisions(kanteWithMiniSegment.getFuehrungsformAttributGruppe().getId(), pageRequest));

		checkRevisions(
			zustaendigkeitAttributGruppeRepository
				.findRevisions(kanteWithOneSegment.getZustaendigkeitAttributGruppe().getId(), pageRequest),
			zustaendigkeitAttributGruppeRepository
				.findRevisions(kanteWithMiniSegment.getZustaendigkeitAttributGruppe().getId(), pageRequest));

		checkRevisions(
			geschwindigkeitAttributGruppeRepository
				.findRevisions(kanteWithOneSegment.getGeschwindigkeitAttributGruppe().getId(), pageRequest),
			geschwindigkeitAttributGruppeRepository
				.findRevisions(kanteWithMiniSegment.getGeschwindigkeitAttributGruppe().getId(), pageRequest));
		TestTransaction.end();
	}

	private <T> void checkRevisions(Page<Revision<Long, T>> revisionsUnchanged,
		Page<Revision<Long, T>> revisionsChanged) {
		Iterator<Revision<Long, T>> revisionUnchanged = revisionsUnchanged.iterator();

		assertThat(revisionUnchanged.hasNext()).isTrue();
		RevisionMetadata<Long> revisionMetadata = revisionUnchanged.next().getMetadata();
		assertThat(revisionMetadata.getRevisionType()).isEqualTo(RevisionMetadata.RevisionType.INSERT);
		assertThat(((RevInfo) revisionMetadata.getDelegate()).getAuditingContext())
			.isNotEqualTo(AuditingContext.LINEARE_REFERENZEN_DEFRAGMENTIERUNG_JOB);

		Iterator<Revision<Long, T>> revisionChanged = revisionsChanged.iterator();

		assertThat(revisionChanged.hasNext()).isTrue();
		revisionMetadata = revisionChanged.next().getMetadata();
		assertThat(revisionMetadata.getRevisionType()).isEqualTo(RevisionMetadata.RevisionType.UPDATE);
		assertThat(((RevInfo) revisionMetadata.getDelegate()).getAuditingContext())
			.isEqualTo(AuditingContext.LINEARE_REFERENZEN_DEFRAGMENTIERUNG_JOB);
	}
}
