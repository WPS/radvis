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

import { FeatureLike } from 'ol/Feature';
import { Color } from 'ol/color';
import { ColorLike } from 'ol/colorlike';
import { Coordinate } from 'ol/coordinate';
import { Point } from 'ol/geom';
import GeometryType from 'ol/geom/GeometryType';
import { Icon } from 'ol/style';
import Circle from 'ol/style/Circle';
import Fill from 'ol/style/Fill';
import IconOrigin from 'ol/style/IconOrigin';
import Stroke from 'ol/style/Stroke';
import Style from 'ol/style/Style';
import Text from 'ol/style/Text';
import invariant from 'tiny-invariant';

export class MapStyles {
  public static DEFAULT_MIN_ZOOM_VALUE = 14.72;

  public static RESOLUTION_SMALL = 5;
  public static RESOLUTION_MEDIUM = 20;

  public static LINE_WIDTH_THIN = 1;
  public static LINE_WIDTH_MEDIUM = 3;
  public static LINE_WIDTH_THICK = 5;
  public static LINE_WIDTH_VERY_THICK = 13;

  public static LINE_WIDTH_FOR_DOUBLE_LINE = 4;
  public static LINE_GAP_FOR_DOUBLE_LINE = 4;

  public static FEATURE_COLOR: Color = [28, 91, 154, 1];
  public static FEATURE_COLOR_LIGHTER: Color = [139, 199, 248, 1];
  public static FEATURE_COLOR_TRANSPARENT: Color = [28, 91, 154, 0.4];
  public static FEATURE_HOVER_COLOR: Color = [234, 131, 169, 1];
  public static FEATURE_SELECT_COLOR: Color = [216, 27, 96, 1];
  public static FEATURE_SELECT_COLOR_TRANSPARENT: Color = [216, 27, 96, 0.4];
  public static ORIGINALGEOMETRIE_TRANSPARENT: Color = [255, 158, 13, 0.6];
  public static FEATURE_MODIFY_COLOR: Color = [0, 153, 255, 1];
  public static HOEHENPROFIL_HOVER_COLOR: Color = [0, 153, 255, 1];
  public static VALID_INPUT_COLOR: Color = [0, 128, 0, 1];
  public static FREMDNETZ_COLOR: Color = [67, 167, 8, 1];
  public static FREMDNETZ_COLOR_LIGHTER: Color = [157, 214, 122, 1];
  public static FAHRRADROUTEN_GEOMETRIE_COLOR: Color = [126, 0, 69, 1];
  public static FEHLERPROTOKOLL_COLOR: Color = [235, 147, 80, 0.8];

  public static VERLAUF_LINKS: Color = [80, 100, 240, 1];
  public static VERLAUF_RECHTS: Color = [100, 240, 80, 1];

  public static INFRASTRUKTUR_ICON_HIGHLIGHT_COLOR: Color = [229, 0, 125];
  public static INFRASTRUKTUR_ICON_COLOR: Color = [126, 0, 69, 1];

  public static getDefaultNetzStyleFunction(
    color: Color | ColorLike = MapStyles.FEATURE_COLOR
  ): (feature: FeatureLike, resolution: number) => Style {
    return (feature: FeatureLike, resolution: number): Style => {
      if (resolution < MapStyles.RESOLUTION_SMALL) {
        if (feature.getGeometry()?.getType() === GeometryType.POINT) {
          return MapStyles.defaultPointStyleLarge(color);
        }
        return new Style({
          stroke: MapStyles.strokeThick(color),
        });
      }

      if (resolution < MapStyles.RESOLUTION_MEDIUM) {
        return new Style({
          stroke: MapStyles.strokeMedium(color),
        });
      }

      return new Style({
        stroke: MapStyles.strokeThin(color),
      });
    };
  }

  public static circleWithFill(radius: number, color: Color | ColorLike = MapStyles.FEATURE_COLOR): Circle {
    return new Circle({
      radius,
      stroke: MapStyles.circleStroke(color),
      fill: new Fill({ color }),
    });
  }

  public static circleWithFillWithoutOutline(
    radius: number,
    color: Color | ColorLike = MapStyles.FEATURE_COLOR
  ): Circle {
    return new Circle({
      radius,
      fill: new Fill({ color }),
    });
  }

  public static circle(radius: number, color: Color | ColorLike = MapStyles.FEATURE_COLOR): Circle {
    return new Circle({
      radius,
      stroke: MapStyles.circleStroke(color),
      fill: undefined,
    });
  }

