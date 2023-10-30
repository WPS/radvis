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
import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { ABSTELLANLAGEN } from 'src/app/viewer/abstellanlage/models/abstellanlage.infrastruktur';
import { AbstellanlageService } from 'src/app/viewer/abstellanlage/services/abstellanlage.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { AbstellanlagenStatus } from 'src/app/viewer/abstellanlage/models/abstellanlagen-status';
import { AbstellanlagenQuellSystem } from 'src/app/viewer/abstellanlage/models/abstellanlagen-quell-system';
import { Groessenklasse } from 'src/app/viewer/abstellanlage/models/groessenklasse';
import { Stellplatzart } from 'src/app/viewer/abstellanlage/models/stellplatzart';

@Injectable({
  providedIn: 'root',
})
export class AbstellanlageFilterService extends AbstractInfrastrukturenFilterService<Abstellanlage> {
  constructor(
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    filterQueryParamsService: FilterQueryParamsService,
    private abstellanlageService: AbstellanlageService
  ) {
    super(infrastrukturenSelektionService, ABSTELLANLAGEN, filterQueryParamsService);

    this.init();
  }

  public getInfrastrukturValueForKey(item: Abstellanlage, key: string): string | string[] {
    switch (key) {
      case 'status':
        return AbstellanlagenStatus.getDisplayText(item.status);
      case 'zustaendig':
        return Verwaltungseinheit.getDisplayName(item.zustaendig);
      case 'quellSystem':
        return AbstellanlagenQuellSystem.getDisplayText(item.quellSystem);
      case 'groessenklasse':
        return item.groessenklasse ? Groessenklasse.getDisplayText(item.groessenklasse) : '';
      case 'stellplatzart':
        return Stellplatzart.getDisplayText(item.stellplatzart);
      case 'istBikeAndRide':
        return item[key] ? 'ja' : 'nein';
      default:
        return (Object.entries(item).find(entry => entry[0] === key)?.[1] ?? '').toString();
    }
  }

  public onAbstellanlageDeleted(id: number): void {
    this.onAlleInfrastrukturenChanged(this.alleInfrastrukturen.filter((value: Abstellanlage) => value.id !== id));
  }

  protected getAll(): Promise<Abstellanlage[]> {
    return this.abstellanlageService.getAll();
  }
}
