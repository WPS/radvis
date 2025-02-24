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
import { MockBuilder, MockRender, ngMocks } from 'ng-mocks';
import { ButtonAccessibilityDirective } from 'src/app/form-elements/components/button-accessibility.dircetive';
import { SharedModule } from 'src/app/shared/shared.module';

describe(ButtonAccessibilityDirective.name, () => {
  beforeEach(() => MockBuilder(ButtonAccessibilityDirective, SharedModule));

  describe('addAriaLabel', () => {
    it('should set textContent', () => {
      MockRender('<button>Hallo</button>');
      expect(getAriaLabel()).toBe('Hallo');
    });

    it('should not include mat-icon texts', () => {
      MockRender('<button><mat-icon>info</mat-icon>Hallo</button>');
      expect(getAriaLabel()).toBe('Hallo');
    });

    it('should set tooltip', () => {
      MockRender('<button matTooltip="Hallo"><mat-icon>info</mat-icon></button>');
      expect(getAriaLabel()).toBe('Hallo');
    });

    it('should prefer textContent over tooltip', () => {
      MockRender('<button matTooltip="Ich bin eine Zusatzinfo">Hallo</button>');
      expect(getAriaLabel()).toBe('Hallo');
    });

    it('should not do anything if ariaLabel already set', () => {
      MockRender('<button matTooltip="Ich bin eine Zusatzinfo" aria-label="Test">Hallo</button>');
      expect(getAriaLabel()).toBe('Test');
    });

    it('should not do anything if ariaLabelledBy already set', () => {
      MockRender(
        '<button matTooltip="Ich bin eine Zusatzinfo" aria-labelledby="Test">Hallo</button><label id="Test">Label</label>'
      );
      expect(getAriaLabel()).toBeNull();
    });

    const getAriaLabel = (): string | null => (ngMocks.find('button').nativeElement as HTMLButtonElement).ariaLabel;
  });
});
