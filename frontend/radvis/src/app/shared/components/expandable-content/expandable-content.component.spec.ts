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

import { ExpandableContentComponent } from './expandable-content.component';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { SharedModule } from 'src/app/shared/shared.module';

describe(ExpandableContentComponent.name, () => {
  let component: ExpandableContentComponent;
  let fixture: MockedComponentFixture<ExpandableContentComponent>;

  beforeEach(() => {
    return MockBuilder(ExpandableContentComponent, SharedModule);
  });

  beforeEach(() => {
    fixture = MockRender(ExpandableContentComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('with string', () => {
    let content: string;

    beforeEach(() => {
      content = 'Dies it ein etwas längerer Text, der nicht ganz dargestellt werden kann.';
      component.content = content;
    });

    it('should return true for ' + ExpandableContentComponent.prototype.exceedsMaxLength, () => {
      expect(component.exceedsMaxLength).toBeTrue();
    });

    it('should return false for ' + ExpandableContentComponent.prototype.isListContent, () => {
      expect(component.isListContent).toBeFalse();
    });

    it('should get first part of content when not expanded', () => {
      expect(component.contentForDisplay).toEqual('Dies it ein etwas längerer Text, der nicht gan ...');
    });

    it('should get whole content when expanded', () => {
      component.expanded = true;
      expect(component.contentForDisplay).toEqual(content);
    });
  });

  describe('with slightly too long string', () => {
    let slightlyTooLongText: string;
    let expectedVisibleString: string;

    beforeEach(() => {
      expectedVisibleString = 'Dies ist ein eeeeeeeeeeeeetwas zu laaaaaaanger';
      slightlyTooLongText = expectedVisibleString + ' Text';
      component.content = slightlyTooLongText;
    });

    it('should get first part of text when not expanded', () => {
      expect(component.contentForDisplay).toEqual(expectedVisibleString + ' ...');
    });
  });

  describe('with list of strings', () => {
    let content: string[];

    beforeEach(() => {
      content = ['Ein', 'paar', 'interessante', 'und', 'wichtige', 'punkte'];
      component.content = content;
    });

    it('should return true for ' + ExpandableContentComponent.prototype.exceedsMaxLength, () => {
      expect(component.exceedsMaxLength).toBeTrue();
    });

    it('should return true for ' + ExpandableContentComponent.prototype.isListContent, () => {
      expect(component.isListContent).toBeTrue();
    });

    it('should get first part of content when not expanded', () => {
      expect(component.contentForDisplay).toEqual([content[0], content[1], content[2]]);
    });

    it('should get whole content when expanded', () => {
      component.expanded = true;
      expect(component.contentForDisplay).toEqual(content);
    });
  });
});
