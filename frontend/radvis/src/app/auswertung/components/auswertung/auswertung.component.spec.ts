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

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatCheckbox, MatCheckboxModule } from '@angular/material/checkbox';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AuswertungService } from 'src/app/auswertung/services/auswertung.service';
import { MaterialDesignModule } from 'src/app/material-design.module';
import { KommazahlPipe } from 'src/app/shared/components/kommazahl.pipe';
import { OrganisationenDropdownControlComponent } from 'src/app/shared/components/organisationen-dropdown-control/organisationen-dropdown-control.component';
import { IstStandard } from 'src/app/shared/models/ist-standard';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { AuswertungComponent } from './auswertung.component';

describe('AuswertungComponent', () => {
  let component: AuswertungComponent;
  let fixture: ComponentFixture<AuswertungComponent>;

  let auswertungService: AuswertungService;

  let checkboxUseNetzklassen: MatCheckbox;
  let checkboxUseIstStandards: MatCheckbox;

  beforeEach(async () => {
    auswertungService = mock(AuswertungService);

    await TestBed.configureTestingModule({
      declarations: [AuswertungComponent, KommazahlPipe, OrganisationenDropdownControlComponent],
      imports: [
        HttpClientTestingModule,
        ReactiveFormsModule,
        MatCheckboxModule,
        NoopAnimationsModule,
        MaterialDesignModule,
        MatCardModule,
      ],
      providers: [{ provide: AuswertungService, useValue: instance(auswertungService) }],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AuswertungComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    checkboxUseNetzklassen = fixture.debugElement.query(By.css('[formControlName="useNetzklassen"]')).componentInstance;
    checkboxUseIstStandards = fixture.debugElement.query(By.css('[formControlName="useIstStandards"]'))
      .componentInstance;
  });

  describe('getAuswertung', () => {
    it('should read correct values from Form', fakeAsync(() => {
      component.form.patchValue({
        gemeindeKreisBezirk: defaultOrganisation,
        useNetzklassen: true,
        netzklassen: [null, null, null, true, null, null, null, null, null, null],
        useIstStandards: true,
        istStandards: [true, null, null, null, null, null],
        baulast: defaultOrganisation,
        unterhalt: null,
        erhalt: null,
      });

      when(auswertungService.getAuswertung(anything())).thenReturn(Promise.resolve(0.0));
      component.getAuswertung();
      tick();

      verify(auswertungService.getAuswertung(anything())).once();
      expect(capture(auswertungService.getAuswertung).last()[0]).toEqual({
        gemeindeKreisBezirkId: defaultOrganisation.id,
        netzklassen: [Netzklasse.KREISNETZ_ALLTAG],
        beachteNichtKlassifizierteKanten: false,
        istStandards: [IstStandard.BASISSTANDARD],
        beachteKantenOhneStandards: false,
        baulastId: defaultOrganisation.id,
        unterhaltId: '',
        erhaltId: '',
      });

      component.form.patchValue({
        gemeindeKreisBezirk: null,
        useNetzklassen: true,
        netzklassen: [true, null, null, null, null, null, null, null, null, true],
        useIstStandards: true,
        istStandards: [null, true, null, null, null, true],
        baulast: null,
        unterhalt: { ...defaultOrganisation, id: 2 },
        erhalt: { ...defaultOrganisation, id: 3 },
      });

      when(auswertungService.getAuswertung(anything())).thenReturn(Promise.resolve(0.0));
      component.getAuswertung();
      tick();

      verify(auswertungService.getAuswertung(anything())).twice();
      expect(capture(auswertungService.getAuswertung).last()[0]).toEqual({
        gemeindeKreisBezirkId: '',
        netzklassen: [Netzklasse.RADNETZ_ALLTAG],
        beachteNichtKlassifizierteKanten: true,
        istStandards: [IstStandard.RADVORRANGROUTEN],
        beachteKantenOhneStandards: true,
        baulastId: '',
        unterhaltId: 2,
        erhaltId: 3,
      });
    }));
  });

  describe('checkbox-group filter', () => {
    it('should check all boxes', () => {
      checkboxUseNetzklassen.change.emit({ checked: true, source: checkboxUseNetzklassen });
      checkboxUseIstStandards.change.emit({ checked: true, source: checkboxUseIstStandards });

      expect(component.netzklassenFormArray.value).toEqual([
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
      ]);

      expect(component.istStandardsFormArray.value).toEqual([true, true, true, true, true, true]);
    });

    it('should uncheck filter when last box is unchecked', fakeAsync(() => {
      component.form.patchValue({
        useNetzklassen: true,
        netzklassen: [true, true, true, true, true, true, true, true, true, true],
        useIstStandards: true,
        istStandards: [true, true, true, true, true, true],
      });

      expect(component.form.get('useNetzklassen')?.value).toBeTrue();
      expect(component.form.get('useIstStandards')?.value).toBeTrue();

      component.form.patchValue({
        netzklassen: [false, false, false, false, false, false, false, false, false, false],
        istStandards: [false, false, false, false, false, false],
      });

      expect(component.form.get('useNetzklassen')?.value).toBeFalse();
      expect(component.form.get('useIstStandards')?.value).toBeFalse();
    }));

    it('should check filter when a box is checked', fakeAsync(() => {
      component.form.patchValue({
        useNetzklassen: false,
        netzklassen: [false, false, false, false, false, false, false, false, false, false],
        useIstStandards: false,
        istStandards: [false, false, false, false, false, false],
      });

      expect(component.form.get('useNetzklassen')?.value).toBeFalse();
      expect(component.form.get('useIstStandards')?.value).toBeFalse();

      component.form.patchValue({
        netzklassen: [true, false, false, false, false, false, false, false, false, false],
        istStandards: [true, false, false, false, false, false],
      });

      expect(component.form.get('useNetzklassen')?.value).toBeTrue();
      expect(component.form.get('useIstStandards')?.value).toBeTrue();
    }));
  });
});
