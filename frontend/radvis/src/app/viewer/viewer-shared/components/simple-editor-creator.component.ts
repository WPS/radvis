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

import { ChangeDetectorRef, Component, HostListener } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';

@Component({
  template: '',
})
export abstract class SimpleEditorCreatorComponent<T> implements DiscardableComponent {
  public isCreator = false;
  public isFetching = false;

  protected abstract entityName: string;

  get isDirty(): boolean {
    return this.formGroup.dirty;
  }

  constructor(
    public formGroup: UntypedFormGroup,
    protected notifyUserService: NotifyUserService,
    protected changeDetector: ChangeDetectorRef,
    protected infrastrukturFilterService: AbstractInfrastrukturenFilterService<T>
  ) {}

  @HostListener('keydown.escape')
  public onEscape(): void {
    this.onClose();
  }

  canDiscard = (): boolean => {
    return !this.isDirty;
  };

  protected save(): void {
    if (!this.isDirty) {
      return;
    }

    if (!this.formGroup.valid) {
      this.notifyUserService.warn('Das Formular kann nicht gespeichert werden, weil es ungültige Einträge enthält.');
      return;
    }

    this.isFetching = true;
    let promise: Promise<void>;

    if (this.isCreator) {
      promise = this.doCreate(this.formGroup);
    } else {
      promise = this.doSave(this.formGroup);
    }

    promise.then(() => {
      this.notifyUserService.inform(`${this.entityName} wurde erfolgreich gespeichert.`);
      this.infrastrukturFilterService.refetchData();
    });

    promise.finally(() => {
      this.isFetching = false;
      this.changeDetector.markForCheck();
    });
  }

  protected abstract doSave(formGroup: UntypedFormGroup): Promise<void>;

  protected abstract doCreate(formGroup: UntypedFormGroup): Promise<void>;

  protected abstract onClose(): void;
}
