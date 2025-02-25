/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AwsS3CompatibleStorageDto {

  @JsonProperty("bucket")
  private String bucket;

  @JsonProperty("path")
  private String path;

  @JsonProperty("awsCredentials")
  private AwsCredentialsDto credentials;

  @JsonProperty("region")
  private String region;

  @JsonProperty("endpoint")
  private String endpoint;

  @JsonProperty("enablePathStyleAddressing")
  private Boolean forcePathStyle;

  @JsonProperty("storageClass")
  private String storageClass;

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public AwsCredentialsDto getCredentials() {
    return credentials;
  }

  public void setCredentials(AwsCredentialsDto credentials) {
    this.credentials = credentials;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public Boolean isForcePathStyle() {
    return forcePathStyle;
  }

  public void setForcePathStyle(Boolean enablePathStyleAddressing) {
    this.forcePathStyle = enablePathStyleAddressing;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
