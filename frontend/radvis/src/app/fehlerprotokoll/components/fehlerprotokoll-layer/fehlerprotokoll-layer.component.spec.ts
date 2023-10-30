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
import { DatePipe } from '@angular/common';
import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatIcon } from '@angular/material/icon';
import { MatToolbar } from '@angular/material/toolbar';
import { RouterTestingModule } from '@angular/router/testing';
import { MockComponent } from 'ng-mocks';
import { Geometry, LineString } from 'ol/geom';
import * as olProj from 'ol/proj';
import { BehaviorSubject, of } from 'rxjs';
import { FehlerprotokollAuswahlComponent } from 'src/app/fehlerprotokoll/components/fehlerprotokoll-auswahl/fehlerprotokoll-auswahl.component';
import { FehlerprotokollDetailViewComponent } from 'src/app/fehlerprotokoll/components/fehlerprotokoll-detail-view/fehlerprotokoll-detail-view.component';
import { FehlerprotokollTyp } from 'src/app/fehlerprotokoll/models/fehlerprotokoll-typ';
import {
  FehlerprotokollLoader,
  FehlerprotokollSelectionService,
} from 'src/app/fehlerprotokoll/services/fehlerprotokoll-selection.service';
import { FehlerprotokollService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll.service';
import { HintergrundAuswahlComponent } from 'src/app/karte/components/hintergrund-auswahl/hintergrund-auswahl.component';
import { HintergrundLayerComponent } from 'src/app/karte/components/hintergrund-layer/hintergrund-layer.component';
import { KarteButtonComponent } from 'src/app/karte/components/karte-button/karte-button.component';
import { KarteMenuItemComponent } from 'src/app/karte/components/karte-menu-item/karte-menu-item.component';
import { LegendeComponent } from 'src/app/karte/components/legende/legende.component';
import { NetzklassenAuswahlComponent } from 'src/app/karte/components/netzklassen-auswahl/netzklassen-auswahl.component';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { OrtsSucheComponent } from 'src/app/karte/components/orts-suche/orts-suche.component';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { OlPopupComponent } from 'src/app/shared/components/ol-popup/ol-popup.component';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { anything, instance, mock, when } from 'ts-mockito';
import { defaultFehlerpotokoll } from './default-fehlerpotokoll.spec';
import { FehlerprotokollLayerComponent } from './fehlerprotokoll-layer.component';
import { RadVisFeatureAttribut } from 'src/app/shared/models/rad-vis-feature-attribut';

@Component({
  template: '<rad-ol-map><rad-fehlerprotokoll-layer [zIndex]="1"></rad-fehlerprotokoll-layer></rad-ol-map>',
  selector: 'rad-test',
})
class TestWrapperComponent {
  @ViewChild(FehlerprotokollLayerComponent)
  component!: FehlerprotokollLayerComponent;
}

describe(FehlerprotokollLayerComponent.name, () => {
  let component: FehlerprotokollLayerComponent;
  let fixture: ComponentFixture<TestWrapperComponent>;

  let fehlerprotokollSelectionService: FehlerprotokollSelectionService;
  let fehlerprotokollService: FehlerprotokollService;

  let fehlerprotokollLoaderSubject$: BehaviorSubject<FehlerprotokollLoader>;

  beforeEach(async () => {
    fehlerprotokollSelectionService = mock(FehlerprotokollSelectionService);
    fehlerprotokollLoaderSubject$ = new BehaviorSubject<FehlerprotokollLoader>(() => of([]));
    when(fehlerprotokollSelectionService.fehlerprotokollLoader$).thenReturn(fehlerprotokollLoaderSubject$);

    fehlerprotokollService = mock(FehlerprotokollService);
    when(
      fehlerprotokollService.getFehlerprotokolle([FehlerprotokollTyp.DLM_REIMPORT_JOB_MASSNAHMEN], anything())
    ).thenReturn(of([]));

    const featureTogglzService = mock(FeatureTogglzService);
    when(featureTogglzService.fehlerprotokoll).thenReturn(true);

    const mapQueryParamsService = mock(MapQueryParamsService);
    when(mapQueryParamsService.mapQueryParamsSnapshot).thenReturn(
      new MapQueryParams([], [], [0, 1000, 100, 1000], false)
    );

    await TestBed.configureTestingModule({
      declarations: [
        TestWrapperComponent,
        OlMapComponent,
        FehlerprotokollLayerComponent,
        FehlerprotokollDetailViewComponent,
        OlPopupComponent,
        LegendeComponent,
        MockComponent(HintergrundLayerComponent),
        MockComponent(KarteMenuItemComponent),
        MockComponent(OrtsSucheComponent),
        MockComponent(HintergrundAuswahlComponent),
        MockComponent(NetzklassenAuswahlComponent),
        MockComponent(KarteButtonComponent),
        MockComponent(MatIcon),
        MockComponent(FehlerprotokollAuswahlComponent),
        MockComponent(MatToolbar),
      ],
      imports: [RouterTestingModule],
      providers: [
        { provide: MapQueryParamsService, useValue: instance(mapQueryParamsService) },
        { provide: FehlerprotokollSelectionService, useValue: instance(fehlerprotokollSelectionService) },
        { provide: FehlerprotokollService, useValue: instance(fehlerprotokollService) },
        { provide: FeatureTogglzService, useValue: instance(featureTogglzService) },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TestWrapperComponent);
    fixture.detectChanges();
    component = fixture.componentInstance.component;
  });

  describe('select FehlerprotokollTyp', () => {
    it('should fill layer, same entity Id', fakeAsync(() => {
      fehlerprotokollLoaderSubject$.next(() =>
        of([
          { ...defaultFehlerpotokoll, id: 1, fehlerprotokollKlasse: 'Klasse1' },
          { ...defaultFehlerpotokoll, id: 1, fehlerprotokollKlasse: 'Klasse2' },
        ])
      );

      tick();

      triggerLoadFeatures();

      expect(component['iconVectorSource'].getFeatures().length).toBe(2);
    }));
  });

  describe('Detail View', () => {
    it('should show correct values', fakeAsync(() => {
      fehlerprotokollLoaderSubject$.next(() => of([defaultFehlerpotokoll]));

      tick();

      triggerLoadFeatures();

      const feature = component['iconVectorSource'].getFeatures()[0];
      const id: number | string | undefined = feature.getId();
      const selectedFeature = RadVisFeature.ofAttributesMap(
        id ? +id : null,
        feature.getProperties(),
        FehlerprotokollLayerComponent.LAYER_ID,
        feature.getGeometry() as Geometry
      );

      component.onSelect(selectedFeature);
      fixture.detectChanges();

      expect(
        ((fixture.debugElement.nativeElement as HTMLElement).querySelector('.popup-titel') as HTMLElement).innerText
      ).toEqual(defaultFehlerpotokoll.titel);
      const expectedDate = new DatePipe('en-EN').transform(defaultFehlerpotokoll.datum, 'dd.MM.yy HH:mm') as string;
      expect(
        ((fixture.debugElement.nativeElement as HTMLElement).querySelector('.date') as HTMLElement).innerText
      ).toEqual(expectedDate);
      expect(
        ((fixture.debugElement.nativeElement as HTMLElement).querySelector('div.fehlerprotokoll-text') as HTMLElement)
          .innerText
      ).toEqual(defaultFehlerpotokoll.beschreibung);
      expect(
        ((fixture.debugElement.nativeElement as HTMLElement).querySelector(
          'a.fehlerprotokoll-text'
        ) as HTMLElement).attributes.getNamedItem('href')?.value
      ).toEqual(defaultFehlerpotokoll.entityLink);
      expect(component['geometryVectorSource'].getFeatures().length).toBe(1);
      expect((component['geometryVectorSource'].getFeatures()[0].getGeometry() as LineString).getCoordinates()).toEqual(
        defaultFehlerpotokoll.originalGeometry.coordinates
      );
      expect(
        component['iconVectorSource'].getFeatures()[0].get(FehlerprotokollLayerComponent['HIGHLIGHTED_PROPERTY_NAME'])
      ).toBeTrue();
    }));

    it('should unhighlight on close', fakeAsync(() => {
      fehlerprotokollLoaderSubject$.next(() => of([defaultFehlerpotokoll]));

      tick();
      triggerLoadFeatures();

      const feature = component['iconVectorSource'].getFeatures()[0];
      const id: number | string | undefined = feature.getId();
      const selectedFeature = RadVisFeature.ofAttributesMap(
        id ? +id : null,
        feature.getProperties(),
        FehlerprotokollLayerComponent.LAYER_ID,
        feature.getGeometry() as Geometry
      );

      component.onSelect(selectedFeature);
      fixture.detectChanges();

      component.onCloseDetailView();

      expect(component['geometryVectorSource'].getFeatures().length).toBe(0);
      expect(
        component['iconVectorSource'].getFeatures()[0].get(FehlerprotokollLayerComponent['HIGHLIGHTED_PROPERTY_NAME'])
      ).toBeFalse();
      expect(component.selectedFeature).toBeNull();
    }));
  });

  describe('Fehlerprotokoll ID Extrahierung', () => {
    it('should throw if attribute for id is missing', () => {
      expect(() => {
        FehlerprotokollLayerComponent.extractProtokollId([
          { key: 'foo', value: 'bar', linearReferenziert: false } as RadVisFeatureAttribut,
        ]);
      }).toThrow();
    });

    it('should throw if attribute contains bad value', () => {
      expect(() => {
        FehlerprotokollLayerComponent.extractProtokollId([
          { key: 'foo', value: 'bar', linearReferenziert: false } as RadVisFeatureAttribut,
          {
            key: FehlerprotokollLayerComponent['PROTOKOLL_ID_PROPERTYNAME'],
            value: undefined,
            linearReferenziert: false,
          } as RadVisFeatureAttribut,
        ]);
      }).toThrow();
    });

    it('should extract from attribute', () => {
      expect(
        FehlerprotokollLayerComponent.extractProtokollId([
          { key: 'foo', value: 'bar', linearReferenziert: false } as RadVisFeatureAttribut,
          {
            key: FehlerprotokollLayerComponent['PROTOKOLL_ID_PROPERTYNAME'],
            value: 'abc/1',
            linearReferenziert: false,
          } as RadVisFeatureAttribut,
        ])
      ).toBe('abc/1');
    });
  });

  const triggerLoadFeatures = (): void => {
    component['iconVectorSource'].loadFeatures([0, 1000, 100, 1000], 0, olProj.get('EPSG:25832'));
  };
});
