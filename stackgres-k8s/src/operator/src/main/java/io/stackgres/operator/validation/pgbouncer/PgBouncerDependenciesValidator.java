/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class PgBouncerDependenciesValidator extends DependenciesValidator<PgBouncerReview>
    implements PgBouncerValidator {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PgBouncerDependenciesValidator.class);

  @Override
  public void validate(PgBouncerReview review, StackGresCluster i) throws ValidationFailed {
    final String name = review.getRequest().getName();
    LOGGER.info("validating deletion of " + name);
    if (name.equals(i.getSpec().getConfigurations().getConnectionPoolingConfig())) {
      fail(review, i);
    }
  }

}
