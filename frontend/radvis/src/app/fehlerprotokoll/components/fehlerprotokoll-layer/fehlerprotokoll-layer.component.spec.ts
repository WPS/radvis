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

import { DatePipe } from '@angular/common';
import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatIcon } from '@angular/material/icon';
import { MatToolbar } from '@angular/material/toolbar';
import { RouterTestingModule } from '@angular/router/testing';
import { MockComponent } from 'ng-mocks';
import { MatomoTracker } from 'ngx-matomo-client';
import { LineString } from 'ol/geom';
import * as olProj from 'ol/proj';
import { of } from 'rxjs';
import { FehlerprotokollAuswahlComponent } from 'src/app/fehlerprotokoll/components/fehlerprotokoll-auswahl/fehlerprotokoll-auswahl.component';
import { FehlerprotokollDetailViewComponent } from 'src/app/fehlerprotokoll/components/fehlerprotokoll-detail-view/fehlerprotokoll-detail-view.component';
import { defaultFehlerpotokoll } from 'src/app/fehlerprotokoll/models/fehlerpotokoll-test-data-provider.spec';
import { DLM_REIMPORT_JOB_MASSNAHMEN_FEHLERPROTOKOLL } from 'src/app/fehlerprotokoll/models/fehlerpotokoll-typ-test-data-provider.spec';
import { FehlerprotokollSelectionService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll-selection.service';
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
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { anything, instance, mock, when } from 'ts-mockito';
import { FehlerprotokollLayerComponent } from './fehlerprotokoll-layer.component';

@Component({
  template: '<rad-ol-map><rad-fehlerprotokoll-layer [zIndex]="1"></rad-fehlerprotokoll-layer></rad-ol-map>',
  selector: 'rad-test',
  standalone: false,
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

  beforeEach(async () => {
    fehlerprotokollSelectionService = new FehlerprotokollSelectionService();

    fehlerprotokollService = mock(FehlerprotokollService);
    when(
      fehlerprotokollService.getFehlerprotokolle([DLM_REIMPORT_JOB_MASSNAHMEN_FEHLERPROTOKOLL], anything())
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
        { provide: FehlerprotokollSelectionService, useValue: fehlerprotokollSelectionService },
        { provide: FehlerprotokollService, useValue: instance(fehlerprotokollService) },
        { provide: FeatureTogglzService, useValue: instance(featureTogglzService) },
        {
          provide: MatomoTracker,
          useValue: instance(mock(MatomoTracker)),
        },
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
      fehlerprotokollSelectionService.fehlerprotokollLoader$.next(() =>
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
      fehlerprotokollSelectionService.fehlerprotokollLoader$.next(() => of([defaultFehlerpotokoll]));

      tick();

      triggerLoadFeatures();

      const feature = component['iconVectorSource'].getFeatures()[0];
      const id: number | string | undefined = feature.getId();
      const selectedFeature = RadVisFeature.ofAttributesMap(
        id ? +id : null,
        feature.getProperties(),
        FehlerprotokollLayerComponent.LAYER_ID,
        feature.getGeometry()!
      );

      component.onSelect(selectedFeature, defaultFehlerpotokoll.iconPosition.coordinates[0]);
      fixture.detectChanges();

      expect(
        ((fixture.debugElement.nativeElement as HTMLElement).querySelector('.popup-titel') as unknown as HTMLDivElement)
          .innerText
      ).toEqual(defaultFehlerpotokoll.titel);
      const expectedDate = new DatePipe('en-EN').transform(defaultFehlerpotokoll.datum, 'dd.MM.yy HH:mm')!;
      expect(
        ((fixture.debugElement.nativeElement as HTMLElement).querySelector('.date') as unknown as HTMLDivElement)
          .innerText
      ).toEqual(expectedDate);
      expect(
        (
          (fixture.debugElement.nativeElement as HTMLElement).querySelector(
            'div.fehlerprotokoll-text'
          ) as unknown as HTMLDivElement
        ).innerText
      ).toEqual(defaultFehlerpotokoll.beschreibung);
      expect(
        (fixture.debugElement.nativeElement as HTMLElement)
          .querySelector('a.fehlerprotokoll-text')!
          .attributes.getNamedItem('href')?.value
      ).toEqual(defaultFehlerpotokoll.entityLink);
      expect(component['geometryVectorSource'].getFeatures().length).toBe(1);
      expect((component['geometryVectorSource'].getFeatures()[0].getGeometry() as LineString).getCoordinates()).toEqual(
        (defaultFehlerpotokoll.originalGeometry as LineStringGeojson).coordinates
      );
      expect(
        component['iconVectorSource'].getFeatures()[0].get(FehlerprotokollLayerComponent['HIGHLIGHTED_PROPERTY_NAME'])
      ).toBeTrue();
    }));

    it('should unhighlight on close', fakeAsync(() => {
      fehlerprotokollSelectionService.fehlerprotokollLoader$.next(() => of([defaultFehlerpotokoll]));

      tick();
      triggerLoadFeatures();

      const feature = component['iconVectorSource'].getFeatures()[0];
      const id: number | string | undefined = feature.getId();
      const selectedFeature = RadVisFeature.ofAttributesMap(
        id ? +id : null,
        feature.getProperties(),
        FehlerprotokollLayerComponent.LAYER_ID,
        feature.getGeometry()!
      );

      component.onSelect(selectedFeature, [0, 0]);
      fixture.detectChanges();

      component.onCloseDetailView();

      expect(component['geometryVectorSource'].getFeatures().length).toBe(0);
      expect(
        component['iconVectorSource'].getFeatures()[0].get(FehlerprotokollLayerComponent['HIGHLIGHTED_PROPERTY_NAME'])
      ).toBeFalse();
      expect(component.selectedFeature).toBeNull();
    }));
  });

  it('should keep selection after reload (e.g. on extent/selected typ changed)', () => {
    fehlerprotokollSelectionService.fehlerprotokollLoader$.next(() =>
      of([
        { ...defaultFehlerpotokoll, id: 1, fehlerprotokollKlasse: 'Klasse1' },
        { ...defaultFehlerpotokoll, id: 2, fehlerprotokollKlasse: 'Klasse1' },
      ])
    );
    component['iconVectorSource'].loadFeatures([0, 1000, 100, 1000], 0, olProj.get('EPSG:25832')!);
    expect(component['iconVectorSource'].getFeatures().length).toBe(2);

    const feature = component['iconVectorSource'].getFeatures()[0];
    const id: number | string | undefined = feature.getId();
    const selectedFeature = RadVisFeature.ofAttributesMap(
      id ? +id : null,
      feature.getProperties(),
      FehlerprotokollLayerComponent.LAYER_ID,
      feature.getGeometry()!
    );

    component.onSelect(selectedFeature, [0, 0]);
    fixture.detectChanges();

    expect(
      component['iconVectorSource'].getFeatures()[0].get(FehlerprotokollLayerComponent.HIGHLIGHTED_PROPERTY_NAME)
    ).toBe(true);

    component['iconVectorSource'].loadFeatures([100, 1000, 200, 1000], 0, olProj.get('EPSG:25832')!);

    expect(
      component['iconVectorSource'].getFeatures()[0].get(FehlerprotokollLayerComponent.HIGHLIGHTED_PROPERTY_NAME)
    ).toBe(true);

    component.onCloseDetailView();
    expect(
      component['iconVectorSource'].getFeatures()[0].get(FehlerprotokollLayerComponent.HIGHLIGHTED_PROPERTY_NAME)
    ).toBe(false);

    triggerLoadFeatures();

    expect(
      component['iconVectorSource'].getFeatures()[0].get(FehlerprotokollLayerComponent.HIGHLIGHTED_PROPERTY_NAME)
    ).toBe(false);
  });

  const triggerLoadFeatures = (): void => {
    component['iconVectorSource'].loadFeatures([0, 1000, 100, 1000], 0, olProj.get('EPSG:25832')!);
  };
});
