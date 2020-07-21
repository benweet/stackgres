/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.transformer.AbstractDependencyResourceTransformer;
import io.stackgres.apiweb.transformer.PoolingConfigTransformer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgbouncerConfigResourceTest
    extends AbstractDependencyCustomResourceTest<PoolingConfigDto, StackGresPoolingConfig,
    ConnectionPoolingConfigResource> {

  @Override
  protected CustomResourceList<StackGresPoolingConfig> getCustomResourceList() {
    return JsonUtil.readFromJson("pooling_config/list.json", StackGresPoolingConfigList.class);
  }

  @Override
  protected PoolingConfigDto getResourceDto() {
    return JsonUtil.readFromJson("pooling_config/dto.json", PoolingConfigDto.class);
  }

  @Override
  protected AbstractDependencyResourceTransformer<PoolingConfigDto, StackGresPoolingConfig> getTransformer() {
    return new PoolingConfigTransformer();
  }

  @Override
  protected ConnectionPoolingConfigResource getService(
      CustomResourceScanner<StackGresPoolingConfig> scanner,
      CustomResourceFinder<StackGresPoolingConfig> finder,
      CustomResourceScheduler<StackGresPoolingConfig> scheduler,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      AbstractDependencyResourceTransformer<PoolingConfigDto, StackGresPoolingConfig> transformer) {
    return new ConnectionPoolingConfigResource(scanner, finder, scheduler, clusterScanner, transformer);
  }

  @Override
  protected String getResourceNamespace() {
    return "stackgres";
  }

  @Override
  protected String getResourceName() {
    return "pgbouncerconf";
  }

  @Override
  protected void checkDto(PoolingConfigDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("pgbouncerconf", resource.getMetadata().getName());
    assertEquals("ceaa793f-2d97-48b7-91e4-8086b22f1c4c", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals(Seq.of(
        "default_pool_size=200", 
        "max_client_conn=100",
        "pool_mode='transaction'")
        .toString("\n"),
        resource.getSpec().getPgBouncer().getPgbouncerConf());
    assertNotNull(resource.getStatus());
    assertNotNull(resource.getStatus().getClusters());
    assertEquals(1, resource.getStatus().getClusters().size());
    assertEquals("stackgres", resource.getStatus().getClusters().get(0));
    assertNotNull(resource.getStatus().getPgBouncer());
    assertNotNull(resource.getStatus().getPgBouncer().getPgbouncerConf());
    assertEquals(3, resource.getStatus().getPgBouncer().getPgbouncerConf().size());
    assertEquals("default_pool_size", resource.getStatus().getPgBouncer().getPgbouncerConf().get(0).getParameter());
    assertEquals("200", resource.getStatus().getPgBouncer().getPgbouncerConf().get(0).getValue());
    assertEquals("max_client_conn", resource.getStatus().getPgBouncer().getPgbouncerConf().get(1).getParameter());
    assertEquals("100", resource.getStatus().getPgBouncer().getPgbouncerConf().get(1).getValue());
    assertEquals("pool_mode", resource.getStatus().getPgBouncer().getPgbouncerConf().get(2).getParameter());
    assertEquals("'transaction'", resource.getStatus().getPgBouncer().getPgbouncerConf().get(2).getValue());
    assertNotNull(resource.getStatus().getPgBouncer().getDefaultParameters());
    assertIterableEquals(ImmutableList.of(
        "default_pool_size",
        "max_client_conn",
        "pool_mode"),
        resource.getStatus().getPgBouncer().getDefaultParameters());
  }

  @Override
  protected void checkCustomResource(StackGresPoolingConfig resource, Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("pgbouncerconf", resource.getMetadata().getName());
    assertEquals("ceaa793f-2d97-48b7-91e4-8086b22f1c4c", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals(ImmutableMap.of(
        "default_pool_size", "200", 
        "max_client_conn", "100",
        "pool_mode", "'transaction'"),
        resource.getSpec().getPgBouncer().getPgbouncerConf());
  }

}