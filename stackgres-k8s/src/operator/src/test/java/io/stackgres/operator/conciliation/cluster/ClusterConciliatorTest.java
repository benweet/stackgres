/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LoadBalancerIngressBuilder;
import io.fabric8.kubernetes.api.model.LoadBalancerStatusBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.ServiceStatusBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatusBuilder;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.DeployedResourcesSnapshot;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceKey;
import io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterConciliatorTest {

  private StackGresCluster cluster;

  @Mock
  private RequiredResourceGenerator<StackGresCluster> requiredResourceGenerator;

  @Mock
  private AbstractDeployedResourcesScanner<StackGresCluster> deployedResourcesScanner;

  private DeployedResourcesCache deployedResourcesCache;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
  }

  @Test
  void nonDeployedResources_shouldAppearInTheCreation() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = new ArrayList<>(deepCopy(requiredResources));

    final List<HasMetadata> deployedResources = new ArrayList<>(deepCopy(lastRequiredResources));

    final List<HasMetadata> lastDeployedResources = new ArrayList<>(deepCopy(deployedResources));

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    var deletedResource = Seq.seq(deployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    lastRequiredResources.remove(deletedResource.v2.intValue());
    deployedResources.remove(deletedResource.v2.intValue());
    lastDeployedResources.remove(deletedResource.v2.intValue());

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(1, result.getCreations().size());
    assertEquals(deletedResource.v1, result.getCreations().get(0));

    assertFalse(result.isUpToDate());
  }

  @Test
  void resourceToDelete_shouldAppearInTheDeletions() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    var deletedResource = Seq.seq(lastDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    requiredResources.remove(deletedResource.v2.intValue());

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(1, result.getDeletions().size());
    assertEquals(deletedResource.v1, result.getDeletions().get(0));

    assertFalse(result.isUpToDate());
  }

  @Test
  void whenThereIsNoChanges_allResourcesShouldBeEmpty() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(0, result.getPatches().size());

    assertTrue(result.isUpToDate());
  }

  @Test
  void whenThereAreRequiredChanges_shouldBeDetected() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    var updatedResource = Seq.seq(requiredResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    updatedResource.v1.getMetadata()
        .setLabels(ImmutableMap.of(StringUtil.generateRandom(), StringUtil.generateRandom()));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource.v1, result.getPatches().get(0).v1);
  }

  @Test
  void whenThereAreDeployedChangesOnMetadataLabels_shouldBeDetected() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    var updatedResource = Seq.seq(lastDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    String key = StringUtil.generateRandom();
    requiredResources.get(updatedResource.v2.intValue()).getMetadata()
        .setAnnotations(ImmutableMap.of(key, StringUtil.generateRandom()));
    updatedResource.v1.getMetadata()
        .setLabels(ImmutableMap.of(key, StringUtil.generateRandom()));

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource.v1, result.getPatches().get(0).v2);
  }

  @Test
  void whenThereAreDeployedChangesOnMetadataAnnotations_shouldBeDetected() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    var updatedResource = Seq.seq(lastDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    String key = StringUtil.generateRandom();
    requiredResources.get(updatedResource.v2.intValue()).getMetadata()
        .setAnnotations(ImmutableMap.of(key, StringUtil.generateRandom()));
    updatedResource.v1.getMetadata()
        .setAnnotations(ImmutableMap.of(key, StringUtil.generateRandom() + "-changed"));

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource.v1, result.getPatches().get(0).v2);
  }

  @Test
  void whenThereAreDeployedChangesOnMetadataOwnerReferences_shouldBeDetected() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    var updatedResource = Seq.seq(lastDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    updatedResource.v1.getMetadata()
        .setOwnerReferences(List.of(ResourceUtil.getOwnerReference(cluster)));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource.v1, result.getPatches().get(0).v2);
  }

  @Test
  void whenThereAreDeployedChangesOnStatefulSetSpec_shouldBeDetected() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    StatefulSet updatedResource = lastDeployedResources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .get();
    updatedResource
        .setSpec(new StatefulSetSpecBuilder()
            .withServiceName("test")
            .build());

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource, result.getPatches().get(0).v2);
  }

  @Test
  void whenThereAreDeployedChangesOnServiceSpec_shouldBeDetected() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    Service updatedResource = lastDeployedResources.stream()
        .filter(Service.class::isInstance)
        .map(Service.class::cast)
        .findFirst()
        .get();
    updatedResource
        .setSpec(new ServiceSpecBuilder()
            .withType("test")
            .build());

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource, result.getPatches().get(0).v2);
  }

  @Test
  void whenThereAreDeployedChangesOnMetadataResourceVersion_shouldNotBeDetected() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    var updatedResource = Seq.seq(lastDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    updatedResource.v1.getMetadata()
        .setResourceVersion("test");

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(0, result.getPatches().size());
  }

  @Test
  void whenThereAreDeployedChangesOnStatefulSetStatus_shouldNotBeDetected() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    lastDeployedResources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .get()
        .setStatus(new StatefulSetStatusBuilder()
            .withCurrentReplicas(3)
            .build());

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(0, result.getPatches().size());
  }

  @Test
  void whenThereAreDeployedChangesOnServiceStatus_shouldNotBeDetected() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    lastDeployedResources.stream()
        .filter(Service.class::isInstance)
        .map(Service.class::cast)
        .findFirst()
        .get()
        .setStatus(new ServiceStatusBuilder()
            .withLoadBalancer(new LoadBalancerStatusBuilder()
                .withIngress(List.of(new LoadBalancerIngressBuilder()
                    .withHostname("test")
                    .build()))
                .build())
            .build());

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(0, result.getPatches().size());
  }

  @Test
  void conciliation_shouldDetectStatefulSetChanges() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    lastRequiredResources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .get().getSpec().setReplicas(10);
    StatefulSet updatedResource = lastDeployedResources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .get();

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);

    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource, result.getPatches().get(0).v2);
  }

  @Test
  void conciliation_shouldIgnoreChangesOnResourcesMarkedWithReconciliationPauseAnnotatinon() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    lastDeployedResources.stream().forEach(resource -> resource
        .getMetadata().setAnnotations(Map.of(
            StackGresContext.RECONCILIATION_PAUSE_KEY, Boolean.TRUE.toString())));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);

    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(0, result.getPatches().size());

    assertTrue(result.isUpToDate());
  }

  @Test
  void conciliation_shouldIgnoreDeletionsOnResourcesMarkedWithReconciliationPauseAnnotation() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    var removedResource = Seq.seq(lastDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .filter(t -> t.v2.intValue() < requiredResources.size() / 2)
        .findFirst()
        .get();
    requiredResources.remove(removedResource.v2.intValue());
    removedResource.v1.getMetadata().setAnnotations(Map.of(
        StackGresContext.RECONCILIATION_PAUSE_KEY,
        Boolean.TRUE.toString()));

    var changedResource = Seq.seq(lastDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .filter(t -> t.v2.intValue() > removedResource.v2.intValue())
        .findFirst()
        .get();
    changedResource.v1.getMetadata().setAnnotations(Map.of(
        StackGresContext.RECONCILIATION_PAUSE_KEY,
        Boolean.FALSE.toString()));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getCreations().size());
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getPatches().size());

    assertTrue(result.isUpToDate());
  }

  @Test
  void conciliation_shouldDetectStatefulSetChangesOnMissingPrimaryPod() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> lastDeployedResources = deepCopy(deployedResources);

    StatefulSet updatedResource = lastDeployedResources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .get();

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        lastDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);

    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource, result.getPatches().get(0).v2);
  }

  protected List<HasMetadata> deepCopy(List<HasMetadata> source) {
    return source.stream().map(JsonUtil::copy).toList();
  }

  protected ClusterConciliator buildConciliator(
      List<HasMetadata> required,
      List<HasMetadata> lastRequired,
      List<HasMetadata> deployed,
      List<HasMetadata> lastDeployed) {
    deployedResourcesCache = new DeployedResourcesCache(
        new OperatorPropertyContext(), JsonUtil.jsonMapper());
    deployed.stream()
        .filter(this::hasControllerOwnerReference)
        .forEach(resource -> deployedResourcesCache
            .put(lastRequired.stream()
                .filter(r -> ResourceKey.same(r, resource))
                .findFirst()
                .orElse(resource),
                resource));
    List<HasMetadata> ownedLastDeployed = lastDeployed.stream()
        .filter(this::hasControllerOwnerReference)
        .toList();
    lastDeployed.stream().forEach(resource -> resource.getMetadata().setResourceVersion("changed"));
    DeployedResourcesSnapshot deplyedResourcesSnapshot =
        deployedResourcesCache.createDeployedResourcesSnapshot(ownedLastDeployed, lastDeployed);

    when(requiredResourceGenerator.getRequiredResources(cluster))
        .thenReturn(required);
    when(deployedResourcesScanner.getDeployedResources(cluster))
        .thenReturn(deplyedResourcesSnapshot);

    final ClusterConciliator clusterConciliator = new ClusterConciliator(
        requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache,
        new ClusterLabelFactory(new ClusterLabelMapper()));
    return clusterConciliator;
  }

  private boolean hasControllerOwnerReference(HasMetadata resource) {
    return resource.getMetadata().getOwnerReferences().size() > 0
        && resource.getMetadata().getOwnerReferences().stream()
        .anyMatch(ownerReference -> ownerReference.getController() != null
        && ownerReference.getController());
  }

  public static <T> Comparator<T> shuffle() {
    Random random = new Random();
    Map<T, Integer> uniqueIds = new HashMap<>();
    return Comparator.comparing(e -> uniqueIds.computeIfAbsent(e, k -> random.nextInt()));
  }

}
