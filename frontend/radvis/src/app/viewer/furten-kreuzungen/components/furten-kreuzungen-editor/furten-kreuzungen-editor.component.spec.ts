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
import { Bauwerksmangel } from 'src/app/shared/models/bauwerksmangel';
import { BauwerksmangelArt } from 'src/app/shared/models/bauwerksmangel-art';
import { KNOTENFORMEN } from 'src/app/shared/models/knotenformen';
import { defaultNetzbezug } from 'src/app/shared/models/netzbezug-test-data-provider.spec';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { QuerungshilfeDetails } from 'src/app/shared/models/querungshilfe-details';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { FurtenKreuzungenModule } from 'src/app/viewer/furten-kreuzungen/furten-kreuzungen.module';
import {
  defaultFurtKreuzung,
  defaultMusterloesungOption,
} from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-test-data-provider.spec';
import { FurtKreuzungTyp } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-typ';
import { GruenAnforderung } from 'src/app/viewer/furten-kreuzungen/models/gruen-anforderung';
import { Linksabbieger } from 'src/app/viewer/furten-kreuzungen/models/linksabbieger';
import { Rechtsabbieger } from 'src/app/viewer/furten-kreuzungen/models/rechtsabbieger';
import { SaveFurtKreuzungCommand } from 'src/app/viewer/furten-kreuzungen/models/save-furt-kreuzung-command';
import { FurtenKreuzungenService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { FurtenKreuzungenEditorComponent } from './furten-kreuzungen-editor.component';

describe(FurtenKreuzungenEditorComponent.name, () => {
  let component: FurtenKreuzungenEditorComponent;
  let fixture: MockedComponentFixture<FurtenKreuzungenEditorComponent>;
  let data$: BehaviorSubject<Data>;
  let furtKreuzungService: FurtenKreuzungenService;

  beforeEach(() => {
    data$ = new BehaviorSubject<Data>({ isCreator: true });
    furtKreuzungService = mock(FurtenKreuzungenService);
    when(furtKreuzungService.getAllMusterloesungen()).thenResolve([defaultMusterloesungOption]);

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
      component.formGroup.controls.radnetzKonform.setValue(true);
      expect(component.formGroup.controls.furtKreuzungMusterloesung.disabled).toBeFalse();

      component.formGroup.controls.radnetzKonform.setValue(false);
      expect(component.formGroup.controls.furtKreuzungMusterloesung.disabled).toBeTrue();
    });

    it('should be null in command if disabled', fakeAsync(() => {
      component.formGroup.patchValue({
        verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
        netzbezug: defaultNetzbezug,
        kommentar: 'test Kommentar',
        radnetzKonform: true,
        shared: {
          knotenForm: 'UEBERFUEHRUNG',
        },
        typ: FurtKreuzungTyp.FURT,
      });
      component.formGroup.controls.radnetzKonform.setValue(false);
      component.formGroup.markAsDirty();
      spyOnProperty(component.formGroup, 'valid').and.returnValue(true);
      when(furtKreuzungService.createFurtKreuzung(anything())).thenResolve(1);

      component.onSave();
      tick();

      verify(furtKreuzungService.createFurtKreuzung(anything())).once();
      expect(capture(furtKreuzungService.createFurtKreuzung).last()[0].furtKreuzungMusterloesung).toBeNull();
    }));

    it('should be reset if radNetzKonform change to false', () => {
      component.formGroup.controls.radnetzKonform.setValue(true);
      component.formGroup.controls.furtKreuzungMusterloesung.setValue(defaultMusterloesungOption.name);

      component.formGroup.controls.radnetzKonform.setValue(false);
      expect(component.formGroup.controls.furtKreuzungMusterloesung.value).toBeFalsy();
    });
  });

  describe('knotenForm und LSA-Eigenschaften', () => {
    beforeEach(() => {
      data$.next({ isCreator: true });
    });

    it('disable & hide LSA-Eigenschaften initially', () => {
      expect(component.formGroup.controls.lichtsignalAnlageEigenschaften.disabled).toBeTrue();
      expect(component.isLSAKnotenForm).toBeFalse();
    });

    it('disable & hide LSA-Eigenschaften if KnotenForm is not LSA', () => {
      component.formGroup.controls.shared.controls.knotenForm.setValue(KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name);
      expect(component.formGroup.controls.lichtsignalAnlageEigenschaften.disabled).toBeFalse();
      expect(component.isLSAKnotenForm).toBeTrue();

      component.formGroup.controls.shared.controls.knotenForm.setValue(KNOTENFORMEN.BAUWERK.options[0].name);
      expect(component.formGroup.controls.lichtsignalAnlageEigenschaften.disabled).toBeTrue();
      expect(component.isLSAKnotenForm).toBeFalse();
    });

    it('should be null in command if disabled', fakeAsync(() => {
      component.formGroup.patchValue({
        verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
        netzbezug: defaultNetzbezug,
        kommentar: 'test Kommentar',
        radnetzKonform: true,
        shared: {
          knotenForm: KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name,
        },
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
      component.formGroup.controls.shared.controls.knotenForm.setValue(KNOTENFORMEN.BAUWERK.options[2].name);
      component.formGroup.markAsDirty();
      when(furtKreuzungService.createFurtKreuzung(anything())).thenResolve(1);

      component.onSave();
      tick();

      verify(furtKreuzungService.createFurtKreuzung(anything())).once();
      expect(capture(furtKreuzungService.createFurtKreuzung).last()[0].lichtsignalAnlageEigenschaften).toBeNull();
    }));

    it('should reset LSA-Eigenschaften if knotenForm changes to non-LSA', () => {
      component.formGroup.controls.shared.controls.knotenForm.setValue(KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name);
      component.formGroup.controls.lichtsignalAnlageEigenschaften.setValue({
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

      component.formGroup.controls.shared.controls.knotenForm.setValue(KNOTENFORMEN.BAUWERK.options[0].name);
      expect(component.formGroup.value.lichtsignalAnlageEigenschaften).toBeUndefined();
      expect(component.formGroup.controls.lichtsignalAnlageEigenschaften.value).toEqual({
        linksabbieger: null,
        rechtsabbieger: null,
        gruenAnforderung: null,
        radAufstellflaeche: false,
        getrenntePhasen: false,
        fahrradSignal: false,
        gruenVorlauf: false,
        vorgezogeneHalteLinie: false,
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

    it('should set isCreator correct from Route', () => {
      data$.next({ isCreator: true });
      expect(component.isCreator).toBeTrue();
    });

    it('should fill form', () => {
      expect(component.formGroup.dirty).toBeFalse();
      expect(component.formGroup.value).toEqual({
        netzbezug: null,
        verantwortlicheOrganisation: null,
        kommentar: null,
        shared: {
          knotenForm: null,
        },
        radnetzKonform: null,
        typ: null,
      });
      expect(component.formGroup.controls.shared.controls.querungshilfeDetails.enabled).toBeFalse();
      expect(component.formGroup.controls.shared.controls.bauwerksmangel.enabled).toBeFalse();
      expect(component.formGroup.controls.lichtsignalAnlageEigenschaften.enabled).toBeFalse();
    });

    describe('onSave', () => {
      it('should create correct command', () => {
        component.formGroup.patchValue({
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
          netzbezug: defaultNetzbezug,
          kommentar: 'test Kommentar',
          radnetzKonform: true,
          shared: {
            knotenForm: 'UEBERFUEHRUNG',
            bauwerksmangel: {
              vorhanden: Bauwerksmangel.NICHT_VORHANDEN,
            },
          },
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
          knotenForm: 'UEBERFUEHRUNG',
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
          lichtsignalAnlageEigenschaften: null,
          bauwerksmangel: Bauwerksmangel.NICHT_VORHANDEN,
          bauwerksmangelArt: null,
          querungshilfeDetails: null,
        };
        verify(furtKreuzungService.createFurtKreuzung(anything())).once();
        expect(capture(furtKreuzungService.createFurtKreuzung).last()[0]).toEqual(createCommand);
      });

      it('should set radNetzKonform=false if control untouched', () => {
        component.formGroup.patchValue({
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
          netzbezug: defaultNetzbezug,
          kommentar: 'test Kommentar',
          shared: {
            knotenForm: 'UEBERFUEHRUNG',
            bauwerksmangel: {
              vorhanden: Bauwerksmangel.NICHT_VORHANDEN,
            },
          },
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
          knotenForm: 'UEBERFUEHRUNG',
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
          lichtsignalAnlageEigenschaften: null,
          bauwerksmangel: Bauwerksmangel.NICHT_VORHANDEN,
          bauwerksmangelArt: null,
          querungshilfeDetails: null,
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
          shared: {
            knotenForm: null,
          },
          radnetzKonform: null,
          typ: null,
        });
      });
    });
  });

  describe('as Editor', () => {
    it('should set isCreator correct from Route', () => {
      data$.next({ isCreator: false, furtKreuzung: { ...defaultFurtKreuzung } });
      expect(component.isCreator).toBeFalse();
    });

    describe('fillForm', () => {
      it('should set bauwerksmangel correct', () => {
        data$.next({
          isCreator: false,
          furtKreuzung: {
            ...defaultFurtKreuzung,
            knotenForm: 'UEBERFUEHRUNG',
            bauwerksmangel: Bauwerksmangel.VORHANDEN,
            bauwerksmangelArt: [BauwerksmangelArt.GELAENDER_ZU_NIEDRIG],
          },
        });

        expect(component.formGroup.value.shared?.bauwerksmangel).toEqual({
          vorhanden: Bauwerksmangel.VORHANDEN,
          bauwerksmangelArt: [BauwerksmangelArt.GELAENDER_ZU_NIEDRIG],
        });
      });

      it('should set querungshilfeDetails correct', () => {
        data$.next({
          isCreator: false,
          furtKreuzung: {
            ...defaultFurtKreuzung,
            knotenForm: 'MITTELINSEL_EINFACH',
            querungshilfeDetails: QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL,
          },
        });

        expect(component.formGroup.value.shared?.querungshilfeDetails).toEqual(
          QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL
        );
      });

      it('should set lsa correct', () => {
        data$.next({
          isCreator: false,
          furtKreuzung: {
            ...defaultFurtKreuzung,
            knotenForm: KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name,
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
          },
        });

        expect(component.formGroup.value.lichtsignalAnlageEigenschaften).toEqual({
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
      });

      it('should mark form as not dirty', () => {
        component.formGroup.markAsDirty();
        data$.next({ isCreator: false, furtKreuzung: { ...defaultFurtKreuzung } });
        expect(component.formGroup.dirty).toBeFalse();
      });

      it('should fill correct values', () => {
        data$.next({
          isCreator: false,
          furtKreuzung: {
            ...defaultFurtKreuzung,
            netzbezug: defaultNetzbezug,
            verantwortlicheOrganisation: defaultOrganisation,
            kommentar: 'test Kommentar',
            radnetzKonform: true,
            typ: FurtKreuzungTyp.FURT,
            furtKreuzungMusterloesung: defaultMusterloesungOption.name,
            knotenForm: KNOTENFORMEN.BAUWERK.options[2].name,
            lichtsignalAnlageEigenschaften: null,
          },
        });

        expect(component.formGroup.value).toEqual({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: defaultOrganisation,
          kommentar: 'test Kommentar',
          shared: {
            knotenForm: KNOTENFORMEN.BAUWERK.options[2].name,
          },
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: defaultMusterloesungOption.name,
        });
      });
    });

    describe('onSave', () => {
      it('should create correct command', () => {
        component.currentFurtKreuzung = { ...defaultFurtKreuzung };
        component.isCreator = false;
        component.formGroup.patchValue({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
          kommentar: 'test Kommentar',
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: defaultMusterloesungOption.name,
          shared: {
            knotenForm: KNOTENFORMEN.BAUWERK.options[2].name,
          },
        });

        component.formGroup.markAsDirty();
        when(furtKreuzungService.updateFurtKreuzung(anything(), anything())).thenResolve({ ...defaultFurtKreuzung });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();

        const updateCommand: SaveFurtKreuzungCommand = {
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: 3456,
          version: 2,
          kommentar: 'test Kommentar',
          knotenForm: KNOTENFORMEN.BAUWERK.options[2].name,
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: defaultMusterloesungOption.name,
          lichtsignalAnlageEigenschaften: null,
          bauwerksmangel: null,
          bauwerksmangelArt: null,
          querungshilfeDetails: null,
        };
        verify(furtKreuzungService.updateFurtKreuzung(anything(), anything())).once();
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[0]).toEqual(2345);
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[1]).toEqual(updateCommand);
      });

      it('should create correct command - mit lsa', () => {
        component.currentFurtKreuzung = { ...defaultFurtKreuzung };
        component.isCreator = false;
        component.formGroup.patchValue({
          ...defaultFurtKreuzung,
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
          shared: {
            knotenForm: KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name,
          },
        });

        component.formGroup.markAsDirty();
        when(furtKreuzungService.updateFurtKreuzung(anything(), anything())).thenResolve({ ...defaultFurtKreuzung });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();

        verify(furtKreuzungService.updateFurtKreuzung(anything(), anything())).once();
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[0]).toEqual(2345);
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[1].lichtsignalAnlageEigenschaften).toEqual({
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
      });

      it('should create correct command - mit bauwerksmangel', () => {
        component.currentFurtKreuzung = { ...defaultFurtKreuzung };
        component.isCreator = false;
        component.formGroup.patchValue({
          ...defaultFurtKreuzung,
          lichtsignalAnlageEigenschaften: undefined,
          shared: {
            knotenForm: KNOTENFORMEN.BAUWERK.options[0].name,
            bauwerksmangel: {
              vorhanden: Bauwerksmangel.VORHANDEN,
              bauwerksmangelArt: [BauwerksmangelArt.ANDERER_MANGEL],
            },
          },
        });

        component.formGroup.markAsDirty();
        when(furtKreuzungService.updateFurtKreuzung(anything(), anything())).thenResolve({ ...defaultFurtKreuzung });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();

        verify(furtKreuzungService.updateFurtKreuzung(anything(), anything())).once();
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[0]).toEqual(2345);
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[1].bauwerksmangel).toEqual(
          Bauwerksmangel.VORHANDEN
        );
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[1].bauwerksmangelArt).toEqual([
          BauwerksmangelArt.ANDERER_MANGEL,
        ]);
      });

      it('should create correct command - mit querungshilfe', () => {
        component.currentFurtKreuzung = { ...defaultFurtKreuzung };
        component.isCreator = false;
        component.formGroup.patchValue({
          ...defaultFurtKreuzung,
          lichtsignalAnlageEigenschaften: undefined,
          shared: {
            knotenForm: 'MITTELINSEL_EINFACH',
            querungshilfeDetails: QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL,
          },
        });

        component.formGroup.markAsDirty();
        when(furtKreuzungService.updateFurtKreuzung(anything(), anything())).thenResolve({ ...defaultFurtKreuzung });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();

        verify(furtKreuzungService.updateFurtKreuzung(anything(), anything())).once();
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[0]).toEqual(2345);
        expect(capture(furtKreuzungService.updateFurtKreuzung).last()[1].querungshilfeDetails).toEqual(
          QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL
        );
      });

      it('should reset form after save', fakeAsync(() => {
        data$.next({ isCreator: false, furtKreuzung: { ...defaultFurtKreuzung } });
        component.formGroup.markAsDirty();
        when(furtKreuzungService.updateFurtKreuzung(anything(), anything())).thenResolve({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 78645 },
          version: 3,
          kommentar: 'test Kommentar',
          knotenForm: 'UEBERFUEHRUNG',
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
          lichtsignalAnlageEigenschaften: null,
          benutzerDarfBearbeiten: true,
          bauwerksmangel: Bauwerksmangel.NICHT_VORHANDEN,
          bauwerksmangelArt: null,
          querungshilfeDetails: null,
        });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();
        tick();

        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.value).toEqual({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 78645 },
          kommentar: 'test Kommentar',
          shared: {
            knotenForm: 'UEBERFUEHRUNG',
            bauwerksmangel: {
              vorhanden: Bauwerksmangel.NICHT_VORHANDEN,
            },
          },
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
        });
      }));

      it('should use correct version', fakeAsync(() => {
        data$.next({ isCreator: false, furtKreuzung: { ...defaultFurtKreuzung } });
        component.formGroup.markAsDirty();
        when(furtKreuzungService.updateFurtKreuzung(anything(), anything())).thenResolve({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 78645 },
          version: 3,
          kommentar: 'test Kommentar',
          knotenForm: 'UEBERFUEHRUNG',
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
          lichtsignalAnlageEigenschaften: null,
          benutzerDarfBearbeiten: true,
          bauwerksmangel: null,
          bauwerksmangelArt: null,
          querungshilfeDetails: null,
        });
        spyOnProperty(component.formGroup, 'valid').and.returnValue(true);
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
        data$.next({ isCreator: false, furtKreuzung: { ...defaultFurtKreuzung } });

        component.formGroup.patchValue({
          shared: {
            knotenForm: KNOTENFORMEN.BAUWERK.options[0].name,
            bauwerksmangel: {
              vorhanden: Bauwerksmangel.VORHANDEN,
              bauwerksmangelArt: [BauwerksmangelArt.GELAENDER_ZU_NIEDRIG],
            },
          },
        });

        component.formGroup.markAsDirty();

        component.onReset();

        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.value).toEqual({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: defaultOrganisation,
          kommentar: 'test Kommentar',
          shared: {
            knotenForm: KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name,
          },
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: defaultMusterloesungOption.name,
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
        data$.next({ isCreator: false, furtKreuzung: { ...defaultFurtKreuzung } });
        component.formGroup.markAsDirty();
        when(furtKreuzungService.updateFurtKreuzung(anything(), anything())).thenResolve({
          netzbezug: defaultNetzbezug,
          verantwortlicheOrganisation: { ...defaultOrganisation, id: 78645 },
          version: 3,
          kommentar: 'test Kommentar',
          knotenForm: 'UEBERFUEHRUNG',
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
          lichtsignalAnlageEigenschaften: null,
          benutzerDarfBearbeiten: true,
          bauwerksmangel: Bauwerksmangel.NICHT_VORHANDEN,
          bauwerksmangelArt: null,
          querungshilfeDetails: null,
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
          shared: {
            knotenForm: 'UEBERFUEHRUNG',
            bauwerksmangel: { vorhanden: Bauwerksmangel.NICHT_VORHANDEN },
          },
          radnetzKonform: true,
          typ: FurtKreuzungTyp.FURT,
          furtKreuzungMusterloesung: null,
        });
      }));
    });
  });
});
