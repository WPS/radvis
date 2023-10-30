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
import { BarriereListenView } from 'src/app/viewer/barriere/models/barriere-listen-view';
import { BARRIEREN } from 'src/app/viewer/barriere/models/barriere.infrastruktur';
import { BarrierenForm } from 'src/app/viewer/barriere/models/barrieren-form';
import { Markierung } from 'src/app/viewer/barriere/models/markierung';
import { Sicherung } from 'src/app/viewer/barriere/models/sicherung';
import { VerbleibendeDurchfahrtsbreite } from 'src/app/viewer/barriere/models/verbleibende-durchfahrtsbreite';
import { BarrierenService } from 'src/app/viewer/barriere/services/barrieren.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';

@Injectable({
  providedIn: 'root',
})
export class BarriereFilterService extends AbstractInfrastrukturenFilterService<BarriereListenView> {
  constructor(
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    filterQueryParamsService: FilterQueryParamsService,
    private barrierenService: BarrierenService
  ) {
    super(infrastrukturenSelektionService, BARRIEREN, filterQueryParamsService);
    this.init();
  }

  public getInfrastrukturValueForKey(item: BarriereListenView, key: string): string {
    const EMPTY_FIELD_INDICATOR = '';
    switch (key) {
      case 'id':
        const id = item.id;
        return id ? `${id}` : EMPTY_FIELD_INDICATOR;
      case 'verantwortlich':
        return Verwaltungseinheit.getDisplayName(item.verantwortlich) ?? EMPTY_FIELD_INDICATOR;
      case 'barriereform':
        return BarrierenForm.getDisplayTextForBarrierenForm(item.barrierenForm) ?? EMPTY_FIELD_INDICATOR;
      case 'durchfahrtsbreite':
        return item.verbleibendeDurchfahrtsbreite
          ? VerbleibendeDurchfahrtsbreite.getDisplayText(item.verbleibendeDurchfahrtsbreite)
          : EMPTY_FIELD_INDICATOR;
      case 'sicherung':
        return item.sicherung ? Sicherung.getDisplayText(item.sicherung) : EMPTY_FIELD_INDICATOR;
      case 'markierung':
        return item.markierung ? Markierung.getDisplayText(item.markierung) : EMPTY_FIELD_INDICATOR;
      default:
        throw Error(`Key ${key} nicht gefunden`);
    }
  }

  protected getAll(): Promise<BarriereListenView[]> {
    return this.barrierenService.getAlleBarrieren();
  }
}
