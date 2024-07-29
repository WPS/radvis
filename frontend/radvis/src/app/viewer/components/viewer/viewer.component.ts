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
  forwardRef,
  HostListener,
  ViewChild,
} from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { Coordinate } from 'ol/coordinate';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { FehlerprotokollLayerComponent } from 'src/app/fehlerprotokoll/components/fehlerprotokoll-layer/fehlerprotokoll-layer.component';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { LocationSelectEvent } from 'src/app/shared/models/location-select-event';
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import { NetzbezugAuswahlModusService } from 'src/app/shared/services/netzbezug-auswahl-modus.service';
import { SelectFeatureMenuComponent } from 'src/app/viewer/components/select-feature-menu/select-feature-menu.component';
import { fehlerprotokollLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';

@Component({
  selector: 'rad-viewer',
  templateUrl: './viewer.component.html',
  styleUrls: ['./viewer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    NetzAusblendenService,
    FeatureHighlightService,
    { provide: NetzbezugAuswahlModusService, useExisting: forwardRef(() => ViewerComponent) },
  ],
})
export class ViewerComponent implements NetzbezugAuswahlModusService {
  @ViewChild(SelectFeatureMenuComponent)
  selectFeatureMenuComponent: SelectFeatureMenuComponent | undefined;

  public selectedLocation: Coordinate | null = null;

  public mitVerlauf$: Observable<boolean>;
  public selectedNetzSignatur$: Observable<Signatur | null>;
  public selectedSignatur$: Observable<Signatur | null>;

  public netzAusgeblendet = false;
  public selektionDisabled = false;
  public fehlerprotokolleEnabled: boolean;
  public measureModeEnabled = false;

  public fehlerprotokollZIndex = fehlerprotokollLayerZIndex;

  constructor(
    private mapQueryParamsService: MapQueryParamsService,
    private changeDetectorRef: ChangeDetectorRef,
    featureTogglzService: FeatureTogglzService,
    iconRegistry: MatIconRegistry,
    sanitizer: DomSanitizer
  ) {
    iconRegistry.addSvgIcon('verlaufOn', sanitizer.bypassSecurityTrustResourceUrl('./assets/verlauf_on.svg'));
    iconRegistry.addSvgIcon('verlaufOff', sanitizer.bypassSecurityTrustResourceUrl('./assets/verlauf_off.svg'));

    this.fehlerprotokolleEnabled = featureTogglzService.fehlerprotokoll;

    this.selectedSignatur$ = this.mapQueryParamsService.signatur$;
    this.selectedNetzSignatur$ = this.selectedSignatur$.pipe(
      map(signatur => {
        if (signatur && signatur.typ === SignaturTyp.MASSNAHME) {
          return null;
        }
        return signatur;
      })
    );
    this.mitVerlauf$ = this.mapQueryParamsService.mitVerlauf$;
  }

  @HostListener('document:keydown.escape')
  public onEscapePressed(): void {
    this.measureModeEnabled = false;
  }

  public startNetzbezugAuswahl(netzausblenden = true): void {
    this.netzAusgeblendet = netzausblenden;
    this.selektionDisabled = true;
    this.changeDetectorRef.markForCheck();
  }

  public stopNetzbezugAuswahl(): void {
    this.netzAusgeblendet = false;
    this.selektionDisabled = false;
    this.changeDetectorRef.markForCheck();
  }

  public onFeatureSelected(event: LocationSelectEvent): void {
    this.selectedLocation = event.coordinate;

    if (
      (event.selectedFeatures.length === 0 ||
        event.selectedFeatures[0].layer !== FehlerprotokollLayerComponent.LAYER_ID) &&
      !this.selektionDisabled
    ) {
      this.selectFeatureMenuComponent?.onLocationSelect(event);
    }
  }

  public onToggleVerlauf(visible: boolean): void {
    this.updateMitVerlaufInRoute(visible);
  }

  public onSelectRadVisSignatur(signatur: Signatur | null): void {
    this.mapQueryParamsService.update({ signatur: signatur ?? undefined });
  }

  public onMeasureClicked(): void {
    this.measureModeEnabled = !this.measureModeEnabled;
  }

  private updateMitVerlaufInRoute(mitVerlauf: boolean): void {
    this.mapQueryParamsService.update({ mitVerlauf });
  }
}
