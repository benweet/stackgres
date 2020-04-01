/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ValidationUtil.CONNPOOLCONFIG_VALIDATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PgBouncerValidationResource implements ValidationResource<PoolingReview> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PgBouncerValidationResource.class);

  private ValidationPipeline<PoolingReview> pipeline;

  @Inject
  public PgBouncerValidationResource(@Any ValidationPipeline<PoolingReview> validationPipeline) {
    this.pipeline = validationPipeline;
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Pgbouncer validation resource started");
  }

  @POST
  public AdmissionReviewResponse validate(PoolingReview admissionReview) {
    return validate(admissionReview, pipeline);
  }
}
