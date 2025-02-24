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

import { ChangeDetectionStrategy, Component, HostListener, OnDestroy } from '@angular/core';
import { ActivatedRoute, IsActiveMatchOptions } from '@angular/router';
import { Subscription } from 'rxjs';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import invariant from 'tiny-invariant';
import { FahrradzaehlstelleRoutingService } from 'src/app/viewer/fahrradzaehlstelle/services/fahrradzaehlstelle-routing.service';
import { FAHRRADZAEHLSTELLE } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle.infrastruktur';
import { FahrradzaehlstelleDetailView } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle-detail-view';
import { OlMapService } from 'src/app/shared/services/ol-map.service';

@Component({
  selector: 'rad-fahrradzaehlstelle-tool',
  templateUrl: './fahrradzaehlstelle-tool.component.html',
  styleUrls: ['./fahrradzaehlstelle-tool.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class FahrradzaehlstelleToolComponent implements OnDestroy {
  eigenschaftenRoute = './' + FahrradzaehlstelleRoutingService.EIGENSCHAFTEN;
  statistikRoute = './' + FahrradzaehlstelleRoutingService.STATISTIK;

  routerLinkActiveOptions: IsActiveMatchOptions = {
    fragment: 'exact',
    matrixParams: 'exact',
    paths: 'exact',
    queryParams: 'ignored',
  };

  fahrradzaehlstelleDetailView: FahrradzaehlstelleDetailView | null = null;

  private subscriptions: Subscription[] = [];

  constructor(
    private activatedRoute: ActivatedRoute,
    private viewerRoutingService: ViewerRoutingService,
    private fahrradzaehlstelleRoutingService: FahrradzaehlstelleRoutingService,
    private infrastrukturenSelektionService: InfrastrukturenSelektionService,
    private olMapService: OlMapService
  ) {
    this.subscriptions.push(
      activatedRoute.data.subscribe(data => {
        this.fahrradzaehlstelleDetailView = data.fahrradzaehlstelleDetailView;
        invariant(this.fahrradzaehlstelleDetailView);
        this.focusFahrradzaehlstelleIntoView();
      })
    );
    this.infrastrukturenSelektionService.selectInfrastrukturen(FAHRRADZAEHLSTELLE);
  }

  @HostListener('keydown.escape')
  public onEscape(): void {
    this.onClose();
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  public focusFahrradzaehlstelleIntoView(): void {
    const toFocus = this.fahrradzaehlstelleDetailView?.geometrie.coordinates;

    if (toFocus) {
      this.olMapService.scrollIntoViewByCoordinate(toFocus);
    }
  }
}
