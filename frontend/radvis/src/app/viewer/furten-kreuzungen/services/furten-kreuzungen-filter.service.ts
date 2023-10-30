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
import { Knotenformen } from 'src/app/shared/models/knotenformen';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { FurtKreuzungListenView } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-listen-view';
import { FurtKreuzungTyp } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-typ';
import { FURTEN_KREUZUNGEN } from 'src/app/viewer/furten-kreuzungen/models/furten-kreuzungen.infrastruktur';
import { FurtenKreuzungenService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';

@Injectable({
  providedIn: 'root',
})
export class FurtenKreuzungenFilterService extends AbstractInfrastrukturenFilterService<FurtKreuzungListenView> {
  constructor(
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    filterQueryParamsService: FilterQueryParamsService,
    private furtenKreuzungenService: FurtenKreuzungenService
  ) {
    super(infrastrukturenSelektionService, FURTEN_KREUZUNGEN, filterQueryParamsService);
    this.init();
  }

  public getInfrastrukturValueForKey(item: FurtKreuzungListenView, key: string): string {
    const EMPTY_FIELD_INDICATOR = '';
    switch (key) {
      case 'id':
        const id = item.id;
        return id ? `${id}` : EMPTY_FIELD_INDICATOR;
      case 'verantwortlich':
        return Verwaltungseinheit.getDisplayName(item.verantwortlich) ?? EMPTY_FIELD_INDICATOR;
      case 'typ':
        return FurtKreuzungTyp.displayTextOf(item.typ);
      case 'radnetzKonform':
        return item.radnetzKonform ? 'ja' : 'nein';
      case 'kommentar':
        return item.kommentar ?? EMPTY_FIELD_INDICATOR;
      case 'knotenForm':
        return Knotenformen.getDisplayText(item.knotenForm);
      default:
        throw Error(`Key ${key} nicht gefunden`);
    }
  }

  protected getAll(): Promise<FurtKreuzungListenView[]> {
    return this.furtenKreuzungenService.getAlleFurtenKreuzungen();
  }
}
