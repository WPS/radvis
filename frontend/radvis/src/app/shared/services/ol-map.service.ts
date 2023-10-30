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

import { Feature, MapBrowserEvent, Overlay } from 'ol';
import { Coordinate } from 'ol/coordinate';
import { FeatureLike } from 'ol/Feature';
import Geometry from 'ol/geom/Geometry';
import Interaction from 'ol/interaction/Interaction';
import BaseLayer from 'ol/layer/Base';
import Layer from 'ol/layer/Layer';
import { Pixel } from 'ol/pixel';
import Source from 'ol/source/Source';
import { Observable } from 'rxjs';
import { LayerQuelle } from 'src/app/shared/models/layer-quelle';
import { LocationSelectEvent } from 'src/app/shared/models/location-select-event';
import { SignaturLegende } from 'src/app/shared/models/signatur-legende';
import { WMSLegende } from 'src/app/shared/models/wms-legende';

export abstract class OlMapService {
  public static LAYER_ID = 'ID';
  public static IS_BBOX_LAYER = 'IS_BBOX_LAYER';

  public abstract addLayer(olLayer: BaseLayer, quelle?: LayerQuelle, legende?: SignaturLegende | WMSLegende): void;

  public abstract addWMSFeatureLayer(
    olLayer: BaseLayer,
    getFeaturesCallback: (coordinate: number[], resolution: number) => Promise<Feature<Geometry>[]>,
    quelle?: LayerQuelle,
    legende?: SignaturLegende | WMSLegende
  ): void;

  public abstract removeLayer(olLayer: BaseLayer): void;

  public abstract addOverlay(olPopup: Overlay): void;

  public abstract removeOverlay(olPopup: Overlay): void;

  public abstract removeInteraction(interaction: Interaction): void;

  public abstract addInteraction(interaction: Interaction): void;

  public abstract getCurrentResolution(): number | undefined;

  public abstract getResolution$(): Observable<number>;

  public abstract click$(): Observable<MapBrowserEvent<UIEvent>>;

  public abstract locationSelected$(): Observable<LocationSelectEvent>;

  public abstract pointerMove$(): Observable<MapBrowserEvent<UIEvent>>;

  public abstract pointerLeave$(): Observable<void>;

  public abstract outsideMapClick$(): Observable<void>;

  public abstract getFeaturesAtPixel(
    pixel: Pixel,
    layerFilter?: (l: Layer<Source>) => boolean,
    hitTolerance?: number
  ): FeatureLike[] | undefined;

  public abstract getFeaturesAtCoordinate(
    coordinate: Coordinate,
    layerFilter?: (l: Layer<Source>) => boolean,
    hitTolerance?: number
  ): FeatureLike[] | undefined;

  public abstract getZoomForResolution(resolution: number): number | undefined;

  public abstract onceOnPostRender(listener: () => void): void;

  public abstract scrollIntoViewByCoordinate(coordinate: Coordinate): void;

  public abstract scrollIntoViewByGeometry(geometry: Geometry): void;

  public abstract setCursor(cssClass: string): void;

  public abstract resetCursor(): void;

  /**
   * Adds or updates Legende for Layer. If Legende is null, removes it
   *
   * @param layer
   * @param legende
   */
  public abstract updateLegende(layer: BaseLayer, legende: SignaturLegende | WMSLegende | null): void;
}
