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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs/operators';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { FahrradzaehlstelleDetailView } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle-detail-view';

@Component({
  selector: 'rad-fahrradzaehlstelle-detail-view',
  templateUrl: './fahrradzaehlstelle-detail-view.component.html',
  styleUrls: ['./fahrradzaehlstelle-detail-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FahrradzaehlstelleDetailViewComponent {
  public MOBIDATA_DATENSATZ_URL = 'https://www.mobidata-bw.de/dataset/eco-counter-fahrradzahler';
  public attribute: Map<string, { [key: string]: string }> = new Map();
  public leereAttributeVisible = false;

  constructor(
    private viewerRoutingService: ViewerRoutingService,
    activatedRoute: ActivatedRoute,
    changeDetector: ChangeDetectorRef,
    olMapService: OlMapService
  ) {
    activatedRoute.parent?.data
      .pipe(map(data => data.fahrradzaehlstelleDetailView))
      .subscribe((fahrradzaehlstelleDetail: FahrradzaehlstelleDetailView) => {
        this.attribute = new Map();
        this.attribute.set('', {
          'Betreiber eigene ID': fahrradzaehlstelleDetail.betreiberEigeneId,
          Bezeichnung: fahrradzaehlstelleDetail.fahrradzaehlstelleBezeichnung,
          Seriennummer: fahrradzaehlstelleDetail.seriennummer,
          Gebietskörperschaft: fahrradzaehlstelleDetail.fahrradzaehlstelleGebietskoerperschaft,
          Zählintervall: fahrradzaehlstelleDetail.zaehlintervall + ' Minuten',
          'Letzte Aktualisierung': fahrradzaehlstelleDetail.neusterZeitstempel,
        });
        fahrradzaehlstelleDetail.channels.forEach((channelDetailView, index) => {
          this.attribute.set('Channel ' + (+index + +1), {
            ID: channelDetailView.channelId.toString(),
            Bezeichnung: channelDetailView.channelBezeichnung,
          });
        });
        /* eslint-enable */
        olMapService.scrollIntoViewByCoordinate(fahrradzaehlstelleDetail.geometrie.coordinates);
        changeDetector.markForCheck();
      });
  }

  public onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  public onToggleLeereAttribute(): void {
    this.leereAttributeVisible = !this.leereAttributeVisible;
  }
}
