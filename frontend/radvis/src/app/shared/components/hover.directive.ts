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

import { Directive, ElementRef, EventEmitter, OnDestroy, Output } from '@angular/core';
import { fromEvent, Subscription } from 'rxjs';

@Directive({
  selector: '[radHover]',
})
export class HoverDirective implements OnDestroy {
  @Output()
  hover = new EventEmitter<boolean>();
  subscriptions: Subscription[];

  constructor(host: ElementRef) {
    this.subscriptions = [
      fromEvent(host.nativeElement, 'mouseenter').subscribe(() => {
        this.hover.next(true);
      }),
      fromEvent(host.nativeElement, 'mouseleave').subscribe(() => {
        this.hover.next(false);
      }),
    ];
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }
}
