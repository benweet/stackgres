/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterReplicateFromUserSecretKeyRef {

  @JsonProperty("username")
  private SecretKeySelector username;

  @JsonProperty("password")
  private SecretKeySelector password;

  public SecretKeySelector getUsername() {
    return username;
  }

  public void setUsername(SecretKeySelector username) {
    this.username = username;
  }

  public SecretKeySelector getPassword() {
    return password;
  }

  public void setPassword(SecretKeySelector password) {
    this.password = password;
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
