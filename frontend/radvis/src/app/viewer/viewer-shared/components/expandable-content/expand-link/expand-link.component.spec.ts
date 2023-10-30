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

import { ExpandLinkComponent } from './expand-link.component';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { SharedModule } from 'src/app/shared/shared.module';
import { instance, mock, verify } from 'ts-mockito';

describe(ExpandLinkComponent.name, () => {
  let component: ExpandLinkComponent;
  let fixture: MockedComponentFixture<ExpandLinkComponent, any>;

  beforeEach(() => {
    return MockBuilder(ExpandLinkComponent, SharedModule);
  });

  beforeEach(() => {
    fixture = MockRender(ExpandLinkComponent, { expanded: false });
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should handle click event correctly', done => {
    expect(component.expanded).toBeFalse();
    const mouseEventMock = mock(MouseEvent);

    component.expandedChange.subscribe(expanded => {
      expect(expanded).toBeTrue();
      done();
    });

    component.expandClicked(instance(mouseEventMock));

    expect(component.expanded).toBeTrue();
  });

  it('should stop event propagation', () => {
    const mouseEventMock = mock(MouseEvent);
    component.expandClicked(instance(mouseEventMock));

    verify(mouseEventMock.preventDefault()).once();
    verify(mouseEventMock.stopPropagation()).once();
    expect().nothing();
  });
});
