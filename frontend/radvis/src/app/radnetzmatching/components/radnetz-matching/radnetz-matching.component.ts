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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostListener, OnDestroy } from '@angular/core';
import { Coordinate } from 'ol/coordinate';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { DlmKantenLayer } from 'src/app/radnetzmatching/components/dlm-kanten-selektion/dlm-kanten-layer';
import { NetzfehlerLayer } from 'src/app/radnetzmatching/components/feature-layer/netzfehler-layer';
import { NetzfehlerMatchingLayer } from 'src/app/radnetzmatching/components/feature-layer/netzfehler-matching-layer';
import { NetzfehlerProjektionLayer } from 'src/app/radnetzmatching/components/feature-layer/netzfehler-projektion-layer';
import { NetzfehlerSackgassenLayer } from 'src/app/radnetzmatching/components/feature-layer/netzfehler-sackgassen-layer';
import { LandkreiseLayer } from 'src/app/radnetzmatching/components/landkreise-layer/landkreise-layer';
import { RadnetzKantenLayer } from 'src/app/radnetzmatching/components/radnetz-kanten-layer/radnetz-kanten-layer';
import { ZugeordneteDlmKantenLayer } from 'src/app/radnetzmatching/components/zugeordnete-dlm-kanten-layer/zugeordnete-dlm-kanten-layer';
import { MatchingRelatedFeatureDetails } from 'src/app/radnetzmatching/models/matching-related-feature-details';
import { NetzfehlerService } from 'src/app/radnetzmatching/services/netzfehler.service';
import { PrimarySelectionService } from 'src/app/radnetzmatching/services/primary-selection.service';
import { ZuordnungService } from 'src/app/radnetzmatching/services/zuordnung.service';
import { LayerId, RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';
import { LocationSelectEvent } from 'src/app/shared/models/location-select-event';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { RadVisFeatureAttributes } from 'src/app/shared/models/rad-vis-feature-attributes';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { NetzFeatureDetailsService } from 'src/app/viewer/netz-details/services/netz-feature-details.service';
import { environment } from 'src/environments/environment';
import invariant from 'tiny-invariant';

export enum RadNetzMatchingState {
  FREE_SELECTION,
  ZUORDNEN_ACTIVE,
  DLM_KANTEN_SELECTED,
}

@Component({
  selector: 'rad-radnetz-matching',
  templateUrl: './radnetz-matching.component.html',
  styleUrls: ['./radnetz-matching.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PrimarySelectionService],
})
export class RadnetzMatchingComponent implements OnDestroy {
  public selectableLayers: RadVisLayer[] = [];
  public zoom: number = Number.MAX_VALUE;

  public visibleZusaetzlicheLayers$: Observable<RadVisLayer[]>;
  public visibileNetzfehlerLayer$: Observable<RadVisLayer[]>;

  public featuresAtDisplay$: Promise<MatchingRelatedFeatureDetails[]> = Promise.resolve([]);

  public secondarySelectedDlmKanteIds: number[] = [];
  public RadNetzMatchingState = RadNetzMatchingState;
  public currentState: RadNetzMatchingState = RadNetzMatchingState.FREE_SELECTION;
  public zugeordneteDlmKanteIds: number[] = [];
  public zugeordneteRadnetzKanteIds: number[] = [];

  public anpassungswunschCreationModeIsOn = false;
  public erledigtMarkierenActive = false;
  public fehlerprotokolleEnabled: boolean;

  public featureWasRemoved$ = new BehaviorSubject<number | null>(null);

  private primarySelectedId: number | null = null;
  private primarySelectedQuellsystem: string | null = null;
  private subscription: Subscription;

  constructor(
    private notifyUserService: NotifyUserService,
    private errorHandlingService: ErrorHandlingService,
    private mapQueryParamsService: MapQueryParamsService,
    private zuordnungService: ZuordnungService,
    private netzfehlerService: NetzfehlerService,
    private changeDetectorRef: ChangeDetectorRef,
    private featureDetailsService: NetzFeatureDetailsService,
    private primarySelectionService: PrimarySelectionService,
    featureTogglzService: FeatureTogglzService
  ) {
    this.fehlerprotokolleEnabled = featureTogglzService.fehlerprotokoll;
    this.selectableLayers = [
      new NetzfehlerMatchingLayer(),
      new NetzfehlerSackgassenLayer(),
      new NetzfehlerProjektionLayer(),
      new RadnetzKantenLayer(),
      new DlmKantenLayer(),
      new ZugeordneteDlmKantenLayer(),
      new LandkreiseLayer(),
    ];
    this.mapQueryParamsService.update({
      layers: [
        ...this.selectableLayers
          .map(layer => layer.id)
          .filter(id => id !== ZugeordneteDlmKantenLayer.LAYER_ID && id !== LandkreiseLayer.LAYER_ID),
      ],
    });
    this.visibleZusaetzlicheLayers$ = this.mapQueryParamsService.layers$.pipe(
      map(ls => this.selectableLayers.filter(l => l.typ === RadVisLayerTyp.GEO_JSON && ls.includes(l.id)))
    );
    this.visibileNetzfehlerLayer$ = this.visibleZusaetzlicheLayers$.pipe(
      map(ls => ls.filter(l => l.id.startsWith(NetzfehlerLayer.ID_PREFIX)))
    );
    this.subscription = this.primarySelectionService.primarySelection$.subscribe(primarySelectedFeatureReference => {
      this.primarySelectedId = primarySelectedFeatureReference ? primarySelectedFeatureReference.featureId : null;
      this.primarySelectedQuellsystem = primarySelectedFeatureReference
        ? primarySelectedFeatureReference.layerId
        : null;
      this.updateDisplayedZuordnungen();
    });
  }

  @HostListener('document:keydown.escape', ['$event'])
  public onKeydownHandler(): void {
    this.anpassungswunschCreationModeIsOn = false;
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  public get radNetzLayerVisible(): boolean {
    return this.currentVisibleLayers.includes(RadnetzKantenLayer.LAYER_ID);
  }

  public get dlmNetzLayerVisible(): boolean {
    return this.currentVisibleLayers.includes(DlmKantenLayer.LAYER_ID);
  }

  public get anpassungswunschLayerVisible(): boolean {
    return false;
  }

  public get landkreiseLayerVisible(): boolean {
    return this.currentVisibleLayers.includes(LandkreiseLayer.LAYER_ID);
  }

  public get zugeordneteDlmKantenLayerVisible(): boolean {
    return this.currentVisibleLayers.includes(ZugeordneteDlmKantenLayer.LAYER_ID);
  }

  public get showFlag(): boolean {
    return this.anpassungswunschLayerVisible && this.anpassungswunschCreationModeIsOn;
  }

  // FIXME, dead method?
  public get highlightedDlmKanteIds(): number[] {
    if (this.primarySelectedQuellsystem === QuellSystem.DLM && this.primarySelectedId) {
      return [this.primarySelectedId];
    } else if (this.primarySelectedQuellsystem === QuellSystem.RadNETZ) {
      return this.secondarySelectedDlmKanteIds;
    } else {
      return [];
    }
  }

  public onDlmKantenSelect(kanteIds: number[]): void {
    this.secondarySelectedDlmKanteIds = kanteIds;
    if (kanteIds.length > 0) {
      this.currentState = RadNetzMatchingState.DLM_KANTEN_SELECTED;
    } else {
      this.currentState = RadNetzMatchingState.ZUORDNEN_ACTIVE;
    }
  }

  private get currentVisibleLayers(): LayerId[] {
    return this.mapQueryParamsService.mapQueryParamsSnapshot.layers;
  }

  public onShowLayer(id: LayerId): void {
    const layers = [...this.currentVisibleLayers, id];
    this.updateLayersInRoute(layers);
  }

  public onHideLayer(id: LayerId): void {
    const layers = this.currentVisibleLayers.filter(layerId => layerId !== id);
    this.updateLayersInRoute(layers);
  }

  public onZoomChanged(zoomLevel: number): void {
    this.zoom = zoomLevel;
  }

  public onCloseDetails(): void {
    this.featuresAtDisplay$ = Promise.resolve([]);
    this.zugeordneteDlmKanteIds = [];
    this.zugeordneteRadnetzKanteIds = [];
    this.primarySelectionService.primarySelectFeature(null);
    this.secondarySelectedDlmKanteIds = [];
    this.currentState = RadNetzMatchingState.FREE_SELECTION;
  }

  public onZuordnen(): void {
    this.currentState = RadNetzMatchingState.ZUORDNEN_ACTIVE;
  }

  public onLocationSelected(event: LocationSelectEvent): void {
    if (this.erledigtMarkierenActive) {
      return;
    }
    if (this.currentState === RadNetzMatchingState.FREE_SELECTION) {
      if (event.selectedFeatures.length === 0) {
        this.onCloseDetails();
      }
      this.featuresAtDisplay$ = this.lazyLoadFeatureDetails(event.selectedFeatures, event.coordinate);
      this.secondarySelectedDlmKanteIds = [];
    }
  }

  public onDelete(): void {
    invariant(this.primarySelectedId !== null && this.primarySelectedQuellsystem === QuellSystem.DLM);
    this.zuordnungService
      .deleteZuordnung({
        dlmnetzKanteId: this.primarySelectedId,
      })
      .then(() => {
        invariant(this.primarySelectedId);
        this.notifyUserService.inform('Zuordnung erfolgreich gelöscht.');
        this.updateDisplayedZuordnungen();
        this.currentState = RadNetzMatchingState.FREE_SELECTION;
      });
  }

  public onSave(): void {
    invariant(this.primarySelectedId !== null && this.primarySelectedQuellsystem === QuellSystem.RadNETZ);
    this.zuordnungService
      .changeZuordnung({
        radnetzKanteId: this.primarySelectedId,
        dlmnetzKanteIds: this.secondarySelectedDlmKanteIds,
      })
      .then(() => {
        this.notifyUserService.inform('Zuordnung erfolgreich geändert.');
        this.loadExistingZuordnungForSelectedRadNetzKante().then(zugeordneteDlmKanteIds => {
          this.zugeordneteDlmKanteIds = zugeordneteDlmKanteIds;
          this.currentState = RadNetzMatchingState.FREE_SELECTION;
          this.changeDetectorRef.detectChanges();
        });
      });
  }

  public onNetzfehlerErledigt(featureId: number): void {
    this.netzfehlerService.alsErledigtMarkieren(featureId).then(() => {
      this.featureWasRemoved$.next(featureId);
      this.notifyUserService.inform('Netzfehler wurde als erledigt markiert und aus der Auswahl entfernt.');

      this.changeDetectorRef.detectChanges();
    });
  }

  public toggleErledigteMarkieren(): void {
    this.anpassungswunschCreationModeIsOn = false;
    this.erledigtMarkierenActive = !this.erledigtMarkierenActive;
  }

  public toggleAnpassungswunschCreationMode(): void {
    this.erledigtMarkierenActive = false;
    this.anpassungswunschCreationModeIsOn = !this.anpassungswunschCreationModeIsOn;
  }

  private lazyLoadFeatureDetails(
    selectedFeatures: RadVisFeature[],
    clickposition: Coordinate
  ): Promise<MatchingRelatedFeatureDetails[]> {
    const promises: Promise<MatchingRelatedFeatureDetails>[] = [];
    selectedFeatures.forEach(feature => {
      switch (feature.layer) {
        case NetzfehlerMatchingLayer.LAYER_ID:
        case NetzfehlerProjektionLayer.LAYER_ID:
        case NetzfehlerSackgassenLayer.LAYER_ID:
          promises.push(
            this.netzfehlerService
              .getAttributeFuerNetzfehler(Number(feature.id))
              .then(nf => this.convertNetzfehlerToMatchingRelatedFeatureDetails(feature.layer, nf))
          );
          break;
        case DlmKantenLayer.LAYER_ID:
        case RadnetzKantenLayer.LAYER_ID:
          if (!feature.isKnoten) {
            promises.push(
              this.featureDetailsService
                .getAttributeFuerKante(Number(feature.id), clickposition)
                .then(k => this.convertKanteToMatchingRelatedFeatureDetails(new Map(Object.entries(k))))
            );
          }
          break;
        default:
          if (!environment.production) {
            console.error('Kein Lazy load für Feature Details aus Layer: ' + feature.layer);
          }
      }
    });
    return Promise.all(promises);
  }

  private convertKanteToMatchingRelatedFeatureDetails(
    attribute: RadVisFeatureAttributes
  ): MatchingRelatedFeatureDetails {
    return {
      id: Number(attribute.get('ID')),
      attributes: attribute,
      layername: attribute.get('Quelle') as QuellSystem,
      layerId: attribute.get('Quelle') as QuellSystem,
      isNetzfehler: false,
    };
  }

  private convertNetzfehlerToMatchingRelatedFeatureDetails(
    layer: LayerId,
    netzfehler: {
      [key: string]: string;
    }
  ): MatchingRelatedFeatureDetails {
    return {
      id: Number(netzfehler.id),
      layername: this.selectableLayers.find(l => l.id === layer)?.bezeichnung || '',
      layerId: layer,
      attributes: new RadVisFeatureAttributes(netzfehler),
      isNetzfehler: true,
    };
  }

  private updateDisplayedZuordnungen(): void {
    this.zugeordneteDlmKanteIds = [];
    this.zugeordneteRadnetzKanteIds = [];
    if (this.primarySelectedQuellsystem === QuellSystem.RadNETZ) {
      this.loadExistingZuordnungForSelectedRadNetzKante().then(zugeordneteDlmKanteIds => {
        this.zugeordneteDlmKanteIds = zugeordneteDlmKanteIds;
        this.changeDetectorRef.detectChanges();
      });
    } else if (this.primarySelectedQuellsystem === QuellSystem.DLM) {
      this.loadExistingZuordnungForSelectedDlmKante().then(zugeordneteRadnetzKanteIds => {
        this.zugeordneteRadnetzKanteIds = zugeordneteRadnetzKanteIds;
        this.changeDetectorRef.detectChanges();
      });
    }
  }

  private loadExistingZuordnungForSelectedRadNetzKante(): Promise<number[]> {
    invariant(this.primarySelectedId);
    return this.zuordnungService.getZuordnungRadnetz(this.primarySelectedId);
  }

  private loadExistingZuordnungForSelectedDlmKante(): Promise<number[]> {
    invariant(this.primarySelectedId);
    return this.zuordnungService.getZuordnungDlm(this.primarySelectedId);
  }

  private updateLayersInRoute(layers: LayerId[]): void {
    this.mapQueryParamsService.update({ layers });
  }
}
