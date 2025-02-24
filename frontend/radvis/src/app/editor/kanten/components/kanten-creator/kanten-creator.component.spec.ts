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

import { ChangeDetectorRef } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MockBuilder, MockRender } from 'ng-mocks';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { EditorModule } from 'src/app/editor/editor.module';
import { KantenCreatorComponent } from 'src/app/editor/kanten/components/kanten-creator/kanten-creator.component';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NotifyGeometryChangedService } from 'src/app/editor/kanten/services/notify-geometry-changed.service';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { CreateAnpassungswunschRouteProvider } from 'src/app/shared/services/create-anpassungswunsch-route.provider';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';
import { anyString, anything, capture, instance, mock, reset, verify, when } from 'ts-mockito';

describe(KantenCreatorComponent.name, () => {
  let component: KantenCreatorComponent;
  let fixture: ComponentFixture<KantenCreatorComponent>;
  let editorRoutingServiceMock: EditorRoutingService;
  let netzServiceMock: NetzService;
  let errorHandlingServiceMock: ErrorHandlingService;
  let notifyUserServiceMock: NotifyUserService;
  let notifyGeometryChangedService: NotifyGeometryChangedService;
  let kanteSelektionService: KantenSelektionService;
  let mapQueryParamsService: MapQueryParamsService;

  beforeEach(() => {
    editorRoutingServiceMock = mock(EditorRoutingService);
    netzServiceMock = mock(NetzService);
    errorHandlingServiceMock = mock(ErrorHandlingService);
    notifyUserServiceMock = mock(NotifyUserService);
    notifyGeometryChangedService = mock(NotifyGeometryChangedService);
    kanteSelektionService = mock(KantenSelektionService);
    mapQueryParamsService = mock(MapQueryParamsService);
    when(mapQueryParamsService.mapQueryParamsSnapshot).thenReturn({ netzklassen: [] } as unknown as MapQueryParams);
    return MockBuilder(KantenCreatorComponent, EditorModule)
      .provide({
        provide: EditorRoutingService,
        useValue: instance(editorRoutingServiceMock),
      })
      .provide({
        provide: NetzService,
        useValue: instance(netzServiceMock),
      })
      .provide({
        provide: ErrorHandlingService,
        useValue: instance(errorHandlingServiceMock),
      })
      .provide({
        provide: NotifyUserService,
        useValue: instance(notifyUserServiceMock),
      })
      .provide({
        provide: NotifyGeometryChangedService,
        useValue: instance(notifyGeometryChangedService),
      })
      .provide({
        provide: MapQueryParamsService,
        useValue: instance(mapQueryParamsService),
      })
      .provide({
        provide: CreateAnpassungswunschRouteProvider,
        useValue: instance(mock(AnpassungenRoutingService)),
      })
      .provide({
        provide: KantenSelektionService,
        useValue: instance(kanteSelektionService),
      });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(KantenCreatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('alle Netzklassen einblenden', () => {
    it('should set currentNetzklassen for guard', () => {
      reset(mapQueryParamsService);
      const previousNetzklassenFilter = [Netzklassefilter.KREISNETZ];
      when(mapQueryParamsService.mapQueryParamsSnapshot).thenReturn({
        netzklassen: previousNetzklassenFilter,
      } as MapQueryParams);
      const fixture2 = MockRender(KantenCreatorComponent, {}, { reset: true });

      expect(fixture2.point.componentInstance.previousNetzklassenfilter).toEqual(previousNetzklassenFilter);
    });

    it('should add all Netzklassen to existing selection', () => {
      reset(mapQueryParamsService);
      when(mapQueryParamsService.mapQueryParamsSnapshot).thenReturn({
        netzklassen: [Netzklassefilter.KREISNETZ],
      } as MapQueryParams);
      MockRender(KantenCreatorComponent, {}, { reset: true });

      verify(mapQueryParamsService.update(anything())).once();
      expect(capture(mapQueryParamsService.update).last()[0]).toEqual({
        netzklassen: Netzklassefilter.getAll(),
      });
    });

    it('should not do anything if all Netzklassen already visible', () => {
      reset(mapQueryParamsService);
      when(mapQueryParamsService.mapQueryParamsSnapshot).thenReturn({
        netzklassen: Netzklassefilter.getAll(),
      } as MapQueryParams);
      MockRender(KantenCreatorComponent, {}, { reset: true });

      verify(mapQueryParamsService.update(anything())).never();
      expect().nothing();
    });
  });

  describe('selection', () => {
    it('should toggle "ausgewählt" and "Bitte auswählen" correctly', () => {
      expect(getVonKnotenText(fixture)).toEqual('Bitte auswählen');
      expect(getBisKnotenText(fixture)).toEqual('Bitte auswählen');

      component.onSelectVonKnoten('1');
      fixture.debugElement.injector.get<ChangeDetectorRef>(ChangeDetectorRef).detectChanges();
      expect(getVonKnotenText(fixture)).toEqual('Ausgewählt');
      expect(getBisKnotenText(fixture)).toEqual('Bitte auswählen');

      component.onSelectBisKnoten('2');
      fixture.debugElement.injector.get<ChangeDetectorRef>(ChangeDetectorRef).detectChanges();
      expect(getVonKnotenText(fixture)).toEqual('Ausgewählt');
      expect(getBisKnotenText(fixture)).toEqual('Ausgewählt');

      component.onSelectBisKnoten(undefined);
      component.onSelectVonKnoten(undefined);
      fixture.debugElement.injector.get<ChangeDetectorRef>(ChangeDetectorRef).detectChanges();
      expect(getVonKnotenText(fixture)).toEqual('Bitte auswählen');
      expect(getBisKnotenText(fixture)).toEqual('Bitte auswählen');
    });
  });

  describe('save', () => {
    it('should toggle validation message correctly', () => {
      expect(getValidationMessage(fixture)).toBeUndefined();

      component.onSelectVonKnoten('1');
      component.onSelectBisKnoten(undefined);
      component.onSave();
      fixture.debugElement.injector.get<ChangeDetectorRef>(ChangeDetectorRef).detectChanges();
      expect(getValidationMessage(fixture)?.trim()).toEqual('Bitte wählen Sie genau zwei Knoten aus.');

      component.onSelectBisKnoten('2');
      when(netzServiceMock.createKanteZwischenKnoten(anything())).thenResolve({ id: 66 } as Kante);
      component.onSave();
      fixture.debugElement.injector.get<ChangeDetectorRef>(ChangeDetectorRef).detectChanges();
      expect(getValidationMessage(fixture)).toBeUndefined();
    });

    it('should send creation command, show message and route', fakeAsync(() => {
      component.onSelectVonKnoten('1');
      component.onSelectBisKnoten('2');
      when(netzServiceMock.createKanteZwischenKnoten(anything())).thenResolve({ id: 66 } as Kante);

      component.onSave();
      tick();

      verify(netzServiceMock.createKanteZwischenKnoten(anything())).once();
      verify(notifyUserServiceMock.inform(anyString())).once();
      verify(editorRoutingServiceMock.toKantenEditor()).once();
      verify(notifyGeometryChangedService.notify()).once();
      verify(kanteSelektionService.select(66, false)).once();
      expect().nothing();
    }));
  });
});

function getVonKnotenText(fixture: ComponentFixture<KantenCreatorComponent>): string {
  return fixture.debugElement.queryAll(By.css('.knoten-info > span.italic'))[0].nativeElement.textContent.trim();
}

function getBisKnotenText(fixture: ComponentFixture<KantenCreatorComponent>): string {
  return fixture.debugElement.queryAll(By.css('.knoten-info > span.italic'))[1].nativeElement.textContent.trim();
}

function getValidationMessage(fixture: ComponentFixture<KantenCreatorComponent>): string | undefined {
  return fixture.debugElement.queryAll(By.css('.fehlertext'))[0]?.nativeElement.textContent.trim();
}
