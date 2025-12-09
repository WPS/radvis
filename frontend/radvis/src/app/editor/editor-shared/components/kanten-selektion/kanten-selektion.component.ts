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

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { Feature, MapBrowserEvent } from 'ol';
import { Color } from 'ol/color';
import { FeatureLike } from 'ol/Feature';
import { GeoJSON } from 'ol/format';
import { Geometry, LineString } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Stroke, Style } from 'ol/style';
import { Observable, Subscription } from 'rxjs';
import { debounceTime, delay, distinctUntilChanged, map } from 'rxjs/operators';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import {
  KantenHoverEvent,
  KantenSelektionHoverService,
} from 'src/app/editor/kanten/services/kanten-selektion-hover.service';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NetzBearbeitungModusService } from 'src/app/editor/kanten/services/netz-bearbeitung-modus.service';
import { NotifyGeometryChangedService } from 'src/app/editor/kanten/services/notify-geometry-changed.service';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { MIN_STRECKEN_RESOLUTION } from 'src/app/shared/models/min-strecken-resolution';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { shiftFeature } from 'src/app/shared/models/radvis-netz-style';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { IS_SELECTABLE_LAYER } from 'src/app/shared/models/selectable-layer-property';
import { StreckenNetzVectorlayer } from 'src/app/shared/models/strecken-netz-vectorlayer';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { createVectorSource } from 'src/app/shared/services/vector-source.factory';

