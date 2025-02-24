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

import { fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, ActivatedRouteSnapshot } from '@angular/router';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { MapBrowserEvent } from 'ol';
import { Coordinate } from 'ol/coordinate';
import { BehaviorSubject, Subject } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { BedienhinweisService } from 'src/app/shared/services/bedienhinweis.service';
import { defaultBenutzer } from 'src/app/shared/models/benutzer-test-data-provider.spec';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { AnpassungswunschModule } from 'src/app/viewer/anpassungswunsch/anpassungswunsch.module';
import { Anpassungswunsch } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch';
import { AnpassungswunschKategorie } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-kategorie';
import { AnpassungswunschStatus } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-status';
import { SaveAnpassungswunschCommand } from 'src/app/viewer/anpassungswunsch/models/save-anpassungswunsch-command';
import { AnpassungswunschService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch.service';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { PositionSelektionControlComponent } from 'src/app/viewer/viewer-shared/components/position-selektion-control/position-selektion-control.component';
import { NetzbezugAuswahlModusService } from 'src/app/shared/services/netzbezug-auswahl-modus.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { AnpassungenEditorComponent } from './anpassungen-editor.component';

describe(AnpassungenEditorComponent.name, () => {
  let component: AnpassungenEditorComponent;
  let fixture: MockedComponentFixture<AnpassungenEditorComponent>;
  let activatedRoute: ActivatedRoute;
  let data$: BehaviorSubject<any>;
  let bedienhinweisService: BedienhinweisService;
  let olMapService: OlMapService;
  let olMapClick$: Subject<MapBrowserEvent<UIEvent>>;
  let anpassungswunschService: AnpassungswunschService;
  let netzbezugAuswahlModusService: NetzbezugAuswahlModusService;
  let nutzerDetailService: BenutzerDetailsService;

  beforeEach(() => {
    activatedRoute = mock(ActivatedRoute);
    data$ = new BehaviorSubject<any>({
      isCreator: true,
    });
    when(activatedRoute.data).thenReturn(data$);
    when(activatedRoute.snapshot).thenReturn({
      data: data$.value,
    } as ActivatedRouteSnapshot);
    bedienhinweisService = mock(BedienhinweisService);
    olMapService = mock(OlMapComponent);
    olMapClick$ = new Subject<MapBrowserEvent<UIEvent>>();
    when(olMapService.click$()).thenReturn(olMapClick$);
    anpassungswunschService = mock(AnpassungswunschService);
    netzbezugAuswahlModusService = mock(ViewerComponent);
    nutzerDetailService = mock(BenutzerDetailsService);

    return MockBuilder(AnpassungenEditorComponent, AnpassungswunschModule)
      .keep(ReactiveFormsModule)
      .keep(PositionSelektionControlComponent)
      .provide({
        provide: ActivatedRoute,
        useValue: instance(activatedRoute),
      })
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({
        provide: AnpassungswunschService,
        useValue: instance(anpassungswunschService),
      })
      .provide({
        provide: NetzbezugAuswahlModusService,
        useValue: instance(netzbezugAuswahlModusService),
      })
      .provide({
        provide: BenutzerDetailsService,
        useValue: instance(nutzerDetailService),
      })
      .provide({
        provide: BedienhinweisService,
        useValue: instance(bedienhinweisService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(AnpassungenEditorComponent);
    component = fixture.point.componentInstance;
  });

  describe('as Creator', () => {
    it('should show Bedienhinweis', () => {
      verify(bedienhinweisService.showBedienhinweis(PositionSelektionControlComponent.BEDIENHINWEIS)).once();
      expect().nothing();
    });

    it('should alter cursor', () => {
      verify(olMapService.setCursor('point-selection-cursor')).once();
      expect().nothing();
    });

    it('should start netzbezug Auswahl', () => {
      verify(netzbezugAuswahlModusService.startNetzbezugAuswahl(anything())).once();
      expect().nothing();
    });

    it('should fill form with default values', () => {
      component.formGroup.patchValue({
        status: AnpassungswunschStatus.ERLEDIGT,
        beschreibung: 'Blubb',
        erstellung: new Date().toISOString(),
        aenderung: new Date().toISOString(),
      });
      component.formGroup.markAsDirty();
      expect(component.isDirty).toBeTrue();

      nextData(true);

      expect(component.isDirty).toBeFalse();
      expect(component.formGroup.value).toEqual({
        status: AnpassungswunschStatus.OFFEN,
        beschreibung: null,
        erstellung: null,
        aenderung: null,
        benutzerLetzteAenderung: null,
        kategorie: null,
        verantwortlicheOrganisation: null,
        geometrie: null,
      });
    });

    it('should use correct save command', fakeAsync(() => {
      component.formGroup.patchValue({
        status: AnpassungswunschStatus.ERLEDIGT,
        beschreibung: 'Blubb',
        erstellung: new Date().toISOString(),
        aenderung: new Date().toISOString(),
        kategorie: AnpassungswunschKategorie.RADVIS,
        verantwortlicheOrganisation: defaultOrganisation,
      });
      component.formGroup.markAsDirty();
      when(anpassungswunschService.createAnpassungswunsch(anything())).thenResolve(null as unknown as Anpassungswunsch);
      const coordinate: Coordinate = [0, 10];
      olMapClick$.next({ coordinate } as unknown as MapBrowserEvent<UIEvent>);

      component.onSave();

      verify(anpassungswunschService.createAnpassungswunsch(anything())).once();
      const expectedCommand: SaveAnpassungswunschCommand = {
        beschreibung: 'Blubb',
        geometrie: {
          coordinates: coordinate,
          type: 'Point',
        },
        status: AnpassungswunschStatus.ERLEDIGT,
        kategorie: AnpassungswunschKategorie.RADVIS,
        verantwortlicheOrganisation: defaultOrganisation.id,
      };
      expect(capture(anpassungswunschService.createAnpassungswunsch).last()[0]).toEqual(expectedCommand);
    }));

    it('should reset to default values', () => {
      component.formGroup.patchValue({
        status: AnpassungswunschStatus.ERLEDIGT,
        beschreibung: 'Blubb',
        erstellung: new Date().toISOString(),
        aenderung: new Date().toISOString(),
      });
      component.formGroup.markAsDirty();
      expect(component.isDirty).toBeTrue();

      component.onReset();

      expect(component.isDirty).toBeFalse();
      expect(component.formGroup.value).toEqual({
        status: AnpassungswunschStatus.OFFEN,
        beschreibung: null,
        erstellung: null,
        aenderung: null,
        benutzerLetzteAenderung: null,
        kategorie: null,
        verantwortlicheOrganisation: null,
        geometrie: null,
      });
    });

    it('should reset position on subsequent click', () => {
      let coordinate: Coordinate = [0, 10];
      olMapClick$.next({ coordinate } as unknown as MapBrowserEvent<UIEvent>);

      expect(component.selectedCoordinate).toEqual(coordinate);

      coordinate = [10, 100];
      olMapClick$.next({ coordinate } as unknown as MapBrowserEvent<UIEvent>);

      expect(component.selectedCoordinate).toEqual(coordinate);
    });

    it('should not save when no position selected', () => {
      component.formGroup.patchValue({
        status: AnpassungswunschStatus.ERLEDIGT,
        beschreibung: 'Blubb',
        erstellung: new Date().toISOString(),
        aenderung: new Date().toISOString(),
      });
      component.formGroup.markAsDirty();

      component.onSave();

      verify(anpassungswunschService.createAnpassungswunsch(anything())).never();
      expect().nothing();
    });
  });

  describe('as Editor', () => {
    const defaultValue: Anpassungswunsch = {
      aenderung: new Date(2022, 10, 2).toISOString(),
      beschreibung: 'Meine Beschreibung',
      erstellung: new Date(2022, 5, 3).toISOString(),
      geometrie: {
        coordinates: [0, 15],
        type: 'Point',
      },
      id: 2354,
      status: AnpassungswunschStatus.KORRIGIERT,
      benutzerLetzteAenderung: defaultBenutzer,
      kategorie: AnpassungswunschKategorie.DLM,
      verantwortlicheOrganisation: null,
      canEdit: false,
      basiertAufKonsistenzregelVerletzung: false,
    };

    describe('canEdit false', () => {
      beforeEach(() => {
        defaultValue.canEdit = false;
        component.formGroup.markAsDirty();
        nextData(false, defaultValue);
      });
      it('should disable form if canEdit false', () => {
        defaultValue.canEdit = false;
        expect(component.formGroup.disabled).toBeTrue();
      });
    });

    describe('canEdit true', () => {
      beforeEach(() => {
        defaultValue.canEdit = true;
        component.formGroup.markAsDirty();
        nextData(false, defaultValue);
      });

      it('should enable form if canEdit true', () => {
        expect(component.formGroup.enabled).toBeTrue();
        expect(component.formGroup.disabled).toBeFalse();
      });

      it('should use correct save command', fakeAsync(() => {
        component.formGroup.patchValue({
          status: AnpassungswunschStatus.NACHBEARBEITUNG,
          beschreibung: 'Hallo',
          kategorie: AnpassungswunschKategorie.TOUBIZ,
          geometrie: [10, 15],
        });
        component.formGroup.markAsDirty();
        when(anpassungswunschService.updateAnpassungswunsch(anything(), anything())).thenResolve(
          null as unknown as Anpassungswunsch
        );

        component.onSave();

        verify(anpassungswunschService.updateAnpassungswunsch(anything(), anything())).once();
        expect(capture(anpassungswunschService.updateAnpassungswunsch).last()[0]).toBe(defaultValue.id);
        const command: SaveAnpassungswunschCommand = {
          beschreibung: 'Hallo',
          status: AnpassungswunschStatus.NACHBEARBEITUNG,
          kategorie: AnpassungswunschKategorie.TOUBIZ,
          verantwortlicheOrganisation: undefined,
          geometrie: { coordinates: [10, 15], type: 'Point' },
        };
        expect(capture(anpassungswunschService.updateAnpassungswunsch).last()[1]).toEqual(command);
      }));

      it('should fill form', () => {
        expect(component.formGroup.value).toEqual({
          aenderung: new Date(2022, 10, 2).toISOString(),
          beschreibung: 'Meine Beschreibung',
          erstellung: new Date(2022, 5, 3).toISOString(),
          status: AnpassungswunschStatus.KORRIGIERT,
          benutzerLetzteAenderung: defaultBenutzer,
          kategorie: AnpassungswunschKategorie.DLM,
          verantwortlicheOrganisation: null,
          geometrie: [0, 15],
        });
        expect(component.isDirty).toBeFalse();
      });

      it('should reset correct after save', fakeAsync(() => {
        when(anpassungswunschService.updateAnpassungswunsch(anything(), anything())).thenResolve({
          ...defaultValue,
          status: AnpassungswunschStatus.NACHBEARBEITUNG,
          beschreibung: 'Hallo',
          kategorie: AnpassungswunschKategorie.RADVIS,
        });

        component.formGroup.patchValue({
          status: AnpassungswunschStatus.NACHBEARBEITUNG,
          beschreibung: 'Hallo',
          kategorie: AnpassungswunschKategorie.RADVIS,
        });
        component.formGroup.markAsDirty();

        component.onSave();
        tick();

        component.formGroup.patchValue({
          status: AnpassungswunschStatus.KORRIGIERT,
        });

        component.onReset();
        expect(component.formGroup.value).toEqual({
          aenderung: new Date(2022, 10, 2).toISOString(),
          erstellung: new Date(2022, 5, 3).toISOString(),
          benutzerLetzteAenderung: defaultBenutzer,
          verantwortlicheOrganisation: null,
          status: AnpassungswunschStatus.NACHBEARBEITUNG,
          beschreibung: 'Hallo',
          kategorie: AnpassungswunschKategorie.RADVIS,
          geometrie: [0, 15],
        });
      }));

      it('should reset to previous values', () => {
        component.formGroup.patchValue({
          status: AnpassungswunschStatus.NACHBEARBEITUNG,
          beschreibung: 'Hallo',
          kategorie: AnpassungswunschKategorie.RADVIS,
        });
        component.formGroup.markAsDirty();

        component.onReset();

        expect(component.formGroup.value).toEqual({
          aenderung: new Date(2022, 10, 2).toISOString(),
          beschreibung: 'Meine Beschreibung',
          erstellung: new Date(2022, 5, 3).toISOString(),
          status: AnpassungswunschStatus.KORRIGIERT,
          benutzerLetzteAenderung: defaultBenutzer,
          kategorie: AnpassungswunschKategorie.DLM,
          verantwortlicheOrganisation: null,
          geometrie: [0, 15],
        });
        expect(component.isDirty).toBeFalse();
      });
    });
  });

  const nextData = (isCreator: boolean, anpassungswunsch?: Anpassungswunsch): void => {
    data$.next({
      isCreator,
      anpassungswunsch,
    });
    when(activatedRoute.snapshot).thenReturn({
      data: data$.value,
    } as ActivatedRouteSnapshot);
  };
});
