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

import { MockBuilder, MockRender, MockedComponentFixture } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { instance, mock, when } from 'ts-mockito';
import { InfrastrukturenTabellenComponent } from './infrastrukturen-tabellen.component';

describe('InfrastrukturenTabellenComponent', () => {
  let component: InfrastrukturenTabellenComponent;
  let fixture: MockedComponentFixture<InfrastrukturenTabellenComponent>;
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;
  let selektierteInfrastrukturen$: BehaviorSubject<Infrastruktur[]>;

  beforeEach(() => {
    infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    selektierteInfrastrukturen$ = new BehaviorSubject<Infrastruktur[]>([]);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(
      selektierteInfrastrukturen$.asObservable()
    );
    return MockBuilder(InfrastrukturenTabellenComponent, ViewerModule).provide({
      provide: InfrastrukturenSelektionService,
      useValue: instance(infrastrukturenSelektionService),
    });
  });

  beforeEach(() => {
    fixture = MockRender(InfrastrukturenTabellenComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('activeInfrastruktur', () => {
    it('be null if no infrastruktur selected', () => {
      expect(component.activeInfrastruktur).toBeNull();
    });

    it('should select first infrastruktur if no preselect', () => {
      selektierteInfrastrukturen$.next([MASSNAHMEN, FAHRRADROUTE]);
      expect(component.activeInfrastruktur).toBe(MASSNAHMEN);
    });

    it('should keep preselection', () => {
      component.activeInfrastruktur = FAHRRADROUTE;
      selektierteInfrastrukturen$.next([MASSNAHMEN, FAHRRADROUTE]);
      expect(component.activeInfrastruktur).toBe(FAHRRADROUTE);
    });

    it('should reset activeInfrastruktur if unselektiert', () => {
      component.activeInfrastruktur = FAHRRADROUTE;
      selektierteInfrastrukturen$.next([]);
      expect(component.activeInfrastruktur).toBeNull();
    });

    it('should reset activeInfrastruktur to first infrastruktur if unselektiert', () => {
      component.activeInfrastruktur = FAHRRADROUTE;
      selektierteInfrastrukturen$.next([MASSNAHMEN]);
      expect(component.activeInfrastruktur).toBe(MASSNAHMEN);
    });
  });
});
