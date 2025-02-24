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
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  HostListener,
  Input,
  NgZone,
  OnDestroy,
  Output,
  ViewChild,
} from '@angular/core';
import { ResizeObserver } from '@juggle/resize-observer';
import { Feature, MapBrowserEvent, Map as OlMap, Overlay } from 'ol';
import { ScaleLine } from 'ol/control';
import { Coordinate } from 'ol/coordinate';
import { buffer, Extent, getCenter } from 'ol/extent';
import { FeatureLike } from 'ol/Feature';
import Geometry from 'ol/geom/Geometry';
import Interaction from 'ol/interaction/Interaction';
import BaseLayer from 'ol/layer/Base';
import Layer from 'ol/layer/Layer';
import { Pixel } from 'ol/pixel';
import * as olProj from 'ol/proj';
import Source from 'ol/source/Source';
import View from 'ol/View';
import { Observable, Subject, Subscription } from 'rxjs';
import { throttleTime } from 'rxjs/operators';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { LayerQuelle } from 'src/app/shared/models/layer-quelle';
import { LocationSelectEvent } from 'src/app/shared/models/location-select-event';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturLegende } from 'src/app/shared/models/signatur-legende';
import { WMSLegende } from 'src/app/shared/models/wms-legende';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { environment } from 'src/environments/environment';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-ol-map',
  templateUrl: './ol-map.component.html',
  styleUrls: ['./ol-map.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: OlMapService, useExisting: forwardRef(() => OlMapComponent) }],
  standalone: false,
})
export class OlMapComponent implements OnDestroy, OlMapService, AfterViewInit {
  public static readonly CLICK_HIT_TOLERANCE = 20;
  private static readonly ZOOM_DURATION: number = 250;
  private static readonly ZOOM_OFFSET: number = 0.5;

  @Input()
  selectedSignatur: Signatur | null = null;

  @Output()
  get locationSelect(): Observable<LocationSelectEvent> {
    return this.locationSelectSubject.asObservable();
  }

  //TODO: ausbauen, wird nur noch für RadNETZ-Matching verwendet
  @Output()
  public zoomChange = new EventEmitter<number>();

  @ViewChild('tool')
  toolContainer: ElementRef | undefined;

  @ViewChild('menuLinks')
  menuLinksContainer: ElementRef | undefined;

  @ViewChild('tabelle')
  tabelleContainer: ElementRef | undefined;

  @ViewChild('olMap')
  mapElement: ElementRef | undefined;

  @ViewChild('scaleBar')
  scaleBar: ElementRef | undefined;

  @ViewChild('ortssucheKarteMenuItem', { read: ElementRef })
  ortssucheKarteMenuItem: ElementRef | undefined;

  cursorClass = '';

  public quellen: LayerQuelle[] = [];
  public legenden: (WMSLegende | SignaturLegende)[] = [];

  private quellenMap: Map<BaseLayer, LayerQuelle> = new Map();
  private legendenMap: Map<BaseLayer, WMSLegende | SignaturLegende> = new Map();
  private wmsFeaturesCallbackMap: Map<
    BaseLayer,
    (coordinate: number[], resolution: number) => Promise<Feature<Geometry>[]>
  > = new Map();

  private initialScrollIntoViewCoordinate: Coordinate | undefined;

  private _zoom: number = Number.MAX_VALUE;
  public get zoom(): number {
    return this._zoom;
  }

  private set zoom(value: number) {
    //TODO: ausbauen, wird nur noch für RadNETZ-Matching verwendet
    this.zoomChange.emit(value);
    this._zoom = value;
  }

  private locationSelectSubject = new Subject<LocationSelectEvent>();
  private resolutionChangeSubject: Subject<number> = new Subject<number>();
  private clickSubject: Subject<MapBrowserEvent<UIEvent>> = new Subject<MapBrowserEvent<UIEvent>>();
  private pointerMoveSubject: Subject<MapBrowserEvent<UIEvent>> = new Subject<MapBrowserEvent<UIEvent>>();
  private pointerLeaveSubject: Subject<void> = new Subject<void>();
  private outsideMapClickSubject: Subject<void> = new Subject<void>();

