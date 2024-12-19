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
import { AbstractQueryParamsService } from 'src/app/shared/services/abstract-query-params.service';
import { Infrastruktur, InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { InfrastrukturenQueryParams } from 'src/app/viewer/viewer-shared/models/infrastruktur-query-params';
import { MatomoTracker } from 'ngx-matomo-client';

@Injectable({
  providedIn: 'root',
})
export class InfrastrukturenSelektionService extends AbstractQueryParamsService<InfrastrukturenQueryParams> {
  selektierteInfrastrukturen$: Observable<Infrastruktur[]>;

  constructor(
    private activatedRoute: ActivatedRoute,
    router: Router,
    @Inject(InfrastrukturToken) private infrastrukturen: Infrastruktur[],
    private matomoTracker: MatomoTracker
  ) {
    super(router);
    this.convertQueryParamToInfrastrukturen(this.activatedRoute.snapshot.queryParams.infrastrukturen).forEach(
      infrastruktur => {
        this.trackInfrastrukturSelektion(infrastruktur);
      }
    );
    this.selektierteInfrastrukturen$ = this.activatedRoute.queryParams.pipe(
      map(params => params.infrastrukturen),
      distinctUntilChanged(),
      map(param => {
        return this.convertQueryParamToInfrastrukturen(param);
      })
    );
  }

  private convertQueryParamToInfrastrukturen(param: string | undefined): Infrastruktur[] {
    const includedPathElements = InfrastrukturenQueryParams.paramToInfrastrukturen(param);
    return this.infrastrukturen.filter(i => includedPathElements.includes(i.pathElement));
  }

  public get selektierteInfrastrukturen(): Infrastruktur[] {
    return this.convertQueryParamToInfrastrukturen(this.activatedRoute.snapshot.queryParams.infrastrukturen);
  }

  public selectInfrastrukturen(infrastrukturen: Infrastruktur): void {
    if (!this.isSelected(infrastrukturen)) {
      // ACHTUNG! Hier können wir in race-conditions laufen, z.B. wenn die Methode zweimal direkt hintereinander aufgerufen wird.
      // Der QueryParams-snapshot erhält bei zweitem Aufruf den alten Wert (da die navigation noch nicht abgeschlossen ist) und
      // wir haben ein Lost-update. In den Tests werden aufeinanderfolgende Aufruf zur Vermeidung in setTimeout gewrappt.
      this.trackInfrastrukturSelektion(infrastrukturen);
      this.updateQueryParams([...this.infrastrukturenQueryParamsSnapshot.infrastrukturen, infrastrukturen.pathElement]);
    }
  }

  public deselectInfrastrukturen(infrastrukturen: Infrastruktur): void {
    if (this.isSelected(infrastrukturen)) {
      this.updateQueryParams(
        this.infrastrukturenQueryParamsSnapshot.infrastrukturen.filter(
          selektion => selektion !== infrastrukturen.pathElement
        )
      );
    }
  }

  public isSelected(infrastrukturen: Infrastruktur): boolean {
    return this.infrastrukturenQueryParamsSnapshot.infrastrukturen.includes(infrastrukturen.pathElement);
  }

  private get infrastrukturenQueryParamsSnapshot(): InfrastrukturenQueryParams {
    return InfrastrukturenQueryParams.fromRoute(this.activatedRoute.snapshot.queryParams);
  }

  private updateQueryParams(infrastrukturen: string[]): void {
    const queryParams = InfrastrukturenQueryParams.merge({ infrastrukturen }, this.infrastrukturenQueryParamsSnapshot);
    this.updateInUrl(queryParams);
  }

  private trackInfrastrukturSelektion(infrastruktur: Infrastruktur): void {
    this.matomoTracker.trackEvent('Infrastruktur', 'Aufruf', infrastruktur.name);
  }
}
