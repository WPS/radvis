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
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { MassnahmeListenView } from 'src/app/viewer/massnahme/models/massnahme-listen-view';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { Massnahmenkategorien } from 'src/app/viewer/massnahme/models/massnahmenkategorien';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';

@Injectable({
  providedIn: 'root',
})
export class MassnahmeFilterService extends AbstractInfrastrukturenFilterService<MassnahmeListenView> {
  public organisation: Verwaltungseinheit | null = null;

  constructor(
    private massnahmeService: MassnahmeService,
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    filterQueryParamsService: FilterQueryParamsService,
    benutzerDetailsService: BenutzerDetailsService
  ) {
    super(infrastrukturenSelektionService, MASSNAHMEN, filterQueryParamsService);
    const benutzerOrganisation = benutzerDetailsService.aktuellerBenutzerOrganisation();

    if (benutzerOrganisation) {
      if (Verwaltungseinheit.isLandesweit(benutzerOrganisation)) {
        this.organisation = null;
      } else {
        this.organisation = benutzerOrganisation;
      }
    }

    this.init();
  }

  public onMassnahmeDeleted(id: number): void {
    this.onAlleInfrastrukturenChanged(this.alleInfrastrukturen.filter((value: MassnahmeListenView) => value.id !== id));
  }

  protected getAll(): Promise<MassnahmeListenView[]> {
    return this.massnahmeService.getAll(this.organisation?.id);
  }

  protected getInfrastrukturValueForKey(item: MassnahmeListenView, key: string): string | string[] {
    if (key === 'massnahmenkategorien' && item.massnahmenkategorien.length > 0) {
      return item.massnahmenkategorien
        .map(
          kat =>
            Massnahmenkategorien.getDisplayTextForMassnahmenKategorie(kat) +
            Massnahmenkategorien.getDisplayTextForOberkategorieVonKategorie(kat) +
            Massnahmenkategorien.getDisplayTextForKategorieArtVonKategorie(kat)
        )
        .join(', ');
    } else {
      return MassnahmeListenView.getDisplayValueForKey(item, key);
    }
  }
}
