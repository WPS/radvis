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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.LineareReferenzenDefragmentierungJobStatistik;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.repository.FuehrungsformAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.GeschwindigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.ZustaendigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LineareReferenzenDefragmentierungJob extends AbstractJob {
	private final ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppeRepository;

	private final KantenRepository kantenRepository;

	private final FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppeRepository;

	private final GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository;

	private final Laenge minimaleSegmentLaenge;

	public LineareReferenzenDefragmentierungJob(JobExecutionDescriptionRepository repository,
		ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppeRepository,
		FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppeRepository,
		GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository,
		KantenRepository kantenRepository, Laenge minimaleSegmentLaenge) {
		super(repository);
		this.zustaendigkeitAttributGruppeRepository = zustaendigkeitAttributGruppeRepository;
		this.fuehrungsformAttributGruppeRepository = fuehrungsformAttributGruppeRepository;
		this.geschwindigkeitAttributGruppeRepository = geschwindigkeitAttributGruppeRepository;
		this.kantenRepository = kantenRepository;
		this.minimaleSegmentLaenge = minimaleSegmentLaenge;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.LINEARE_REFERENZEN_DEFRAGMENTIERUNG_JOB)
	public JobExecutionDescription run() {
		return super.run(false);
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.LINEARE_REFERENZEN_DEFRAGMENTIERUNG_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		HashSet<Kante> kantenWithSegmenteKleinerAls = new HashSet<Kante>();
		LineareReferenzenDefragmentierungJobStatistik statistik = new LineareReferenzenDefragmentierungJobStatistik();

		List<ZustaendigkeitAttributGruppe> zustaendigkeitAttributGruppenWithSegmenteKleinerAls = zustaendigkeitAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(minimaleSegmentLaenge.getValue());
		statistik.anzahlZustaendigkeitAttributGruppen += zustaendigkeitAttributGruppenWithSegmenteKleinerAls.size();

		kantenWithSegmenteKleinerAls.addAll(kantenRepository
			.findAllByZustaendigkeitAttributGruppeIn(zustaendigkeitAttributGruppenWithSegmenteKleinerAls));

		List<FuehrungsformAttributGruppe> fuehrungsformAttributGruppenWithSegmenteKleinerAls = fuehrungsformAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(minimaleSegmentLaenge.getValue());
		statistik.anzahlFuehrungsformAttributGruppen += fuehrungsformAttributGruppenWithSegmenteKleinerAls.size();

		kantenWithSegmenteKleinerAls.addAll(kantenRepository
			.findAllByFuehrungsformAttributGruppeIn(fuehrungsformAttributGruppenWithSegmenteKleinerAls));

		List<GeschwindigkeitAttributGruppe> geschwindigkeitAttributGruppenWithSegmenteKleinerAls = geschwindigkeitAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(minimaleSegmentLaenge.getValue());
		statistik.anzahlGeschwindigkeitAttributGruppen += geschwindigkeitAttributGruppenWithSegmenteKleinerAls.size();

		kantenWithSegmenteKleinerAls.addAll(kantenRepository
			.findAllByGeschwindigkeitAttributeGruppeIn(geschwindigkeitAttributGruppenWithSegmenteKleinerAls));

		statistik.anzahlBetrachteteKanten += kantenWithSegmenteKleinerAls.size();

		kantenWithSegmenteKleinerAls.forEach(k -> {
			int anzahlZustaendigkeitAttributeVorher = k.getZustaendigkeitAttributGruppe()
				.getImmutableZustaendigkeitAttribute().size();
			int anzahlGeschwindigkeitAttributeVorher = k.getGeschwindigkeitAttributGruppe()
				.getGeschwindigkeitAttribute().size();
			int anzahlFuehrungsformAttributeVorher = k.getFuehrungsformAttributGruppe()
				.getImmutableFuehrungsformAttributeLinks().size()
				+ k.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().size();

			if (k.getLaengeBerechnet().getValue() <= minimaleSegmentLaenge.getValue()) {
				statistik.anzahlKantenKleinerAlsMinimaleSegmentLaenge += 1;
			}
			k.mergeSegmenteKleinerAls(minimaleSegmentLaenge);

			int anzahlZustaendigkeitAttributeNachher = k.getZustaendigkeitAttributGruppe()
				.getImmutableZustaendigkeitAttribute().size();
			int anzahlGeschwindigkeitAttributeNachher = k.getGeschwindigkeitAttributGruppe()
				.getGeschwindigkeitAttribute().size();
			int anzahlFuehrungsformAttributeNachher = k.getFuehrungsformAttributGruppe()
				.getImmutableFuehrungsformAttributeLinks().size()
				+ k.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().size();

			statistik.anzahlEntfernterSegmente += anzahlFuehrungsformAttributeVorher
				- anzahlFuehrungsformAttributeNachher;
			statistik.anzahlEntfernterSegmente += anzahlGeschwindigkeitAttributeVorher
				- anzahlGeschwindigkeitAttributeNachher;
			statistik.anzahlEntfernterSegmente += anzahlZustaendigkeitAttributeVorher
				- anzahlZustaendigkeitAttributeNachher;
		});

		kantenRepository.saveAll(kantenWithSegmenteKleinerAls);

		log.info("JobStatistik:\n{}", statistik.toPrettyJSON());

		return Optional.of(statistik);
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Fasst zu kleine Segmente auf Kanten zusammen, sodass möglichst alle Kantensegmente eine gewisse Mindestlänge erfüllen.",
			"Kantensegmente werden zusammengefasst, wodurch alle Attributgruppen angepasst werden können. Es ist möglich, dass hierbei Attribute wegfallen, wenn diese sich auf benachbarten Segmenten unterschieden haben.",
			"Sollte nach netzverändernden Jobs laufen, insb. nach dem DLM-Reimport.",
			JobExecutionDurationEstimate.SHORT
		);
	}
}
