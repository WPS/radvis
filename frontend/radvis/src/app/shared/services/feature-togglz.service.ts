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

interface FeatureTogglzView {
  toggle: string;
  enabled: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class FeatureTogglzService {
  public static readonly TOGGLZ_FAHRRADROUTE: string = 'FAHRRADROUTE';
  public static readonly TOGGLZ_FAHRRADROUTE_FEHLER_INFOS: string = 'FAHRRADROUTE_FEHLER_INFOS';
  public static readonly TOGGLZ_FEHLERPROTOKOLL: string = 'FEHLERPROTOKOLL';
  public static readonly TOGGLZ_ANPASSUNGEN: string = 'ANPASSUNGEN';
  public static readonly TOGGLZ_LOESCHEN_VON_ANPASSUNGSWUENSCHEN: string = 'LOESCHEN_VON_ANPASSUNGSWUENSCHEN';
  public static readonly TOGGLZ_FAHRRADROUTE_IMPORTPROTOKOLLE = 'FAHRRADROUTE_IMPORTPROTOKOLLE';
  public static readonly TOGGLZ_FURTEN_KREUZUNGEN = 'FURTEN_KREUZUNGEN';
  public static readonly TOGGLZ_BARRIEREN = 'BARRIEREN';
  public static readonly TOGGLZ_KONSISTENZREGELN = 'KONSISTENZREGELN';
  public static readonly TOGGLZ_WEITERE_KARTENEBENEN = 'WEITERE_KARTENEBENEN';
  public static readonly ORGANISATIONEN_ERSTELLEN_UND_BEARBEITEN = 'ORGANISATIONEN_ERSTELLEN_UND_BEARBEITEN';
  public static readonly TOGGLZ_SERVICESTATIONEN = 'SERVICESTATIONEN';
  public static readonly TOGGLZ_LEIHSTATIONEN = 'LEIHSTATIONEN';
  public static readonly TOGGLZ_ABSTELLANLAGEN = 'ABSTELLANLAGEN';
  public static readonly TOGGLZ_WEGWEISENDE_BESCHILDERUNG = 'WEGWEISENDE_BESCHILDERUNG';
  public static readonly TOGGLZ_VORDEFINIERTE_EXPORTE = 'VORDEFINIERTE_EXPORTE';
  public static readonly TOGGLZ_SICHERHEITSTRENNSTREIFEN = 'SICHERHEITSTRENNSTREIFEN';
  public static readonly TOGGLZ_DATEILAYER_HOCHLADEN_ANZEIGEN = 'DATEILAYER_HOCHLADEN_ANZEIGEN';
  public static readonly TOGGLZ_IMPORT_MASSNAHMEN = 'IMPORT_MASSNAHMEN';
  public static readonly TOGGLZ_IMPORT_DATEIANHAENGE_MASSNAHMEN = 'IMPORT_DATEIANHAENGE_MASSNAHMEN';

  public static readonly TOGGLZ_LEIHSTATIONEN_CSV_IMPORT = 'LEIHSTATIONEN_CSV_IMPORT';
  public static readonly TOGGLZ_SERVICESTATIONEN_CSV_IMPORT = 'SERVICESTATIONEN_CSV_IMPORT';
  public static readonly TOGGLZ_ABSTELLANLAGEN_CSV_IMPORT = 'ABSTELLANLAGEN_CSV_IMPORT';
  public static readonly TOGGLZ_BASIC_AUTH_VERWALTEN_ANZEIGEN = 'BASIC_AUTH_VERWALTEN_ANZEIGEN';

  private static readonly URL_TOGGLZ: string = '/api/togglz';

  private featureTogglz: Map<string, boolean> = new Map();

  constructor(private httpClient: HttpClient) {}

  public fetchTogglz(): Promise<void> {
    return this.httpClient
      .get<FeatureTogglzView[]>(FeatureTogglzService.URL_TOGGLZ)
      .toPromise()
      .then((featureTogglzViews: FeatureTogglzView[]) =>
        featureTogglzViews.forEach(togglz => this.featureTogglz.set(togglz.toggle, togglz.enabled))
      );
  }

  isToggledOn(toggleName: string): boolean {
    return this.featureTogglz.get(toggleName) ?? false;
  }

  get fahrradrouteFehlerInfos(): boolean {
    return this.isToggledOn(FeatureTogglzService.TOGGLZ_FAHRRADROUTE_FEHLER_INFOS);
  }

  get fehlerprotokoll(): boolean {
    return this.isToggledOn(FeatureTogglzService.TOGGLZ_FEHLERPROTOKOLL);
  }
}
