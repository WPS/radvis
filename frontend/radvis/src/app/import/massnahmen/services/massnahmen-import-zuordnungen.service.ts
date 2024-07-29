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
import { BehaviorSubject, Observable } from 'rxjs';
import { MassnahmenImportZuordnungUeberpruefung } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-ueberpruefung';
import { Geojson } from 'src/app/shared/models/geojson-geometrie';
import { Netzbezug } from 'src/app/shared/models/netzbezug';

@Injectable()
export class MassnahmenImportZuordnungenService {
  private zuordnungenSubject = new BehaviorSubject<MassnahmenImportZuordnungUeberpruefung[]>([]);
  private selektierteZuordnungsIdSubject = new BehaviorSubject<number | undefined>(undefined);

  constructor() {}

  public get zuordnungen$(): Observable<MassnahmenImportZuordnungUeberpruefung[]> {
    return this.zuordnungenSubject.asObservable();
  }

  public get selektierteZuordnungsId$(): Observable<number | undefined> {
    return this.selektierteZuordnungsIdSubject.asObservable();
  }

  public get selektierterZuordnungsNetzbezug(): Netzbezug | null {
    return this.selektierteZuordnung?.netzbezug || null;
  }

  public get selektierteZuordnungsOriginalGeometrie(): Geojson | null {
    return this.selektierteZuordnung?.originalGeometrie || null;
  }

  public get selektierteZuordnung(): MassnahmenImportZuordnungUeberpruefung | undefined {
    return this.zuordnungenSubject.value.find(zuordnung => zuordnung.id === this.selektierteZuordnungsIdSubject.value);
  }

  public get zuordnungen(): MassnahmenImportZuordnungUeberpruefung[] {
    return this.zuordnungenSubject.value;
  }

  public get selektierteZuordnungsId(): number | undefined {
    return this.selektierteZuordnungsIdSubject.value;
  }

  public updateZuordnungen(newZuordnungen: MassnahmenImportZuordnungUeberpruefung[] | null): void {
    if (!newZuordnungen) {
      newZuordnungen = [];
    }

    this.zuordnungenSubject.next(newZuordnungen);

    // Wenn eine Zuordnung wegfällt, ist sie auch nicht mehr selektiert. Daher filtern nach selektierter Zuordnung, die es noch gibt.
    if (
      !!this.selektierteZuordnungsId &&
      !newZuordnungen.some(zuordnung => zuordnung.id === this.selektierteZuordnungsId)
    ) {
      this.deselektiereZuordnung();
    }
  }

  public selektiereZuordnung(zuordnungsId: number): void {
    this.selektierteZuordnungsIdSubject.next(zuordnungsId);
  }

  public deselektiereZuordnung(): void {
    this.selektierteZuordnungsIdSubject.next(undefined);
  }
}
