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

/* eslint-disable no-unused-vars */
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { UntypedFormGroup } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MockBuilder } from 'ng-mocks';
import { of, Subject } from 'rxjs';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { EditorModule } from 'src/app/editor/editor.module';
import { KantenAttributeEditorComponent } from 'src/app/editor/kanten/components/kanten-attribute-editor/kanten-attribute-editor.component';
import { Beleuchtung } from 'src/app/editor/kanten/models/beleuchtung';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { anotherKante, defaultKante } from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { SaveKantenAttributGruppeCommand } from 'src/app/editor/kanten/models/save-kanten-attribut-gruppe-command';
import { Status } from 'src/app/editor/kanten/models/status';
import { StrassenquerschnittRASt06 } from 'src/app/editor/kanten/models/strassenquerschnittrast06';
import { Umfeld } from 'src/app/editor/kanten/models/umfeld';
import { WegeNiveau } from 'src/app/editor/kanten/models/wege-niveau';
import { StrassenkategorieRIN } from 'src/app/editor/kanten/models/strassenkategorie-rin';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NotifyGeometryChangedService } from 'src/app/editor/kanten/services/notify-geometry-changed.service';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { ConfirmationDialogComponent } from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { IstStandard } from 'src/app/shared/models/ist-standard';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import {
  defaultOrganisation,
  defaultUebergeordneteOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { anything, capture, instance, mock, objectContaining, verify, when } from 'ts-mockito';

describe(KantenAttributeEditorComponent.name, () => {
  let component: KantenAttributeEditorComponent;
  let fixture: ComponentFixture<KantenAttributeEditorComponent>;
  let netzService: NetzService;
  let organisationenService: OrganisationenService;
  let kantenSelektionService: KantenSelektionService;
  let kantenSubject$: Subject<Kante[]>;
  let kantenSelektionSubject$: Subject<KantenSelektion[]>;
  let notifyGeometryChangedService: NotifyGeometryChangedService;
  let dialog: MatDialog;
  const defaultLandkreis = defaultUebergeordneteOrganisation;

  // Bitte wieder component.formGroup.value nutzen, wenn RadNETZ-Klassen an Grundnetzkanten gesetzt werden können!
  const getFormValues: () => any = () => {
    const {
      laengeBerechnet,
      landkreis,
      ...rawValuesWithoutLandkreisUndLaengeBerechnet
    } = component.formGroup.getRawValue();

    return rawValuesWithoutLandkreisUndLaengeBerechnet;
  };

  beforeEach(() => {
    netzService = mock(NetzService);
    organisationenService = mock(OrganisationenService);
    kantenSelektionService = mock(KantenSelektionService);
    notifyGeometryChangedService = mock(NotifyGeometryChangedService);
    dialog = mock(MatDialog);
    kantenSubject$ = new Subject();
    kantenSelektionSubject$ = new Subject();
    when(kantenSelektionService.selektierteKanten$).thenReturn(kantenSubject$);
    when(kantenSelektionService.selektion$).thenReturn(kantenSelektionSubject$);
    when(organisationenService.getOrganisation(defaultLandkreis.id)).thenResolve(defaultLandkreis);
    when(organisationenService.liegenAlleInQualitaetsgesichertenLandkreisen(anything())).thenResolve(false);
    return MockBuilder(KantenAttributeEditorComponent, EditorModule)
      .provide({
        provide: NetzService,
        useValue: instance(netzService),
      })
      .provide({
        provide: OrganisationenService,
        useValue: instance(organisationenService),
      })
      .provide({
        provide: NotifyGeometryChangedService,
        useValue: instance(notifyGeometryChangedService),
      })
      .provide({
        provide: MatDialog,
        useValue: instance(dialog),
      })
      .provide({ provide: KantenSelektionService, useValue: instance(kantenSelektionService) });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(KantenAttributeEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('fill form', () => {
    it('should set values correctly', fakeAsync(() => {
      const expectedValues = {
        wegeNiveau: WegeNiveau.FAHRBAHN,
        beleuchtung: Beleuchtung.VORHANDEN,
        strassenquerschnittRASt06: StrassenquerschnittRASt06.ANBAUFREIE_STRASSE,
        umfeld: Umfeld.GESCHAEFTSSTRASSE,
        strassenkategorieRIN: StrassenkategorieRIN.KLEINRAEUMIG,
        laengeManuellErfasst: 100,
        dtvFussverkehr: 1,
        dtvRadverkehr: 2,
        dtvPkw: 2,
        sv: 2,
        kommentar: 'kommentar',
        strassenName: 'ABC-Straße',
        strassenNummer: '1a',
        gemeinde: defaultOrganisation,
        status: Status.UNTER_VERKEHR,
      };
      updateSelektierteKanten([
        {
          ...defaultKante,
          id: 1,
          kantenAttributGruppe: {
            ...defaultKante.kantenAttributGruppe,
            ...expectedValues,
            netzklassen: [],
            istStandards: [],
          },
          kantenVersion: 1,
        },
      ]);

      tick();

      expect(component.formGroup.getRawValue()).toEqual({
        ...expectedValues,
        netzklassen: {
          radnetzAlltag: false,
          radnetzFreizeit: false,
          radnetzZielnetz: false,
          kreisnetzAlltag: false,
          kreisnetzFreizeit: false,
          kommunalnetzAlltag: false,
          kommunalnetzFreizeit: false,
          radschnellverbindung: false,
          radvorrangrouten: false,
        },
        istStandards: {
          radnetzStartstandard: false,
          radnetzZielstandard: false,
          radschnellverbindung: false,
          basisstandard: false,
          radvorrangrouten: false,
        },
        laengeBerechnet: '100,23',
        landkreis: defaultLandkreis.name,
      });
    }));

    it('should set multiple values correctly', fakeAsync(() => {
      const equalValues = {
        beleuchtung: Beleuchtung.VORHANDEN,
        strassenquerschnittRASt06: StrassenquerschnittRASt06.ANBAUFREIE_STRASSE,
        umfeld: Umfeld.GESCHAEFTSSTRASSE,
        laengeManuellErfasst: 100,
        dtvFussverkehr: 1,
        dtvPkw: 2,
        sv: 2,
        kommentar: 'kommentar',
        strassenName: 'ABC-Straße',
        strassenNummer: '1a',
        status: Status.UNTER_VERKEHR,
        netzklassen: [Netzklasse.RADNETZ_ALLTAG],
        istStandards: [],
      };
      const kante1 = {
        ...defaultKante,
        id: 1,
        kantenVersion: 1,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          ...equalValues,
          gemeinde: { ...defaultOrganisation, id: 1 },
          dtvRadverkehr: 2,
          wegeNiveau: WegeNiveau.FAHRBAHN,
          strassenkategorieRIN: StrassenkategorieRIN.KLEINRAEUMIG,
        },
      };
      const kante2 = {
        ...defaultKante,
        id: 2,
        kantenVersion: 2,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          ...equalValues,
          gemeinde: { ...defaultOrganisation, id: 2 },
          dtvRadverkehr: 5,
          wegeNiveau: WegeNiveau.GEHWEG,
          strassenkategorieRIN: StrassenkategorieRIN.NAHRAEUMIG,
        },
      };

      updateSelektierteKanten([kante1, kante2]);

      tick();

      const expectedValues = {
        ...equalValues,
        gemeinde: new UndeterminedValue(),
        dtvRadverkehr: new UndeterminedValue(),
        wegeNiveau: new UndeterminedValue(),
        strassenkategorieRIN: new UndeterminedValue(),
        netzklassen: {
          radnetzAlltag: true,
          radnetzFreizeit: false,
          radnetzZielnetz: false,
          kreisnetzAlltag: false,
          kreisnetzFreizeit: false,
          kommunalnetzAlltag: false,
          kommunalnetzFreizeit: false,
          radschnellverbindung: false,
          radvorrangrouten: false,
        },
        istStandards: {
          radnetzStartstandard: false,
          radnetzZielstandard: false,
          radschnellverbindung: false,
          basisstandard: false,
          radvorrangrouten: false,
        },
      };

      // Bitte wieder component.formGroup.value nutzen, wenn RadNETZ-Klassen an Grundnetzkanten gesetzt werden können!
      expect(getFormValues()).toEqual(expectedValues);
    }));

    it('should find equal organisation correctly', fakeAsync(() => {
      const kante1 = {
        ...defaultKante,
        id: 1,
        kantenVersion: 1,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          gemeinde: { ...defaultOrganisation, id: 1 },
        },
      };
      const kante2 = {
        ...defaultKante,
        id: 2,
        kantenVersion: 2,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          gemeinde: { ...defaultOrganisation, id: 1 },
        },
      };

      updateSelektierteKanten([kante1, kante2]);
      tick();

      expect(component.formGroup.value.gemeinde).not.toBeInstanceOf(UndeterminedValue);
    }));

    it('should handle missing organisation correctly', fakeAsync(() => {
      const kante1 = {
        ...defaultKante,
        id: 1,
        kantenVersion: 1,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          gemeinde: null,
        },
      };
      const kante2 = {
        ...defaultKante,
        id: 2,
        kantenVersion: 2,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          gemeinde: { ...defaultOrganisation, id: 1 },
        },
      };

      updateSelektierteKanten([kante1, kante2]);
      tick();

      expect(component.formGroup.value.gemeinde).toBeInstanceOf(UndeterminedValue);
    }));
  });

  describe('save', () => {
    it('should set equal values for all kanten', fakeAsync(() => {
      const equalValues = {
        beleuchtung: Beleuchtung.NICHT_VORHANDEN,
        strassenquerschnittRASt06: StrassenquerschnittRASt06.ANBAUFREIE_STRASSE,
        laengeManuellErfasst: 100,
        dtvPkw: 2,
        sv: 2,
        kommentar: 'kommentar',
        status: Status.NICHT_FUER_RADVERKEHR_FREIGEGEBEN,
        gemeinde: { ...defaultOrganisation, id: 274 },
        netzklassen: [Netzklasse.KREISNETZ_ALLTAG],
        istStandards: [IstStandard.BASISSTANDARD],
      };

      const kanteId1 = 1;
      const kantenVersion1 = 1;
      const gruppenId1 = 1;
      const gruppenVersion1 = 10;
      const kante1 = {
        ...defaultKante,
        id: kanteId1,
        kantenVersion: kantenVersion1,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          id: gruppenId1,
          version: gruppenVersion1,
          wegeNiveau: WegeNiveau.GEHWEG,
          umfeld: Umfeld.GEWERBEGEBIET,
          strassenkategorieRIN: StrassenkategorieRIN.KLEINRAEUMIG,
          dtvFussverkehr: 9,
          dtvRadverkehr: 10,
          kommentar: 'kommentar1',
        },
      };

      const kanteId2 = 2;
      const kantenVersion2 = 20;
      const gruppenId2 = 2;
      const gruppenVersion2 = 15;
      const kante2 = {
        ...defaultKante,
        id: kanteId2,
        kantenVersion: kantenVersion2,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          id: gruppenId2,
          version: gruppenVersion2,
          wegeNiveau: WegeNiveau.FAHRBAHN,
          umfeld: Umfeld.UNBEKANNT,
          strassenkategorieRIN: StrassenkategorieRIN.NAHRAEUMIG,
          dtvFussverkehr: 7,
          dtvRadverkehr: 8,
          kommentar: 'kommentar2',
        },
      };

      when(organisationenService.getOrganisation(anything())).thenResolve(equalValues.gemeinde);
      updateSelektierteKanten([kante1, kante2]);
      tick();

      component.formGroup.patchValue(equalValues);
      component.formGroup.markAsDirty();
      const updatedKanten = [
        {
          ...kante1,
          kantenAttributGruppe: { ...kante1.kantenAttributGruppe, version: kante1.kantenAttributGruppe.version + 1 },
        },
        {
          ...kante2,
          kantenAttributGruppe: { ...kante2.kantenAttributGruppe, version: kante2.kantenAttributGruppe.version + 1 },
        },
      ];
      when(netzService.saveKanteAllgemein(anything())).thenReturn(Promise.resolve(updatedKanten));
      component.onSave();
      tick();

      verify(netzService.saveKanteAllgemein(anything())).once();
      verify(kantenSelektionService.updateKanten(anything())).once();
      expect(capture(kantenSelektionService.updateKanten).last()[0]).toEqual(updatedKanten);
      expect(capture(netzService.saveKanteAllgemein).last()[0]).toEqual([
        {
          ...equalValues,
          kanteId: 1,
          gruppenId: gruppenId1,
          gruppenVersion: gruppenVersion1,
          wegeNiveau: WegeNiveau.GEHWEG,
          umfeld: Umfeld.GEWERBEGEBIET,
          strassenkategorieRIN: StrassenkategorieRIN.KLEINRAEUMIG,
          dtvFussverkehr: 9,
          dtvRadverkehr: 10,
          gemeinde: equalValues.gemeinde.id,
        } as SaveKantenAttributGruppeCommand,
        {
          ...equalValues,
          kanteId: kanteId2,
          gruppenId: gruppenId2,
          gruppenVersion: gruppenVersion2,
          wegeNiveau: WegeNiveau.FAHRBAHN,
          umfeld: Umfeld.UNBEKANNT,
          strassenkategorieRIN: StrassenkategorieRIN.NAHRAEUMIG,
          dtvFussverkehr: 7,
          dtvRadverkehr: 8,
          gemeinde: equalValues.gemeinde.id,
        } as SaveKantenAttributGruppeCommand,
      ]);
    }));

    it('should read different values correctly', fakeAsync(() => {
      const kanteId1 = 1;
      const kantenVersion1 = 1;
      const gruppenId1 = 1;
      const gruppenVersion1 = 10;
      const kante1 = {
        ...defaultKante,
        id: kanteId1,
        kantenVersion: kantenVersion1,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          id: gruppenId1,
          version: gruppenVersion1,
          wegeNiveau: WegeNiveau.GEHWEG,
          umfeld: Umfeld.GEWERBEGEBIET,
          strassenkategorieRIN: StrassenkategorieRIN.KLEINRAEUMIG,
          dtvFussverkehr: 9,
          dtvRadverkehr: 10,
          kommentar: 'kommentar1',
          beleuchtung: Beleuchtung.NICHT_VORHANDEN,
          strassenquerschnittRASt06: StrassenquerschnittRASt06.ANBAUFREIE_STRASSE,
          laengeManuellErfasst: 100,
          dtvPkw: 1,
          sv: 1,
          status: Status.UNTER_VERKEHR,
          gemeinde: { ...defaultOrganisation, id: 275 },
          netzklassen: [Netzklasse.KREISNETZ_FREIZEIT],
          istStandards: [IstStandard.RADVORRANGROUTEN],
        },
      };

      const kanteId2 = 2;
      const kantenVersion2 = 20;
      const gruppenId2 = 2;
      const gruppenVersion2 = 15;
      const kante2 = {
        ...defaultKante,
        id: kanteId2,
        kantenVersion: kantenVersion2,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          id: gruppenId2,
          version: gruppenVersion2,
          wegeNiveau: WegeNiveau.FAHRBAHN,
          umfeld: Umfeld.UNBEKANNT,
          strassenkategorieRIN: StrassenkategorieRIN.NAHRAEUMIG,
          dtvFussverkehr: 7,
          dtvRadverkehr: 8,
          kommentar: 'kommentar2',
          beleuchtung: Beleuchtung.VORHANDEN,
          strassenquerschnittRASt06: StrassenquerschnittRASt06.DOERFLICHE_HAUPTSTRASSE,
          laengeManuellErfasst: 110,
          dtvPkw: 2,
          sv: 2,
          status: Status.FIKTIV,
          gemeinde: { ...defaultOrganisation, id: 274 },
          netzklassen: [Netzklasse.KREISNETZ_ALLTAG],
          istStandards: [IstStandard.BASISSTANDARD],
        },
      };

      when(organisationenService.getOrganisation(kante1.kantenAttributGruppe.gemeinde.id)).thenResolve(
        kante1.kantenAttributGruppe.gemeinde
      );
      when(organisationenService.getOrganisation(kante2.kantenAttributGruppe.gemeinde.id)).thenResolve(
        kante2.kantenAttributGruppe.gemeinde
      );
      updateSelektierteKanten([kante1, kante2]);
      tick();

      component.formGroup.markAsDirty();
      const updatedKanten = [
        {
          ...kante1,
          kantenAttributGruppe: { ...kante1.kantenAttributGruppe, version: kante1.kantenAttributGruppe.version + 1 },
        },
        {
          ...kante2,
          kantenAttributGruppe: { ...kante2.kantenAttributGruppe, version: kante2.kantenAttributGruppe.version + 1 },
        },
      ];
      when(netzService.saveKanteAllgemein(anything())).thenReturn(Promise.resolve(updatedKanten));
      component.onSave();
      tick();

      verify(netzService.saveKanteAllgemein(anything())).once();
      verify(kantenSelektionService.updateKanten(anything())).once();
      expect(capture(kantenSelektionService.updateKanten).last()[0]).toEqual(updatedKanten);
      const {
        id,
        version,
        landkreis,
        strassenName,
        strassenNummer,
        laengeBerechnet,
        ...expectedKantenAttributeCommandValues1
      } = kante1.kantenAttributGruppe;

      const {
        id: id2,
        version: version2,
        landkreis: landkreis2,
        strassenName: strassenName2,
        strassenNummer: strassenNummer2,
        laengeBerechnet: laengeBerechnet2,
        ...expectedKantenAttributeCommandValues2
      } = kante2.kantenAttributGruppe;

      expect(capture(netzService.saveKanteAllgemein).last()[0]).toEqual([
        {
          ...expectedKantenAttributeCommandValues1,
          kanteId: kanteId1,
          gruppenId: gruppenId1,
          gruppenVersion: gruppenVersion1,
          gemeinde: kante1.kantenAttributGruppe.gemeinde.id,
        } as SaveKantenAttributGruppeCommand,
        {
          ...expectedKantenAttributeCommandValues2,
          kanteId: kanteId2,
          gruppenId: gruppenId2,
          gruppenVersion: gruppenVersion2,
          gemeinde: kante2.kantenAttributGruppe.gemeinde.id,
        } as SaveKantenAttributGruppeCommand,
      ]);
    }));

    it('should read form correctly', fakeAsync(() => {
      const expectedValues = {
        wegeNiveau: WegeNiveau.FAHRBAHN,
        beleuchtung: Beleuchtung.NICHT_VORHANDEN,
        strassenquerschnittRASt06: StrassenquerschnittRASt06.ANBAUFREIE_STRASSE,
        umfeld: Umfeld.STRASSE_MIT_GERINGER_BIS_MITTLERER_WOHNDICHTE,
        strassenkategorieRIN: StrassenkategorieRIN.KLEINRAEUMIG,
        laengeManuellErfasst: 100,
        dtvFussverkehr: 3,
        dtvRadverkehr: 4,
        dtvPkw: 2,
        sv: 2,
        kommentar: 'kommentar',
        status: Status.UNTER_VERKEHR,
        gemeinde: { ...defaultOrganisation, id: 274 },
        netzklassen: [Netzklasse.KREISNETZ_ALLTAG],
        istStandards: [IstStandard.BASISSTANDARD],
      };
      const kanteId = 1;
      const kantenVersion = 1;
      const kante = {
        ...defaultKante,
        id: kanteId,
        kantenVersion,
        landkreis: defaultOrganisation,
        laengeBerechnet: 100.23475,
      };
      when(organisationenService.getOrganisation(anything())).thenResolve(expectedValues.gemeinde);
      updateSelektierteKanten([kante]);
      tick();

      component.formGroup.patchValue({
        ...expectedValues,
        netzklassen: [Netzklasse.KREISNETZ_ALLTAG],
        istStandards: [IstStandard.BASISSTANDARD],
      });
      component.formGroup.markAsDirty();
      when(netzService.saveKanteAllgemein(anything())).thenReturn(
        Promise.resolve([
          {
            ...kante,
            kantenAttributGruppe: { ...kante.kantenAttributGruppe, version: kante.kantenAttributGruppe.version + 1 },
          },
        ])
      );
      component.onSave();
      tick();

      verify(netzService.saveKanteAllgemein(anything())).once();
      verify(kantenSelektionService.updateKanten(anything())).once();
      expect(capture(kantenSelektionService.updateKanten).last()[0]).toEqual([
        {
          ...kante,
          kantenAttributGruppe: { ...kante.kantenAttributGruppe, version: kante.kantenAttributGruppe.version + 1 },
        },
      ]);
      expect(capture(netzService.saveKanteAllgemein).last()[0]).toEqual([
        {
          ...expectedValues,
          kanteId,
          gruppenId: kante.kantenAttributGruppe.id,
          gruppenVersion: kante.kantenAttributGruppe.version,
          gemeinde: expectedValues.gemeinde.id,
        } as SaveKantenAttributGruppeCommand,
      ]);
    }));
  });

  describe('onSeitenbezugChange', () => {
    it('should update kanten und karte on toggle to seitenbezogen', fakeAsync(() => {
      const updatedKante = { ...defaultKante, seitenbezogen: true, kantenVersion: defaultKante.kantenVersion + 1 };
      when(netzService.updateSeitenbezug(anything())).thenResolve([updatedKante]);
      updateSelektierteKanten([defaultKante]);
      tick();
      component.seitenbezogen = false;

      component.onSeitenbezugChange();
      tick();

      verify(kantenSelektionService.updateKanten(anything())).once();
      expect(capture(kantenSelektionService.updateKanten).last()[0]).toEqual([updatedKante]);
      verify(notifyGeometryChangedService.notify()).once();
    }));

    it('should update kanten und karte on toggle to not seitenbezogen after confirmed', fakeAsync(() => {
      when(dialog.open(anything(), anything())).thenReturn({
        afterClosed: () => of(true),
      } as MatDialogRef<ConfirmationDialogComponent>);
      const updatedKante = { ...defaultKante, seitenbezogen: true, kantenVersion: defaultKante.kantenVersion + 1 };
      when(netzService.updateSeitenbezug(anything())).thenResolve([updatedKante]);
      updateSelektierteKanten([defaultKante]);
      component.seitenbezogen = true;
      tick();

      component.onSeitenbezugChange();
      tick();

      verify(kantenSelektionService.updateKanten(anything())).once();
      expect(capture(kantenSelektionService.updateKanten).last()[0]).toEqual([updatedKante]);
      verify(notifyGeometryChangedService.notify()).once();
    }));

    it('should do nothing on toggle to not seitenbezogen after declined', fakeAsync(() => {
      when(dialog.open(anything(), anything())).thenReturn({
        afterClosed: () => of(false),
      } as MatDialogRef<ConfirmationDialogComponent>);
      const updatedKante = { ...defaultKante, seitenbezogen: true, kantenVersion: defaultKante.kantenVersion + 1 };
      when(netzService.updateSeitenbezug(anything())).thenResolve([updatedKante]);
      updateSelektierteKanten([defaultKante]);
      component.seitenbezogen = true;
      tick();

      component.onSeitenbezugChange();
      tick();

      verify(kantenSelektionService.updateKanten(anything())).never();
      verify(notifyGeometryChangedService.notify()).never();
      expect().nothing();
    }));
  });

  describe('canDelete', () => {
    it('should be false when multiple Kanten selected', fakeAsync(() => {
      updateSelektierteKanten([
        { ...defaultKante, loeschenErlaubt: true },
        {
          ...anotherKante,
          loeschenErlaubt: false,
        },
      ]);
      tick();
      expect(component.canDelete).toBeFalse();
      updateSelektierteKanten([
        { ...defaultKante, loeschenErlaubt: true },
        { ...anotherKante, loeschenErlaubt: true },
      ]);
      tick();
      expect(component.canDelete).toBeFalse();
    }));

    it('should be false when selected Kante is not deletable', fakeAsync(() => {
      updateSelektierteKanten([{ ...defaultKante, loeschenErlaubt: false }]);
      tick();
      expect(component.canDelete).toBeFalse();
    }));

    it('should be true when selected Kante is deletable', fakeAsync(() => {
      updateSelektierteKanten([{ ...defaultKante, loeschenErlaubt: true }]);
      tick();
      expect(component.canDelete).toBeTrue();
    }));
  });

  describe(KantenAttributeEditorComponent.prototype.onDelete.name, () => {
    it('should throw Error when canDelete is false', fakeAsync(() => {
      updateSelektierteKanten([{ ...defaultKante, loeschenErlaubt: false }]);
      tick();
      expect(component.canDelete).toBeFalse();
      expect(() => component.onDelete()).toThrowError('Invariant failed');
    }));

    it('should open Dialog ', fakeAsync(() => {
      updateSelektierteKanten([defaultKante]);
      tick();
      expect(component.canDelete).toBeTrue();

      when(dialog.open(anything(), anything())).thenReturn({
        afterClosed: () => of(),
      } as MatDialogRef<ConfirmationDialogComponent>);

      component.onDelete();
      verify(
        dialog.open(
          ConfirmationDialogComponent,
          objectContaining({
            data: {
              question:
                'Wollen Sie die Kante wirklich löschen? Durch das Löschen der Kante kann es zur Anpassung des Netzbezugs von anderen Objekten (z.B. Maßnahmen) kommen.',
            },
          })
        )
      ).once();
    }));

    it('should call delete-api-endpoint if dialog was confirmed', fakeAsync(() => {
      updateSelektierteKanten([{ ...defaultKante, id: 8 }]);
      tick();
      expect(component.canDelete).toBeTrue();

      when(dialog.open(anything(), anything())).thenReturn({
        afterClosed: () => of(true),
      } as MatDialogRef<ConfirmationDialogComponent>);

      when(netzService.deleteKante(anything())).thenResolve();

      component.onDelete();
      tick();

      verify(netzService.deleteKante(8)).once();
    }));

    it('should not call delete-api-endpoint if dialog was rejected', fakeAsync(() => {
      updateSelektierteKanten([defaultKante]);
      tick();
      expect(component.canDelete).toBeTrue();

      when(dialog.open(anything(), anything())).thenReturn({
        afterClosed: () => of(false),
      } as MatDialogRef<ConfirmationDialogComponent>);

      when(netzService.deleteKante(anything())).thenResolve();

      component.onDelete();
      tick();

      verify(netzService.deleteKante(anything())).never();
    }));
  });

  describe('seitenbezogen', () => {
    it('should be false if all false', fakeAsync(() => {
      updateSelektierteKanten([
        { ...defaultKante, zweiseitig: true },
        { ...defaultKante, zweiseitig: true },
      ]);
      tick();

      expect(component.seitenbezogen).toBeTrue();
      expect(component.seitenbezogenUndetermined).toBeFalse();
    }));

    it('should be true if all true', fakeAsync(() => {
      updateSelektierteKanten([
        { ...defaultKante, zweiseitig: false },
        { ...defaultKante, zweiseitig: false },
      ]);
      tick();

      expect(component.seitenbezogen).toBeFalse();
      expect(component.seitenbezogenUndetermined).toBeFalse();
    }));

    it('should be undetemined ', fakeAsync(() => {
      component.seitenbezogen = true;
      updateSelektierteKanten([
        { ...defaultKante, zweiseitig: true },
        { ...defaultKante, zweiseitig: false },
      ]);
      tick();

      expect(component.seitenbezogenUndetermined).toBeTrue();
      expect(component.seitenbezogen).toBeFalse();
    }));
  });

  describe('netzklassen', () => {
    it('should flat netzklassen', fakeAsync(() => {
      const kante: Kante = {
        ...defaultKante,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          netzklassen: [Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADSCHNELLVERBINDUNG],
        },
      };
      updateSelektierteKanten([kante]);

      tick();

      // Bitte wieder component.formGroup.get('netzklassen')?.value nutzen, wenn RadNETZ-Klassen an Grundnetzkanten gesetzt werden können!
      const rawNetzklassenValue = (component.formGroup.get('netzklassen') as UntypedFormGroup).getRawValue();
      expect(rawNetzklassenValue).toEqual({
        radnetzAlltag: true,
        radnetzFreizeit: false,
        radnetzZielnetz: false,
        kreisnetzAlltag: false,
        kreisnetzFreizeit: false,
        kommunalnetzAlltag: true,
        kommunalnetzFreizeit: false,
        radschnellverbindung: true,
        radvorrangrouten: false,
      });
    }));

    it('should flat istStandards', fakeAsync(() => {
      const kante: Kante = {
        ...defaultKante,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          netzklassen: [Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADSCHNELLVERBINDUNG],
          istStandards: [
            IstStandard.BASISSTANDARD,
            IstStandard.STARTSTANDARD_RADNETZ,
            IstStandard.RADSCHNELLVERBINDUNG,
          ],
        },
      };
      updateSelektierteKanten([kante]);

      tick();

      expect(component.formGroup.get('istStandards')?.value).toEqual({
        radnetzStartstandard: true,
        radnetzZielstandard: false,
        radschnellverbindung: true,
        basisstandard: true,
        radvorrangrouten: false,
      });
    }));

    it('should find undetermined', fakeAsync(() => {
      const kante1: Kante = {
        ...defaultKante,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          netzklassen: [Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG],
          istStandards: [IstStandard.BASISSTANDARD, IstStandard.STARTSTANDARD_RADNETZ],
        },
      };
      const kante2: Kante = {
        ...defaultKante,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          netzklassen: [Netzklasse.KREISNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG],
          istStandards: [IstStandard.RADVORRANGROUTEN, IstStandard.STARTSTANDARD_RADNETZ],
        },
      };
      updateSelektierteKanten([kante1, kante2]);

      tick();

      // Bitte wieder component.formGroup.get('netzklassen')?.value nutzen, wenn RadNETZ-Klassen an Grundnetzkanten gesetzt werden können!
      const rawNetzklassenValue = (component.formGroup.get('netzklassen') as UntypedFormGroup).getRawValue();
      expect(rawNetzklassenValue.radnetzAlltag).toBeTrue();
      expect(rawNetzklassenValue.kreisnetzAlltag).toBeInstanceOf(UndeterminedValue);
      expect(rawNetzklassenValue.kommunalnetzAlltag).toBeInstanceOf(UndeterminedValue);
      expect(rawNetzklassenValue.radnetzFreizeit).toBeFalse();
      expect(rawNetzklassenValue.kreisnetzFreizeit).toBeFalse();
      expect(rawNetzklassenValue.kommunalnetzFreizeit).toBeFalse();
      expect(rawNetzklassenValue.radvorrangrouten).toBeFalse();
      expect(rawNetzklassenValue.radschnellverbindung).toBeFalse();

      expect(component.formGroup.get('istStandards')?.value.radnetzStartstandard).toBeTrue();
      expect(component.formGroup.get('istStandards')?.value.basisstandard).toBeInstanceOf(UndeterminedValue);
      expect(component.formGroup.get('istStandards')?.value.radvorrangrouten).toBeInstanceOf(UndeterminedValue);
      expect(component.formGroup.get('istStandards')?.value.radnetzZielstandard).toBeFalse();
      expect(component.formGroup.get('istStandards')?.value.radschnellverbindung).toBeFalse();
    }));

    it('should unflat netzklassen', fakeAsync(() => {
      const kante: Kante = {
        ...defaultKante,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          netzklassen: [Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADSCHNELLVERBINDUNG],
        },
      };
      updateSelektierteKanten([kante]);

      tick();

      component.formGroup.get('netzklassen')?.get('kommunalnetzAlltag')?.setValue(false);
      component.formGroup.get('netzklassen')?.get('kreisnetzAlltag')?.setValue(true);
      component.formGroup.markAsDirty();
      when(netzService.saveKanteAllgemein(anything())).thenReturn(Promise.resolve([kante]));
      component.onSave();
      tick();

      verify(netzService.saveKanteAllgemein(anything())).once();
      expect(
        (capture(netzService.saveKanteAllgemein).last()[0][0] as SaveKantenAttributGruppeCommand).netzklassen.sort()
      ).toEqual([Netzklasse.KREISNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADSCHNELLVERBINDUNG]);
    }));

    it('should unflat istStandards', fakeAsync(() => {
      const kante: Kante = {
        ...defaultKante,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          netzklassen: [Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADSCHNELLVERBINDUNG],
          istStandards: [
            IstStandard.BASISSTANDARD,
            IstStandard.STARTSTANDARD_RADNETZ,
            IstStandard.RADSCHNELLVERBINDUNG,
          ],
        },
      };
      updateSelektierteKanten([kante]);

      tick();

      component.formGroup.get('istStandards')?.get('basisstandard')?.setValue(false);
      component.formGroup.get('istStandards')?.get('radvorrangrouten')?.setValue(true);
      component.formGroup.markAsDirty();
      when(netzService.saveKanteAllgemein(anything())).thenReturn(Promise.resolve([kante]));
      component.onSave();
      tick();

      verify(netzService.saveKanteAllgemein(anything())).once();
      expect(
        (capture(netzService.saveKanteAllgemein).last()[0][0] as SaveKantenAttributGruppeCommand).istStandards.sort()
      ).toEqual([IstStandard.RADSCHNELLVERBINDUNG, IstStandard.RADVORRANGROUTEN, IstStandard.STARTSTANDARD_RADNETZ]);
    }));

    it('should overwrite equal values if undetermined', fakeAsync(() => {
      const kante1: Kante = {
        ...defaultKante,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          netzklassen: [Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADVORRANGROUTEN],
          istStandards: [
            IstStandard.BASISSTANDARD,
            // Bitte wieder einkommentieren, wenn RadNETZ-Klassen an Grundnetzkanten gesetzt werden können!
            // IstStandard.STARTSTANDARD_RADNETZ
          ],
        },
      };
      const kante2: Kante = {
        ...defaultKante,
        kantenAttributGruppe: {
          ...defaultKante.kantenAttributGruppe,
          netzklassen: [Netzklasse.KREISNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG],
          istStandards: [
            IstStandard.RADVORRANGROUTEN,
            // Bitte wieder einkommentieren, wenn RadNETZ-Klassen an Grundnetzkanten gesetzt werden können!
            // IstStandard.STARTSTANDARD_RADNETZ,
            IstStandard.RADSCHNELLVERBINDUNG,
          ],
        },
      };
      updateSelektierteKanten([kante1, kante2]);

      tick();

      component.formGroup.get('netzklassen')?.get('radvorrangrouten')?.setValue(true);
      component.formGroup.get('istStandards')?.get('radschnellverbindung')?.setValue(true);
      component.formGroup.markAsDirty();
      when(netzService.saveKanteAllgemein(anything())).thenReturn(Promise.resolve([kante1, kante2]));
      component.onSave();
      tick();

      verify(netzService.saveKanteAllgemein(anything())).once();
      const commands: SaveKantenAttributGruppeCommand[] = capture(netzService.saveKanteAllgemein).last()[0];
      expect(commands[0].netzklassen.sort()).toEqual([
        Netzklasse.KOMMUNALNETZ_ALLTAG,
        Netzklasse.RADNETZ_ALLTAG,
        Netzklasse.RADVORRANGROUTEN,
      ]);
      expect(commands[1].netzklassen.sort()).toEqual([
        Netzklasse.KREISNETZ_ALLTAG,
        Netzklasse.RADNETZ_ALLTAG,
        Netzklasse.RADVORRANGROUTEN,
      ]);
      expect(commands[0].istStandards.sort()).toEqual([
        IstStandard.BASISSTANDARD,
        IstStandard.RADSCHNELLVERBINDUNG,
        // Bitte wieder einkommentieren, wenn RadNETZ-Klassen an Grundnetzkanten gesetzt werden können!
        // IstStandard.STARTSTANDARD_RADNETZ,
      ]);
      expect(commands[1].istStandards.sort()).toEqual([
        IstStandard.RADSCHNELLVERBINDUNG,
        IstStandard.RADVORRANGROUTEN,
        // Bitte wieder einkommentieren, wenn RadNETZ-Klassen an Grundnetzkanten gesetzt werden können!
        // IstStandard.STARTSTANDARD_RADNETZ,
      ]);
    }));

    // Bitte wieder aktivieren, wenn RadNETZ-Klassen an Grundnetzkanten gesetzt werden können!
    xdescribe('disable forbidden IstStandards', () => {
      it('should disable RadNetzStandards when no Radnetz', fakeAsync(() => {
        const kante: Kante = {
          ...defaultKante,
          kantenAttributGruppe: {
            ...defaultKante.kantenAttributGruppe,
            netzklassen: [Netzklasse.KOMMUNALNETZ_ALLTAG],
          },
        };

        updateSelektierteKanten([kante]);
        tick();

        expect(component.formGroup.get('istStandards')?.get('radnetzStartstandard')?.disabled).toBeTrue();
        expect(component.formGroup.get('istStandards')?.get('radnetzZielstandard')?.disabled).toBeTrue();

        component.formGroup.get('netzklassen')?.get('radnetzAlltag')?.setValue(true);
        tick();

        expect(component.formGroup.get('istStandards')?.get('radnetzStartstandard')?.disabled).toBeFalse();
        expect(component.formGroup.get('istStandards')?.get('radnetzZielstandard')?.disabled).toBeFalse();
      }));

      it('should disable RadNetzStandards when not all have RadNetz', fakeAsync(() => {
        const kante1: Kante = {
          ...defaultKante,
          kantenAttributGruppe: {
            ...defaultKante.kantenAttributGruppe,
            netzklassen: [Netzklasse.KOMMUNALNETZ_ALLTAG],
          },
        };
        const kante2: Kante = {
          ...defaultKante,
          kantenAttributGruppe: {
            ...defaultKante.kantenAttributGruppe,
            netzklassen: [Netzklasse.RADNETZ_ALLTAG],
          },
        };

        updateSelektierteKanten([kante1, kante2]);
        tick();

        expect(component.formGroup.get('istStandards')?.get('radnetzStartstandard')?.disabled).toBeTrue();
        expect(component.formGroup.get('istStandards')?.get('radnetzZielstandard')?.disabled).toBeTrue();
      }));

      describe('with Undetermined values', () => {
        beforeEach(fakeAsync(() => {
          const kante1: Kante = {
            ...defaultKante,
            kantenAttributGruppe: {
              ...defaultKante.kantenAttributGruppe,
              netzklassen: [Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT],
            },
          };
          const kante2: Kante = {
            ...defaultKante,
            kantenAttributGruppe: {
              ...defaultKante.kantenAttributGruppe,
              netzklassen: [Netzklasse.RADNETZ_ALLTAG],
            },
          };

          updateSelektierteKanten([kante1, kante2]);
          tick();
        }));

        it('should not disable RadNetzStandards when all have RadNetz', fakeAsync(() => {
          expect(component.formGroup.get('istStandards')?.get('radnetzStartstandard')?.disabled).toBeFalse();
          expect(component.formGroup.get('istStandards')?.get('radnetzZielstandard')?.disabled).toBeFalse();
        }));

        it('should not disable RadNetzStandards when other netz selected', fakeAsync(() => {
          component.formGroup.get('netzklassen')?.get('kommunalnetzAlltag')?.setValue(true);
          tick();

          expect(component.formGroup.get('istStandards')?.get('radnetzStartstandard')?.disabled).toBeFalse();
          expect(component.formGroup.get('istStandards')?.get('radnetzZielstandard')?.disabled).toBeFalse();
        }));

        it('should disable RadNetzStandards when last radnetz for one ist unselected', fakeAsync(() => {
          component.formGroup.get('netzklassen')?.get('radnetzAlltag')?.setValue(false);
          tick();

          expect(component.formGroup.get('istStandards')?.get('radnetzStartstandard')?.disabled).toBeTrue();
          expect(component.formGroup.get('istStandards')?.get('radnetzZielstandard')?.disabled).toBeTrue();
        }));

        it('should disable RadNetzStandards after deselect radnetz when others still exist', fakeAsync(() => {
          component.formGroup.get('netzklassen')?.get('radnetzZielnetz')?.setValue(true);
          tick();

          expect(component.formGroup.get('istStandards')?.get('radnetzStartstandard')?.disabled).toBeFalse();
          expect(component.formGroup.get('istStandards')?.get('radnetzZielstandard')?.disabled).toBeFalse();

          component.formGroup.get('netzklassen')?.get('radnetzZielnetz')?.setValue(false);
          tick();

          expect(component.formGroup.get('istStandards')?.get('radnetzStartstandard')?.disabled).toBeFalse();
          expect(component.formGroup.get('istStandards')?.get('radnetzZielstandard')?.disabled).toBeFalse();
        }));
      });

      it('should read disabled values', fakeAsync(() => {
        const kante1: Kante = {
          ...defaultKante,
          kantenAttributGruppe: {
            ...defaultKante.kantenAttributGruppe,
            netzklassen: [Netzklasse.KOMMUNALNETZ_ALLTAG],
            istStandards: [IstStandard.BASISSTANDARD],
          },
        };
        const kante2: Kante = {
          ...defaultKante,
          kantenAttributGruppe: {
            ...defaultKante.kantenAttributGruppe,
            netzklassen: [Netzklasse.RADNETZ_ALLTAG],
            istStandards: [IstStandard.BASISSTANDARD, IstStandard.ZIELSTANDARD_RADNETZ],
          },
        };

        updateSelektierteKanten([kante1, kante2]);
        tick();

        expect(component.formGroup.get('istStandards')?.get('radnetzStartstandard')?.disabled).toBeTrue();
        expect(component.formGroup.get('istStandards')?.get('radnetzZielstandard')?.disabled).toBeTrue();

        component.formGroup.markAsDirty();
        when(netzService.saveKanteAllgemein(anything())).thenReturn(Promise.resolve([kante1, kante2]));
        component.onSave();
        tick();

        verify(netzService.saveKanteAllgemein(anything())).once();
        const commands: SaveKantenAttributGruppeCommand[] = capture(netzService.saveKanteAllgemein).last()[0];

        expect(commands[0].istStandards.sort()).toEqual([IstStandard.BASISSTANDARD]);
        expect(commands[1].istStandards.sort()).toEqual([IstStandard.BASISSTANDARD, IstStandard.ZIELSTANDARD_RADNETZ]);
      }));

      it('should remove forbidden values if radnetz changes', fakeAsync(() => {
        const kante: Kante = {
          ...defaultKante,
          kantenAttributGruppe: {
            ...defaultKante.kantenAttributGruppe,
            netzklassen: [Netzklasse.RADNETZ_ZIELNETZ],
            istStandards: [IstStandard.STARTSTANDARD_RADNETZ],
          },
        };

        updateSelektierteKanten([kante]);
        tick();

        // eslint-disable-next-line @typescript-eslint/dot-notation
        expect(component['getIstStandardsFromGroup']().getRawValue().radnetzStartstandard).toBeTrue();
        component.formGroup.get('netzklassen')?.get('radnetzZielnetz')?.setValue(false);
        tick();

        // eslint-disable-next-line @typescript-eslint/dot-notation
        expect(component['getIstStandardsFromGroup']().getRawValue().radnetzStartstandard).toBeFalse();
        component.formGroup.markAsDirty();
        when(netzService.saveKanteAllgemein(anything())).thenReturn(Promise.resolve([kante]));
        component.onSave();
        tick();

        verify(netzService.saveKanteAllgemein(anything())).once();
        const commands: SaveKantenAttributGruppeCommand[] = capture(netzService.saveKanteAllgemein).last()[0];
        expect(commands[0].istStandards).toEqual([]);
      }));

      it('should remove forbidden values if radnetz changes - multiple values', fakeAsync(() => {
        const kante1: Kante = {
          ...defaultKante,
          kantenAttributGruppe: {
            ...defaultKante.kantenAttributGruppe,
            netzklassen: [Netzklasse.RADNETZ_FREIZEIT],
            istStandards: [IstStandard.STARTSTANDARD_RADNETZ],
          },
        };

        const kante2: Kante = {
          ...defaultKante,
          kantenAttributGruppe: {
            ...defaultKante.kantenAttributGruppe,
            netzklassen: [Netzklasse.RADNETZ_ZIELNETZ],
            istStandards: [IstStandard.STARTSTANDARD_RADNETZ],
          },
        };

        updateSelektierteKanten([kante1, kante2]);
        tick();

        // eslint-disable-next-line @typescript-eslint/dot-notation
        expect(component['getIstStandardsFromGroup']().getRawValue().radnetzStartstandard).toBeTrue();
        component.formGroup.get('netzklassen')?.get('radnetzFreizeit')?.setValue(false);
        tick();
        // eslint-disable-next-line @typescript-eslint/dot-notation
        expect(component['getIstStandardsFromGroup']().getRawValue().radnetzStartstandard).toBeInstanceOf(
          UndeterminedValue
        );

        component.formGroup.markAsDirty();
        when(netzService.saveKanteAllgemein(anything())).thenReturn(Promise.resolve([kante1, kante2]));
        component.onSave();
        tick();

        verify(netzService.saveKanteAllgemein(anything())).once();
        const commands: SaveKantenAttributGruppeCommand[] = capture(netzService.saveKanteAllgemein).last()[0];
        expect(commands[0].istStandards).toEqual([]);
        expect(commands[1].istStandards).toEqual([IstStandard.STARTSTANDARD_RADNETZ]);
      }));
    });
  });
  describe('edit RadNETZ', () => {
    it('should disable form if RadNETZ-Kante is selected', fakeAsync(() => {
      const kante: Kante = {
        ...defaultKante,
        quelle: QuellSystem.RadNETZ,
      };
      updateSelektierteKanten([kante]);

      tick();

      expect(component.formGroup.disabled).toBeTrue();
    }));

    it('should reanable correct controls when last RadNETZ-Kante is deselected', fakeAsync(() => {
      const kanteRadNETZ: Kante = {
        ...defaultKante,
        quelle: QuellSystem.RadNETZ,
      };
      const kanteDLM: Kante = {
        ...defaultKante,
        quelle: QuellSystem.DLM,
      };
      updateSelektierteKanten([kanteRadNETZ, kanteDLM]);

      tick();

      expect(component.formGroup.disabled).toBeTrue();

      updateSelektierteKanten([kanteDLM]);

      tick();

      expect(component.formGroup.disabled).toBeFalse();
      expect(component.formGroup.get('landkreis')?.disabled).toBeTrue();
      expect(component.formGroup.get('laengeBerechnet')?.disabled).toBeTrue();
    }));
  });
  describe('handle readOnly-Kanten', () => {
    it('should disable form if Kante is readonly', fakeAsync(() => {
      const kante: Kante = {
        ...defaultKante,
        liegtInZustaendigkeitsbereich: false,
      };
      updateSelektierteKanten([kante]);

      tick();

      expect(component.formGroup.disabled).toBeTrue();
    }));

    it('should reanable correct controls when last readonly-Kante is deselected', fakeAsync(() => {
      const editableKante: Kante = {
        ...defaultKante,
        liegtInZustaendigkeitsbereich: false,
      };
      const readonlyKante: Kante = {
        ...defaultKante,
        liegtInZustaendigkeitsbereich: true,
      };
      updateSelektierteKanten([editableKante, readonlyKante]);

      tick();

      expect(component.formGroup.disabled).toBeTrue();

      updateSelektierteKanten([readonlyKante]);

      tick();

      expect(component.formGroup.disabled).toBeFalse();
      expect(component.formGroup.get('landkreis')?.disabled).toBeTrue();
      expect(component.formGroup.get('laengeBerechnet')?.disabled).toBeTrue();
    }));
  });

  const updateSelektierteKanten = (kanten: Kante[]): void => {
    when(kantenSelektionService.selektierteKanten).thenReturn(kanten);
    when(kantenSelektionService.selektion).thenReturn(kanten.map(k => KantenSelektion.ofGesamteKante(k)));
    kantenSubject$.next(kanten);
    kantenSelektionSubject$.next(kanten.map(k => KantenSelektion.ofGesamteKante(k)));
  };
});
