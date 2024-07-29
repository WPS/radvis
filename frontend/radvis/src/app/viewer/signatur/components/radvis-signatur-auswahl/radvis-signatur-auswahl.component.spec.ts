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

import { fakeAsync, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { RadvisSignaturAuswahlComponent } from 'src/app/viewer/signatur/components/radvis-signatur-auswahl/radvis-signatur-auswahl.component';
import { SignaturService } from 'src/app/viewer/signatur/services/signatur.service';
import { SignaturModule } from 'src/app/viewer/signatur/signatur.module';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { anything, capture, instance, mock, resetCalls, verify, when } from 'ts-mockito';

describe(RadvisSignaturAuswahlComponent.name, () => {
  let fixture: MockedComponentFixture<RadvisSignaturAuswahlComponent>;
  let component: RadvisSignaturAuswahlComponent;

  let signaturService: SignaturService;
  let featureTogglzService: FeatureTogglzService;
  let infrastrukturenSelectionService: InfrastrukturenSelektionService;
  let netzklassenAuswahlService: NetzklassenAuswahlService;

  const selectedInfrastrukturenSubject: BehaviorSubject<Infrastruktur[]> = new BehaviorSubject<Infrastruktur[]>([]);
  const selectedNetzklasseSubject: BehaviorSubject<Netzklassefilter[]> = new BehaviorSubject<Netzklassefilter[]>([
    Netzklassefilter.RADNETZ,
  ]);

  beforeEach(() => {
    signaturService = mock(SignaturService);
    when(signaturService.getSignaturen(anything())).thenReturn([]);

    featureTogglzService = mock(FeatureTogglzService);
    infrastrukturenSelectionService = mock(InfrastrukturenSelektionService);
    when(infrastrukturenSelectionService.selektierteInfrastrukturen$).thenReturn(
      selectedInfrastrukturenSubject.asObservable()
    );
    when(infrastrukturenSelectionService.isSelected(anything())).thenCall((infrastruktur: Infrastruktur) => {
      return selectedInfrastrukturenSubject.value.includes(infrastruktur);
    });

    netzklassenAuswahlService = mock(NetzklassenAuswahlService);
    when(netzklassenAuswahlService.currentAuswahl$).thenReturn(selectedNetzklasseSubject.asObservable());
    when(netzklassenAuswahlService.currentAuswahl).thenCall(() => selectedNetzklasseSubject.value);

    return MockBuilder(RadvisSignaturAuswahlComponent, SignaturModule)
      .provide({
        provide: SignaturService,
        useValue: instance(signaturService),
      })
      .provide({
        provide: InfrastrukturenSelektionService,
        useValue: instance(infrastrukturenSelectionService),
      })
      .provide({
        provide: NetzklassenAuswahlService,
        useValue: instance(netzklassenAuswahlService),
      })
      .provide({
        provide: FeatureTogglzService,
        useValue: instance(featureTogglzService),
      });
  });

  beforeEach(waitForAsync(() => {
    fixture = MockRender(RadvisSignaturAuswahlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  }));

  it('should init form correctly', () => {
    expect(component.formControl.value).toEqual(null);
  });

  describe('emit changes', () => {
    it('should emit', fakeAsync(() => {
      const signatur = { name: 'Signatür', typ: SignaturTyp.MASSNAHME };

      when(signaturService.getSignaturen(anything())).thenReturn([
        signatur,
        { name: 'Sigmatur', typ: SignaturTyp.NETZ },
      ]);
      const emitterSpy = spyOn(component.selectRadVisSignatur, 'emit');
      component.formControl.patchValue(signatur);
      expect(emitterSpy).toHaveBeenCalled();
      expect(emitterSpy.calls.mostRecent().args[0]).toEqual(signatur);
    }));
  });

  describe('onInit', () => {
    it('should show options in dropdown menu', fakeAsync(() => {
      when(signaturService.getSignaturen(anything())).thenReturn([
        { name: 'Harry', typ: SignaturTyp.NETZ },
        { name: 'Pppppotter', typ: SignaturTyp.NETZ },
      ]);
      const matSelect = fixture.debugElement.query(By.css('mat-select')).nativeElement;
      matSelect.click();

      selectedInfrastrukturenSubject.next([]);

      fixture.detectChanges();
      const allOptions = fixture.debugElement.queryAll(By.css('mat-option'));
      // Neben den 3 oben gepushten Signaturen gibt es auch noch 'Keine' als Auswahl
      expect(allOptions.length).toEqual(3);
    }));
  });

  describe('determine compatibility with netzklassen', () => {
    const radNetzSignatur = { name: 'RadNETZ', typ: SignaturTyp.NETZ };
    const netzklassenSignatur = { name: 'Netzklassen', typ: SignaturTyp.NETZ };
    const sonstigeSignatur = { name: 'Sonstiges', typ: SignaturTyp.NETZ };
    const massnahmeSignatur = { name: 'RadNETZ-Maßnahme', typ: SignaturTyp.MASSNAHME };

    beforeEach(() => {
      when(signaturService.getSignaturen(anything())).thenReturn([
        radNetzSignatur,
        netzklassenSignatur,
        sonstigeSignatur,
        massnahmeSignatur,
      ]);
    });

    describe('without any Signatur selection', () => {
      it('should be compatible with netzklasse RadNETZ', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.RADNETZ]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeTrue();
      });

      it('should be compatible with netzklasse Kreisnetz', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.KREISNETZ]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeTrue();
      });

      it('should be compatible with nicht klassifiziert', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeTrue();
      });
    });

    describe('with RadNETZ Signatur selected', () => {
      beforeEach(() => {
        component.formControl.setValue({ name: 'RadNETZ', typ: SignaturTyp.NETZ });
      });

      it('should be compatible with netzklasse RadNETZ', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.RADNETZ]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeTrue();
      });

      it('should not be compatible with netzklasse Kreisnetz', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.KREISNETZ]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeFalse();
      });

      it('should not be compatible with nicht klassifiziert', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeFalse();
      });
    });

    describe('with RadNETZ Signatur and multiple Netzklassen selected', () => {
      beforeEach(() => {
        component.formControl.setValue({ name: 'RadNETZ', typ: SignaturTyp.NETZ });
      });

      it('should be compatible with netzklasse RadNETZ', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.RADNETZ, Netzklassefilter.NICHT_KLASSIFIZIERT]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeTrue();
      });

      it('should be compatible with netzklasse RadNETZ', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.RADNETZ, Netzklassefilter.KREISNETZ]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeTrue();
      });

      it('should not be compatible with netzklasse RadNETZ', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.NICHT_KLASSIFIZIERT, Netzklassefilter.KREISNETZ]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeFalse();
      });
    });

    describe('with Netzklassen Signatur selected', () => {
      beforeEach(() => {
        component.formControl.setValue({ name: 'Netzklassen', typ: SignaturTyp.NETZ });
      });

      it('should be compatible with netzklasse RadNETZ', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.RADNETZ]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeTrue();
      });

      it('should be compatible with netzklasse Kreisnetz', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.KREISNETZ]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeTrue();
      });

      it('should not be compatible with nicht klassifiziert', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeFalse();
      });
    });

    describe('with Belagart Signatur selected', () => {
      beforeEach(() => {
        component.formControl.setValue({ name: 'Belagart', typ: SignaturTyp.NETZ });
      });

      it('should be compatible with netzklasse RadNETZ', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.RADNETZ]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeTrue();
      });

      it('should be compatible with netzklasse Kreisnetz', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.KREISNETZ]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeTrue();
      });

      it('should be compatible with nicht klassifiziert', () => {
        selectedNetzklasseSubject.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        expect(component.isSelectionCompatibleWithNetzklassenfilter()).toBeTrue();
      });
    });
  });

  describe('loadSignaturen', () => {
    let alleSignaturen: Signatur[];
    let keineMassnahmenSignaturen: Signatur[];

    beforeEach(() => {
      alleSignaturen = [
        { name: 'RadNETZ', typ: SignaturTyp.MASSNAHME },
        { name: 'Maßnahmen - Ist endlich fertig?', typ: SignaturTyp.NETZ },
        { name: 'Super duper Signatur', typ: SignaturTyp.MASSNAHME },
        { name: 'Maßnahmen - Was letzte Preis?', typ: SignaturTyp.NETZ },
      ];
      keineMassnahmenSignaturen = alleSignaturen.filter(s => s.typ !== SignaturTyp.MASSNAHME);
    });

    it('should show all signaturen when massnahmen selected', fakeAsync(() => {
      resetCalls(signaturService);
      when(signaturService.getSignaturen(anything())).thenReturn(alleSignaturen);
      selectedInfrastrukturenSubject.next([MASSNAHMEN]);
      tick();

      verify(signaturService.getSignaturen(anything())).once();
      expect(capture(signaturService.getSignaturen).last()[0]).toEqual([SignaturTyp.MASSNAHME, SignaturTyp.NETZ]);
      expect(component.signaturen).toEqual(alleSignaturen);
    }));

    it('should show filtered signaturen when massnahmen not selected', () => {
      resetCalls(signaturService);
      when(signaturService.getSignaturen(anything())).thenReturn(keineMassnahmenSignaturen);
      selectedInfrastrukturenSubject.next([]);

      verify(signaturService.getSignaturen(anything())).once();
      expect(capture(signaturService.getSignaturen).last()[0]).toEqual([SignaturTyp.NETZ]);
      expect(component.signaturen).toEqual(keineMassnahmenSignaturen);
    });
  });
});
