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

import { ChangeDetectionStrategy, Component, EventEmitter, NgZone, OnDestroy, Output } from '@angular/core';
import { Feature } from 'ol';
import { Coordinate } from 'ol/coordinate';
import GeoJSON from 'ol/format/GeoJSON';
import { Point } from 'ol/geom';
import Geometry from 'ol/geom/Geometry';
import Select, { SelectEvent } from 'ol/interaction/Select';
import VectorLayer from 'ol/layer/Vector';
import { bbox } from 'ol/loadingstrategy';
import VectorSource from 'ol/source/Vector';
import { Circle, Style } from 'ol/style';
import Fill from 'ol/style/Fill';
import Text from 'ol/style/Text';
import { Subscription } from 'rxjs';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';

@Component({
  selector: 'rad-kanten-creator-knoten-selektion',
  templateUrl: './kanten-creator-knoten-selektion.component.html',
  styleUrls: ['./kanten-creator-knoten-selektion.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KantenCreatorKnotenSelektionComponent implements OnDestroy {
  @Output()
  selectVonKnoten = new EventEmitter<string>();

  @Output()
  selectBisKnoten = new EventEmitter<string | Point>();

  olLayer: VectorLayer;
  selectInteraction: Select;

  private readonly newBKnotenId = 'new';

  private selectedVonKnoten: Feature | null = null;
  private selectedBisKnoten: Feature | null = null;
  private selectedNetzklassen: Netzklassefilter[] = [];
  private selectedNetzklassenSubscription: Subscription;

  constructor(
    private olMapService: OlMapService,
    private zone: NgZone,
    private errorHandlingService: ErrorHandlingService,
    private featureService: NetzausschnittService,
    private mapQueryParamsService: MapQueryParamsService
  ) {
    const vectorSource = new VectorSource({
      format: new GeoJSON(),
      useSpatialIndex: false,
      loader: (extent): void => {
        this.featureService.getKnotenForView(extent, this.selectedNetzklassen).subscribe(
          featureCollection => {
            vectorSource.clear(true);
            vectorSource.addFeatures(new GeoJSON().readFeatures(featureCollection));
            this.zone.run(() => this.refreshSelection());
          },
          error => this.errorHandlingService.handleError(error)
        );
      },
      strategy: bbox,
    });

    this.olLayer = new VectorLayer({
      source: vectorSource,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: new Style({
        image: MapStyles.circleWithFill(5),
      }),
      minZoom: MapStyles.DEFAULT_MIN_ZOOM_VALUE,
      zIndex: EditorLayerZindexConfig.KANTEN_CREATOR_KNOTEN_SELECTION_LAYER,
    });

    this.olMapService.addLayer(this.olLayer);

    this.selectInteraction = new Select({
      hitTolerance: 25,
      layers: [this.olLayer],
      style: this.getKnotenStyle(),
    });
    this.selectInteraction.on('select', this.onSelect);

    // Weil die select-Interaktion nur getriggert wird, wenn es ein selektierbares Feature gibt
    // oder ein Feature bereits selektiert wurde und daher deselektiert werden kann,
    // müssen wir den Handler hier auch bei Click einmal anstoßen. Wenn jm eine schönere
    // Möglichkeit sieht, gerne!
    this.olMapService.click$().subscribe(event => {
      if (this.selectInteraction.getFeatures().getLength() === 0) {
        this.onSelect(new SelectEvent('select', [], [], event));
      }
    });

    this.olMapService.addInteraction(this.selectInteraction);

    this.selectedNetzklassenSubscription = this.mapQueryParamsService.netzklassen$.subscribe(newSelectedNetzklassen => {
      this.selectedNetzklassen = newSelectedNetzklassen;
      this.olLayer.getSource()?.refresh();
    });
  }

  public ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.olMapService.removeInteraction(this.selectInteraction);
    this.selectedNetzklassenSubscription.unsubscribe();
  }

  public resetSelection(): void {
    this.selectedVonKnoten = null;
    this.selectedBisKnoten = null;
    this.selectInteraction.getFeatures().clear();
    this.selectVonKnoten.emit(undefined);
    this.selectBisKnoten.emit(undefined);
  }

  private onSelect = (event: SelectEvent): void => {
    // strg oder cmd
    const modifierKey =
      (event.mapBrowserEvent.originalEvent as PointerEvent).ctrlKey ||
      (event.mapBrowserEvent.originalEvent as PointerEvent).metaKey;
    const shiftKey = (event.mapBrowserEvent.originalEvent as PointerEvent).shiftKey;

    if (modifierKey && shiftKey) {
      this.selectedBisKnoten = this.createOrChangeNewBKnoten(event.mapBrowserEvent.coordinate);
      if (this.selectedVonKnoten) {
        this.selectInteraction.getFeatures().push(this.selectedVonKnoten);
      }
      this.selectInteraction.getFeatures().push(this.selectedBisKnoten);
    } else if (event.selected.length === 1 && event.deselected.length > 0) {
      if (modifierKey) {
        if (event.deselected.some(deselectedFeature => deselectedFeature.getId() === this.selectedBisKnoten?.getId())) {
          this.selectedBisKnoten = null;
        }
        this.removeNewBisKnotenIfExists();
        if (this.selectedVonKnoten) {
          this.selectInteraction.getFeatures().push(this.selectedVonKnoten);
        }
      } else {
        if (event.deselected.some(deselectedFeature => deselectedFeature.getId() === this.selectedVonKnoten?.getId())) {
          this.selectedVonKnoten = null;
        }
        if (this.selectedBisKnoten) {
          this.selectInteraction.getFeatures().push(this.selectedBisKnoten);
        }
      }
    } else if (event.deselected.length > 0) {
      this.selectedVonKnoten = null;
      this.selectedBisKnoten = null;
      this.selectInteraction.getFeatures().clear();
      this.removeNewBisKnotenIfExists();
    }

    if (event.selected.length > 0 && !shiftKey) {
      if (modifierKey) {
        this.selectedBisKnoten = event.selected[0];
      } else {
        this.selectedVonKnoten = event.selected[0];
      }
    }

    this.restyleSelectedKnoten();
    this.zone.run(() => {
      this.selectVonKnoten.emit(this.selectedVonKnoten?.getId() as string);
      const idBisKnoten = this.selectedBisKnoten?.getId() as string;
      this.selectBisKnoten.emit(
        idBisKnoten === this.newBKnotenId ? (this.selectedBisKnoten?.getGeometry() as Point) : idBisKnoten
      );
    });
  };

  private createOrChangeNewBKnoten(coordinate: Coordinate): Feature<Geometry> {
    let feature = this.olLayer.getSource()?.getFeatureById(this.newBKnotenId);
    if (!feature) {
      feature = new Feature<Geometry>(new Point(coordinate));
      feature.setId(this.newBKnotenId);
      this.olLayer.getSource()?.addFeature(feature);
    } else {
      feature.setGeometry(new Point(coordinate));
      feature.changed();
    }
    return feature;
  }

  private removeNewBisKnotenIfExists(): void {
    const newBisKnotenFeature = this.olLayer.getSource()?.getFeatureById('new');
    if (newBisKnotenFeature) {
      this.olLayer.getSource()?.removeFeature(newBisKnotenFeature);
    }
  }

  private restyleSelectedKnoten(): void {
    if (this.selectedVonKnoten) {
      this.selectedVonKnoten.setStyle(this.getKnotenStyle('A'));
    }
    if (this.selectedBisKnoten) {
      this.selectedBisKnoten.setStyle(this.getKnotenStyle('B'));
    }
  }

  private getKnotenStyle(text?: string): Style {
    if (!text) {
      return new Style();
    }

    return new Style({
      image: new Circle({
        radius: MapStyles.POINT_WIDTH_THICK,
        fill: new Fill({
          color: text ? MapStyles.VALID_INPUT_COLOR : MapStyles.FEATURE_HOVER_COLOR,
        }),
      }),
      text: text
        ? new Text({
            text,
            font: 'bold 20px Roboto',
            fill: new Fill({
              color: MapStyles.VALID_INPUT_COLOR,
            }),
            offsetY: -15,
          })
        : undefined,
    });
  }

  private refreshSelection(): void {
    this.selectInteraction.getFeatures().clear();
    this.olLayer
      .getSource()
      ?.getFeatures()
      .forEach(feature => {
        if (feature.getId() === this.selectedVonKnoten?.getId()) {
          this.selectedVonKnoten = feature;
          this.selectInteraction.getFeatures().push(feature);
        }
        if (feature.getId() === this.selectedBisKnoten?.getId()) {
          this.selectedBisKnoten = feature;
          this.selectInteraction.getFeatures().push(feature);
        }
      });
    this.restyleSelectedKnoten();
  }
}
