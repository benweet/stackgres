/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;

@ApplicationScoped
public class ScriptConciliator extends AbstractConciliator<StackGresScript> {

  @Inject
  public ScriptConciliator(
      RequiredResourceGenerator<StackGresScript> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresScript> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache) {
    super(requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
  }

}
