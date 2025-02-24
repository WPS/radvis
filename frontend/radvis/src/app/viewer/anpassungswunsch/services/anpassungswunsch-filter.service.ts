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

import { DatePipe } from '@angular/common';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { AnpassungswunschKategorie } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-kategorie';
import { AnpassungswunschListenView } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-listen-view';
import { AnpassungswunschStatus } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-status';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { ErweiterterAnpassungswunschFilter } from 'src/app/viewer/anpassungswunsch/models/erweiterter-anpassungswunsch-filter';
import { AnpassungswunschService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch.service';
import { FahrradrouteFilter } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';

@Injectable({
  providedIn: 'root',
})
export class AnpassungswunschFilterService extends AbstractInfrastrukturenFilterService<AnpassungswunschListenView> {
  private readonly defaultErweiterterFilter = {
    abgeschlosseneAusblenden: true,
    fahrradrouteFilter: null,
  };

  private _erweiterterFilter: ErweiterterAnpassungswunschFilter = this.defaultErweiterterFilter;

  public get erweiterterFilter(): ErweiterterAnpassungswunschFilter {
    return { ...this._erweiterterFilter };
  }

  private erweiterterFilterActive$$: BehaviorSubject<boolean>;

  get erweiterterFilterActive$(): Observable<boolean> {
    return this.erweiterterFilterActive$$.asObservable().pipe(distinctUntilChanged());
  }

  constructor(
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    filterQueryParamsService: FilterQueryParamsService,
    private anpassungswunschService: AnpassungswunschService
  ) {
    super(infrastrukturenSelektionService, ANPASSUNGSWUNSCH, filterQueryParamsService);
    this.erweiterterFilterActive$$ = new BehaviorSubject<boolean>(
      !ErweiterterAnpassungswunschFilter.isEmpty(this.erweiterterFilter)
    );
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
      case 'erstelltAm':
        return this.convertDateToString(item.erstelltAm) ?? EMPTY_FIELD_INDICATOR;
      case 'zuletztGeaendertAm':
        return this.convertDateToString(item.zuletztGeaendertAm) ?? EMPTY_FIELD_INDICATOR;
      default:
        throw Error(`Key ${key} nicht gefunden`);
    }
  }

  private convertDateToString(date: string): string | null {
    return date ? new DatePipe('en-US').transform(new Date(date), 'dd.MM.yy HH:mm')! : null;
  }

  protected getAll(): Promise<AnpassungswunschListenView[]> {
    return (
      this.anpassungswunschService
        .getAlleAnpassungswuensche(
          this.erweiterterFilter.abgeschlosseneAusblenden,
          this.erweiterterFilter.fahrradrouteFilter?.fahrradroutenIds
        )
        // Verhindere, dass eine weitere redundante Fehlermeldung kommt, was zu RAD-4681 fuehrte.
        .catch(() => [])
    );
  }

  public override reset(): void {
    super.reset();
    this.updateErweiterterFilter(this.defaultErweiterterFilter);
  }

  public updateErweiterterFilter(value: ErweiterterAnpassungswunschFilter): void {
    const valueChanged =
      this._erweiterterFilter.abgeschlosseneAusblenden !== value.abgeschlosseneAusblenden ||
      !FahrradrouteFilter.equal(value.fahrradrouteFilter, this._erweiterterFilter.fahrradrouteFilter);

    this._erweiterterFilter = value;
    this.erweiterterFilterActive$$.next(!ErweiterterAnpassungswunschFilter.isEmpty(value));

    if (valueChanged) {
      this.refetchData();
    }
  }
}
