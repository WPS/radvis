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

import { DefaultRenderComponent, MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { Feature } from 'ol';
import { LineString } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { Subject } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { NetzDetailSelektion } from 'src/app/shared/models/netzdetail-selektion';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { Seitenbezug } from 'src/app/shared/models/seitenbezug';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { NetzDetailsModule } from 'src/app/viewer/netz-details/netz-details.module';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { NetzAusblendenService } from 'src/app/viewer/viewer-shared/services/netz-ausblenden.service';
import { anything, capture, instance, mock, reset, verify, when } from 'ts-mockito';
import { KantenHighlightLayerComponent } from './kanten-highlight-layer.component';

describe('KantenHighlightLayerComponent', () => {
  let component: DefaultRenderComponent<KantenHighlightLayerComponent>;
  let fixture: MockedComponentFixture<KantenHighlightLayerComponent>;
  let olMapService: OlMapService;
  let netzAusblendenService: NetzAusblendenService;
  let featureHighlightService: FeatureHighlightService;
  let layer: VectorLayer;

  const highlightFeatureSubject = new Subject<RadVisFeature>();
  const unhighlightFeatureSubject = new Subject<RadVisFeature>();

  const initialKanteId = 123;
  const initialLinestring = [
    [0, 1],
    [1, 1],
  ];
  let highlightedProperty: string;

  beforeEach(async () => {
    olMapService = mock(OlMapComponent);
    netzAusblendenService = mock(NetzAusblendenService);
    featureHighlightService = mock(FeatureHighlightService);

    when(featureHighlightService.highlightedFeature$).thenReturn(highlightFeatureSubject.asObservable());
    when(featureHighlightService.unhighlightedFeature$).thenReturn(unhighlightFeatureSubject.asObservable());

    return MockBuilder(KantenHighlightLayerComponent, NetzDetailsModule)
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({
        provide: NetzAusblendenService,
        useValue: instance(netzAusblendenService),
      })
      .provide({
        provide: FeatureHighlightService,
        useValue: instance(featureHighlightService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(KantenHighlightLayerComponent, {
      selektion: {
        hauptGeometry: {
          coordinates: initialLinestring,
          type: 'LineString',
        },
        id: initialKanteId,
        seite: null,
      } as NetzDetailSelektion,
      verlauf: false,
    } as any);
    component = fixture.componentInstance;
    fixture.detectChanges();
    layer = capture(olMapService.addLayer).last()[0] as VectorLayer;
    // eslint-disable-next-line @typescript-eslint/dot-notation
    highlightedProperty = fixture.point.componentInstance['HIGHLIGHTED'];
  });

  it('should initialize correct', () => {
    verify(netzAusblendenService.kanteAusblenden(anything())).once();
    expect(capture(netzAusblendenService.kanteAusblenden).last()[0]).toEqual(initialKanteId);
    expect(layer.getSource().getFeatures()).toHaveSize(1);
    expect((layer.getSource().getFeatures()[0] as Feature<LineString>).getGeometry()?.getCoordinates()).toEqual(
      initialLinestring
    );
  });

  describe('einseitige Kante ohne verlauf', () => {
    const currentLinestring = [
      [10, 22],
      [46, 3],
    ];
    const currentKantenId = 53;

    beforeEach(() => {
      reset(netzAusblendenService);
      component.selektion = {
        hauptGeometry: {
          coordinates: currentLinestring,
          type: 'LineString',
        },
        id: currentKantenId,
        seite: null,
      };
      fixture.detectChanges();
    });

    it('should refill vector layer and netz ausblenden', () => {
      verify(netzAusblendenService.kanteAusblenden(anything())).once();
      expect(capture(netzAusblendenService.kanteAusblenden).last()[0]).toEqual(currentKantenId);
      expect(layer.getSource().getFeatures()).toHaveSize(1);
      let feature = layer.getSource().getFeatures()[0] as Feature<LineString>;
      expect(feature.getGeometry()?.getCoordinates()).toEqual(currentLinestring);
      expect(feature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).toBeFalsy();

      reset(netzAusblendenService);
      const updatedLinestring = [...currentLinestring, [3435, 2]];
      component.selektion = {
        hauptGeometry: {
          coordinates: updatedLinestring,
          type: 'LineString',
        },
        id: currentKantenId + 10,
        seite: null,
      };
      fixture.detectChanges();

      verify(netzAusblendenService.kanteEinblenden(anything())).once();
      expect(capture(netzAusblendenService.kanteEinblenden).last()[0]).toEqual(currentKantenId);
      verify(netzAusblendenService.kanteAusblenden(anything())).once();
      expect(capture(netzAusblendenService.kanteAusblenden).last()[0]).toEqual(currentKantenId + 10);
      expect(layer.getSource().getFeatures()).toHaveSize(1);
      feature = layer.getSource().getFeatures()[0] as Feature<LineString>;
      expect(feature.getGeometry()?.getCoordinates()).toEqual(updatedLinestring);
      expect(feature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).toBeFalsy();
    });

    it('should not change on verlauf toggled', () => {
      reset(netzAusblendenService);
      component.verlauf = true;
      fixture.detectChanges();
      expect(layer.getSource().getFeatures()).toHaveSize(1);
      verify(netzAusblendenService.kanteEinblenden(anything())).never();
      verify(netzAusblendenService.kanteAusblenden(anything())).never();
      expect((layer.getSource().getFeatures()[0] as Feature<LineString>).getGeometry()?.getCoordinates()).toEqual(
        currentLinestring
      );
    });
  });

  describe('einseitige Kante mit verlauf', () => {
    const currentLinestring = [
      [10, 22],
      [46, 3],
    ];
    const verlauf = [
      [444, 34],
      [54, 3],
    ];
    const currentKantenId = 53;

    beforeEach(() => {
      reset(netzAusblendenService);
      component.selektion = {
        hauptGeometry: {
          coordinates: currentLinestring,
          type: 'LineString',
        },
        verlaufLinks: {
          coordinates: verlauf,
          type: 'LineString',
        },
        verlaufRechts: {
          coordinates: verlauf,
          type: 'LineString',
        },
        id: currentKantenId,
        seite: null,
      };
      fixture.detectChanges();
    });

    it('should use hauptGeometry', () => {
      verify(netzAusblendenService.kanteAusblenden(anything())).once();
      expect(capture(netzAusblendenService.kanteAusblenden).last()[0]).toEqual(currentKantenId);
      expect(layer.getSource().getFeatures()).toHaveSize(1);
      const feature = layer.getSource().getFeatures()[0] as Feature<LineString>;
      expect(feature.getGeometry()?.getCoordinates()).toEqual(currentLinestring);
      expect(feature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).toBeFalsy();
    });

    it('should display verlauf on verlauf toggled', () => {
      reset(netzAusblendenService);
      component.verlauf = true;
      fixture.detectChanges();
      expect(layer.getSource().getFeatures()).toHaveSize(1);
      verify(netzAusblendenService.kanteEinblenden(anything())).never();
      verify(netzAusblendenService.kanteAusblenden(anything())).never();
      expect((layer.getSource().getFeatures()[0] as Feature<LineString>).getGeometry()?.getCoordinates()).toEqual(
        verlauf
      );
      expect(
        (layer.getSource().getFeatures()[0] as Feature<LineString>).get(FeatureProperties.SEITE_PROPERTY_NAME)
      ).toBeFalsy();
    });
  });

  describe('zweiseitige Kante ohne verlauf', () => {
    const currentLinestring = [
      [10, 22],
      [46, 3],
    ];
    const currentKantenId = 53;

    beforeEach(() => {
      reset(netzAusblendenService);
      component.selektion = {
        hauptGeometry: {
          coordinates: currentLinestring,
          type: 'LineString',
        },
        seite: Seitenbezug.LINKS,
        id: currentKantenId,
      };
      fixture.detectChanges();
    });

    it('should draw zweiseitig and highlight correct seite', () => {
      verify(netzAusblendenService.kanteAusblenden(anything())).once();
      expect(capture(netzAusblendenService.kanteAusblenden).last()[0]).toEqual(currentKantenId);
      expect(layer.getSource().getFeatures()).toHaveSize(2);

      const linkesFeature = layer
        .getSource()
        .getFeatures()
        .find(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME) === Seitenbezug.LINKS) as Feature<LineString>;
      const rechtesFeature = layer
        .getSource()
        .getFeatures()
        .find(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME) === Seitenbezug.RECHTS) as Feature<LineString>;
      expect(rechtesFeature).toBeTruthy();
      expect(linkesFeature).toBeTruthy();
      expect(rechtesFeature.getGeometry()?.getCoordinates()).toEqual(currentLinestring);
      expect(linkesFeature.getGeometry()?.getCoordinates()).toEqual(currentLinestring);
      expect(rechtesFeature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).toBeTruthy();
      expect(linkesFeature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).toBeTruthy();
      expect(rechtesFeature.get(highlightedProperty)).toBeFalsy();
      expect(linkesFeature.get(highlightedProperty)).toBeTruthy();
    });

    it('should refill vector layer on seite change', () => {
      reset(netzAusblendenService);
      component.selektion = {
        hauptGeometry: {
          coordinates: currentLinestring,
          type: 'LineString',
        },
        seite: Seitenbezug.RECHTS,
        id: currentKantenId,
      };
      fixture.detectChanges();

      verify(netzAusblendenService.kanteAusblenden(anything())).once();
      expect(capture(netzAusblendenService.kanteAusblenden).last()[0]).toEqual(currentKantenId);
      verify(netzAusblendenService.kanteEinblenden(anything())).once();
      expect(capture(netzAusblendenService.kanteEinblenden).last()[0]).toEqual(currentKantenId);
      expect(layer.getSource().getFeatures()).toHaveSize(2);

      const linkesFeature = layer
        .getSource()
        .getFeatures()
        .find(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME) === Seitenbezug.LINKS) as Feature<LineString>;
      const rechtesFeature = layer
        .getSource()
        .getFeatures()
        .find(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME) === Seitenbezug.RECHTS) as Feature<LineString>;
      expect(rechtesFeature).toBeTruthy();
      expect(linkesFeature).toBeTruthy();
      expect(rechtesFeature.getGeometry()?.getCoordinates()).toEqual(currentLinestring);
      expect(linkesFeature.getGeometry()?.getCoordinates()).toEqual(currentLinestring);
      expect(rechtesFeature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).toBeTruthy();
      expect(linkesFeature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).toBeTruthy();
      expect(linkesFeature.get(highlightedProperty)).toBeFalsy();
      expect(rechtesFeature.get(highlightedProperty)).toBeTruthy();
    });

    it('should not change on verlauf toggled', () => {
      reset(netzAusblendenService);
      component.verlauf = true;
      fixture.detectChanges();
      expect(layer.getSource().getFeatures()).toHaveSize(2);
      verify(netzAusblendenService.kanteEinblenden(anything())).never();
      verify(netzAusblendenService.kanteAusblenden(anything())).never();
      expect((layer.getSource().getFeatures()[0] as Feature<LineString>).getGeometry()?.getCoordinates()).toEqual(
        currentLinestring
      );
      expect((layer.getSource().getFeatures()[1] as Feature<LineString>).getGeometry()?.getCoordinates()).toEqual(
        currentLinestring
      );
    });
  });

  describe('zweiseitige Kante mit verlauf', () => {
    const currentLinestring = [
      [10, 22],
      [46, 3],
    ];
    const verlaufLinks = [
      [444, 34],
      [54, 3],
    ];
    const verlaufRechts = [
      [667, 789],
      [988, 34],
    ];
    const currentKantenId = 53;

    beforeEach(() => {
      reset(netzAusblendenService);
      component.selektion = {
        hauptGeometry: {
          coordinates: currentLinestring,
          type: 'LineString',
        },
        verlaufLinks: {
          coordinates: verlaufLinks,
          type: 'LineString',
        },
        verlaufRechts: {
          coordinates: verlaufRechts,
          type: 'LineString',
        },
        id: currentKantenId,
        seite: Seitenbezug.LINKS,
      };
      fixture.detectChanges();
    });

    it('should draw zweiseitig and highlight correct seite, wenn kein verlauf angezeigt', () => {
      verify(netzAusblendenService.kanteAusblenden(anything())).once();
      expect(capture(netzAusblendenService.kanteAusblenden).last()[0]).toEqual(currentKantenId);
      expect(layer.getSource().getFeatures()).toHaveSize(2);

      const linkesFeature = layer
        .getSource()
        .getFeatures()
        .find(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME) === Seitenbezug.LINKS) as Feature<LineString>;
      const rechtesFeature = layer
        .getSource()
        .getFeatures()
        .find(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME) === Seitenbezug.RECHTS) as Feature<LineString>;
      expect(rechtesFeature).toBeTruthy();
      expect(linkesFeature).toBeTruthy();
      expect(rechtesFeature.getGeometry()?.getCoordinates()).toEqual(currentLinestring);
      expect(linkesFeature.getGeometry()?.getCoordinates()).toEqual(currentLinestring);
      expect(rechtesFeature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).toBeTruthy();
      expect(linkesFeature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).toBeTruthy();
      expect(rechtesFeature.get(highlightedProperty)).toBeFalsy();
      expect(linkesFeature.get(highlightedProperty)).toBeTruthy();
    });

    it('should change on verlauf toggled', () => {
      reset(netzAusblendenService);
      component.verlauf = true;
      fixture.detectChanges();
      expect(layer.getSource().getFeatures()).toHaveSize(2);
      verify(netzAusblendenService.kanteEinblenden(anything())).never();
      verify(netzAusblendenService.kanteAusblenden(anything())).never();

      const linkesFeature = layer
        .getSource()
        .getFeatures()
        .find(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME) === Seitenbezug.LINKS) as Feature<LineString>;
      const rechtesFeature = layer
        .getSource()
        .getFeatures()
        .find(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME) === Seitenbezug.RECHTS) as Feature<LineString>;
      expect(rechtesFeature).toBeTruthy();
      expect(linkesFeature).toBeTruthy();
      expect(rechtesFeature.getGeometry()?.getCoordinates()).toEqual(verlaufRechts);
      expect(linkesFeature.getGeometry()?.getCoordinates()).toEqual(verlaufLinks);
      expect(rechtesFeature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).toBeTruthy();
      expect(linkesFeature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).toBeTruthy();
      expect(rechtesFeature.get(highlightedProperty)).toBeFalsy();
      expect(linkesFeature.get(highlightedProperty)).toBeTruthy();
    });
  });
});
