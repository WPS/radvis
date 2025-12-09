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

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { BenutzerStatus } from 'src/app/administration/models/benutzer-status';
import { BenutzerDetails } from 'src/app/shared/models/benutzer-details';
import { Recht } from 'src/app/shared/models/recht';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';

@Injectable({
  providedIn: 'root',
})
export class BenutzerDetailsService {
  private benutzer!: BenutzerDetails;

  constructor(private http: HttpClient) {}

  fetchBenutzerDetails(): Promise<void> {
    return this.http
      .get<BenutzerDetails>('/api/benutzerdetails')
      .toPromise()
      .then(benutzerDetails => {
        this.benutzer = benutzerDetails;
      });
  }

  beantrageReaktivierung(): Observable<BenutzerDetails> {
    return this.http.post<BenutzerDetails>('/api/benutzer/reaktivierung/beantrage-reaktivierung', {}).pipe(
      tap(benutzerDetails => {
        this.benutzer = benutzerDetails;
      })
    );
  }

  istAktuellerBenutzerRegistriert(): boolean {
    return this.benutzer.registriert;
  }

  istAktuellerBenutzerAktiv(): boolean {
    return this.benutzer.status === BenutzerStatus.AKTIV;
  }

  istAktuellerBenutzerOrgaUndNutzerVerwalter(): boolean {
    const verwalterrechte = [
      Recht.ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN,
      Recht.BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN,
    ];
    return this.benutzer.rechte?.some(r => verwalterrechte.includes(r)) || false;
  }

  canRoutingprofileBearbeiten(): boolean {
    return this.benutzer.rechte?.includes(Recht.ROUTINGPROFILE_VERWALTEN) ?? false;
  }

  istAktuellerBenutzerRadNETZQualitaetsSicherInOrAdmin(): boolean {
    const radNETZQualitaetsSicherInRechte = [
      Recht.MANUELLES_MATCHING_ZUORDNEN_UND_BEARBEITEN,
      Recht.ANPASSUNGSWUENSCHE_BEARBEITEN,
    ];
    return (
      this.benutzer.rechte != null &&
      radNETZQualitaetsSicherInRechte.every(r => this.benutzer.rechte?.includes(r)) &&
      !this.benutzer.rechte?.includes(Recht.JOBS_AUSFUEHREN)
    );
  }

  canEditGesamtesNetz(): boolean {
    return this.benutzer.rechte?.includes(Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN) ?? false;
  }

  canKreisnetzVerlegen(): boolean {
    return this.benutzer.rechte?.includes(Recht.KREISNETZ_ROUTENVERLEGUNGEN) ?? false;
  }

  canMassnahmenStornieren(): boolean {
    return this.benutzer.rechte?.includes(Recht.MASSNAHMEN_STORNIEREN) ?? false;
  }

  canMassnahmenArchivieren(): boolean {
    return this.benutzer.rechte?.includes(Recht.MASSNAHMEN_ARCHIVIEREN) ?? false;
  }

  canEdit(): boolean {
    const editRechte = [
      Recht.RADNETZ_ROUTENVERLEGUNGEN,
      Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT,
      Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN,
      Recht.KREISNETZ_ROUTENVERLEGUNGEN,
    ];
    return this.benutzer.rechte?.some(r => editRechte.includes(r)) || false;
  }

  canRadNetzVerlegen(): boolean {
    return this.benutzer.rechte?.includes(Recht.RADNETZ_ROUTENVERLEGUNGEN) ?? false;
  }

  canEditZustaendigkeitsBereichOfOrganisation(): boolean {
    return this.benutzer.rechte?.includes(Recht.EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN) ?? false;
  }

  canCreateMassnahmen(): boolean {
    const massnahmenRechte = [
      Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN,
      Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
    ];
    return this.benutzer.rechte?.some(r => massnahmenRechte.includes(r)) || false;
  }

  canCreateFahrradrouten(): boolean {
    const fahrradroutenRechte = [
      Recht.ALLE_RADROUTEN_ERFASSEN_BEARBEITEN,
      Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
    ];
    return this.benutzer.rechte?.some(r => fahrradroutenRechte.includes(r)) || false;
  }

  canCreateAnpassungswunsch(): boolean {
    const anpassungswunschRechte = [Recht.ANPASSUNGSWUENSCHE_ERFASSEN];
    return this.benutzer.rechte?.some(r => anpassungswunschRechte.includes(r)) || false;
  }

  canCreateOrganisationen(): boolean {
    const organisationRechte = [
      Recht.ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN,
      Recht.BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN,
    ];
    return this.benutzer.rechte?.some(r => organisationRechte.includes(r)) || false;
  }

  canStartUmsetzungsstandsabfragen(): boolean {
    return this.benutzer.rechte?.includes(Recht.UMSETZUNGSSTANDSABFRAGEN_STARTEN) || false;
  }

  canEvaluateUmsetzungsstandsabfragen(): boolean {
    return this.benutzer.rechte?.includes(Recht.UMSETZUNGSSTANDSABFRAGEN_AUSWERTEN) || false;
  }

  canCreateFurtenKreuzungen(): boolean {
    const furtKreuzungRechte = [Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN];
    return this.benutzer.rechte?.some(r => furtKreuzungRechte.includes(r)) || false;
  }

  canBenutzerImport(): boolean {
    const importRechte = [
      Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT,
      Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN,
      Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN,
      Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
    ];
    return this.benutzer.rechte?.some(r => importRechte.includes(r)) || false;
  }

  canBenutzerImportNetzklassenAndAttribute(): boolean {
    const importRechte = [
      Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN,
      Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT,
    ];
    return this.benutzer.rechte?.some(r => importRechte.includes(r)) || false;
  }

  canBenutzerImportMassnahmenAndDateianhaenge(): boolean {
    const importRechte = [
      Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN,
      Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
    ];
    return this.benutzer.rechte?.some(r => importRechte.includes(r)) || false;
  }

  canDateiLayerVerwalten(): boolean {
    return this.benutzer.rechte?.includes(Recht.DATEI_LAYER_VERWALTEN) ?? false;
  }

  canLayerAlsDefaultFestlegen(): boolean {
    return this.benutzer.rechte?.includes(Recht.DATEI_LAYER_VERWALTEN) ?? false;
  }

  aktuellerBenutzerVorname(): string | undefined {
    return this.benutzer.vorname;
  }

  aktuellerBenutzerNachname(): string | undefined {
    return this.benutzer.name;
  }

  aktuellerBenutzerBasicAuthAnmeldename(): string | undefined {
    return this.benutzer.basicAuthAnmeldename;
  }

  aktuellerBenutzerOrganisationName(): string | undefined {
    return this.benutzer.organisation?.name;
  }

  aktuellerBenutzerOrganisation(): Verwaltungseinheit | undefined {
    return this.benutzer.organisation;
  }

  aktuellerBenutzerStatus(): BenutzerStatus {
    return this.benutzer.status;
  }
}
