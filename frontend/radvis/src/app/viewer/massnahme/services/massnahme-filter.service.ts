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

import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';
import { ErweiterterMassnahmenFilter } from 'src/app/viewer/massnahme/models/erweiterter-massnahmen-filter';
import { FahrradrouteFilter } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter';

@Injectable({
  providedIn: 'root',
})
export class MassnahmeFilterService extends AbstractInfrastrukturenFilterService<MassnahmeListenView> {
  private readonly defaultErweiterterFilter: ErweiterterMassnahmenFilter;

  private _erweiterterFilter: ErweiterterMassnahmenFilter;
  public get erweiterterFilter(): ErweiterterMassnahmenFilter {
    return { ...this._erweiterterFilter };
  }
  private erweiterterFilterAktiv$$: BehaviorSubject<boolean>;
  get erweiterterFilterAktiv$(): Observable<boolean> {
    return this.erweiterterFilterAktiv$$.asObservable().pipe(distinctUntilChanged());
  }

  constructor(
    private massnahmeService: MassnahmeService,
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    filterQueryParamsService: FilterQueryParamsService,
    benutzerDetailsService: BenutzerDetailsService
  ) {
    super(infrastrukturenSelektionService, MASSNAHMEN, filterQueryParamsService);
    const benutzerOrganisation = benutzerDetailsService.aktuellerBenutzerOrganisation();

    let erweiterterFilterDefaultOrganisation: Verwaltungseinheit | null = null;
    if (benutzerOrganisation && !Verwaltungseinheit.isLandesOderBundesweit(benutzerOrganisation)) {
      erweiterterFilterDefaultOrganisation = benutzerOrganisation;
    }

    this.defaultErweiterterFilter = {
      historischeMassnahmenAnzeigen: false,
      fahrradrouteFilter: null,
      organisation: erweiterterFilterDefaultOrganisation,
    };
    this._erweiterterFilter = this.defaultErweiterterFilter;

    this.erweiterterFilterAktiv$$ = new BehaviorSubject<boolean>(
      !ErweiterterMassnahmenFilter.isEmpty(this.defaultErweiterterFilter)
    );

    this.init();
  }

  public override reset(): void {
    super.reset();

    this.updateErweiterterFilter(this.defaultErweiterterFilter);
  }

  public onMassnahmeDeleted(id: number): void {
    this.onAlleInfrastrukturenChanged(this.alleInfrastrukturen.filter((value: MassnahmeListenView) => value.id !== id));
  }

  public updateErweiterterFilter(value: ErweiterterMassnahmenFilter): void {
    const valueChanged =
      this._erweiterterFilter.historischeMassnahmenAnzeigen !== value.historischeMassnahmenAnzeigen ||
      this._erweiterterFilter.organisation?.id !== value.organisation?.id ||
      !FahrradrouteFilter.equal(this.erweiterterFilter.fahrradrouteFilter, value.fahrradrouteFilter);

    this._erweiterterFilter = value;
    this.erweiterterFilterAktiv$$.next(!ErweiterterMassnahmenFilter.isEmpty(value));

    if (valueChanged) {
      this.refetchData();
    }
  }

  protected getAll(): Promise<MassnahmeListenView[]> {
    return this.massnahmeService.getAll(this.erweiterterFilter);
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
