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
import { MockBuilder, MockRender, MockedComponentFixture } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { NetzklassenAuswahlComponent } from 'src/app/karte/components/netzklassen-auswahl/netzklassen-auswahl.component';
import { KarteModule } from 'src/app/karte/karte.module';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';
import { anything, instance, mock, verify, when } from 'ts-mockito';

describe('NetzklassenAuswahlComponent', () => {
  let component: NetzklassenAuswahlComponent;
  let fixture: MockedComponentFixture<NetzklassenAuswahlComponent>;
  let netzklassenAuswahlSubject$: BehaviorSubject<Netzklassefilter[]>;
  let netzklassenAuswahlService: NetzklassenAuswahlService;

  beforeEach(() => {
    netzklassenAuswahlSubject$ = new BehaviorSubject<Netzklassefilter[]>([Netzklassefilter.RADNETZ]);
    netzklassenAuswahlService = mock(NetzklassenAuswahlService);
    when(netzklassenAuswahlService.currentAuswahl$).thenReturn(netzklassenAuswahlSubject$);
    when(netzklassenAuswahlService.currentAuswahl).thenCall(() => netzklassenAuswahlSubject$.value);

    return MockBuilder([NetzklassenAuswahlComponent, ReactiveFormsModule], KarteModule).provide({
      provide: NetzklassenAuswahlService,
      useValue: instance(netzklassenAuswahlService),
    });
  });

  beforeEach(() => {
    fixture = MockRender(NetzklassenAuswahlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('is Signatur Compatible', () => {
    it('should be false if no signatur selected', () => {
      netzklassenAuswahlSubject$.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
      fixture = MockRender(NetzklassenAuswahlComponent, { selectedSignatur: null } as NetzklassenAuswahlComponent, {
        reset: true,
      });
      fixture.detectChanges();
      expect(fixture.point.componentInstance['showSignaturIncompatibleHinweis']).toBe(false);
    });

    it('should unsubscribe', () => {
      fixture.destroy();

      expect(netzklassenAuswahlSubject$.observers.length).toBe(0);
    });

    describe('with RadNETZ-Signatur', () => {
      beforeEach(() => {
        fixture = MockRender(
          NetzklassenAuswahlComponent,
          {
            selectedSignatur: { name: 'RadNETZ', typ: SignaturTyp.NETZ },
          } as NetzklassenAuswahlComponent,
          { reset: true, detectChanges: false }
        );
      });

      it('should be true if nur nicht klassifiziert', () => {
        netzklassenAuswahlSubject$.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        fixture.detectChanges();
        expect(fixture.point.componentInstance['showSignaturIncompatibleHinweis']).toBe(true);
      });

      it('should be false if radNETZ visible', () => {
        netzklassenAuswahlSubject$.next([Netzklassefilter.RADNETZ]);
        fixture.detectChanges();
        expect(fixture.point.componentInstance['showSignaturIncompatibleHinweis']).toBe(false);
      });
    });

    describe('with Netzklassen-Signatur', () => {
      beforeEach(() => {
        fixture = MockRender(
          NetzklassenAuswahlComponent,
          {
            selectedSignatur: { name: 'Netzklassen', typ: SignaturTyp.NETZ },
          } as NetzklassenAuswahlComponent,
          { reset: true, detectChanges: false }
        );
      });

      it('should be true if nur nicht klassifiziert', () => {
        netzklassenAuswahlSubject$.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        fixture.detectChanges();
        expect(fixture.point.componentInstance['showSignaturIncompatibleHinweis']).toBe(true);
      });

      it('should be false if Kreisnetz ausgewählt', () => {
        netzklassenAuswahlSubject$.next([Netzklassefilter.KREISNETZ]);
        fixture.detectChanges();
        expect(fixture.point.componentInstance['showSignaturIncompatibleHinweis']).toBe(false);
      });
    });

    it('should change according to selectedNetzklassen', fakeAsync(() => {
      netzklassenAuswahlSubject$.next([]);
      fixture = MockRender(
        NetzklassenAuswahlComponent,
        {
          selectedSignatur: { name: 'RadNETZ', typ: SignaturTyp.NETZ },
        } as NetzklassenAuswahlComponent,
        { reset: true }
      );
      expect(fixture.point.componentInstance['showSignaturIncompatibleHinweis']).toBe(false);

      netzklassenAuswahlSubject$.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
      tick();
      expect(fixture.point.componentInstance['showSignaturIncompatibleHinweis']).toBe(true);

      netzklassenAuswahlSubject$.next([Netzklassefilter.NICHT_KLASSIFIZIERT, Netzklassefilter.RADNETZ]);
      tick();
      expect(fixture.point.componentInstance['showSignaturIncompatibleHinweis']).toBe(false);
    }));
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
