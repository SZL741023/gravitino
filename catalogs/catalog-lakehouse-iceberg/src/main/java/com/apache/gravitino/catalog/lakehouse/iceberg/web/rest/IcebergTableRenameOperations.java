/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.apache.gravitino.catalog.lakehouse.iceberg.web.rest;

import com.apache.gravitino.catalog.lakehouse.iceberg.ops.IcebergTableOps;
import com.apache.gravitino.catalog.lakehouse.iceberg.web.IcebergRestUtils;
import com.apache.gravitino.metrics.MetricNames;
import com.codahale.metrics.annotation.ResponseMetered;
import com.codahale.metrics.annotation.Timed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.iceberg.rest.requests.RenameTableRequest;

@Path("/v1/{prefix:([^/]*/)?}tables/rename")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IcebergTableRenameOperations {

  @SuppressWarnings("UnusedVariable")
  @Context
  private HttpServletRequest httpRequest;

  private IcebergTableOps icebergTableOps;

  @Inject
  public IcebergTableRenameOperations(IcebergTableOps icebergTableOps) {
    this.icebergTableOps = icebergTableOps;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Timed(name = "rename-table." + MetricNames.HTTP_PROCESS_DURATION, absolute = true)
  @ResponseMetered(name = "rename-table", absolute = true)
  public Response renameTable(RenameTableRequest renameTableRequest) {
    icebergTableOps.renameTable(renameTableRequest);
    return IcebergRestUtils.okWithoutContent();
  }
}