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
import { Coordinate } from 'ol/coordinate';
import { FeatureLike } from 'ol/Feature';
import { LineString, Point } from 'ol/geom';
import GeometryType from 'ol/geom/GeometryType';
import { Draw, Modify } from 'ol/interaction';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { RegularShape, Stroke, Style } from 'ol/style';
import CircleStyle from 'ol/style/Circle';
import Fill from 'ol/style/Fill';
import TextStyle from 'ol/style/Text';
import { BedienhinweisService } from 'src/app/shared/services/bedienhinweis.service';
import { ColorToCssPipe } from 'src/app/shared/components/color-to-css.pipe';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { messwerkzeugZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { NetzbezugAuswahlModusService } from 'src/app/shared/services/netzbezug-auswahl-modus.service';

@Component({
  selector: 'rad-measure-distance-layer',
  templateUrl: './measure-distance-layer.component.html',
  styleUrls: ['./measure-distance-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeasureDistanceLayerComponent implements OnDestroy {
  private featureSource: VectorSource = new VectorSource();
  private featureLayer: VectorLayer;
  private drawInteraction: Draw;
  private modifyInteraction: Modify;

  private lineStyle: Style = new Style({
    stroke: new Stroke({
      color: ColorToCssPipe.convertToCss(MapStyles.FEATURE_SELECT_COLOR),
      lineDash: [10, 5],
      width: 3,
    }),
  });

  private totalLengthLabelStyle: Style = new Style({
    text: new TextStyle({
      font: '12px sans-serif',
      fill: new Fill({
        color: 'rgba(255, 255, 255, 1)',
      }),
      backgroundFill: new Fill({
        color: 'rgba(0, 0, 0, 0.8)',
      }),
      padding: [5, 5, 5, 5],
      textBaseline: 'bottom',
      offsetY: -15,
    }),
    image: new RegularShape({
      radius: 8,
      points: 3,
      angle: Math.PI,
      displacement: [0, 6],
      fill: new Fill({
        color: 'rgba(0, 0, 0, 0.8)',
      }),
    }),
  });

  private segmentLengthLabelStyle: Style = new Style({
    text: new TextStyle({
      font: '12px sans-serif',
      fill: new Fill({
        color: 'rgba(255, 255, 255, 1)',
      }),
      backgroundFill: new Fill({
        color: 'rgba(0, 0, 0, 0.6)',
      }),
      padding: [5, 5, 5, 5],
      textBaseline: 'bottom',
      offsetY: -12,
    }),
    image: new RegularShape({
      radius: 6,
      points: 3,
      angle: Math.PI,
      displacement: [0, 4],
      fill: new Fill({
        color: 'rgba(0, 0, 0, 0.6)',
      }),
    }),
  });

  private modifyStyle = new Style({
    image: new CircleStyle({
      radius: 5,
      stroke: new Stroke({
        color: 'rgba(0, 0, 0, 0.8)',
      }),
      fill: new Fill({
        color: 'rgba(0, 0, 0, 0.6)',
      }),
    }),
  });

  private readonly DRAW_BEDIEN_HINWEIS =
    'Auf die Karte klicken, um Streckenabschnitt hinzuzufügen.\nDurch Doppelklick die Strecke abschließen.';
  private readonly MODIFY_BEDIEN_HINWEIS =
    'Auf einen Streckenabschnitt klicken und ziehen, um Stützpunkt hinzuzufügen.\nAuf die Karte klicken, um neue Strecke zu beginnen.';

  constructor(
    private olMapService: OlMapService,
    private netzbezugAuswahlModusService: NetzbezugAuswahlModusService,
    private bedienhinweisService: BedienhinweisService
  ) {
    this.netzbezugAuswahlModusService.startNetzbezugAuswahl(false);
    this.bedienhinweisService.showBedienhinweis(this.DRAW_BEDIEN_HINWEIS);

    this.drawInteraction = new Draw({
      source: this.featureSource,
      type: GeometryType.LINE_STRING,
      style: (feature: FeatureLike): Style[] => this.styleFunction(feature),
    });
    this.drawInteraction.on('drawstart', () => {
      this.modifyInteraction.setActive(false);
      this.bedienhinweisService.showBedienhinweis(this.DRAW_BEDIEN_HINWEIS);
    });
    this.drawInteraction.on('drawend', () => {
      this.modifyInteraction.setActive(true);
      this.bedienhinweisService.showBedienhinweis(this.MODIFY_BEDIEN_HINWEIS);
    });

    this.modifyInteraction = new Modify({ source: this.featureSource, style: this.modifyStyle });

    this.featureLayer = new VectorLayer({
      source: this.featureSource,
      zIndex: messwerkzeugZIndex,
      style: (feature: FeatureLike): Style[] => this.styleFunction(feature),
    });

    this.olMapService.addInteraction(this.drawInteraction);
    this.olMapService.addInteraction(this.modifyInteraction);
    this.olMapService.addLayer(this.featureLayer);
  }

  ngOnDestroy(): void {
    this.olMapService.removeInteraction(this.drawInteraction);
    this.olMapService.removeInteraction(this.modifyInteraction);
    this.olMapService.removeLayer(this.featureLayer);
    this.netzbezugAuswahlModusService.stopNetzbezugAuswahl();
    this.bedienhinweisService.hideBedienhinweis();
  }

  private styleFunction(feature: FeatureLike): Style[] {
    if (!(feature.getGeometry() instanceof LineString)) {
      // Beim ersten Klick der Fall, da gibt es nur einen Punkt, aber keinen LineString
      return [];
    }

    const line = feature.getGeometry() as LineString;
    const lastPoint = new Point(line.getLastCoordinate());
    const styles = [this.lineStyle];

    line.forEachSegment((a: Coordinate, b: Coordinate) => {
      const segment = new LineString([a, b]);
      const segmentPoint = new Point(segment.getCoordinateAt(0.5));
      const segmentStyle = this.segmentLengthLabelStyle.clone();
      segmentStyle.setGeometry(segmentPoint);
      segmentStyle.getText().setText(this.getLengthOfLine(segment));
      styles.push(segmentStyle);
    });

    this.totalLengthLabelStyle.setGeometry(lastPoint);
    this.totalLengthLabelStyle.getText().setText('Gesamtlänge:\n' + this.getLengthOfLine(line));
    styles.push(this.totalLengthLabelStyle);

    return styles;
  }

  private getLengthOfLine(line: LineString): string {
    let length = line.getLength();
    let unit = 'm';

    if (length > 1000) {
      // Stelle Länge als km statt m dar
      length /= 1000;
      unit = 'km';
    }

    return (
      length.toLocaleString('de', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      }) +
      ' ' +
      unit
    );
  }
}
