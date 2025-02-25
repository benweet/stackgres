/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;

@ApplicationScoped
public class DbOpsConciliator extends AbstractConciliator<StackGresDbOps> {

  @Inject
  public DbOpsConciliator(
      RequiredResourceGenerator<StackGresDbOps> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresDbOps> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache) {
    super(requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
  }

}