  private map: OlMap | undefined;
  private mapResizeObserver: ResizeObserver;

  private initialLayers: BaseLayer[] = [];
  private initialInteractions: Interaction[] = [];
  private initialOverlays: Overlay[] = [];
  private subscriptions: Subscription[] = [];

  constructor(
    private ngZone: NgZone,
    private mapQueryParamsService: MapQueryParamsService,
    private changeDetector: ChangeDetectorRef,
    public featureTogglzService: FeatureTogglzService
  ) {
    this.mapResizeObserver = new ResizeObserver(() => {
      this.map?.updateSize();
    });
  }

  @HostListener('document:keydown.control.alt.shift.k')
  onShortcut(): void {
    this.ortssucheKarteMenuItem?.nativeElement.querySelector('button').focus();
  }

  @HostListener('document:click', ['$event.target'])
  onClick(target: any): void {
    const clickedInside = this.mapElement?.nativeElement.contains(target);
    // z.B. SelectBox in Popup
    const clickedOnOverlay = document.querySelector('.cdk-overlay-container')?.contains(target);
    if (!clickedInside && !clickedOnOverlay) {
      this.outsideMapClickSubject.next();
    }
  }

  ngAfterViewInit(): void {
    invariant(this.mapElement);
    this.ngZone.runOutsideAngular(() => {
      this.map = new OlMap({
        target: this.mapElement?.nativeElement,
        layers: this.initialLayers,
        controls: [
          // Maßstabsanzeige
          new ScaleLine({
            target: this.scaleBar?.nativeElement,
            bar: true,
            steps: 4,
            text: false, // sowas wie "1 : 2.051.834" ausblenden
            minWidth: 140,
          }),
        ],
        view: new View({
          center: olProj.fromLonLat([8.90260477174281, 48.836894251814996], 'EPSG:25832'),
          zoom: 11,
          projection: 'EPSG:25832',
        }),
      });

      // Der Extent muss nach dem Konstruktor der Map gefittet werden, da sonst der
      // Extent in der View fest gesetzt ist und unveränderlich bleibt.
      const ext = this.mapQueryParamsService.mapQueryParamsSnapshot.view;
      if (ext) {
        this.map.getView().fit(ext, { size: this.map.getSize() });
        const currentZoom = this.map?.getView().getZoom();
        if (currentZoom) {
          this.zoom = currentZoom;
        }
      }

      // Interaktionen zum Editieren der Kante (drag-drop) müssen nach dem Konstruktor
      // der Map hinzugefügt werden, da sonst nur die gesetzten Interactions auf der
      // Karte aktiv sind und die standard Interactions (Bewegen, Zoomen, etc) fehlen.
      if (this.initialInteractions.length > 0) {
        for (const interaction of this.initialInteractions) {
          this.map.addInteraction(interaction);
        }
      }

      if (this.initialOverlays.length > 0) {
        for (const overlay of this.initialOverlays) {
          this.map.addOverlay(overlay);
        }
      }

      this.map.getView().on('change', () => {
        this.ngZone.run(() => {
          this.mapQueryParamsService.update({ view: this.map?.getView().calculateExtent() });
        });
      });

      this.map.getView().on('change:resolution', () => {
        this.notifyResolutionChange();
      });

      this.map.once('postrender', () => {
        this.notifyResolutionChange();
      });

      this.map.on('pointermove', event => {
        this.pointerMoveSubject.next(event);
      });

      this.map.on('click', event => {
        if (!environment.production) {
          console.debug('coordinate', event.coordinate);
        }

        let features: RadVisFeature[] = [];
        this.map?.forEachFeatureAtPixel(
          event.pixel,
          (feature, layer) => {
            // Bei ol-internen, 'unmanaged' Layer ruft OL diesen Callback mit layer = null auf
            if (layer) {
              const layerId = layer.get(OlMapService.LAYER_ID);
              if (layerId) {
                const id: number | string | undefined = feature.getId();
                features.push(
                  RadVisFeature.ofAttributesMap(
                    id ? +id : null,
                    feature.getProperties(),
                    layerId,
                    feature.getGeometry() as Geometry
                  )
                );
              }
            }
          },
          { hitTolerance: 10 }
        );

        const featuresFromWMS$: Promise<RadVisFeature[]>[] = [];

        this.map?.getLayers().forEach(layer => {
          const currentResolution = this.getCurrentResolution();
          if (this.wmsFeaturesCallbackMap.has(layer) && currentResolution) {
            const getFeatures = this.wmsFeaturesCallbackMap.get(layer);
            if (getFeatures) {
              const featurePromise = getFeatures(event.coordinate, currentResolution).then(
                (feats: Feature<Geometry>[]) => {
                  return feats.map((feature: Feature<Geometry>) => {
                    const id = feature.getId();
                    return RadVisFeature.ofAttributesMap(
                      id ? +id : null,
                      feature.getProperties(),
                      layer.get(OlMapService.LAYER_ID),
                      feature.getGeometry()!
                    );
                  });
                }
              );
              featuresFromWMS$.push(featurePromise);
            }
          }
        });

        Promise.all(featuresFromWMS$)
          .then(featureCollections => {
            featureCollections.forEach(coll => features.push(...coll));
          })
          .finally(() => {
            features = features.slice(0, 10);

            this.ngZone.run(() => {
              this.clickSubject.next(event);

              this.locationSelectSubject.next({
                selectedFeatures: features,
                coordinate: event.coordinate,
              });
            });
          });
      });
    });
    if (this.initialScrollIntoViewCoordinate) {
      this.scrollIntoViewByCoordinate(this.initialScrollIntoViewCoordinate);
    }
    this.mapResizeObserver.observe(this.mapElement.nativeElement);
  }

