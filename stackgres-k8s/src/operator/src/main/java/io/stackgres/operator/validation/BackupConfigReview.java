/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operatorframework.AdmissionReview;

@RegisterForReflection
public class BackupConfigReview extends AdmissionReview<StackGresBackupConfig> {

  private static final long serialVersionUID = 1L;
}
