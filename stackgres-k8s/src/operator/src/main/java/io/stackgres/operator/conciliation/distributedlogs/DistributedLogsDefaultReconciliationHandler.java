/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.AbstractReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresDistributedLogs.class, kind = "HasMetadata")
@ApplicationScoped
public class DistributedLogsDefaultReconciliationHandler
    extends AbstractReconciliationHandler<StackGresDistributedLogs> {

  @Inject
  public DistributedLogsDefaultReconciliationHandler(KubernetesClient client) {
    super(client);
  }

}