  ngOnDestroy(): void {
    invariant(this.mapElement);
    this.mapResizeObserver.unobserve(this.mapElement.nativeElement);
    this.map?.dispose();
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }

  addLayer(olLayer: BaseLayer, quelle?: LayerQuelle, legende?: WMSLegende | SignaturLegende): void {
    if (this.map) {
      this.ngZone.runOutsideAngular(() => {
        this.map?.addLayer(olLayer);
      });
    } else {
      this.initialLayers.push(olLayer);
    }

    if (quelle) {
      this.quellenMap.set(olLayer, quelle);
      this.quellen = Array.from(this.quellenMap.values());
      this.changeDetector.markForCheck();
    }

    if (legende) {
      this.legendenMap.set(olLayer, legende);
      this.legenden = Array.from(this.legendenMap.values());
      this.changeDetector.markForCheck();
    }
  }

  addWMSFeatureLayer(
    olLayer: BaseLayer,
    getFeaturesCallback: (coordinate: number[], resolution: number) => Promise<Feature<Geometry>[]>,
    quelle?: LayerQuelle,
    legende?: SignaturLegende | WMSLegende
  ): void {
    this.addLayer(olLayer, quelle, legende);
    this.wmsFeaturesCallbackMap.set(olLayer, getFeaturesCallback);
  }

  removeLayer(olLayer: BaseLayer): void {
    if (this.map) {
      this.ngZone.runOutsideAngular(() => {
        this.map?.removeLayer(olLayer);
      });
    } else if (this.initialLayers.findIndex(v => v === olLayer) > -1) {
      this.initialLayers.splice(
        this.initialLayers.findIndex(v => v === olLayer),
        1
      );
    }

    if (this.quellenMap.has(olLayer)) {
      this.quellenMap.delete(olLayer);
      this.quellen = Array.from(this.quellenMap.values());
      this.changeDetector.markForCheck();
    }

    if (this.legendenMap.has(olLayer)) {
      this.legendenMap.delete(olLayer);
      this.legenden = Array.from(this.legendenMap.values());
      this.changeDetector.markForCheck();
    }

    if (this.wmsFeaturesCallbackMap.has(olLayer)) {
      this.wmsFeaturesCallbackMap.delete(olLayer);
    }
  }

