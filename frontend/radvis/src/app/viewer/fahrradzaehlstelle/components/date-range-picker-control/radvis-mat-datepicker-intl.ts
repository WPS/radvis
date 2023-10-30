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

import { MatDatepickerIntl } from '@angular/material/datepicker';
import { Subject } from 'rxjs';

export class RadvisMatDatepickerIntl implements MatDatepickerIntl {
  calendarLabel = 'calendarLabel';
  closeCalendarLabel = 'Kalender schließen';
  nextMonthLabel = 'Nächster Monat';
  nextMultiYearLabel = 'Nächste Jahre';
  nextYearLabel = 'Nächstes Jahr';
  openCalendarLabel = 'Kalender öffnen';
  prevMonthLabel = 'Vorheriger Monat';
  prevMultiYearLabel = 'Vorherige Jahre';
  prevYearLabel = 'Vorheriges Jahr';
  switchToMonthViewLabel = 'Zu Monatsansicht wechseln';
  switchToMultiYearViewLabel = 'Zu mehrjähriger Ansicht wechseln';
  readonly changes: Subject<void> = new Subject();

  formatYearRange(start: string, end: string): string {
    return `${start} - ${end}`;
  }
}
