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

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MassnahmenImportAttributeAuswaehlenCommand } from 'src/app/import/massnahmen/models/massnahmen-import-attribute-uebernehmen-command';
import { MassnahmenImportMassnahmenAuswaehlenCommand } from 'src/app/import/massnahmen/models/massnahmen-import-massnahmen-auswaehlen-command';
import { MassnahmenImportNetzbezugAktualisierenCommand } from 'src/app/import/massnahmen/models/massnahmen-import-netzbezug-aktualisieren-command';
import { MassnahmenImportProtokollStats } from 'src/app/import/massnahmen/models/massnahmen-import-protokoll-stats';
import { MassnahmenImportSessionView } from 'src/app/import/massnahmen/models/massnahmen-import-session-view';
import { MassnahmenImportZuordnungAttributfehler } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-attributfehler';
import { MassnahmenImportZuordnungUeberpruefung } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-ueberpruefung';
import { StartMassnahmenImportSessionCommand } from 'src/app/import/massnahmen/models/start-massnahmen-import-session-command';
import { ImportService } from 'src/app/import/services/import.service';

@Injectable({
  providedIn: 'root',
})
export class MassnahmenImportService extends ImportService {
  readonly massnahmenImportApi = '/api/import/massnahmen';

  public getImportSession(): Observable<MassnahmenImportSessionView | null> {
    return this.http.get<MassnahmenImportSessionView | null>(`${this.massnahmenImportApi}/session`);
  }

  public getZuordnungUeberpruefung(): Observable<MassnahmenImportZuordnungUeberpruefung[] | null> {
    return this.http.get<MassnahmenImportZuordnungUeberpruefung[] | null>(
      `${this.massnahmenImportApi}/session/zuordnungen-ueberpruefung`
    );
  }

  public getZuordnungenAttributfehler(): Observable<MassnahmenImportZuordnungAttributfehler[] | null> {
    return this.http.get<MassnahmenImportZuordnungAttributfehler[] | null>(
      `${this.massnahmenImportApi}/session/zuordnungen-attributfehler`
    );
  }

  public createSessionAndStartMassnahmenImport(
    command: StartMassnahmenImportSessionCommand,
    file: File
  ): Observable<void> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http.post<void>(`${this.massnahmenImportApi}/start-import`, formData);
  }

  public attributeAuswaehlen(command: MassnahmenImportAttributeAuswaehlenCommand): Observable<void> {
    return this.http.post<void>(`${this.massnahmenImportApi}/attribute-auswaehlen`, command);
  }

  public netzbezuegeErstellen(): Observable<void> {
    return this.http.post<void>(`${this.massnahmenImportApi}/netzbezuege-erstellen`, null);
  }

  public netzbezugAktualisieren(command: MassnahmenImportNetzbezugAktualisierenCommand): Observable<void> {
    return this.http.post<void>(`${this.massnahmenImportApi}/netzbezug-aktualisieren`, command);
  }

  public massnahmenSpeichern(command: MassnahmenImportMassnahmenAuswaehlenCommand): Observable<void> {
    return this.http.post<void>(`${this.massnahmenImportApi}/save-massnahmen`, command);
  }

  public getProtokollStats(): Observable<MassnahmenImportProtokollStats> {
    return this.http.get<MassnahmenImportProtokollStats>(`${this.massnahmenImportApi}/protokoll-stats`);
  }

  public downloadFehlerprotokoll(): Observable<Blob> {
    return this.http.get<Blob>(`${this.massnahmenImportApi}/download-fehlerprotokoll`, {
      responseType: 'blob' as 'json',
    });
  }
}
