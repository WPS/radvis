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
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { AnpassungswunschKategorie } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-kategorie';
import { AnpassungswunschListenView } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-listen-view';
import { AnpassungswunschStatus } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-status';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { AnpassungswunschService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';

@Injectable({
  providedIn: 'root',
})
export class AnpassungswunschFilterService extends AbstractInfrastrukturenFilterService<AnpassungswunschListenView> {
  public abgeschlosseneSindAusgeblendet = true;

  constructor(
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    filterQueryParamsService: FilterQueryParamsService,
    private anpassungswunschService: AnpassungswunschService
  ) {
    super(infrastrukturenSelektionService, ANPASSUNGSWUNSCH, filterQueryParamsService);
    this.init();
  }

  public getInfrastrukturValueForKey(item: AnpassungswunschListenView, key: string): string {
    const EMPTY_FIELD_INDICATOR = '';

    switch (key) {
      case 'beschreibung':
        return item.beschreibung ?? EMPTY_FIELD_INDICATOR;
      case 'status':
        return item.status ? AnpassungswunschStatus.displayTextOf(item.status) : '';
      case 'kategorie':
        return item.kategorie ? AnpassungswunschKategorie.displayTextOf(item.kategorie) : '';
      case 'verantwortlicheOrganisation':
        return Verwaltungseinheit.getDisplayName(item.verantwortlicheOrganisation);
      default:
        throw Error(`Key ${key} nicht gefunden`);
    }
  }

  public abgeschlosseneAusblenden(): void {
    this.abgeschlosseneSindAusgeblendet = true;
    this.refetchData();
  }

  public abgeschlosseneEinblenden(): void {
    this.abgeschlosseneSindAusgeblendet = false;
    this.refetchData();
  }

  protected getAll(): Promise<AnpassungswunschListenView[]> {
    return (
      this.anpassungswunschService
        .getAlleAnpassungswuensche(this.abgeschlosseneSindAusgeblendet)
        // Verhindere, dass eine weitere redundante Fehlermeldung kommt, was zu RAD-4681 fuehrte.
        .catch(() => [])
    );
  }
}
