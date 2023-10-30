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

import { Inject, Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { AbstractQueryParams } from 'src/app/shared/models/abstract-query-params';
import { AbstractQueryParamsService } from 'src/app/shared/services/abstract-query-params.service';
import { Infrastruktur, InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { InfrastrukturenQueryParams } from 'src/app/viewer/viewer-shared/models/infrastruktur-query-params';

@Injectable({
  providedIn: 'root',
})
export class InfrastrukturenSelektionService extends AbstractQueryParamsService<InfrastrukturenQueryParams> {
  selektierteInfrastrukturen$: Observable<Infrastruktur[]>;
  tabellenVisible$: Observable<boolean>;

  constructor(
    private activatedRoute: ActivatedRoute,
    router: Router,
    @Inject(InfrastrukturToken) infrastrukturen: Infrastruktur[]
  ) {
    super(router);
    this.selektierteInfrastrukturen$ = this.activatedRoute.queryParams.pipe(
      map(params => params.infrastrukturen),
      distinctUntilChanged(),
      map(param => {
        const includedPathElements = InfrastrukturenQueryParams.paramToInfrastrukturen(param);
        return infrastrukturen.filter(i => includedPathElements.includes(i.pathElement));
      })
    );

    this.tabellenVisible$ = this.activatedRoute.queryParams.pipe(
      map(params => params.tabellenVisible),
      distinctUntilChanged(),
      map(param => AbstractQueryParams.paramToBoolean(param) ?? false)
    );
  }

  public selectInfrastrukturen(infrastrukturen: Infrastruktur): void {
    if (!this.isSelected(infrastrukturen)) {
      // ACHTUNG! Hier können wir in race-conditions laufen, z.B. wenn die Methode zweimal direkt hintereinander aufgerufen wird.
      // Der QueryParams-snapshot erhält bei zweiten Aufruf den alten Wert (da die navigation noch nicht abgeschlossen ist) und
      // wir haben ein Lost-update. In den Tests werden aufeinanderfolgende Aufruf zur Vermeidung in setTimeout gewrappt.
      this.updateQueryParams({
        infrastrukturen: [...this.infrastrukturenQueryParamsSnapshot.infrastrukturen, infrastrukturen.pathElement],
        tabellenVisible: true,
      });
    }
  }

  public deselectInfrastrukturen(infrastrukturen: Infrastruktur): void {
    if (this.isSelected(infrastrukturen)) {
      this.updateQueryParams({
        infrastrukturen: this.infrastrukturenQueryParamsSnapshot.infrastrukturen.filter(
          selektion => selektion !== infrastrukturen.pathElement
        ),
      });
    }
  }

  public isSelected(infrastrukturen: Infrastruktur): boolean {
    return this.infrastrukturenQueryParamsSnapshot.infrastrukturen.includes(infrastrukturen.pathElement);
  }

  public toggleTabellenVisible(): void {
    this.updateQueryParams({
      tabellenVisible: !this.infrastrukturenQueryParamsSnapshot.tabellenVisible,
    });
  }

  public showTabelle(): void {
    this.updateQueryParams({
      tabellenVisible: true,
    });
  }

  private get infrastrukturenQueryParamsSnapshot(): InfrastrukturenQueryParams {
    return InfrastrukturenQueryParams.fromRoute(this.activatedRoute.snapshot.queryParams);
  }

  private updateQueryParams(opts: { infrastrukturen?: string[]; tabellenVisible?: boolean }): void {
    const queryParams = InfrastrukturenQueryParams.merge(opts, this.infrastrukturenQueryParamsSnapshot);
    this.updateInUrl(queryParams);
  }
}