  addOverlay(olPopup: Overlay): void {
    if (this.map) {
      this.map.addOverlay(olPopup);
    } else {
      this.initialOverlays.push(olPopup);
    }
  }

  removeOverlay(olPopup: Overlay): void {
    if (this.map) {
      this.map.removeOverlay(olPopup);
    } else {
      this.initialOverlays.splice(
        this.initialOverlays.findIndex(v => v === olPopup),
        1
      );
    }
  }

  onMouseLeave(): void {
    this.pointerLeaveSubject.next();
  }

  public addInteraction(interaction: Interaction): void {
    if (this.map) {
      this.map.addInteraction(interaction);
    } else {
      this.initialInteractions.push(interaction);
    }
  }

  public removeInteraction(interaction: Interaction): void {
    if (this.map) {
      this.map?.removeInteraction(interaction);
    } else {
      this.initialInteractions.splice(
        this.initialInteractions.findIndex(v => v === interaction),
        1
      );
    }
  }

  public getCurrentResolution(): number | undefined {
    return this.map?.getView().getResolution();
  }

  public getResolution$(): Observable<number> {
    return this.resolutionChangeSubject.asObservable();
  }

  public click$(): Observable<MapBrowserEvent<UIEvent>> {
    return this.clickSubject.asObservable();
  }

  public locationSelected$(): Observable<LocationSelectEvent> {
    return this.locationSelect;
  }

  public pointerMove$(): Observable<MapBrowserEvent<UIEvent>> {
    return this.pointerMoveSubject.pipe(throttleTime(50));
  }

  public pointerLeave$(): Observable<void> {
    return this.pointerLeaveSubject.asObservable();
  }

  public outsideMapClick$(): Observable<void> {
    return this.outsideMapClickSubject.asObservable();
  }

  public getFeaturesAtPixel(
    pixel: Pixel,
    layerFilter?: (l: Layer<Source>) => boolean,
    hitTolerance = OlMapComponent.CLICK_HIT_TOLERANCE
  ): FeatureLike[] | undefined {
    return this.map?.getFeaturesAtPixel(pixel, {
      hitTolerance,
      layerFilter,
    });
  }

  public getFeaturesAtCoordinate(
    coordinate: Coordinate,
    layerFilter?: (l: Layer<Source>) => boolean,
    hitTolerance = OlMapComponent.CLICK_HIT_TOLERANCE
  ): FeatureLike[] | undefined {
    const pixel = this.map?.getPixelFromCoordinate(coordinate);
    invariant(pixel);
    return this.getFeaturesAtPixel(pixel, layerFilter, hitTolerance);
  }

  public getZoomForResolution(resolution: number): number | undefined {
    return this.map?.getView().getZoomForResolution(resolution);
  }

  public onSelectCenter({ coordinate, extent }: { coordinate: Coordinate; extent?: Extent }): void {
    if (extent) {
      this.map?.getView().fit(extent, { size: this.map?.getSize() });
      this.mapQueryParamsService.update({ view: this.map?.getView().calculateExtent() });
    } else {
      this.onCenterChanged(coordinate);
    }
  }

  public zoomIn(): void {
    const zoom = this.map?.getView()?.getZoom();
    if (zoom) {
      this.map?.getView().animate({ zoom: zoom + OlMapComponent.ZOOM_OFFSET, duration: OlMapComponent.ZOOM_DURATION });
    }
  }

  public zoomOut(): void {
    const zoom = this.map?.getView()?.getZoom();
    if (zoom) {
      this.map?.getView().animate({ zoom: zoom - OlMapComponent.ZOOM_OFFSET, duration: OlMapComponent.ZOOM_DURATION });
    }
  }

  public onceOnPostRender(listener: () => void): void {
    this.map?.once('postrender', listener);
  }

