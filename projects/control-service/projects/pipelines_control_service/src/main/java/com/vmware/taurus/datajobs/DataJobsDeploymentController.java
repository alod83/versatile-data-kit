/*
 * Copyright (c) 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.vmware.taurus.datajobs;


import com.vmware.taurus.controlplane.model.api.DataJobsDeploymentApi;
import com.vmware.taurus.controlplane.model.data.DataJobDeployment;
import com.vmware.taurus.controlplane.model.data.DataJobDeploymentStatus;
import com.vmware.taurus.controlplane.model.data.DataJobMode;
import com.vmware.taurus.controlplane.model.data.Enable;
import com.vmware.taurus.exception.ExternalSystemError;
import com.vmware.taurus.service.JobsService;
import com.vmware.taurus.service.deploy.DeploymentService;
import com.vmware.taurus.service.diag.OperationContext;
import com.vmware.taurus.service.model.JobDeployment;
import com.vmware.taurus.service.model.JobDeploymentStatus;
import io.kubernetes.client.ApiException;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * REST controller for operations on data job deployments
 *
 * <p>
 * The controller may throw exception which will be handled by
 * {@link com.vmware.taurus.exception.ExceptionControllerAdvice}. The advice class logs error so no need to log them here.
 *
 * <p>
 * Wrap {@link org.springframework.dao.DataAccessException} from JobsService in {@link ExternalSystemError} either here
 * or in the service itself.
 */
@RestController
@AllArgsConstructor
@NoArgsConstructor
@Api(tags = {"Data Jobs Deployment"})
public class DataJobsDeploymentController implements DataJobsDeploymentApi {
   static Logger log = LoggerFactory.getLogger(DataJobsDeploymentController.class);

   @Autowired
   private JobsService jobsService;

   @Autowired
   private DeploymentService deploymentService;

   @Autowired
   private OperationContext operationContext;

   @Override
   @Deprecated
   public ResponseEntity<Void> deploymentDeleteDeprecated(final String teamName,
                                                          final String jobName,
                                                          final String deploymentId) {
      return deploymentDelete(teamName, jobName, deploymentId);
   }

   @Override
   @Deprecated
   public ResponseEntity<Void> deploymentEnableDeprecated(final String teamName,
                                                          final String jobName,
                                                          final String deploymentId,
                                                          final Enable enable) {
      return deploymentEnable(teamName, jobName, deploymentId, enable);
   }

   @Override
   @Deprecated
   public ResponseEntity<List<DataJobDeploymentStatus>> deploymentListDeprecated(final String teamName,
                                                                                 final String jobName,
                                                                                 final String deploymentId,
                                                                                 final DataJobMode dataJobMode) {
      return deploymentList(teamName, jobName, deploymentId, dataJobMode);
   }

   @Override
   @Deprecated
   public ResponseEntity<DataJobDeploymentStatus> deploymentReadDeprecated(final String teamName,
                                                                           final String jobName,
                                                                           final String deploymentId) {
      return deploymentRead(teamName, jobName, deploymentId);
   }

   @Override
   @Deprecated
   public ResponseEntity<Void> deploymentUpdateDeprecated(final String teamName,
                                                          final String jobName,
                                                          final Boolean sendNotification,
                                                          final DataJobDeployment dataJobDeployment) {
      return deploymentUpdate(teamName, jobName, sendNotification, dataJobDeployment);
   }

   @Override
   public ResponseEntity<Void> deploymentDelete(String teamName, String jobName, String deploymentId) {
      if (jobsService.jobWithTeamExists(jobName, teamName)) {
         // TODO: deploymentId not implemented
         if (jobName != null) {
            deploymentService.deleteDeployment(jobName);
            return ResponseEntity.accepted().build();
         }
         return ResponseEntity.notFound().build();
      }
      return ResponseEntity.notFound().build();
   }

   @Override
   public ResponseEntity<Void> deploymentEnable(String teamName, String jobName, String deploymentId, Enable enable) {
      if (jobsService.jobWithTeamExists(jobName, teamName)) {
         // TODO: deploymentId not implemented
         Optional<com.vmware.taurus.service.model.DataJob> job = jobsService.getByName(jobName);
         Optional<JobDeploymentStatus> jobDeploymentStatus = deploymentService.readDeployment(jobName.toLowerCase());

         if (job.isPresent() && jobDeploymentStatus.isPresent()) {
            try {
               JobDeployment jobDeployment = DeploymentStatusToDeploymentConverter.toJobDeployment(jobDeploymentStatus.get());
               deploymentService.enableDeployment(job.get(), jobDeployment, enable.getEnabled(), operationContext.getUser());
               return ResponseEntity.accepted().build();
            } catch (ApiException e) {
               throw new ExternalSystemError(ExternalSystemError.MainExternalSystem.KUBERNETES, e);
            }
         }
      }
      return ResponseEntity.notFound().build();
   }

   @Override
   public ResponseEntity<List<DataJobDeploymentStatus>> deploymentList(String teamName, String jobName, String deploymentId, DataJobMode dataJobMode) {
      if (jobsService.jobWithTeamExists(jobName, teamName)) {
         // TODO: deploymentId and mode not implemented
         List<DataJobDeploymentStatus> deployments = Collections.emptyList();
         Optional<JobDeploymentStatus> jobDeploymentStatus = deploymentService.readDeployment(jobName.toLowerCase());
         if (jobDeploymentStatus.isPresent()) {
            deployments = Arrays.asList(ToApiModelConverter.toDataJobDeploymentStatus(jobDeploymentStatus.get()));
         }
         return ResponseEntity.ok(deployments);
      }
      return ResponseEntity.notFound().build();
   }

   @Override
   public ResponseEntity<DataJobDeploymentStatus> deploymentRead(String teamName, String jobName, String deploymentId) {
      if (jobsService.jobWithTeamExists(jobName, teamName)) {
         // TODO: deploymentId are not implemented.
         Optional<JobDeploymentStatus> jobDeploymentStatus = deploymentService.readDeployment(jobName.toLowerCase());
         if (jobDeploymentStatus.isPresent()) {
            return ResponseEntity.ok(ToApiModelConverter.toDataJobDeploymentStatus(jobDeploymentStatus.get()));
         }
      }
      return ResponseEntity.notFound().build();
   }

   @Override
   public ResponseEntity<Void> deploymentUpdate(String teamName, String jobName, Boolean sendNotification, DataJobDeployment dataJobDeployment) {
      if (jobsService.jobWithTeamExists(jobName, teamName)) {
         Optional<com.vmware.taurus.service.model.DataJob> job = jobsService.getByName(jobName.toLowerCase());
         if (job.isPresent()) {
            var jobDeployment = ToModelApiConverter.toJobDeployment(jobName.toLowerCase(), dataJobDeployment);
            //TODO: Consider using a Task-oriented API approach
            deploymentService.updateDeployment(job.get(), jobDeployment, sendNotification,
                    operationContext.getUser(), operationContext.getOpId());

            return ResponseEntity.accepted().build();
         }
      }
      return ResponseEntity.notFound().build();
   }
}