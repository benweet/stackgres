/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import javax.inject.Singleton;

import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractPatroniStaticVolumesFactory;

@Singleton
@OperatorVersionBinder
public class PatroniStaticVolumesFactory
    extends AbstractPatroniStaticVolumesFactory<StackGresClusterContext> {
}
