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
import { Feature } from 'ol';
import { Coordinate } from 'ol/coordinate';
import { Geometry, LineString, Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Style } from 'ol/style';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import {
  geojsonGeometryToFeature,
  isLineString,
  isMultiLineString,
  isPoint,
} from 'src/app/shared/models/geojson-geometrie';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { MassnahmeListenView } from 'src/app/viewer/massnahme/models/massnahme-listen-view';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { Massnahmenkategorien } from 'src/app/viewer/massnahme/models/massnahmenkategorien';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { SignaturStyleProviderService } from 'src/app/viewer/signatur/services/signatur-style-provider.service';
import { AbstractInfrastrukturLayerComponent } from 'src/app/viewer/viewer-shared/components/abstract-infrastruktur-layer.component';
import { infrastrukturSignaturLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';

@Component({
  selector: 'rad-massnahmen-layer',
  templateUrl: './massnahmen-layer.component.html',
  styleUrls: ['./massnahmen-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class MassnahmenLayerComponent
  extends AbstractInfrastrukturLayerComponent<MassnahmeListenView>
  implements OnDestroy
{
  private olLayer: VectorLayer;

  private selectedSignatur$: Observable<Signatur | null>;
  private signaturVectorSource: VectorSource;
  private signaturLayer: VectorLayer;

  constructor(
    private olMapService: OlMapService,
    private notifyUserService: NotifyUserService,
    featureHighlightService: FeatureHighlightService,
    massnahmeFilterService: MassnahmeFilterService,
    massnahmenRoutingService: MassnahmenRoutingService,
    mapQueryParamsService: MapQueryParamsService,
    private signaturStyleProvider: SignaturStyleProviderService
  ) {
    super(massnahmenRoutingService, massnahmeFilterService, featureHighlightService, MASSNAHMEN);

    this.olLayer = this.createLayer(15);
    this.olMapService.addLayer(this.olLayer);

    this.signaturVectorSource = new VectorSource();
    this.signaturLayer = new VectorLayer({
      source: this.signaturVectorSource,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      zIndex: infrastrukturSignaturLayerZIndex,
    });
    this.olMapService.addLayer(this.signaturLayer);

    this.initServiceSubscriptions();

    const currentMapResolution = this.olMapService.getCurrentResolution();
    if (!currentMapResolution) {
      // Karte ist noch nicht initialisiert. Zeige die Meldung, wenn die Karte fertig ist und eine Resolution besitzt.
      this.olMapService
        .getResolution$()
        .pipe(take(1))
        .subscribe(resolution => {
          this.showZoomInNotification(resolution);
        });
    } else {
      this.showZoomInNotification(currentMapResolution);
    }

    this.selectedSignatur$ = mapQueryParamsService.signatur$.pipe(
      map(signatur => {
        if (signatur && signatur.typ === SignaturTyp.NETZ) {
          return null;
        }
        return signatur;
      })
    );
    this.subscriptions.push(
      this.selectedSignatur$.subscribe(signatur => {
        if (signatur) {
          this.showSignatur(signatur);
          this.signaturStyleProvider.getLegendeForSignatur(signatur).then(legende => {
            this.olMapService.updateLegende(this.olLayer, legende);
          });
        } else {
          this.hideSignatur();
          this.olMapService.updateLegende(this.olLayer, null);
        }
      })
    );
    this.subscriptions.push(
      this.filterService.filteredList$.subscribe(() => {
        if (
          mapQueryParamsService.mapQueryParamsSnapshot.signatur &&
          mapQueryParamsService.mapQueryParamsSnapshot.signatur.typ === SignaturTyp.MASSNAHME
        ) {
          this.showSignatur(mapQueryParamsService.mapQueryParamsSnapshot.signatur);
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.olMapService.removeLayer(this.signaturLayer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  protected convertToFeature(infrastruktur: MassnahmeListenView): Feature<Geometry>[] {
    let feature;
    if (!infrastruktur.geometry) {
      return [];
    }

    if (isMultiLineString(infrastruktur.geometry)) {
      feature = new Feature(new Point(new LineString(infrastruktur.geometry.coordinates[0]).getCoordinateAt(0.5)));
    } else if (isLineString(infrastruktur.geometry)) {
      feature = new Feature(new Point(new LineString(infrastruktur.geometry.coordinates).getCoordinateAt(0.5)));
    } else if (isPoint(infrastruktur.geometry)) {
      feature = new Feature(new Point(infrastruktur.geometry.coordinates as unknown as Coordinate));
    } else {
      throw new Error('Geometrie ist weder Punkt noch LineString');
    }
    feature.setId(infrastruktur.id);

    feature.set(AbstractInfrastrukturLayerComponent.BEZEICHNUNG_PROPERTY_NAME, infrastruktur.bezeichnung);

    return [feature];
  }

  protected override addIconColorAttribute(infrastrukturen: MassnahmeListenView[]): void {
    const styleFunction = this.signaturLayer.getStyleFunction();

    for (const infrastruktur of infrastrukturen) {
      const massnahmeFeature = this.signaturVectorSource.getFeatureById(infrastruktur.id);
      const iconFeature = this.vectorSource.getFeatureById(infrastruktur.id);

      // Es gibt viele Fälle in denen der Style zurückgesetzt werden sollte (um sicherheitshalber die neutrale Icon-
      // Farbe statt einer falschen Färbung anzuzeigen). Daher wird hier gemerkt ob das Setzen der Farbe erfolgreich
      // war oder nicht.
      let shouldResetStyle = true;

      // Dummy feature benötigt um aus diesem den Style samt Einfärbung zu ermitteln. Das Feature für das Icon hat nömlich
      // nur eine Punktgeometrie und bekäme daher keinen Stroke-Style.
      if (massnahmeFeature && iconFeature && styleFunction && infrastruktur.geometry) {
        const styleFunctionResult = styleFunction(massnahmeFeature, 0);

        if (styleFunctionResult) {
          const styleForFeature = styleFunctionResult instanceof Style ? [styleFunctionResult] : styleFunctionResult;

          if (styleForFeature.length > 0) {
            const strokeColor = styleForFeature
              .find(style => !!style.getStroke())
              ?.getStroke()
              .getColor();
            iconFeature.set(this.ICON_COLOR_PROPERTY_NAME, strokeColor);
            shouldResetStyle = false;
          }
        }
      }

      if (shouldResetStyle && iconFeature) {
        iconFeature.set(this.ICON_COLOR_PROPERTY_NAME, null);
      }
    }
  }

  protected extractIdFromFeature(hf: RadVisFeature): number {
    return Number(hf.id);
  }

  private showZoomInNotification(resolution: number): void {
    const zoom = this.olMapService.getZoomForResolution(resolution) ?? 0;
    if (zoom <= this.olLayer.getMinZoom()) {
      this.notifyUserService.inform('Bitte zoomen Sie hinein um Maßnahmen anzuzeigen!');
    }
  }

  private hideSignatur(): void {
    this.signaturVectorSource.clear();
    this.signaturVectorSource.changed();
    this.signaturLayer.setStyle(null);
    this.addIconColorAttribute(this.filterService.currentFilteredList);
  }

  private showSignatur(signatur: Signatur): void {
    this.signaturVectorSource.clear();

    this.filterService.currentFilteredList
      // Die MassnahmenListView enthält für Massnahmen mit gemischtem Netzbezug (Punkt&Strecke, FeatureCollection) nur
      // den StreckenNetzbezug als LineString/MultiLineString
      .filter(m => isLineString(m.geometry) || isMultiLineString(m.geometry))
      .forEach(massnahme => {
        const feature = geojsonGeometryToFeature(massnahme.geometry)!;
        feature.set(
          'Umsetzungsstatus',
          Umsetzungsstatus.options.find(opt => opt.name === massnahme.umsetzungsstatus)?.displayText
        );
        feature.set('sollStandard', SollStandard.options.find(opt => opt.name === massnahme.sollStandard)?.displayText);
        feature.set(
          'Maßnahmenkategorie',
          Massnahmenkategorien.getOberkategorieGewichtet(massnahme.massnahmenkategorien).displayText
        );
        // Feature-ID für die Ermittlung der Icon-Farbe anhand der gewählten Signatur benötigt.
        feature.setId(massnahme.id);
        this.signaturVectorSource.addFeature(feature);
      });
    this.signaturVectorSource.changed();

    this.signaturStyleProvider.getStyleInformation(signatur).then(styleInfo => {
      this.signaturLayer.setStyle(styleInfo.styleFunction);
      this.signaturLayer.changed();
      this.addIconColorAttribute(this.filterService.currentFilteredList);
    });
  }
}
