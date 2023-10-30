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

import { AfterViewInit, Directive, ElementRef } from '@angular/core';

@Directive({
  selector: '[radAccessabilityTabCircleGroup]',
})
export class AccessabilityTabCircleGroupDirective implements AfterViewInit {
  private elementToHandlerMap: Map<any, (event: KeyboardEvent) => void> = new Map();

  constructor(private elRef: ElementRef) {}

  ngAfterViewInit(): void {
    this.refresh();
  }

  refresh(): void {
    const hostElement = this.elRef.nativeElement as HTMLElement;
    const elements: any[] = Array.from(hostElement.querySelectorAll('[radAccessabilityTabCircleElement]'));
    elements.forEach((element, index) => {
      const handleKeydown = (event: KeyboardEvent): void => {
        if ((event as KeyboardEvent).key !== 'Tab') {
          return;
        }
        let nextElement: any;
        if (!event.shiftKey) {
          if (elements.slice(index + 1).some(e => this.isEnabled(e))) {
            return;
          }
          nextElement = elements.slice(0, index + 1).find(e => this.isEnabled(e));
        } else {
          if (elements.slice(0, index).some(e => this.isEnabled(e))) {
            return;
          }
          nextElement = elements
            .slice(index)
            .reverse()
            .find(e => this.isEnabled(e));
        }
        event.preventDefault();

        if (!nextElement) {
          return;
        }

        if (nextElement.tagName === 'MAT-CHECKBOX') {
          nextElement.querySelector('input').focus();
        } else {
          nextElement.focus();
        }
      };
      if (this.elementToHandlerMap.has(element)) {
        element.removeEventListener('keydown', this.elementToHandlerMap.get(element));
      }
      element.addEventListener('keydown', handleKeydown);
      this.elementToHandlerMap.set(element, handleKeydown);
    });
  }

  private isEnabled(element: any): boolean {
    if (element.tagName === 'MAT-CHECKBOX') {
      return !element.querySelector('input').disabled;
    } else {
      return !element.disabled;
    }
  }
}
