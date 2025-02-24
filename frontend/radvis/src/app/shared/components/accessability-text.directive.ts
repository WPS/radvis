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

import { AfterViewInit, Directive, ElementRef, Input, OnDestroy } from '@angular/core';

@Directive({
  selector: 'mat-icon[radAccessabilityText]',
  standalone: false,
})
export class AccessabilityTextDirective implements AfterViewInit, OnDestroy {
  @Input()
  radAccessabilityText = '';

  private accessabilityTextEl: any;

  constructor(private elRef: ElementRef) {}

  ngAfterViewInit(): void {
    const el = this.elRef.nativeElement as HTMLElement;
    const span = document.createElement('span');
    span.textContent = this.radAccessabilityText;
    span.classList.add('cdk-visually-hidden');
    this.accessabilityTextEl = span;
    const parent = el.parentNode;
    parent?.insertBefore(span, el.nextSibling);
  }

  ngOnDestroy(): void {
    if (this.accessabilityTextEl) {
      const el = this.elRef.nativeElement as HTMLElement;
      el.parentNode?.removeChild(this.accessabilityTextEl);
    }
  }
}
