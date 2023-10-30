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
import { ChangeSeitenbezugCommand } from 'src/app/editor/kanten/models/change-seitenbezug-command';
import { CreateKanteZwischenKnotenCommand } from 'src/app/editor/kanten/models/create-kante-zwischen-knoten-command';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { SaveFahrtrichtungAttributGruppeCommand } from 'src/app/editor/kanten/models/save-fahrtrichtung-attribut-gruppe-command';
import { SaveFuehrungsformAttributGruppeCommand } from 'src/app/editor/kanten/models/save-fuehrungsform-attribut-gruppe-command';
import { SaveGeschwindigkeitAttributGruppeCommand } from 'src/app/editor/kanten/models/save-geschwindigkeit-attribut-gruppe-command';
import { SaveKanteVerlaufCommand } from 'src/app/editor/kanten/models/save-kante-verlauf-command';
import { SaveKantenAttributGruppeCommand } from 'src/app/editor/kanten/models/save-kanten-attribut-gruppe-command';
import { SaveZustaendigkeitAttributGruppeCommand } from 'src/app/editor/kanten/models/save-zustaendigkeit-attribut-gruppe-command';
import { Knoten } from 'src/app/editor/knoten/models/knoten';
import { SaveKnotenCommand } from 'src/app/editor/knoten/models/save-knoten-command';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import invariant from 'tiny-invariant';

export type FeatureForVerlaufDTO = {
  geometry: LineStringGeojson;
};

@Injectable({
  providedIn: 'root',
})
export class NetzService {
  public static BASE_URL = '/api/netz';

  constructor(private http: HttpClient) {}

  getKnotenForEdit(id: number): Promise<Knoten> {
    invariant(id, 'Id muss gesetzt sein');
    return this.http.get<Knoten>(`${NetzService.BASE_URL}/knoten/${id}/edit`).toPromise();
  }

  saveKnoten(command: SaveKnotenCommand): Promise<Knoten> {
    invariant(command);
    return this.http.post<Knoten>(`${NetzService.BASE_URL}/knoten/save`, command).toPromise();
  }

  updateSeitenbezug(commands: ChangeSeitenbezugCommand[]): Promise<Kante[]> {
    return this.http.post<Kante[]>(`${NetzService.BASE_URL}/kanten/changeSeitenbezug`, commands).toPromise();
  }

  getKanteForEdit(id: number): Promise<Kante> {
    invariant(id, 'Id muss gesetzt sein');
    return this.http.get<Kante>(`${NetzService.BASE_URL}/kante/${id}/edit`).toPromise();
  }

  berechneVerlaufLinks(id: number): Promise<LineStringGeojson> {
    return this.http
      .get<FeatureForVerlaufDTO>(`${NetzService.BASE_URL}/kanten/berechneNebenKante/${id}/LINKS`)
      .toPromise()
      .then(featureLinks => featureLinks.geometry);
  }

  berechneVerlaufRechts(id: number): Promise<LineStringGeojson> {
    return this.http
      .get<FeatureForVerlaufDTO>(`${NetzService.BASE_URL}/kanten/berechneNebenKante/${id}/RECHTS`)
      .toPromise()
      .then(featureRechts => featureRechts.geometry);
  }

  saveKanteAllgemein(commands: SaveKantenAttributGruppeCommand[]): Promise<Kante[]> {
    invariant(commands, 'Command muss gesetzt sein');
    invariant(commands.length > 0, 'Command muss gesetzt sein');
    return this.http.post<Kante[]>(`${NetzService.BASE_URL}/kanten/saveKanteAllgemein`, commands).toPromise();
  }

  saveKanteVerlauf(commands: SaveKanteVerlaufCommand[]): Promise<Kante[]> {
    invariant(commands, 'Command muss gesetzt sein');
    invariant(commands.length > 0, 'Command muss gesetzt sein');
    return this.http.post<Kante[]>(`${NetzService.BASE_URL}/kanten/saveVerlauf`, commands).toPromise();
  }

  saveKanteFuehrungsform(commands: SaveFuehrungsformAttributGruppeCommand[]): Promise<Kante[]> {
    invariant(commands, 'Command muss gesetzt sein');
    return this.http
      .post<Kante[]>(`${NetzService.BASE_URL}/kanten/saveFuehrungsformAttributGruppe`, commands)
      .toPromise();
  }

  saveZustaendigkeitsGruppe(commands: SaveZustaendigkeitAttributGruppeCommand[]): Promise<Kante[]> {
    invariant(commands, 'Command muss gesetzt sein');
    return this.http
      .post<Kante[]>(`${NetzService.BASE_URL}/kanten/saveZustaendigkeitAttributGruppe`, commands)
      .toPromise();
  }

  saveGeschwindigkeitsGruppe(commands: SaveGeschwindigkeitAttributGruppeCommand[]): Promise<Kante[]> {
    invariant(commands, 'Command muss gesetzt sein');
    return this.http
      .post<Kante[]>(`${NetzService.BASE_URL}/kanten/saveGeschwindigkeitAttributGruppe`, commands)
      .toPromise();
  }

  saveFahrtrichtungAttributgruppe(commands: SaveFahrtrichtungAttributGruppeCommand[]): Promise<Kante[]> {
    invariant(commands, 'Command muss gesetzt sein');
    return this.http
      .post<Kante[]>(`${NetzService.BASE_URL}/kanten/saveFahrtrichtungAttributGruppe`, commands)
      .toPromise();
  }

  createKanteZwischenKnoten(command: CreateKanteZwischenKnotenCommand): Promise<Kante> {
    invariant(command, 'Kante muss gesetzt sein');
    return this.http.post<Kante>(`${NetzService.BASE_URL}/kanten/create`, command).toPromise();
  }
}
