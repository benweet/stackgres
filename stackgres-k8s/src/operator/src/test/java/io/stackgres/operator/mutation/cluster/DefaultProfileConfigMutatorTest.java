/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutatorTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultProfileConfigMutatorTest
    extends AbstractDefaultResourceMutatorTest<StackGresProfile, StackGresCluster,
        StackGresClusterReview, DefaultProfileMutator> {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  @Override
  protected StackGresClusterReview getAdmissionReview() {
    return AdmissionReviewFixtures.cluster().loadCreate().get();
  }

  @Override
  protected DefaultProfileMutator getDefaultConfigMutator() {
    var resourceFactory = new DefaultProfileFactory(new OperatorPropertyContext());
    resourceFactory.init();
    var mutator = new DefaultProfileMutator(
        resourceFactory, finder, scheduler);
    return mutator;
  }

  @Override
  protected Class<StackGresCluster> getResourceClass() {
    return StackGresCluster.class;
  }

  @Override
  protected void checkConfigurationIsSet(StackGresCluster newResource) {
    assertNotNull(newResource.getSpec().getResourceProfile());
  }

  @Override
  protected void setUpExistingConfiguration() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
  }

  @Override
  protected void setUpMissingConfiguration() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getSpec().setResourceProfile(null);
  }

  @Override
  protected void setUpMissingConfigurationSection() {
    // Nothing to do
  }

  @Test
  @Disabled("There is no configuration section for profile")
  @Override
  protected void clusteWithNoConfigurationSection_shouldSetOne() throws Exception {
    super.clusteWithNoConfigurationSection_shouldSetOne();
  }

}