  public scrollIntoViewByCoordinate(coordinate: Coordinate): void {
    if (this.map) {
      // check if coor is in view
      const pixelFromCoordinate = this.map.getPixelFromCoordinate(coordinate);

      // Die Map muss bereits gerendert sein, damit wir den Pixel bestimmen können. Siehe: https://github.com/openlayers/openlayers/issues/8222#issuecomment-391960667
      if (!pixelFromCoordinate) {
        this.map.once('postrender', () => this.scrollIntoViewByCoordinate(coordinate));
        return;
      }

      const visibleMapPortion = this.calculateVisibleMapPortion();
      if (visibleMapPortion.contains(pixelFromCoordinate)) {
        return;
      }
      // scroll into view if necessary
      const size = this.map.getSize();
      invariant(size);
      this.map.getView().centerOn(coordinate, size, visibleMapPortion.middle);
      this.refreshBboxLayers();
    } else {
      this.initialScrollIntoViewCoordinate = coordinate;
    }
  }

  public scrollIntoViewByGeometry(geometry: Geometry): void {
    if (this.map) {
      const mapPortion = this.calculateVisibleMapPortion();
      const extent = buffer(geometry.getExtent(), 50);
      const heightToolbarTop = this.map.getViewport().getBoundingClientRect().y;
      const size = this.map.getSize();

      this.map.getView().fit(extent, { size: [mapPortion.width, mapPortion.height - heightToolbarTop] });
      invariant(size);
      this.map
        .getView()
        .centerOn(getCenter(extent), size, [mapPortion.middle[0], mapPortion.middle[1] - heightToolbarTop / 2]);
    }
  }

  public setCursor(cssClass: string): void {
    this.cursorClass = cssClass;
    this.changeDetector.detectChanges();
  }

  public resetCursor(): void {
    this.cursorClass = '';
    this.changeDetector.detectChanges();
  }

  public updateLegende(layer: BaseLayer, legende: SignaturLegende | WMSLegende | null): void {
    if (legende === null) {
      this.legendenMap.delete(layer);
    } else {
      this.legendenMap.set(layer, legende);
    }

    this.legenden = Array.from(this.legendenMap.values());
    this.changeDetector.markForCheck();
  }

  private calculateVisibleMapPortion(): VisibleRectangle {
    return new VisibleRectangle(
      (this.menuLinksContainer?.nativeElement as HTMLElement).getBoundingClientRect().width ?? 0,
      0,
      (this.toolContainer?.nativeElement as HTMLElement).getBoundingClientRect().x ?? 200,
      (this.tabelleContainer?.nativeElement as HTMLElement).getBoundingClientRect().y ?? 200
    );
  }

  private refreshBboxLayers(): void {
    invariant(this.map);
    this.map.getLayers().forEach(layer => {
      if (layer.get(OlMapService.IS_BBOX_LAYER)) {
        (layer as Layer).getSource().refresh();
      }
    });
  }

  private onCenterChanged(center: Coordinate): void {
    this.map?.getView().setCenter(center);
    this.mapQueryParamsService.update({ view: this.map?.getView().calculateExtent() });
  }

  private notifyResolutionChange(): void {
    const currentResolution = this.getCurrentResolution();
    if (currentResolution) {
      this.resolutionChangeSubject.next(currentResolution);
      const currentZoom = this.map?.getView().getZoom();
      if (currentZoom) {
        this.zoom = currentZoom;
      }
    }
  }
}

class VisibleRectangle {
  constructor(
    public left: number,
    public top: number,
    public right: number,
    public bottom: number
  ) {
    invariant(left <= right);
    invariant(top <= bottom);
  }

  public get height(): number {
    return this.bottom - this.top;
  }

  public get width(): number {
    return this.right - this.left;
  }

  public contains(position: Pixel): boolean {
    return position[0] > this.left && position[0] < this.right && position[1] > this.top && position[1] < this.bottom;
  }

  public get middle(): Pixel {
    return [this.left + this.width / 2, this.top + this.height / 2];
  }
}
