/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupSpec;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConstraintValidationTest extends ConstraintValidationTest<BackupReview> {

  @Override
  protected ConstraintValidator<BackupReview> buildValidator() {
    return new BackupConstraintValidation();
  }

  @Override
  protected BackupReview getValidReview() {
    return JsonUtil.readFromJson("backup_allow_request/create.json",
          BackupReview.class);
  }

  @Override
  protected BackupReview getInvalidReview() {
    final BackupReview backupReview = JsonUtil.readFromJson("backup_allow_request/create.json",
        BackupReview.class);
    backupReview.getRequest().getObject().setSpec(null);
    return backupReview;
  }

  @Test
  void nullSpec_shouldFail() {

    final BackupReview backupReview = getInvalidReview();

    checkNotNullErrorCause(StackGresBackup.class, "spec", backupReview);

  }

  @Test
  void nullClusterName_shouldFail(){

    final BackupReview backupReview = getValidReview();

    backupReview.getRequest().getObject().getSpec().setSgCluster(null);

    checkNotNullErrorCause(StackGresBackupSpec.class, "spec.sgCluster", backupReview);

  }

}