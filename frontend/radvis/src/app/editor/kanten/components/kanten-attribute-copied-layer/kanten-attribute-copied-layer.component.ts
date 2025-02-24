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
import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { Feature } from 'ol';
import { LineString, Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-kanten-attribute-copied-layer',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KantenAttributeCopiedLayerComponent implements OnChanges, OnDestroy, OnInit {
  @Input()
  copiedKante!: number;

  private kanteVectorSource = new VectorSource<LineString>();
  private kanteLayer: VectorLayer;

  private symbolVectorSource = new VectorSource<Point>();
  private symbolLayer: VectorLayer;

  constructor(
    private netzService: NetzService,
    private netzAusblendenService: NetzAusblendenService,
    private olMapService: OlMapService
  ) {
    this.kanteLayer = new VectorLayer({
      source: this.kanteVectorSource,
      style: MapStyles.getDefaultNetzStyleFunction(),
    });
    this.symbolLayer = new VectorLayer({
      source: this.symbolVectorSource,
      style: MapStyles.getInfrastrukturIconStyle('icon-attribute-copied.svg', false),
    });
  }

  ngOnInit(): void {
    this.olMapService.addLayer(this.kanteLayer);
    this.olMapService.addLayer(this.symbolLayer);
  }

  ngOnChanges(changes: SimpleChanges): void {
    invariant(this.copiedKante);
    if (changes.copiedKante.previousValue) {
      this.netzAusblendenService.kanteEinblenden(changes.copiedKante.previousValue);
    }

    this.netzService.getKanteForEdit(this.copiedKante).then(kante => {
      this.netzAusblendenService.kanteAusblenden(this.copiedKante);

      const kanteLineString = new LineString(kante.geometry.coordinates);

      this.kanteVectorSource.clear(true);
      this.kanteVectorSource.addFeature(new Feature(kanteLineString));
      this.kanteVectorSource.changed();

      this.symbolVectorSource.clear(true);
      const midpoint = kanteLineString.getFlatMidpoint();
      this.symbolVectorSource.addFeature(new Feature(new Point(midpoint)));
      this.symbolVectorSource.changed();
    });
  }

  ngOnDestroy(): void {
    this.netzAusblendenService.kanteEinblenden(this.copiedKante);
    this.olMapService.removeLayer(this.kanteLayer);
    this.olMapService.removeLayer(this.symbolLayer);
  }
}
