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
import { Geometry, LineString, MultiLineString, Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { isLineString, isMultiLineString, isPoint } from 'src/app/shared/models/geojson-geometrie';
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
})
export class MassnahmenLayerComponent
  extends AbstractInfrastrukturLayerComponent<MassnahmeListenView>
  implements OnDestroy {
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
      // @ts-ignore
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
      feature = new Feature(new Point((infrastruktur.geometry.coordinates as unknown) as Coordinate));
    } else {
      throw new Error('Geometrie ist weder Punkt noch LineString');
    }
    feature.setId(infrastruktur.id);

    feature.set(AbstractInfrastrukturLayerComponent.BEZEICHNUNG_PROPERTY_NAME, infrastruktur.bezeichnung);

    return [feature];
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
  }

  private showSignatur(signatur: Signatur): void {
    this.signaturVectorSource.clear();

    this.filterService.currentFilteredList
      // Die MassnahmenListView enthält für Massnahmen mit gemischtem Netzbezug (Punkt&Strecke, FeatureCollection) nur
      // den StreckenNetzbezug als LineString/MultiLineString
      .filter(m => isLineString(m.geometry) || isMultiLineString(m.geometry))
      .forEach(massnahme => {
        let feature;
        if (isMultiLineString(massnahme.geometry)) {
          feature = new Feature({ geometry: new MultiLineString(massnahme.geometry.coordinates) });
        } else {
          feature = new Feature({ geometry: new LineString(massnahme.geometry.coordinates) });
        }
        feature.set(
          'Umsetzungsstatus',
          Umsetzungsstatus.options.find(opt => opt.name === massnahme.umsetzungsstatus)?.displayText
        );
        feature.set('sollStandard', SollStandard.options.find(opt => opt.name === massnahme.sollStandard)?.displayText);
        feature.set(
          'Maßnahmenkategorie',
          Massnahmenkategorien.getOberkategorieGewichtet(massnahme.massnahmenkategorien).displayText
        );
        this.signaturVectorSource.addFeature(feature);
      });
    this.signaturVectorSource.changed();

    this.signaturStyleProvider.getStyleInformation(signatur).then(styleInfo => {
      this.signaturLayer.setStyle(styleInfo.styleFunction);
      this.signaturLayer.changed();
    });
  }
}
