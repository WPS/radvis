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
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  Input,
  OnInit,
  ViewChild,
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatInput } from '@angular/material/input';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { MatMenuTrigger } from '@angular/material/menu';

@Component({
  selector: 'rad-filter-menu',
  templateUrl: './filter-menu.component.html',
  styleUrls: ['./filter-menu.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilterMenuComponent implements OnInit {
  @Input()
  public hovered = false;

  @Input()
  public field!: string;

  @ViewChild('filterInput')
  private filterInput?: ElementRef<MatInput>;

  @ViewChild('menuTrigger')
  private menuTrigger?: MatMenuTrigger;

  public isAktiv = false;

  public filterControl = new FormControl('');

  constructor(
    private filterService: AbstractInfrastrukturenFilterService<any>,
    private changeDetector: ChangeDetectorRef
  ) {}

  @HostListener('keydown.enter', ['$event'])
  onEnter(event: Event): void {
    event.stopPropagation();
    this.menuTrigger?.openMenu();
    this.menuOpened();
  }

  ngOnInit(): void {
    this.filterControl.valueChanges
      .pipe(debounceTime(50))
      .subscribe(filterValue => this.filterService.filterField(this.field, filterValue));
    this.filterService.filter$
      .pipe(
        map(fieldFilters => fieldFilters.find(fieldFilter => fieldFilter.field === this.field)?.value ?? ''),
        distinctUntilChanged()
      )
      .subscribe(value => {
        if (this.filterControl.value !== value) {
          this.filterControl.setValue(value, { emitEvent: false });
        }

        this.isAktiv = !!value;
        this.changeDetector.markForCheck();
      });
  }

  public menuClosed(): void {
    this.isAktiv = !!this.filterControl.value;
  }

  public menuOpened(): void {
    this.isAktiv = true;
    this.filterInput?.nativeElement.focus();
  }

  public onFilterClear(): void {
    this.filterControl.patchValue('');
    this.filterService.filterField(this.field, '');
  }
}
