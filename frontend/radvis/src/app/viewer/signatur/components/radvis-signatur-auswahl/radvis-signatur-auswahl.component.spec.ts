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
import { By } from '@angular/platform-browser';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { of, Subject } from 'rxjs';
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { RadvisSignaturAuswahlComponent } from 'src/app/viewer/signatur/components/radvis-signatur-auswahl/radvis-signatur-auswahl.component';
import { SignaturService } from 'src/app/viewer/signatur/services/signatur.service';
import { SignaturModule } from 'src/app/viewer/signatur/signatur.module';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { instance, mock, when } from 'ts-mockito';

describe(RadvisSignaturAuswahlComponent.name, () => {
  let fixture: MockedComponentFixture<RadvisSignaturAuswahlComponent>;
  let component: RadvisSignaturAuswahlComponent;

  let signaturService: SignaturService;
  let featureTogglzService: FeatureTogglzService;
  let infrastrukturenSelectionService: InfrastrukturenSelektionService;

  const selectedInfrastrukturenSubject: Subject<Infrastruktur[]> = new Subject<Infrastruktur[]>();

  beforeEach(() => {
    signaturService = mock(SignaturService);
    when(signaturService.getSignaturen()).thenReturn(of([]));

    featureTogglzService = mock(FeatureTogglzService);
    infrastrukturenSelectionService = mock(InfrastrukturenSelektionService);
    when(infrastrukturenSelectionService.selektierteInfrastrukturen$).thenReturn(
      selectedInfrastrukturenSubject.asObservable()
    );

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
        provide: FeatureTogglzService,
        useValue: instance(featureTogglzService),
      });
  });

  beforeEach(fakeAsync(() => {
    fixture = MockRender(RadvisSignaturAuswahlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
    tick();
  }));

  it('should init form correctly', () => {
    expect(component.formControl.value).toEqual(null);
  });

  describe('emit changes', () => {
    it('should emit', fakeAsync(() => {
      const signatur = { name: 'Signatür', typ: SignaturTyp.MASSNAHME };

      when(signaturService.getSignaturen()).thenReturn(of([signatur, { name: 'Sigmatur', typ: SignaturTyp.NETZ }]));
      const emitterSpy = spyOn(component.selectRadVisSignatur, 'emit');
      component.formControl.patchValue(signatur);
      expect(emitterSpy).toHaveBeenCalled();
      expect(emitterSpy.calls.mostRecent().args[0]).toEqual(signatur);
    }));
  });

  describe('onInit', () => {
    it('should show options in dropdown menu', fakeAsync(() => {
      when(signaturService.getSignaturen()).thenReturn(
        of([
          { name: 'Harry', typ: SignaturTyp.NETZ },
          { name: 'Pppppotter', typ: SignaturTyp.NETZ },
        ])
      );
      const matSelect = fixture.debugElement.query(By.css('mat-select')).nativeElement;
      matSelect.click();

      selectedInfrastrukturenSubject.next([]);

      fixture.detectChanges();
      const allOptions = fixture.debugElement.queryAll(By.css('mat-option'));
      // Neben den 3 oben gepushten Signaturen gibt es auch noch 'Keine' als Auswahl
      expect(allOptions.length).toEqual(3);
    }));
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
      when(signaturService.getSignaturen()).thenReturn(of(alleSignaturen));
    });

    it('should show all signaturen when massnahmen selected', () => {
      selectedInfrastrukturenSubject.next([MASSNAHMEN]);

      expect(component.signaturenSubject$.value).toEqual(alleSignaturen);
    });

    it('should show filtered signaturen when massnahmen not selected', () => {
      selectedInfrastrukturenSubject.next([]);

      expect(component.signaturenSubject$.value).toEqual(keineMassnahmenSignaturen);
    });
  });
});
