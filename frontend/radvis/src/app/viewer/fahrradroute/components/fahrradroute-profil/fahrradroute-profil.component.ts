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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { Coordinate } from 'ol/coordinate';
import { LineString } from 'ol/geom';
import { Subscription } from 'rxjs';
import { BelagArt } from 'src/app/shared/models/belag-art';
import {
  belagArtLegende,
  flatRadverkehrsfuehrungOptions,
  radverkehrsfuehrungLegende,
} from 'src/app/viewer/fahrradroute/components/fahrradroute-profil/strecken-eigenschaften-legende.config';
import { FahrradrouteProfil } from 'src/app/viewer/fahrradroute/models/fahrradroute-profil';
import { FahrradrouteProfilService } from 'src/app/viewer/fahrradroute/services/fahrradroute-profil.service';

Chart.register(...registerables);

@Component({
  selector: 'rad-fahrradrouten-profil',
  templateUrl: './fahrradroute-profil.component.html',
  styleUrls: ['./fahrradroute-profil.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FahrradrouteProfilComponent implements OnInit, OnDestroy {
  @Output()
  closeRoutenProfil = new EventEmitter<void>();

  route: FahrradrouteProfil | null = null;

  lineString: LineString | null = null;

  currentPosition: Coordinate | null = null;

  subscriptions: Subscription[] = [];

  belagArtLegende = belagArtLegende;
  currentBelagArt?: BelagArt;
  belagArtOptions = BelagArt.options;

  radverkehrsfuehrungLegende = radverkehrsfuehrungLegende;
  currentRadverkehrsfuehrung?: string;
  radverkehrsfuehrungOptions = flatRadverkehrsfuehrungOptions;

  constructor(
    private fahrradroutenProfilService: FahrradrouteProfilService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  onClose(): void {
    this.closeRoutenProfil.next();
  }

  ngOnInit(): void {
    this.subscriptions.push(
      this.fahrradroutenProfilService.currentRouteProfil$.subscribe(route => {
        this.route = route;
        if (this.route && this.is3D(this.route) && this.route.geometrie) {
          this.lineString = new LineString(this.route.geometrie.coordinates);
        } else {
          this.lineString = null;
        }
        this.changeDetectorRef.detectChanges();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  onHover(meterMarke: number): void {
    const LR = meterMarke / (this.lineString?.getLength() ?? 1);
    this.currentBelagArt = this.route?.profilEigenschaften?.find(b => b.vonLR <= LR && b.bisLR >= LR)?.belagArt;
    this.currentRadverkehrsfuehrung = this.route?.profilEigenschaften?.find(
      b => b.vonLR <= LR && b.bisLR >= LR
    )?.radverkehrsfuehrung;
    const coor = this.lineString?.getCoordinateAt(LR);
    this.currentPosition = coor ?? null;
  }

  private is3D(profil: FahrradrouteProfil): boolean {
    const geometrie = profil.geometrie;
    if (!geometrie) {
      return false;
    }
    return geometrie.coordinates && geometrie.coordinates.length > 0 && geometrie.coordinates[0].length === 3;
  }
}
