/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourceValue;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;

@ApplicationScoped
public class ClusterConciliator extends AbstractConciliator<StackGresCluster> {

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public ClusterConciliator(
      RequiredResourceGenerator<StackGresCluster> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresCluster> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache,
      LabelFactoryForCluster<StackGresCluster> labelFactory) {
    super(requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
    this.labelFactory = labelFactory;
  }

  @Override
  protected boolean forceChange(HasMetadata requiredResource, StackGresCluster config) {
    if (requiredResource instanceof StatefulSet requiredStatefulSet
        && requiredStatefulSet.getMetadata().getName().equals(
            config.getMetadata().getName())) {
      Map<String, String> primaryLabels =
          labelFactory.clusterPrimaryLabelsWithoutUidAndScope(config);
      boolean result = deployedResourcesCache
          .stream()
          .map(DeployedResourceValue::latestDeployed)
          .noneMatch(latestDeployedResource -> latestDeployedResource instanceof Pod lastDeployedPod
              && Optional.of(lastDeployedPod.getMetadata())
              .map(ObjectMeta::getLabels)
              .filter(labels -> primaryLabels.entrySet().stream()
                  .allMatch(primaryLabel -> labels.entrySet().stream()
                      .anyMatch(primaryLabel::equals)))
              .isPresent());
      if (result && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Will force StatefulSet reconciliation since no primary pod with labels {} was"
            + " found for SGCluster {}.{}",
            primaryLabels,
            config.getMetadata().getNamespace(),
            config.getMetadata().getName());
      }
      return result;
    }
    return false;
  }

}
