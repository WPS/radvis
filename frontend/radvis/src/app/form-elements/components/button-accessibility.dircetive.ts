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
import { environment } from 'src/environments/environment';
/**
 * Setzt Textinhalt des Buttons oder (wenn nicht vorhanden) den MatTooltip als ariaLabel. Funktioniert nur mit statischen Tooltips
 */
@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: 'button',
  standalone: false,
})
export class ButtonAccessibilityDirective implements AfterViewInit {
  constructor(private host: ElementRef) {}

  ngAfterViewInit(): void {
    const button = this.host.nativeElement as HTMLButtonElement;
    if (!button.ariaLabel && !button.getAttribute('aria-labelledby')) {
      this.addAriaLabel();
    }
  }

  private addAriaLabel(): void {
    const button = this.host.nativeElement as HTMLButtonElement;
    let textContent = button.textContent ?? '';
    const containedMatIcon = button.querySelector('mat-icon');
    if (containedMatIcon) {
      textContent = textContent.replace(containedMatIcon.textContent ?? '', '').trim();
    }
    const beschreibung = textContent || button.getAttribute('mattooltip');

    if (beschreibung) {
      button.ariaLabel = beschreibung;
      // Marker, damit man aus dem Browser heraus nachvollziehen kann, wo das ariaLabel herkommt
      button.classList.add('rad-button-accessibility-directive');
    } else {
      if (!environment.production) {
        console.warn(
          `no ariaLabel set for Button, please provide manually! (s. ${ButtonAccessibilityDirective.name})`,
          button
        );
      }
    }
  }
}
