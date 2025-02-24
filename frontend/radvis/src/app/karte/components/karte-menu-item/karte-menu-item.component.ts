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

import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  Output,
  ViewChild,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { Observable, Subject } from 'rxjs';
import { KarteButtonComponent } from 'src/app/karte/components/karte-button/karte-button.component';
import { MenuCloseReason } from 'src/app/shared/models/menu-close-reason';
import { MenuEventService } from 'src/app/shared/services/menu-event.service';

@Component({
  selector: 'rad-karte-menu-item',
  templateUrl: './karte-menu-item.component.html',
  styleUrls: ['./karte-menu-item.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: MenuEventService, useExisting: forwardRef(() => KarteMenuItemComponent) }],
  standalone: false,
})
export class KarteMenuItemComponent implements MenuEventService {
  @Input() icon = '';
  @Input() headline = '';

  @Output() menuOpen = new EventEmitter<void>();

  @ViewChild('menuTrigger', { static: true })
  private menuTrigger: MatMenuTrigger | null = null;

  @ViewChild('menuContent')
  private menuContent: ElementRef | null = null;

  @ViewChild('karteButton')
  private karteButton: KarteButtonComponent | null = null;

  private menuClosedSubject = new Subject<MenuCloseReason>();

  close(): void {
    this.menuTrigger?.closeMenu();
  }

  onMenuOpen(): void {
    this.menuOpen.emit();
  }

  onMenuClosed(event: MenuCloseReason): void {
    this.karteButton?.focus();
    this.menuClosedSubject.next(event);
  }

  get menuClosed$(): Observable<MenuCloseReason> {
    return this.menuClosedSubject.asObservable();
  }

  setFocus(): void {
    this.menuContent?.nativeElement.focus();
  }

  openMenu(): void {
    this.menuTrigger?.openMenu();
    const firstFocusableElement = this.menuContent?.nativeElement.querySelector(
      '[radAccessabilityTabCircleElement]:not(:disabled)'
    );
    if (!firstFocusableElement) {
      return;
    }
    if (firstFocusableElement.tagName === 'MAT-CHECKBOX') {
      firstFocusableElement.querySelector('input').focus();
    } else {
      firstFocusableElement.focus();
    }
  }
}
