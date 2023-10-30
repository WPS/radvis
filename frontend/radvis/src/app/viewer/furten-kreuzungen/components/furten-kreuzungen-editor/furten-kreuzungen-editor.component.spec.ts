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
import { ActivatedRoute, convertToParamMap, Data, RouterModule } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { KNOTENFORMEN } from 'src/app/shared/models/knotenformen';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { FurtenKreuzungenModule } from 'src/app/viewer/furten-kreuzungen/furten-kreuzungen.module';
import { FurtKreuzung } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung';
import { FurtKreuzungTyp } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-typ';
import { GruenAnforderung } from 'src/app/viewer/furten-kreuzungen/models/gruen-anforderung';
import { Linksabbieger } from 'src/app/viewer/furten-kreuzungen/models/linksabbieger';
import { Rechtsabbieger } from 'src/app/viewer/furten-kreuzungen/models/rechtsabbieger';
import { SaveFurtKreuzungCommand } from 'src/app/viewer/furten-kreuzungen/models/save-furt-kreuzung-command';
import { FurtenKreuzungenService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen.service';
import { defaultNetzbezug } from 'src/app/viewer/viewer-shared/models/netzbezug-test-data-provider.spec';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { FurtenKreuzungenEditorComponent } from './furten-kreuzungen-editor.component';

describe(FurtenKreuzungenEditorComponent.name, () => {
  let component: FurtenKreuzungenEditorComponent;
  let fixture: MockedComponentFixture<FurtenKreuzungenEditorComponent>;
  let data$: BehaviorSubject<Data>;
  let furtKreuzungService: FurtenKreuzungenService;
  const musterloesungOption = { name: 'TEST', displayText: 'Testmusterlösung' };

  beforeEach(() => {
    data$ = new BehaviorSubject<Data>({ isCreator: true });
    furtKreuzungService = mock(FurtenKreuzungenService);
    when(furtKreuzungService.getAllMusterloesungen()).thenResolve([musterloesungOption]);

    return MockBuilder(FurtenKreuzungenEditorComponent, FurtenKreuzungenModule)
      .replace(RouterModule, RouterTestingModule)
      .provide({
        provide: OlMapService,
        useValue: instance(mock(OlMapComponent)),
      })
      .provide({
        provide: ActivatedRoute,
        useValue: {
          data: data$,
          snapshot: {
            paramMap: convertToParamMap({}),
          },
        },
      })
      .provide({
        provide: FurtenKreuzungenService,
        useValue: instance(furtKreuzungService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(FurtenKreuzungenEditorComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('radnetzKomform und Musterlösung', () => {
    beforeEach(() => {
      data$.next({ isCreator: true });
    });

    it('should disable if radNetzKonform change', () => {
      component.formGroup.get('radnetzKonform')?.setValue(true);
      expect(component.formGroup.get('furtKreuzungMusterloesung')?.disabled).toBeFalse();

      component.formGroup.get('radnetzKonform')?.setValue(false);
      expect(component.formGroup.get('furtKreuzungMusterloesung')?.disabled).toBeTrue();
    });

    it('should be null in command if disabled', fakeAsync(() => {
      component.formGroup.patchValue({
        verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
        netzbezug: defaultNetzbezug,
        kommentar: 'test Kommentar',
        radnetzKonform: true,
        knotenForm: 'Überführung',
        typ: FurtKreuzungTyp.FURT,
      });
      component.formGroup.get('radnetzKonform')?.setValue(false);
      component.formGroup.markAsDirty();
      when(furtKreuzungService.createFurtKreuzung(anything())).thenResolve(1);

      component.onSave();
      tick();

      expect(capture(furtKreuzungService.createFurtKreuzung).last()[0].furtKreuzungMusterloesung).toBeNull();
    }));

    it('should be reset if radNetzKonform change to false', () => {
      component.formGroup.get('radnetzKonform')?.setValue(true);
      component.formGroup.get('furtKreuzungMusterloesung')?.setValue(musterloesungOption.name);

      component.formGroup.get('radnetzKonform')?.setValue(false);
      expect(component.formGroup.get('furtKreuzungMusterloesung')?.value).toBeFalsy();
    });
  });

  describe('knotenForm und LSA-Eigenschaften', () => {
    beforeEach(() => {
      data$.next({ isCreator: true });
    });

    it('disable & hide LSA-Eigenschaften initially', () => {
      expect(component.formGroup.get('lichtsignalAnlageEigenschaften')?.disabled).toBeTrue();
      expect(component.isLSAKnotenForm).toBeFalse();
    });

    it('disable & hide LSA-Eigenschaften if KnotenForm is not LSA', () => {
      component.formGroup.get('knotenForm')?.setValue(KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name);
      expect(component.formGroup.get('lichtsignalAnlageEigenschaften')?.disabled).toBeFalse();
      expect(component.isLSAKnotenForm).toBeTrue();

      component.formGroup.get('knotenForm')?.setValue(KNOTENFORMEN.BAUWERK.options[0].name);
      expect(component.formGroup.get('lichtsignalAnlageEigenschaften')?.disabled).toBeTrue();
      expect(component.isLSAKnotenForm).toBeFalse();
    });

    it('should be null in command if disabled', fakeAsync(() => {
      component.formGroup.patchValue({
        verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
        netzbezug: defaultNetzbezug,
        kommentar: 'test Kommentar',
        radnetzKonform: true,
        knotenForm: KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name,
        typ: FurtKreuzungTyp.FURT,
        lichtsignalAnlageEigenschaften: {
          linksabbieger: Linksabbieger.EIGENES_SIGNALISIEREN,
          rechtsabbieger: Rechtsabbieger.RECHTSABBIEGER,
          gruenAnforderung: GruenAnforderung.AUTOMATISCH,
          radAufstellflaeche: false,
          getrenntePhasen: true,
          fahrradSignal: true,
          gruenVorlauf: false,
          vorgezogeneHalteLinie: true,
          umlaufzeit: null,
        },
      });
      component.formGroup.get('knotenForm')?.setValue(KNOTENFORMEN.BAUWERK.options[0].name);
      component.formGroup.markAsDirty();
      when(furtKreuzungService.createFurtKreuzung(anything())).thenResolve(1);

      component.onSave();
      tick();

      expect(capture(furtKreuzungService.createFurtKreuzung).last()[0].lichtsignalAnlageEigenschaften).toBeNull();
    }));

    it('should reset LSA-Eigenschaften if knotenForm changes to non-LSA', () => {
      component.formGroup.get('knotenForm')?.setValue(KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name);
      component.formGroup.get('lichtsignalAnlageEigenschaften')?.setValue({
        linksabbieger: Linksabbieger.EIGENES_SIGNALISIEREN,
        rechtsabbieger: Rechtsabbieger.RECHTSABBIEGER,
        gruenAnforderung: GruenAnforderung.AUTOMATISCH,
        radAufstellflaeche: false,
        getrenntePhasen: true,
        fahrradSignal: true,
        gruenVorlauf: false,
        vorgezogeneHalteLinie: true,
        umlaufzeit: null,
      });

      component.formGroup.get('knotenForm')?.setValue(KNOTENFORMEN.BAUWERK.options[0].name);
      expect(component.formGroup.value.lichtsignalAnlageEigenschaften).toBeUndefined();
      expect(component.formGroup.get('lichtsignalAnlageEigenschaften')?.value).toEqual({
        linksabbieger: null,
        rechtsabbieger: null,
        gruenAnforderung: null,
        radAufstellflaeche: null,
        getrenntePhasen: null,
        fahrradSignal: null,
        gruenVorlauf: null,
        vorgezogeneHalteLinie: null,
        umlaufzeit: null,
      });
    });
  });

  describe('as Creator', () => {
    beforeEach(() => {
      component.formGroup.patchValue({
        verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
      });
      component.formGroup.markAsDirty();
      data$.next({ isCreator: true });
    });

    describe('fillForm', () => {
      it('should reset', () => {
        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.value).toEqual({
          netzbezug: null,
          verantwortlicheOrganisation: null,
          kommentar: null,
          knotenForm: null,
          radnetzKonform: null,
          typ: null,
        });
      });
    });

    describe('onSave', () => {
      it('should create correct command', () => {
        component.formGroup.patchValue({
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
          netzbezug: defaultNetzbezug,
          kommentar: 'test Kommentar',
          radnetzKonform: true,
          knotenForm: 'Überführung',
          typ: FurtKreuzungTyp.FURT,
        });
        component.formGroup.markAsDirty();
        when(furtKreuzungService.createFurtKreuzung(anything())).thenResolve(1);

        component.onSave();

        const createCommand: SaveFurtKreuzungCommand = {
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: 3456,
          kommentar: 'test Kommentar',
          radnetzKonform: true,
          knotenForm: 'Überführung',
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
          lichtsignalAnlageEigenschaften: null,
        };
        verify(furtKreuzungService.createFurtKreuzung(anything())).once();
        expect(capture(furtKreuzungService.createFurtKreuzung).last()[0]).toEqual(createCommand);
      });

      it('should set radNetzKonform=false if control untouched', () => {
        component.formGroup.patchValue({
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
          netzbezug: defaultNetzbezug,
          kommentar: 'test Kommentar',
          knotenForm: 'Überführung',
          typ: FurtKreuzungTyp.FURT,
        });
        component.formGroup.markAsDirty();
        when(furtKreuzungService.createFurtKreuzung(anything())).thenResolve(1);

        component.onSave();

        const createCommand: SaveFurtKreuzungCommand = {
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: 3456,
          kommentar: 'test Kommentar',
          radnetzKonform: false,
          knotenForm: 'Überführung',
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
          lichtsignalAnlageEigenschaften: null,
        };
        verify(furtKreuzungService.createFurtKreuzung(anything())).once();
        expect(capture(furtKreuzungService.createFurtKreuzung).last()[0]).toEqual(createCommand);
      });
    });

    describe('onReset', () => {
      it('should reset', () => {
        component.formGroup.patchValue({
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
          netzbezug: defaultNetzbezug,
        });
        component.formGroup.markAsDirty();

        component.onReset();

        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.value).toEqual({
          netzbezug: null,
          verantwortlicheOrganisation: null,
          kommentar: null,
          knotenForm: null,
          radnetzKonform: null,
          typ: null,
        });
      });
    });
  });

  describe('as Editor', () => {
    const furtKreuzung: FurtKreuzung = {
      netzbezug: defaultNetzbezug,
      verantwortlicheOrganisation: defaultOrganisation,
      version: 2,
      kommentar: 'test Kommentar',
      knotenForm: KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name,
      radnetzKonform: true,
      typ: FurtKreuzungTyp.FURT,
      furtKreuzungMusterloesung: musterloesungOption.name,
      benutzerDarfBearbeiten: true,
      lichtsignalAnlageEigenschaften: {
        linksabbieger: Linksabbieger.EIGENES_SIGNALISIEREN,
        rechtsabbieger: Rechtsabbieger.RECHTSABBIEGER,
        gruenAnforderung: GruenAnforderung.AUTOMATISCH,
        radAufstellflaeche: false,
        getrenntePhasen: true,
        fahrradSignal: true,
        gruenVorlauf: false,
        vorgezogeneHalteLinie: true,
        umlaufzeit: null,
      },
    };

    beforeEach(() => {
      component.formGroup.patchValue({
        verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
      });
      component.formGroup.markAsDirty();

      data$.next({ isCreator: false, furtKreuzung });
    });

    describe('fillForm', () => {
      it('should reset', () => {
        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.value).toEqual({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: defaultOrganisation,
          kommentar: 'test Kommentar',
          knotenForm: KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name,
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: musterloesungOption.name,
          lichtsignalAnlageEigenschaften: {
            linksabbieger: Linksabbieger.EIGENES_SIGNALISIEREN,
            rechtsabbieger: Rechtsabbieger.RECHTSABBIEGER,
            gruenAnforderung: GruenAnforderung.AUTOMATISCH,
            radAufstellflaeche: false,
            getrenntePhasen: true,
            fahrradSignal: true,
            gruenVorlauf: false,
            vorgezogeneHalteLinie: true,
            umlaufzeit: null,
          },
        });
      });
    });

    describe('onSave', () => {
      it('should create correct command', () => {
        component.formGroup.patchValue({
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
        });

        component.formGroup.markAsDirty();
        when(furtKreuzungService.updateFurtKreuzung(anything(), anything())).thenResolve({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 78645 },
          version: 3,
          kommentar: 'test Kommentar',
          knotenForm: 'Überführung',
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
          benutzerDarfBearbeiten: true,
          lichtsignalAnlageEigenschaften: {
            linksabbieger: Linksabbieger.EIGENES_SIGNALISIEREN,
            rechtsabbieger: Rechtsabbieger.RECHTSABBIEGER,
            gruenAnforderung: GruenAnforderung.AUTOMATISCH,
            radAufstellflaeche: false,
            getrenntePhasen: true,
            fahrradSignal: true,
            gruenVorlauf: false,
            vorgezogeneHalteLinie: true,
            umlaufzeit: null,
          },
        });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();

        const updateCommand: SaveFurtKreuzungCommand = {
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: 3456,
          version: 2,
          kommentar: 'test Kommentar',
          knotenForm: KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name,
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: musterloesungOption.name,
          lichtsignalAnlageEigenschaften: {
            linksabbieger: Linksabbieger.EIGENES_SIGNALISIEREN,
            rechtsabbieger: Rechtsabbieger.RECHTSABBIEGER,
            gruenAnforderung: GruenAnforderung.AUTOMATISCH,
            radAufstellflaeche: false,
            getrenntePhasen: true,
            fahrradSignal: true,
            gruenVorlauf: false,
            vorgezogeneHalteLinie: true,
            umlaufzeit: null,
          },
        };
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[0]).toEqual(2345);
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[1]).toEqual(updateCommand);
      });

      it('should reset form', fakeAsync(() => {
        component.formGroup.markAsDirty();
        when(furtKreuzungService.updateFurtKreuzung(anything(), anything())).thenResolve({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 78645 },
          version: 3,
          kommentar: 'test Kommentar',
          knotenForm: 'Überführung',
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
          lichtsignalAnlageEigenschaften: null,
          benutzerDarfBearbeiten: true,
        });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();
        tick();

        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.value).toEqual({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 78645 },
          kommentar: 'test Kommentar',
          knotenForm: 'Überführung',
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
        });
      }));

      it('should use correct version', fakeAsync(() => {
        component.formGroup.markAsDirty();
        when(furtKreuzungService.updateFurtKreuzung(anything(), anything())).thenResolve({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 78645 },
          version: 3,
          kommentar: 'test Kommentar',
          knotenForm: 'Überführung',
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
          lichtsignalAnlageEigenschaften: null,
          benutzerDarfBearbeiten: true,
        });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();
        tick();
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[1].version).toEqual(2);

        component.onSave();
        tick();
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[1].version).toEqual(3);
      }));
    });

    describe('onReset', () => {
      it('should reset form', () => {
        component.formGroup.patchValue({
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
        });

        component.formGroup.markAsDirty();

        component.onReset();

        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.value).toEqual({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: defaultOrganisation,
          kommentar: 'test Kommentar',
          knotenForm: KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name,
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: musterloesungOption.name,
          lichtsignalAnlageEigenschaften: {
            linksabbieger: Linksabbieger.EIGENES_SIGNALISIEREN,
            rechtsabbieger: Rechtsabbieger.RECHTSABBIEGER,
            gruenAnforderung: GruenAnforderung.AUTOMATISCH,
            radAufstellflaeche: false,
            getrenntePhasen: true,
            fahrradSignal: true,
            gruenVorlauf: false,
            vorgezogeneHalteLinie: true,
            umlaufzeit: null,
          },
        });
      });

      it('should reset after previous save', fakeAsync(() => {
        component.formGroup.markAsDirty();
        when(furtKreuzungService.updateFurtKreuzung(anything(), anything())).thenResolve({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 78645 },
          version: 3,
          kommentar: 'test Kommentar',
          knotenForm: 'Überführung',
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
          lichtsignalAnlageEigenschaften: null,
          benutzerDarfBearbeiten: true,
        });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();
        tick();

        component.formGroup.patchValue({
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
        });

        component.formGroup.markAsDirty();

        component.onReset();

        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.value).toEqual({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 78645 },
          kommentar: 'test Kommentar',
          knotenForm: 'Überführung',
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
        });
      }));
    });
  });
});
