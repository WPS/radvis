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
import { of } from 'rxjs';
import { BenutzerStatus } from 'src/app/administration/models/benutzer-status';
import { FreischaltungComponent } from 'src/app/freischaltung/components/freischaltung.component';
import { FreischaltungModule } from 'src/app/freischaltung/freischaltung.module';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { instance, mock, verify, when } from 'ts-mockito';

describe(FreischaltungComponent.name, () => {
  let component: FreischaltungComponent;
  let fixture: MockedComponentFixture<FreischaltungComponent>;

  let benutzerDetailsService: BenutzerDetailsService;

  beforeEach(() => {
    benutzerDetailsService = mock(BenutzerDetailsService);
    when(benutzerDetailsService.aktuellerBenutzerStatus()).thenReturn(BenutzerStatus.INAKTIV);

    when(benutzerDetailsService.beantrageReaktivierung()).thenReturn(
      of({
        vorname: 'Johannes',
        name: 'RadVIS',
        status: BenutzerStatus.WARTE_AUF_FREISCHALTUNG,
        registriert: true,
      })
    );

    return MockBuilder(FreischaltungComponent, FreischaltungModule).provide({
      provide: BenutzerDetailsService,
      useValue: instance(benutzerDetailsService),
    });
  });

  beforeEach(() => {
    fixture = MockRender(FreischaltungComponent);
    fixture.detectChanges();
    component = fixture.point.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should get benutzer status on init', () => {
    expect(component.status).toBe(BenutzerStatus.INAKTIV);
  });

  it('should call service on Beantragung einer Reaktivierung', () => {
    component.beantrageReaktivierung();
    verify(benutzerDetailsService.beantrageReaktivierung()).once();
    expect(component.status).toBe(BenutzerStatus.WARTE_AUF_FREISCHALTUNG);
  });
});
