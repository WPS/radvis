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

import { ImportMassnahmenImportUeberpruefenLayerComponent } from './import-massnahmen-import-ueberpruefen-layer.component';
import { MockBuilder, MockedComponentFixture, MockRender, ngMocks } from 'ng-mocks';
import { deepEqual, instance, mock, verify, when } from 'ts-mockito';
import { MassnahmenImportZuordnungenService } from 'src/app/import/massnahmen/services/massnahmen-import-zuordnungen.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { MassnahmenImportZuordnungUeberpruefung } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-ueberpruefung';
import { BehaviorSubject, Subject } from 'rxjs';
import { getDefaultZuordnung } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-ueberpruefung-test-data-provider.spec';
import { MassnahmenImportModule } from 'src/app/import/massnahmen/massnahmen-import.module';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { LocationSelectEvent } from 'src/app/shared/models/location-select-event';
import { Feature } from 'ol';
import Geometry from 'ol/geom/Geometry';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { toRadVisFeatureAttributesFromMap } from 'src/app/shared/models/rad-vis-feature-attributes';
import { Coordinate } from 'ol/coordinate';

/* eslint-disable @typescript-eslint/dot-notation */
describe(ImportMassnahmenImportUeberpruefenLayerComponent.name, () => {
  let component: ImportMassnahmenImportUeberpruefenLayerComponent;
  let fixture: MockedComponentFixture<ImportMassnahmenImportUeberpruefenLayerComponent>;

  let olMapService: OlMapService;
  let massnahmenImportZuordnungenService: MassnahmenImportZuordnungenService;

  let zuordnungenSubject: BehaviorSubject<MassnahmenImportZuordnungUeberpruefung[]>;
  let selektierteZuordnungsIdSubject: Subject<number | undefined>;
  let locationSelectSubject: Subject<LocationSelectEvent>;

  ngMocks.faster();

  beforeAll(() => {
    olMapService = mock(OlMapComponent);
    massnahmenImportZuordnungenService = mock(MassnahmenImportZuordnungenService);

    zuordnungenSubject = new BehaviorSubject<MassnahmenImportZuordnungUeberpruefung[]>([]);
    when(massnahmenImportZuordnungenService.zuordnungen$).thenReturn(zuordnungenSubject.asObservable());
    when(massnahmenImportZuordnungenService.zuordnungen).thenReturn(zuordnungenSubject.value);

    selektierteZuordnungsIdSubject = new BehaviorSubject<number | undefined>(undefined);
    when(massnahmenImportZuordnungenService.selektierteZuordnungsId$).thenReturn(
      selektierteZuordnungsIdSubject.asObservable()
    );

    locationSelectSubject = new Subject<LocationSelectEvent>();
    when(olMapService.locationSelected$()).thenReturn(locationSelectSubject.asObservable());

    return MockBuilder(ImportMassnahmenImportUeberpruefenLayerComponent, MassnahmenImportModule)
      .provide({ provide: OlMapService, useValue: instance(olMapService) })
      .provide({ provide: MassnahmenImportZuordnungenService, useValue: instance(massnahmenImportZuordnungenService) });
  });

  beforeEach(() => {
    fixture = MockRender(ImportMassnahmenImportUeberpruefenLayerComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('with zuordnungen features', () => {
    let zuordnungen: MassnahmenImportZuordnungUeberpruefung[];
    let features: Feature<Geometry>[];

    beforeEach(() => {
      zuordnungen = [
        { ...getDefaultZuordnung(), id: 1, massnahmeKonzeptId: 'a' },
        { ...getDefaultZuordnung(), id: 2, massnahmeKonzeptId: 'b' },
        { ...getDefaultZuordnung(), id: 3, massnahmeKonzeptId: 'c' },
      ] as MassnahmenImportZuordnungUeberpruefung[];

      zuordnungenSubject.next(zuordnungen);

      features = component['vectorSource'].getFeatures();
    });

    it('should add feature for zuordnung', () => {
      // Assert
      expect(features.length).toEqual(zuordnungen.length);

      zuordnungen.forEach((zuordnung, i) => {
        expect(features[i].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureIdField)).toEqual(zuordnung.id);
        expect(features[i].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureStatusField)).toEqual(
          zuordnung.status
        );
      });
    });

    it('should select correct feature on locationSelection', () => {
      // Arrange
      const event = {
        selectedFeatures: [
          {
            attributes: toRadVisFeatureAttributesFromMap([
              [
                ImportMassnahmenImportUeberpruefenLayerComponent.featureIdField,
                features[1].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureIdField),
              ],
            ]),
          } as RadVisFeature,
        ],
        coordinate: [],
      } as LocationSelectEvent;

      // Act
      locationSelectSubject.next(event);

      // Assert
      verify(massnahmenImportZuordnungenService.selektiereZuordnung(zuordnungen[1].id));
      expect().nothing();
    });

    it('should update feature attributes on selected Zuordnung', () => {
      // Act
      selektierteZuordnungsIdSubject.next(zuordnungen[1].id);

      // Assert
      features = component['vectorSource'].getFeatures();
      expect(features[0].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField)).toBeFalse();
      expect(features[1].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField)).toBeTrue();
      expect(features[2].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField)).toBeFalse();
    });

    it('should focus map on Originalgeometrie on selected Zuordnung', () => {
      // Assert
      const originalGeometrie = zuordnungen[1].originalGeometrie;
      when(massnahmenImportZuordnungenService.selektierteZuordnungsOriginalGeometrie).thenReturn(originalGeometrie);

      // Act
      selektierteZuordnungsIdSubject.next(zuordnungen[1].id);

      // Assert
      const exceptedCoordinateToFocusOn = [0, 2] as Coordinate;
      verify(olMapService.scrollIntoViewByCoordinate(deepEqual(exceptedCoordinateToFocusOn))).once();
      expect().nothing();
    });

    it('should update feature attributes on successively selected Zuordnungen', () => {
      // Act
      selektierteZuordnungsIdSubject.next(zuordnungen[1].id);
      selektierteZuordnungsIdSubject.next(zuordnungen[2].id);

      // Assert
      features = component['vectorSource'].getFeatures();
      expect(features[0].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField)).toBeFalse();
      expect(features[1].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField)).toBeFalse();
      expect(features[2].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField)).toBeTrue();
    });

    it('should update feature attributes on deselect', () => {
      // Act
      selektierteZuordnungsIdSubject.next(zuordnungen[1].id);
      selektierteZuordnungsIdSubject.next(undefined);

      // Assert
      features = component['vectorSource'].getFeatures();
      expect(features[0].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField)).toBeFalse();
      expect(features[1].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField)).toBeFalse();
      expect(features[2].get(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField)).toBeFalse();
    });
  });
});
