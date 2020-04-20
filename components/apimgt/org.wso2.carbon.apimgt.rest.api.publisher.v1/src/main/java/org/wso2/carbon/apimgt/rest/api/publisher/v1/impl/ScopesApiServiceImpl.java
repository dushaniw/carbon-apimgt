/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.rest.api.publisher.v1.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.ExceptionCodes;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.ScopesApiService;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.ScopeDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.ScopeListDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.utils.mappings.SharedScopeMappingUtil;
import org.wso2.carbon.apimgt.rest.api.util.RestApiConstants;
import org.wso2.carbon.apimgt.rest.api.util.utils.RestApiUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.core.Response;

/**
 * This is the service implementation class for scopes related operations
 */
public class ScopesApiServiceImpl implements ScopesApiService {
    private static final Log log = LogFactory.getLog(ScopesApiServiceImpl.class);

    /**
     * Check whether the given scope already used in APIs
     *
     * @param name           Base64 URL encoded form of scope name -Base64URLEncode{scope name}
     * @param messageContext
     * @return boolean to indicate existence
     */
    public Response validateScope(String name, MessageContext messageContext) {
        boolean isScopeExist = false;
        String scopeName = new String(Base64.getUrlDecoder().decode(name));
        if (!APIUtil.isWhiteListedScope(scopeName)) {
            try {
                APIProvider apiProvider = RestApiUtil.getLoggedInUserProvider();
                String tenantDomain = RestApiUtil.getLoggedInUserTenantDomain();
                isScopeExist = apiProvider.isScopeKeyExist(scopeName, tenantDomain);
            } catch (APIManagementException e) {
                RestApiUtil.handleInternalServerError("Error occurred while checking scope name", e, log);
            }
        }

        if (isScopeExist) {
            return Response.status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Add Shared Scope.
     *
     * @param body           Scope DTO object to add
     * @param messageContext CXF Message Context
     * @return Created Scope as DTO
     * @throws APIManagementException If an error occurs while adding shared scope.
     */
    public Response addSharedScope(ScopeDTO body, MessageContext messageContext) throws APIManagementException {

        String scopeName = body.getName();
        try {
            APIProvider apiProvider = RestApiUtil.getLoggedInUserProvider();
            String tenantDomain = RestApiUtil.getLoggedInUserTenantDomain();
            if (StringUtils.isEmpty(scopeName)) {
                throw new APIManagementException("Shared Scope Name cannot be null or empty",
                        ExceptionCodes.SHARED_SCOPE_NAME_NOT_SPECIFIED);
            }
            if (apiProvider.isScopeKeyExist(scopeName, tenantDomain)) {
                throw new APIManagementException(ExceptionCodes.from(ExceptionCodes.SCOPE_ALREADY_REGISTERED,
                        scopeName));
            }
            Scope scopeToAdd = SharedScopeMappingUtil.fromDTOToScope(body);
            String sharedScopeId = apiProvider.addSharedScope(scopeToAdd, tenantDomain);
            //Get registered shared scope
            Scope createdScope = apiProvider.getSharedScopeByUUID(sharedScopeId, tenantDomain);
            ScopeDTO createdScopeDTO = SharedScopeMappingUtil.fromScopeToDTO(createdScope);
            String createdScopeURIString = RestApiConstants.RESOURCE_PATH_SHARED_SCOPES_SCOPE_ID
                    .replace(RestApiConstants.SHARED_SCOPE_ID_PARAM, createdScopeDTO.getId());
            URI createdScopeURI = new URI(createdScopeURIString);
            return Response.created(createdScopeURI).entity(createdScopeDTO).build();
        } catch (URISyntaxException e) {
            throw new APIManagementException("Error while creating shared scope: " + scopeName, e);
        }
    }

    /**
     * Delete shared scope.
     *
     * @param scopeId        Scope UUID
     * @param messageContext CXF Message Context
     * @return Deletion Response
     * @throws APIManagementException If an error occurs while deleting shared scope
     */
    public Response deleteSharedScope(String scopeId, MessageContext messageContext) throws APIManagementException {

        APIProvider apiProvider = RestApiUtil.getLoggedInUserProvider();
        String tenantDomain = RestApiUtil.getLoggedInUserTenantDomain();
        if (StringUtils.isEmpty(scopeId)) {
            throw new APIManagementException("Scope Id cannot be null or empty",
                    ExceptionCodes.SHARED_SCOPE_ID_NOT_SPECIFIED);
        }
        Scope existingScope = apiProvider.getSharedScopeByUUID(scopeId, tenantDomain);
        if (apiProvider.isScopeKeyAssignedToAPI(existingScope.getName(), tenantDomain)) {
            throw new APIManagementException("Cannot remove the Shared Scope " + scopeId + " as it is used by one "
                    + "or more APIs", ExceptionCodes.from(ExceptionCodes.SHARED_SCOPE_ALREADY_ATTACHED, scopeId));
        }
        apiProvider.deleteSharedScope(existingScope.getName(), tenantDomain);
        return Response.ok().build();
    }

    /**
     * Get shared scope by Id.
     *
     * @param scopeId        UUID of the scope
     * @param messageContext CXF Message Context
     * @return Shared Scope DTO
     * @throws APIManagementException If an error occurs while getting shared scope
     */
    public Response getSharedScope(String scopeId, MessageContext messageContext) throws APIManagementException {

        APIProvider apiProvider = RestApiUtil.getLoggedInUserProvider();
        String tenantDomain = RestApiUtil.getLoggedInUserTenantDomain();
        if (StringUtils.isEmpty(scopeId)) {
            throw new APIManagementException("Scope Id cannot be null or empty",
                    ExceptionCodes.SHARED_SCOPE_ID_NOT_SPECIFIED);
        }
        Scope scope = apiProvider.getSharedScopeByUUID(scopeId, tenantDomain);
        ScopeDTO scopeDTO = SharedScopeMappingUtil.fromScopeToDTO(scope);
        return Response.ok().entity(scopeDTO).build();
    }

    /**
     * Get all shared scopes for tenant.
     * TODO: Add pagination from rest api layer only
     *
     * @param messageContext CXF Message Context
     * @return Shared Scopes DTO List
     * @throws APIManagementException if an error occurs while retrieving shared scope
     */
    public Response getSharedScopes(MessageContext messageContext) throws APIManagementException {

        APIProvider apiProvider = RestApiUtil.getLoggedInUserProvider();
        String tenantDomain = RestApiUtil.getLoggedInUserTenantDomain();

        List<Scope> scopeList = apiProvider.getAllSharedScopes(tenantDomain);
        ScopeListDTO sharedScopeListDTO = SharedScopeMappingUtil.fromScopeListToDTO(scopeList);
        return Response.ok().entity(sharedScopeListDTO).build();
    }

    /**
     * Update Shared Scope By Id.
     *
     * @param scopeId        Shared Scope Id
     * @param body           Shared Scope DTO
     * @param messageContext CXF Message Context
     * @return Updated Shared Scope DTO
     * @throws APIManagementException if an error occurs while updating shared scope
     */
    public Response updateSharedScope(String scopeId, ScopeDTO body, MessageContext messageContext)
            throws APIManagementException {

        APIProvider apiProvider = RestApiUtil.getLoggedInUserProvider();
        String tenantDomain = RestApiUtil.getLoggedInUserTenantDomain();
        if (StringUtils.isEmpty(scopeId)) {
            throw new APIManagementException("Shared Scope Id cannot be null or empty",
                    ExceptionCodes.SHARED_SCOPE_ID_NOT_SPECIFIED);
        }
        Scope existingScope = apiProvider.getSharedScopeByUUID(scopeId, tenantDomain);
        //Override scope Id and name in request body from existing scope
        body.setId(existingScope.getId());
        body.setName(existingScope.getName());
        Scope scope = SharedScopeMappingUtil.fromDTOToScope(body);
        apiProvider.updateSharedScope(scope, tenantDomain);
        //Get updated shared scope
        scope = apiProvider.getSharedScopeByUUID(scope.getId(), tenantDomain);
        ScopeDTO updatedScopeDTO = SharedScopeMappingUtil.fromScopeToDTO(scope);
        return Response.ok().entity(updatedScopeDTO).build();
    }
}
