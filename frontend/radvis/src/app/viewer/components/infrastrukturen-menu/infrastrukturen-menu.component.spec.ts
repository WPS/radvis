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
import { MatIcon } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MockComponent } from 'ng-mocks';
import { of, Subject } from 'rxjs';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { InfrastrukturenMenuComponent } from 'src/app/viewer/components/infrastrukturen-menu/infrastrukturen-menu.component';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { Infrastruktur, InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { anything, instance, mock, verify, when } from 'ts-mockito';

describe(InfrastrukturenMenuComponent.name, () => {
  let component: InfrastrukturenMenuComponent;
  let fixture: ComponentFixture<InfrastrukturenMenuComponent>;
  let selektierteInfrastrukturenSubject$: Subject<Infrastruktur[]>;
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;
  let viewerRoutingService: ViewerRoutingService;

  let featureTogglzService: FeatureTogglzService;
  let weitereKartenebenenService: WeitereKartenebenenService;

  beforeEach(() => {
    selektierteInfrastrukturenSubject$ = new Subject<Infrastruktur[]>();
    infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    viewerRoutingService = mock(ViewerRoutingService);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(selektierteInfrastrukturenSubject$);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen).thenReturn([]);

    featureTogglzService = mock(FeatureTogglzService);
    when(featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_FAHRRADROUTE)).thenReturn(true);

    weitereKartenebenenService = mock(WeitereKartenebenenService);
    when(weitereKartenebenenService.weitereKartenebenen$).thenReturn(of([]));
    when(weitereKartenebenenService.selectedWeitereKartenebenen$).thenReturn(of([]));

    return TestBed.configureTestingModule({
      providers: [
        {
          provide: InfrastrukturenSelektionService,
          useValue: instance(infrastrukturenSelektionService),
        },
        {
          provide: FeatureTogglzService,
          useValue: instance(featureTogglzService),
        },
        {
          provide: WeitereKartenebenenService,
          useValue: instance(weitereKartenebenenService),
        },
        {
          provide: ViewerRoutingService,
          useValue: instance(viewerRoutingService),
        },
        {
          provide: InfrastrukturToken,
          useValue: [MASSNAHMEN, FAHRRADROUTE],
        },
      ],
      declarations: [InfrastrukturenMenuComponent, MockComponent(MatCheckbox), MockComponent(MatIcon)],
      imports: [ReactiveFormsModule, MatTooltipModule, NoopAnimationsModule],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(InfrastrukturenMenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  const testFeatureTogglz = (fahrradrouteOn: boolean, expectedFeatures: Infrastruktur[]): void => {
    it(`should have ${expectedFeatures.join() || 'nothing'}`, () => {
      when(featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_FAHRRADROUTE)).thenReturn(fahrradrouteOn);

      fixture = TestBed.createComponent(InfrastrukturenMenuComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.infrastrukturenAllgemein).toEqual(expectedFeatures);
    });
  };

  describe('infrastrukturenAnalyseDefaultVisible', () => {
    it('should be true if ANPASSUNGSWUNSCH initially selected', () => {
      when(infrastrukturenSelektionService.selektierteInfrastrukturen).thenReturn([ANPASSUNGSWUNSCH]);
      fixture = TestBed.createComponent(InfrastrukturenMenuComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.infrastrukturenAnalyseDefaultVisible).toBe(true);
    });

    it('should be false if only allgemeine initially selected', () => {
      when(infrastrukturenSelektionService.selektierteInfrastrukturen).thenReturn([FAHRRADROUTE]);
      fixture = TestBed.createComponent(InfrastrukturenMenuComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.infrastrukturenAnalyseDefaultVisible).toBe(false);
    });

    it('should be false if empty', () => {
      when(infrastrukturenSelektionService.selektierteInfrastrukturen).thenReturn([]);
      fixture = TestBed.createComponent(InfrastrukturenMenuComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.infrastrukturenAnalyseDefaultVisible).toBe(false);
    });
  });

  describe('Togglz and alleAktivenInfrastrukturen should match', () => {
    testFeatureTogglz(false, [MASSNAHMEN]);
    testFeatureTogglz(true, [MASSNAHMEN, FAHRRADROUTE]);
    testFeatureTogglz(true, [MASSNAHMEN, FAHRRADROUTE]);
  });

  describe('netzklassen output', () => {
    it('should fire correctly when checkbox selected', fakeAsync(() => {
      when(infrastrukturenSelektionService.isSelected(MASSNAHMEN)).thenReturn(false);

      component.onClickInfrastrukturen(MASSNAHMEN);

      verify(infrastrukturenSelektionService.selectInfrastrukturen(MASSNAHMEN)).once();
      verify(infrastrukturenSelektionService.deselectInfrastrukturen(anything())).never();
      expect().nothing();
    }));

    it('should fire correctly when checkbox deselected', () => {
      when(infrastrukturenSelektionService.isSelected(MASSNAHMEN)).thenReturn(true);

      component.onClickInfrastrukturen(MASSNAHMEN);

      verify(infrastrukturenSelektionService.selectInfrastrukturen(anything())).never();
      verify(infrastrukturenSelektionService.deselectInfrastrukturen(MASSNAHMEN)).once();
      expect().nothing();
    });
  });
});
