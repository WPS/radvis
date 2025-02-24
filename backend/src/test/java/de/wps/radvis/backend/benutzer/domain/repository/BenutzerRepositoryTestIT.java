package de.wps.radvis.backend.benutzer.domain.repository;

import static de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider.benutzerDBListViewComparator;
import static de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider.getDbListView;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerDBListView;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportRepository;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@Tag("group4")
@ContextConfiguration(classes = {
	BenutzerConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
})
@EnableConfigurationProperties(value = {
	TechnischerBenutzerConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	CommonConfigurationProperties.class,
})
class BenutzerRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	private BenutzerRepository benutzerRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	OrganisationRepository organisationenRepository;

	@MockitoBean
	VerwaltungseinheitImportRepository verwaltungseinheitImportRepository;

	@Test
	public void testFindAllDBListViews() {
		// arrange
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		Benutzer admin = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Benutzer kreiskoordinator = benutzerRepository.save(
			BenutzerTestDataProvider.kreiskoordinator(gebietskoerperschaft).build());

		// act
		List<BenutzerDBListView> dbListViews = benutzerRepository.findAllDBListViews();

		// assert
		assertThat(dbListViews)
			.usingElementComparator(benutzerDBListViewComparator)
			.containsExactlyInAnyOrder(getDbListView(admin), getDbListView(kreiskoordinator));
	}

	@Test
	public void testFindAllDBListViewsInVerwaltungseinheitWithId() {
		// arrange
		Gebietskoerperschaft obereGebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.build());

		Gebietskoerperschaft mittlereGebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.uebergeordneteOrganisation(obereGebietskoerperschaft)
				.build());

		Benutzer adminAufObersterStufe = benutzerRepository.save(
			BenutzerTestDataProvider.admin(obereGebietskoerperschaft).build());
		Benutzer benutzerAuObersterStufe = benutzerRepository.save(
			BenutzerTestDataProvider.defaultBenutzer().organisation(obereGebietskoerperschaft).build());
		Benutzer kreiskoordinatorAufMittlererStufe = benutzerRepository.save(
			BenutzerTestDataProvider.kreiskoordinator(mittlereGebietskoerperschaft).build());

		// act
		List<BenutzerDBListView> dbListViewsObereGK = benutzerRepository.findAllDBListViewsInVerwaltungseinheitWithId(
			obereGebietskoerperschaft.getId());

		List<BenutzerDBListView> dbListViewsMittlereGK = benutzerRepository
			.findAllDBListViewsInVerwaltungseinheitWithId(
				mittlereGebietskoerperschaft.getId());

		// assert
		assertThat(dbListViewsObereGK)
			.usingElementComparator(benutzerDBListViewComparator)
			.containsExactlyInAnyOrder(getDbListView(adminAufObersterStufe),
				getDbListView(benutzerAuObersterStufe));

		assertThat(dbListViewsMittlereGK)
			.usingElementComparator(benutzerDBListViewComparator)
			.containsExactlyInAnyOrder(getDbListView(kreiskoordinatorAufMittlererStufe));
	}

	@Test
	public void findByStatusAndRollenIsNotContainingAndLetzteAktivitaetBefore() {
		// arrange
		LocalDate grenzDatum = LocalDate.now();
		int sbwid = 0;

		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Benutzer benutzerMitStatusAktivZuLangeNichtEingeloggt = benutzerRepository.save(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid" + ++sbwid))
			.status(BenutzerStatus.AKTIV)
			.letzteAktivitaet(LocalDate.now().minusDays(1))
			.build());

		Benutzer benutzerMitStatusAktivUndRechtzeitigEingeloggt = benutzerRepository.save(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid" + ++sbwid))
			.status(BenutzerStatus.AKTIV)
			.letzteAktivitaet(LocalDate.now())
			.build());

		Benutzer benutzerMitStatusInaktiv = benutzerRepository.save(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid" + ++sbwid))
			.status(BenutzerStatus.INAKTIV)
			.build());

		Benutzer benutzerMitStatusWarteAufFreischaltung = benutzerRepository.save(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid" + ++sbwid))
			.status(BenutzerStatus.WARTE_AUF_FREISCHALTUNG)
			.build());

		Benutzer benutzerMitStatusAbgelehnt = benutzerRepository.save(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid" + ++sbwid))
			.status(BenutzerStatus.ABGELEHNT)
			.build());

		Benutzer admin = benutzerRepository.save(BenutzerTestDataProvider
			.admin(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid" + ++sbwid))
			.status(BenutzerStatus.AKTIV)
			.rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR))
			.letzteAktivitaet(LocalDate.now().minusDays(1)) // sollte trotzdem ignoriert werden!
			.build());

		Benutzer adminMitMehrfachrolle = benutzerRepository.save(BenutzerTestDataProvider
			.admin(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid" + ++sbwid))
			.status(BenutzerStatus.AKTIV)
			.rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR, Rolle.RADWEGE_ERFASSERIN))
			.letzteAktivitaet(LocalDate.now().minusDays(1)) // sollte trotzdem ignoriert werden!
			.build());

		// act
		List<Benutzer> result = benutzerRepository.findByStatusAndRollenIsNotContainingAndLetzteAktivitaetBefore(
			BenutzerStatus.AKTIV,
			Rolle.RADVIS_ADMINISTRATOR, grenzDatum);

		// assert
		assertThat(result).contains(benutzerMitStatusAktivZuLangeNichtEingeloggt);
		assertThat(result).doesNotContain(benutzerMitStatusAktivUndRechtzeitigEingeloggt);
		assertThat(result).doesNotContain(benutzerMitStatusInaktiv);
		assertThat(result).doesNotContain(benutzerMitStatusWarteAufFreischaltung);
		assertThat(result).doesNotContain(benutzerMitStatusAbgelehnt);
		assertThat(result).doesNotContain(admin);
		assertThat(result).doesNotContain(adminMitMehrfachrolle);
	}

	@Test
	public void testFindByRollenAndStatus() {
		// arrange
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Benutzer aktiverBenutzer = benutzerRepository.save(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid1"))
			.build());

		Benutzer inaktiverBenutzer = benutzerRepository.save(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid2"))
			.status(BenutzerStatus.INAKTIV)
			.build());

		Benutzer wartenderBenutzer = benutzerRepository.save(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid3"))
			.status(BenutzerStatus.WARTE_AUF_FREISCHALTUNG)
			.build());

		Benutzer benutzerMitFalscherRolle = benutzerRepository.save(BenutzerTestDataProvider
			.admin(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid4"))
			.rollen(Set.of(Rolle.KREISKOORDINATOREN))
			.build());

		Benutzer benutzerMitMehrfachrolle = benutzerRepository.save(BenutzerTestDataProvider
			.admin(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid5"))
			.rollen(Set.of(Rolle.KREISKOORDINATOREN, Rolle.RADWEGE_ERFASSERIN))
			.build());

		// act
		List<Benutzer> result = benutzerRepository.findByRollenAndStatus(Rolle.RADWEGE_ERFASSERIN,
			BenutzerStatus.AKTIV);

		// assert
		assertThat(result).contains(aktiverBenutzer);
		assertThat(result).doesNotContain(inaktiverBenutzer);
		assertThat(result).doesNotContain(wartenderBenutzer);
		assertThat(result).doesNotContain(benutzerMitFalscherRolle);
		assertThat(result).contains(benutzerMitMehrfachrolle);
	}

	@Test
	public void testFindByOrganisationAndRollen() {
		// arrange
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Organisation organisation = organisationenRepository.save(
			VerwaltungseinheitTestDataProvider.defaultOrganisation().build());

		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid1"))
			.build());

		Benutzer benutzerFalscheVerwaltungseinheit = benutzerRepository.save(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(organisation)
			.serviceBwId(ServiceBwId.of("sbwid2"))
			.build());

		Benutzer benutzerMitFalscherRolle = benutzerRepository.save(BenutzerTestDataProvider
			.admin(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid3"))
			.rollen(Set.of(Rolle.KREISKOORDINATOREN))
			.build());

		Benutzer BenutzerMitMehrfachrolle = benutzerRepository.save(BenutzerTestDataProvider
			.admin(gebietskoerperschaft)
			.serviceBwId(ServiceBwId.of("sbwid4"))
			.rollen(Set.of(Rolle.KREISKOORDINATOREN, Rolle.RADWEGE_ERFASSERIN))
			.build());

		// act
		List<Benutzer> result = benutzerRepository.findByOrganisationAndRollen(gebietskoerperschaft,
			Rolle.RADWEGE_ERFASSERIN);

		// assert
		assertThat(result).contains(benutzer);
		assertThat(result).doesNotContain(benutzerFalscheVerwaltungseinheit);
		assertThat(result).doesNotContain(benutzerMitFalscherRolle);
		assertThat(result).contains(BenutzerMitMehrfachrolle);
	}
}