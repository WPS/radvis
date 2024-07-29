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

import { MockBuilder, MockedComponentFixture, MockRender, ngMocks } from 'ng-mocks';
import { SicherheitstrennstreifenAnzeigeComponent } from 'src/app/shared/components/sicherheitstrennstreifen-anzeige/sicherheitstrennstreifen-anzeige.component';
import { TrennstreifenSeite } from 'src/app/shared/models/trennstreifen-seite';
import { SharedModule } from 'src/app/shared/shared.module';

describe(SicherheitstrennstreifenAnzeigeComponent.name, () => {
  let component: SicherheitstrennstreifenAnzeigeComponent;
  let fixture: MockedComponentFixture<SicherheitstrennstreifenAnzeigeComponent>;

  ngMocks.faster();

  beforeAll(() => {
    return MockBuilder(SicherheitstrennstreifenAnzeigeComponent, SharedModule);
  });

  beforeEach(() => {
    fixture = MockRender(SicherheitstrennstreifenAnzeigeComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit change on selecting a different Seite', () => {
    const selectedSeiteChangeSpy = spyOn(component.selectedSeiteChange, 'emit');
    component.selectedSeite = TrennstreifenSeite.A;
    component.onTrennstreifenClicked(TrennstreifenSeite.B);
    expect(selectedSeiteChangeSpy).toHaveBeenCalled();
  });

  it('should not emit change on selecting the same Seite', () => {
    const selectedSeiteChangeSpy = spyOn(component.selectedSeiteChange, 'emit');
    component.selectedSeite = TrennstreifenSeite.A;
    component.onTrennstreifenClicked(TrennstreifenSeite.A);
    expect(selectedSeiteChangeSpy).not.toHaveBeenCalled();
  });
});