@Component({
  selector: 'rad-kanten-selektion',
  templateUrl: './kanten-selektion.component.html',
  styleUrls: ['./kanten-selektion.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KantenSelektionComponent implements OnDestroy {
  private selektion$: Observable<KantenSelektion[]>;

  private readonly kantenLayers: VectorLayer[] = [];
  private readonly selektionLayerSource: VectorSource = new VectorSource();
  private readonly selektionLayer!: VectorLayer;

  private netzklassen: Netzklassefilter[] = [];
  private activeAttributGruppe: AttributGruppe | null = null;
  private active = false;
  private hoveredKante: { kanteId: number; kantenSeite?: KantenSeite } | null = null;

  private subscriptions: Subscription[] = [];

  constructor(
    private olMapService: OlMapService,
    private errorHandlingService: ErrorHandlingService,
    private notifyGeometryChangedService: NotifyGeometryChangedService,
    private notifyUserService: NotifyUserService,
    private radNetzFeatureService: NetzausschnittService,
    private kantenSelektionService: KantenSelektionService,
    bearbeitungModusService: NetzBearbeitungModusService,
    private kantenSelektionHoverService: KantenSelektionHoverService,
    netzklassenAuswahlService: NetzklassenAuswahlService
  ) {
    this.selektion$ = this.kantenSelektionService.selektion$;
    netzklassenAuswahlService.currentAuswahl$.subscribe(selectedNetzklassen => {
      this.netzklassen = selectedNetzklassen;
      this.updateNetzklassenVisibility();
    });

    Netzklassefilter.getAll().forEach(nk => {
      const kantenVectorSource = createVectorSource({
        getFeaturesObservable: extent => this.radNetzFeatureService.getKantenForView(extent, [nk], false),
        parseFeatures: featureCollection => {
          const features: Feature<Geometry>[] = [];
          new GeoJSON().readFeatures(featureCollection).forEach(feature => {
            if (
              AttributGruppe.isSeitenbezogen(this.activeAttributGruppe) &&
              feature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)
            ) {
              features.push(this.cloneFeatureForSeitenbezug(feature, KantenSeite.LINKS));
              features.push(this.cloneFeatureForSeitenbezug(feature, KantenSeite.RECHTS));
            } else {
              feature.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, Number(feature.getId()));
              features.push(feature);
            }
          });
          return features;
        },
        onFeaturesLoaded: () => {},
        onError: error => this.errorHandlingService.handleError(error),
      });

      const vectorLayer = new VectorLayer({
        source: kantenVectorSource,
        // @ts-expect-error Migration von ts-ignore
        renderOrder: null,
        style: this.getStyleFunctionForKantenLayer(MapStyles.FEATURE_COLOR),
        minZoom: nk.minZoom,
        zIndex: EditorLayerZindexConfig.KANTEN_ANZEIGEN_LAYER,
        netzklasse: nk,
      });
      this.kantenLayers.push(vectorLayer);
      if (nk === Netzklassefilter.RADNETZ) {
        vectorLayer.setMaxResolution(MIN_STRECKEN_RESOLUTION);
      }
    });

    const streckenNetzVectorlayer = new StreckenNetzVectorlayer(
      () => this.radNetzFeatureService.getAlleRadNETZKantenForView(false),
      EditorLayerZindexConfig.KANTEN_ANZEIGEN_LAYER
    );
    streckenNetzVectorlayer.set('netzklasse', Netzklassefilter.RADNETZ, true);
    streckenNetzVectorlayer.setMinResolution(MIN_STRECKEN_RESOLUTION);
    this.kantenLayers.push(streckenNetzVectorlayer);

    this.updateNetzklassenVisibility();

    this.kantenLayers.forEach(kl => {
      kl.set(IS_SELECTABLE_LAYER, true);
      this.olMapService.addLayer(kl);
    });

    this.selektionLayerSource.setLoader(this.loadingSelection);
    this.selektionLayer = new VectorLayer({
      source: this.selektionLayerSource,
      style: this.getStyleFunctionForSelektionLayer(MapStyles.FEATURE_SELECT_COLOR),
      minZoom: 0,
      zIndex: EditorLayerZindexConfig.KANTEN_SELECTION_LAYER,
    });
    this.selektionLayer.set(IS_SELECTABLE_LAYER, true);
    this.olMapService.addLayer(this.selektionLayer);

    this.subscriptions.push(
      this.notifyGeometryChangedService.geometryChanged$.subscribe(() => {
        this.kantenLayers.forEach(kl => kl.getSource()?.refresh());
      })
    );

    this.subscriptions.push(
      this.selektion$.pipe(debounceTime(100)).subscribe(() => {
        this.selektionLayerSource.refresh();
        this.kantenLayers.forEach(kl => kl.changed());
        // Wenn die letzte Teilselektion einer Kante auf der Karte aufgehoben wird, kann kein Unhover mehr ausgelöst werden
        if (this.hoveredKante && !this.kantenSelektionService.isSelektiert(this.hoveredKante.kanteId)) {
          this.onUnhover();
        }
      })
    );

    this.subscriptions.push(
      bearbeitungModusService.getAktiveKantenGruppe().subscribe(gruppe => {
        this.activeAttributGruppe = gruppe;
        this.active = gruppe !== null;
      })
    );

    const selektionsLayerHiddenChange$ = bearbeitungModusService.getAktiveKantenGruppe().pipe(
      map(gruppe => {
        return (
          gruppe === AttributGruppe.VERLAUF ||
          gruppe === AttributGruppe.ZUSTAENDIGKEIT ||
          gruppe === AttributGruppe.FUEHRUNGSFORM ||
          gruppe === AttributGruppe.GESCHWINDIGKEIT
        );
      }),
      distinctUntilChanged(),
      // damit this.activeAttributgruppe schon gesetzt ist
      delay(1)
    );
    this.subscriptions.push(
      selektionsLayerHiddenChange$.subscribe(hidden => {
        this.selektionLayer.setVisible(!hidden);
      })
    );

    const isZweiseitigChanged$ = bearbeitungModusService.getAktiveKantenGruppe().pipe(
      map(gruppe => {
        return AttributGruppe.isSeitenbezogen(gruppe);
      }),
      distinctUntilChanged(),
      // damit this.activeAttributgruppe schon gesetzt ist
      delay(1)
    );
    this.subscriptions.push(
      isZweiseitigChanged$.subscribe(() => {
        this.kantenLayers.forEach(kl => kl.getSource()?.refresh());
        this.selektionLayerSource.refresh();
      })
    );

    this.subscriptions.push(this.olMapService.click$().subscribe(clickEvent => this.onMapClick(clickEvent)));

    this.subscriptions.push(this.olMapService.pointerMove$().subscribe(moveEvent => this.onMapPointerMove(moveEvent)));
    this.subscriptions.push(
      this.olMapService.pointerLeave$().subscribe(() => this.kantenSelektionHoverService.notifyUnhover())
    );
    this.subscriptions.push(kantenSelektionHoverService.hoverKante$.subscribe(event => this.onHover(event)));
    this.subscriptions.push(kantenSelektionHoverService.unhoverKante$.subscribe(() => this.onUnhover()));
  }

  ngOnDestroy(): void {
    this.kantenLayers.forEach(kl => this.olMapService.removeLayer(kl));
    this.olMapService.removeLayer(this.selektionLayer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  private updateNetzklassenVisibility(): void {
    this.kantenLayers.forEach(kl => {
      kl.setVisible(this.netzklassen.includes(kl.getProperties().netzklasse));
    });
  }

  private onMapClick(clickEvent: MapBrowserEvent<PointerEvent | KeyboardEvent | WheelEvent>): void {
    // nicht selektierbare Layer rausfiltern, z.B. Originalgeometrie von FehlerprotokollEintrag
    const featuresAtPixel = this.olMapService.getFeaturesAtPixel(
      clickEvent.pixel,
      l => this.active && l.get(IS_SELECTABLE_LAYER)
    );
    if (!featuresAtPixel || featuresAtPixel.length === 0) {
      return;
    }
    // Das erste Feature im Array ist das am nähesten zur Click-Position liegende
    const clickedFeature = featuresAtPixel[0] as Feature<Geometry>;
    if (this.kantenLayers.some(kl => kl.getSource()?.hasFeature(clickedFeature))) {
      //<-- ist nicht unbedingt durch LayerFilter gegeben (und darf auch nicht darüber gemacht werden)
      if (clickedFeature.get(FeatureProperties.STRECKE_PROPERTY_NAME)) {
        this.notifyUserService.inform('Um RadNETZ-Kanten auszuwählen, zoomen Sie weiter rein.');
        return;
      }

      const pointerEvent = clickEvent.originalEvent as PointerEvent;
      const toggle = pointerEvent.ctrlKey || pointerEvent.metaKey;
      const kanteId = this.getKanteIdFromFeature(clickedFeature);
      const seitenbezug = clickedFeature.get(FeatureProperties.SEITE_PROPERTY_NAME);
      if (!toggle) {
        this.kantenSelektionService.select(kanteId, false, seitenbezug);
      } else {
        if (this.kantenSelektionService.isSelektiert(kanteId, seitenbezug)) {
          this.kantenSelektionService.deselect(kanteId, seitenbezug);
        } else {
          this.kantenSelektionService.select(kanteId, true, seitenbezug);
        }
      }
    }
  }

  private onMapPointerMove(pointerMoveEvent: MapBrowserEvent<PointerEvent | KeyboardEvent | WheelEvent>): void {
    if (
      this.kantenSelektionService.selektion.length === 0 ||
      !AttributGruppe.isSeitenbezogen(this.activeAttributGruppe) ||
      AttributGruppe.isLinearReferenziert(this.activeAttributGruppe)
    ) {
      return;
    }
    const featuresAtPixel = this.olMapService.getFeaturesAtPixel(pointerMoveEvent.pixel, undefined, 5);
    if (!featuresAtPixel || featuresAtPixel.length === 0) {
      this.kantenSelektionHoverService.notifyUnhover();
    } else {
      const hoveredFeature = featuresAtPixel[0] as Feature<Geometry>;
      const kanteId = this.getKanteIdFromFeature(hoveredFeature);
      const seitenbezug = hoveredFeature.get(FeatureProperties.SEITE_PROPERTY_NAME);
      if (this.kantenSelektionService.isSelektiert(kanteId)) {
        this.kantenSelektionHoverService.notifyHover({ kanteId, kantenSeite: seitenbezug });
      } else {
        this.kantenSelektionHoverService.notifyUnhover();
      }
    }
  }

  private onHover(event: KantenHoverEvent): void {
    if (this.hoveredKante?.kanteId !== event.kanteId || this.hoveredKante?.kantenSeite !== event.kantenSeite) {
      this.onUnhover();
      this.setColorForKante(
        event.kanteId,
        event.kantenSeite,
        MapStyles.FEATURE_HOVER_COLOR,
        MapStyles.FEATURE_HOVER_COLOR
      );
      this.hoveredKante = { kanteId: event.kanteId, kantenSeite: event.kantenSeite };
    }
  }

  private onUnhover(): void {
    if (this.hoveredKante) {
      this.setColorForKante(
        this.hoveredKante.kanteId,
        this.hoveredKante.kantenSeite,
        MapStyles.FEATURE_COLOR,
        MapStyles.FEATURE_SELECT_COLOR
      );
      this.hoveredKante = null;
    }
  }

  private setColorForKante(
    kanteId: number,
    kantenSeite: KantenSeite | undefined,
    kantenLayerColor: Color,
    selektionLayerColor: Color
  ): void {
    const condition = (feature: Feature): boolean =>
      this.getKanteIdFromFeature(feature) === kanteId &&
      feature.get(FeatureProperties.SEITE_PROPERTY_NAME) === kantenSeite;
    const featuresOnKantenLayer: Feature<Geometry>[] = [];
    this.kantenLayers.forEach(kl => {
      const feature = kl.getSource()?.getFeatures().find(condition);
      if (feature) {
        featuresOnKantenLayer.push(feature);
      }
    });
    if (featuresOnKantenLayer.length > 0) {
      featuresOnKantenLayer.forEach(feature => feature.setStyle(this.getStyleFunctionForKantenLayer(kantenLayerColor)));
    }
    const featureOnSelektionLayer = this.selektionLayerSource.getFeatures().find(condition);
    if (featureOnSelektionLayer) {
      featureOnSelektionLayer.setStyle(this.getStyleFunctionForSelektionLayer(selektionLayerColor));
    }
  }

  private loadingSelection = (): void => {
    this.selektionLayerSource.clear();
    this.kantenSelektionService.selektion.forEach(kantenSelektion => {
      const selectedFeature = new Feature(new LineString(kantenSelektion.kante.geometry.coordinates));
      selectedFeature.setId(kantenSelektion.kante.id);
      selectedFeature.set(FeatureProperties.ZWEISEITIG_PROPERTY_NAME, kantenSelektion.kante.zweiseitig);
      if (kantenSelektion.kante.zweiseitig && AttributGruppe.isSeitenbezogen(this.activeAttributGruppe)) {
        if (kantenSelektion.istSeiteSelektiert(KantenSeite.LINKS)) {
          const clonedFeature = this.cloneFeatureForSeitenbezug(selectedFeature, KantenSeite.LINKS);
          this.selektionLayerSource.addFeature(clonedFeature);
        }
        if (kantenSelektion.istSeiteSelektiert(KantenSeite.RECHTS)) {
          const clonedFeature = this.cloneFeatureForSeitenbezug(selectedFeature, KantenSeite.RECHTS);
          this.selektionLayerSource.addFeature(clonedFeature);
        }
      } else {
        this.selektionLayerSource.addFeature(selectedFeature);
      }
    });
  };

  private cloneFeatureForSeitenbezug(feature: Feature<Geometry>, kantenSeite: KantenSeite): Feature<Geometry> {
    const clonedFeature = feature.clone();
    clonedFeature.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, Number(feature.getId()));
    clonedFeature.set(FeatureProperties.SEITE_PROPERTY_NAME, kantenSeite);
    clonedFeature.setId(feature.getId() + kantenSeite);
    return clonedFeature;
  }

  private getStyleFunctionForKantenLayer(
    featureColor: Color
  ): (feature: FeatureLike, resolution: number) => Style | Style[] {
    return (feature: FeatureLike, resolution: number): Style | Style[] => {
      const kanteSelektiert = this.kantenSelektionService.isSelektiert(this.getKanteIdFromFeature(feature));
      const kantenSeiteSelektiert = this.kantenSelektionService.isSelektiert(
        this.getKanteIdFromFeature(feature),
        feature.get(FeatureProperties.SEITE_PROPERTY_NAME)
      );
      const editorIsResponsibleForKantenPresentation =
        AttributGruppe.isLinearReferenziert(this.activeAttributGruppe) && kanteSelektiert;
      if (kantenSeiteSelektiert || editorIsResponsibleForKantenPresentation) {
        return new Style();
      }
      if (
        AttributGruppe.isSeitenbezogen(this.activeAttributGruppe) &&
        feature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)
      ) {
        return [this.getStyleWithSeitenbezug(feature, resolution, featureColor)];
      } else {
        return MapStyles.getDefaultNetzStyleFunction(featureColor)(feature, resolution);
      }
    };
  }

  private getStyleFunctionForSelektionLayer(
    featureColor: Color
  ): (feature: FeatureLike, resolution: number) => Style[] {
    return (feature: FeatureLike, resolution: number): Style[] => {
      const result: Style[] = [];

      // für linear referenzierte Attributgruppen darf das nicht angezeigt werden, weil dann ein anderer Layer übernimmt
      // Pfeile müssen dann direkt in der Attributgruppe dargestellt werden, s. GeschwindigkeitsAttributgruppe
      const withArrow =
        this.activeAttributGruppe === AttributGruppe.FAHRTRICHTUNG ||
        this.activeAttributGruppe === AttributGruppe.VERLAUF;

      if (
        AttributGruppe.isSeitenbezogen(this.activeAttributGruppe) &&
        feature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)
      ) {
        result.push(this.getStyleWithSeitenbezug(feature, resolution, featureColor));
      } else {
        result.push(...MapStyles.getDefaultHighlightStyle(featureColor));
      }

      if (withArrow) {
        result.push(
          MapStyles.createArrowBegleitend(
            (feature.getGeometry() as LineString).getCoordinates(),
            MapStyles.FEATURE_COLOR
          )
        );
      }
      return result;
    };
  }

  private getStyleWithSeitenbezug(
    feature: FeatureLike,
    resolution: number,
    color: Color = MapStyles.FEATURE_COLOR
  ): Style {
    const shiftedSegmentLineString = shiftFeature(feature, resolution, MapStyles.LINE_GAP_FOR_DOUBLE_LINE);
    return new Style({
      geometry: shiftedSegmentLineString,
      stroke: new Stroke({
        color,
        // Strichbreite fuer eine einzelne Linie
        width: MapStyles.LINE_WIDTH_FOR_DOUBLE_LINE,
      }),
    });
  }

  private getKanteIdFromFeature(feature: FeatureLike): number {
    let kanteIdFromFeature: number;
    if (feature.getId() && this.isNumeric(feature.getId())) {
      kanteIdFromFeature = +(feature.getId() as number);
    } else {
      kanteIdFromFeature = +feature.get(FeatureProperties.KANTE_ID_PROPERTY_NAME);
    }
    return kanteIdFromFeature;
  }

  private isNumeric(value: string | number | undefined): boolean {
    if (value) {
      if (typeof value === 'string') {
        return new RegExp('^[0-9]+$').test(value);
      }
      return true;
    }

    return false;
  }
}
