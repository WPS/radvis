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

/* eslint-disable @typescript-eslint/dot-notation */
import { DefaultRenderComponent, MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { Feature } from 'ol';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Style } from 'ol/style';
import { BehaviorSubject, of } from 'rxjs';
import { ImportNetzklasseAbbildungBearbeitenLayerComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-abbildung-bearbeiten-layer/import-netzklasse-abbildung-bearbeiten-layer.component';
import { ManuellerImportModule } from 'src/app/editor/manueller-import/manueller-import.module';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { anything, instance, mock, when } from 'ts-mockito';

describe(ImportNetzklasseAbbildungBearbeitenLayerComponent.name, () => {
  describe('static private functions', () => {
    describe('Correct Color Is Chosen', () => {
      it('Should choose Insert Style', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['selectFeatureColor'](false, true)).toBe(
          ImportNetzklasseAbbildungBearbeitenLayerComponent['COLOR_NETZKLASSE_INSERTED']
        );
      });

      it('Should choose Equal Style', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['selectFeatureColor'](true, true)).toBe(
          ImportNetzklasseAbbildungBearbeitenLayerComponent['COLOR_NETZKLASSE_ALREADYPRESENT']
        );
      });

      it('Should choose Delete Style', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['selectFeatureColor'](true, false)).toBe(
          ImportNetzklasseAbbildungBearbeitenLayerComponent['COLOR_NETZKLASSE_DELETED']
        );
      });

      it('Should choose No Style', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['selectFeatureColor'](false, false)).toBeUndefined();
      });
    });

    describe('Is Feature hidden', () => {
      it('Should hide if already present and low zoom', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['isFeatureHidden'](false, false, 1)).toBeTrue();
      });

      it('Should not hide if already present and high zoom', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['isFeatureHidden'](false, false, 50)).toBeFalse();
      });

      it('Should hide if both not present and low zoom', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['isFeatureHidden'](true, true, 1)).toBeTrue();
      });

      it('Should not hide if both not present and high zoom', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['isFeatureHidden'](true, true, 50)).toBeFalse();
      });

      it('Should hide if insert and low zoom', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['isFeatureHidden'](false, true, 1)).toBeFalse();
      });

      it('Should hide if insert and high zoom', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['isFeatureHidden'](false, true, 50)).toBeFalse();
      });

      it('Should hide if delete and low zoom', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['isFeatureHidden'](true, false, 1)).toBeFalse();
      });

      it('Should hide if delete and high zoom', () => {
        expect(ImportNetzklasseAbbildungBearbeitenLayerComponent['isFeatureHidden'](true, false, 50)).toBeFalse();
      });
    });
  });

  describe('component', () => {
    let component: ImportNetzklasseAbbildungBearbeitenLayerComponent;
    let fixture: MockedComponentFixture<ImportNetzklasseAbbildungBearbeitenLayerComponent>;
    let olMapService: OlMapComponent;
    const resolution$: BehaviorSubject<number> = new BehaviorSubject<number>(0);

    beforeEach(() => {
      olMapService = mock(OlMapComponent);
      when(olMapService.click$).thenReturn(() => of());
      when(olMapService.getResolution$).thenReturn(() => resolution$.asObservable());
      when(olMapService.getZoomForResolution(anything())).thenReturn(20);
      return MockBuilder(ImportNetzklasseAbbildungBearbeitenLayerComponent, ManuellerImportModule).provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      });
    });

    beforeEach(() => {
      const alleFeatures = {
        type: 'FeatureCollection',
        features: [],
      } as GeoJSONFeatureCollection;
      fixture = MockRender(ImportNetzklasseAbbildungBearbeitenLayerComponent, {
        alleFeatures,
      } as DefaultRenderComponent<ImportNetzklasseAbbildungBearbeitenLayerComponent>);
      component = fixture.point.componentInstance;
      component.kanteIdsMitNetzklasse = [];
      fixture.detectChanges();
    });

    it('it should choose correct Style', () => {
      const feature1 = new Feature();
      feature1.setId(1);
      feature1.set('hasNetzklasse', false);

      const feature2 = new Feature();
      feature2.setId(2);
      feature2.set('hasNetzklasse', true);

      const feature3 = new Feature();
      feature3.setId(3);
      feature3.set('hasNetzklasse', true);

      const feature4 = new Feature();
      feature4.setId(4);
      feature4.set('hasNetzklasse', false);

      component.kanteIdsMitNetzklasse = [1, 2];

      const styleFeature1 = component['olLayer'].getStyleFunction()?.(feature1, 20) as Style;
      expect(styleFeature1.getStroke().getColor()).toEqual(
        ImportNetzklasseAbbildungBearbeitenLayerComponent['COLOR_NETZKLASSE_INSERTED']
      );
      const styleFeature2 = component['olLayer'].getStyleFunction()?.(feature2, 20) as Style;
      expect(styleFeature2.getStroke().getColor()).toEqual(
        ImportNetzklasseAbbildungBearbeitenLayerComponent['COLOR_NETZKLASSE_ALREADYPRESENT']
      );
      const styleFeature3 = component['olLayer'].getStyleFunction()?.(feature3, 20) as Style;
      expect(styleFeature3.getStroke().getColor()).toEqual(
        ImportNetzklasseAbbildungBearbeitenLayerComponent['COLOR_NETZKLASSE_DELETED']
      );
      const styleFeature4 = component['olLayer'].getStyleFunction()?.(feature4, 20) as Style;
      expect(styleFeature4.getStroke().getColor()).toEqual(MapStyles.FEATURE_COLOR);
    });

    it('should emit korrekt info about feature-visibilty', () => {
      spyOn(component.featuresWithUnchangedNetzklasseVisible, 'emit');
      when(olMapService.getZoomForResolution(10)).thenReturn(Netzklassefilter.NICHT_KLASSIFIZIERT.minZoom + 1);
      resolution$.next(10);
      expect(component.featuresWithUnchangedNetzklasseVisible.emit).toHaveBeenCalledWith(true);

      when(olMapService.getZoomForResolution(30)).thenReturn(Netzklassefilter.NICHT_KLASSIFIZIERT.minZoom - 1);
      resolution$.next(30);
      expect(component.featuresWithUnchangedNetzklasseVisible.emit).toHaveBeenCalledWith(false);
    });
  });
});
