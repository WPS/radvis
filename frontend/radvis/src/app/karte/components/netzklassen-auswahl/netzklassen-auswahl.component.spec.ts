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

import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatToolbar } from '@angular/material/toolbar';
import { MockComponent } from 'ng-mocks';
import { Subject } from 'rxjs';
import { NetzklassenAuswahlComponent } from 'src/app/karte/components/netzklassen-auswahl/netzklassen-auswahl.component';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { anything, instance, mock, verify, when } from 'ts-mockito';

describe('NetzklassenAuswahlComponent', () => {
  let component: NetzklassenAuswahlComponent;
  let fixture: ComponentFixture<NetzklassenAuswahlComponent>;
  let netzklassenAuswahlSubject$: Subject<Netzklassefilter[]>;
  let netzklassenAuswahlService: NetzklassenAuswahlService;

  beforeEach(async () => {
    netzklassenAuswahlSubject$ = new Subject<Netzklassefilter[]>();
    netzklassenAuswahlService = mock(NetzklassenAuswahlService);
    when(netzklassenAuswahlService.currentAuswahl$).thenReturn(netzklassenAuswahlSubject$);
    await TestBed.configureTestingModule({
      providers: [
        {
          provide: NetzklassenAuswahlService,
          useValue: instance(netzklassenAuswahlService),
        },
      ],
      declarations: [NetzklassenAuswahlComponent, MockComponent(MatCheckbox), MockComponent(MatToolbar)],
      imports: [ReactiveFormsModule],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(NetzklassenAuswahlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('netzklassen output', () => {
    it('should fire correctly when checkbox selected', fakeAsync(() => {
      component.onNetzklassenAuswahlChange(Netzklassefilter.RADNETZ, true);

      verify(netzklassenAuswahlService.selectNetzklasse(Netzklassefilter.RADNETZ)).once();
      verify(netzklassenAuswahlService.deselectNetzklasse(anything())).never();
      expect().nothing();
    }));

    it('should fire correctly when checkbox deselected', () => {
      component.onNetzklassenAuswahlChange(Netzklassefilter.RADNETZ, false);

      verify(netzklassenAuswahlService.selectNetzklasse(anything())).never();
      verify(netzklassenAuswahlService.deselectNetzklasse(Netzklassefilter.RADNETZ)).once();
      expect().nothing();
    });
  });
});
