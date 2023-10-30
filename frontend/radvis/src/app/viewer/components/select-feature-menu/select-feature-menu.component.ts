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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { Coordinate } from 'ol/coordinate';
import { Subscription } from 'rxjs';
import { LocationSelectEvent } from 'src/app/shared/models/location-select-event';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { LayerRegistryService } from 'src/app/viewer/services/layer-registry.service';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-select-feature-menu',
  templateUrl: './select-feature-menu.component.html',
  styleUrls: ['./select-feature-menu.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectFeatureMenuComponent implements OnDestroy {
  location: Coordinate | null = null;

  selectedFeatures: RadVisFeature[] = [];

  private outsideMapClickSubscription: Subscription;

  constructor(
    public layerRegistryService: LayerRegistryService,
    private changeDetector: ChangeDetectorRef,
    private featureHighlightService: FeatureHighlightService,
    private notifyUserService: NotifyUserService,
    olMapService: OlMapService
  ) {
    this.outsideMapClickSubscription = olMapService.outsideMapClick$().subscribe(() => {
      this.reset();
      this.changeDetector.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.outsideMapClickSubscription.unsubscribe();
  }

  public onSelectFeature(f: RadVisFeature): void {
    invariant(this.location);
    this.layerRegistryService.toEditor(f, this.location);
    this.reset();
  }

  public onLocationSelect(event: LocationSelectEvent): void {
    this.reset();

    // Durch Verwendung der Map werden doppelte Features (gleiche getUniqueKey(f)) entdoppelt
    // -> das zweite ueberschreibt den Wert des ersten unter dem gleichen key
    const selectableFeatures = new Map<string, RadVisFeature>();
    event.selectedFeatures
      .filter(feature => !feature.istStrecke)
      .forEach(f => {
        const uniqueKey = this.layerRegistryService.getUniqueKey(f);
        if (uniqueKey) {
          selectableFeatures.set(uniqueKey, f);
        }
      });

    if (event.selectedFeatures.some(feature => feature.istStrecke) && selectableFeatures.size === 0) {
      this.notifyUserService.inform('Um Kanten auszuwählen, zoomen Sie weiter rein.');
    }

    if (selectableFeatures.size === 1) {
      this.layerRegistryService.toEditor(selectableFeatures.values().next().value, event.coordinate);
    } else if (selectableFeatures.size > 1) {
      this.location = event.coordinate;
      this.selectedFeatures = Array.from(selectableFeatures.values());
    }

    this.changeDetector.detectChanges();
  }

  public onFeatureHover(hovered: boolean, f: RadVisFeature): void {
    if (hovered) {
      this.featureHighlightService.highlight(f);
    } else {
      this.featureHighlightService.unhighlight(f);
    }
  }

  private reset(): void {
    this.location = null;
    this.selectedFeatures.forEach(f => this.featureHighlightService.unhighlight(f));
    this.selectedFeatures = [];
  }
}