  public static createArrowBegleitend(coordinates: Coordinate[], color: Color | ColorLike): Style {
    if (coordinates.length > 2) {
      coordinates = coordinates.slice(0, 2);
    }
    const [[startX, startY], [endX, endY]] = coordinates;

    const arrowX = (startX + endX) / 2;
    const arrowY = (startY + endY) / 2;
    const arrowRotation = -Math.atan2(endY - startY, endX - startX) + Math.PI / 2;

    return new Style({
      geometry: new Point([arrowX, arrowY]),
      text: new Text({
        text: '↑',
        font: '30px Roboto',
        fill: new Fill({
          color,
        }),
        rotateWithView: false,
        rotation: arrowRotation,
        offsetX: 10,
      }),
    });
  }

  public static getDefaultHighlightStyle(
    color: Color | ColorLike = MapStyles.FEATURE_HOVER_COLOR,
    withArrow: boolean = false,
    coordinates?: Coordinate[]
  ): Style[] {
    const styles: Style[] = [];

    styles.push(
      new Style({
        stroke: MapStyles.strokeThick(color),
        image: MapStyles.circleWithFill(5, color),
        zIndex: 5,
      })
    );

    if (withArrow) {
      invariant(
        coordinates && coordinates.length > 0 && coordinates[0].length > 1,
        'Um Pfeile im Style darzustellen, müssen Koordinaten übergeben werden'
      );
      styles.push(this.createArrowBegleitend(coordinates, MapStyles.FEATURE_COLOR));
    }

    return styles;
  }

  public static getOriginalgeometrieStyle(): Style[] {
    const color: Color | ColorLike = MapStyles.ORIGINALGEOMETRIE_TRANSPARENT;
    const styles: Style[] = [];

    styles.push(
      new Style({
        stroke: MapStyles.strokeVeryThick(color),
        image: MapStyles.circleWithFillWithoutOutline(13, color),
        zIndex: 5,
      })
    );

    return styles;
  }

  public static getModifyPointStyle(): Style {
    return new Style({
      image: new Circle({
        radius: 5,
        fill: new Stroke({
          color: MapStyles.FEATURE_MODIFY_COLOR,
          width: 3,
        }),
      }),
    });
  }

  public static getPositionPointStyle(): Style {
    return new Style({
      image: new Circle({
        radius: 8,
        stroke: MapStyles.strokeMedium(MapStyles.HOEHENPROFIL_HOVER_COLOR),
        fill: new Stroke({
          color: MapStyles.HOEHENPROFIL_HOVER_COLOR,
          width: 3,
        }),
      }),
    });
  }

  public static getInfrastrukturIconStyle(iconFileName: string, highlighted: boolean, color?: Color): Style[] {
    const opacity = highlighted ? 1 : 0.8;
    if (!color || highlighted) {
      color = highlighted ? MapStyles.INFRASTRUKTUR_ICON_HIGHLIGHT_COLOR : MapStyles.INFRASTRUKTUR_ICON_COLOR;
    }

    return [
      new Style({
        image: new Icon({
          anchor: [0, 0],
          src: './assets/map-icon.svg',
          color,
          opacity,
          anchorOrigin: IconOrigin.BOTTOM_LEFT,
        }),
      }),
      new Style({
        image: new Icon({
          anchor: [0, 0],
          src: './assets/map-icon-halo.svg',
          color: [255, 255, 255, 1],
          opacity,
          anchorOrigin: IconOrigin.BOTTOM_LEFT,
        }),
      }),
      new Style({
        image: new Icon({
          anchor: [-0.08, -0.205],
          src: `./assets/${iconFileName}`,
          scale: 0.85,
          color: [255, 255, 255, 1],
          opacity,
          anchorOrigin: IconOrigin.BOTTOM_LEFT,
        }),
      }),
    ];
  }

  public static defaultPointStyleLarge(color: Color | ColorLike): Style {
    return new Style({
      image: MapStyles.circleWithFill(5, color),
    });
  }

  private static strokeVeryThick(color: Color | ColorLike): Stroke {
    return new Stroke({
      width: MapStyles.LINE_WIDTH_VERY_THICK,
      color,
    });
  }

  private static strokeThick(color: Color | ColorLike): Stroke {
    return new Stroke({
      width: MapStyles.LINE_WIDTH_THICK,
      color,
    });
  }

  private static circleStroke(color: Color | ColorLike): Stroke {
    return new Stroke({
      width: MapStyles.LINE_WIDTH_MEDIUM,
      lineCap: 'square',
      color,
    });
  }

  private static strokeMedium(color: Color | ColorLike): Stroke {
    return new Stroke({
      width: MapStyles.LINE_WIDTH_MEDIUM,
      color,
    });
  }

  private static strokeThin(color: Color | ColorLike): Stroke {
    return new Stroke({
      width: MapStyles.LINE_WIDTH_THIN,
      color,
    });
  }
}
