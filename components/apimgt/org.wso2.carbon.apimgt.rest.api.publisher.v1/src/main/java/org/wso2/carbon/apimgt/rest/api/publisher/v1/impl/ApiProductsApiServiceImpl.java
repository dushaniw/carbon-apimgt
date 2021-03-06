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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIMgtResourceNotFoundException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.ExceptionCodes;
import org.wso2.carbon.apimgt.api.FaultGatewaysException;
import org.wso2.carbon.apimgt.api.model.APIProduct;
import org.wso2.carbon.apimgt.api.model.APIProductIdentifier;
import org.wso2.carbon.apimgt.api.model.APIProductResource;
import org.wso2.carbon.apimgt.api.model.Documentation;
import org.wso2.carbon.apimgt.api.model.HistoryEvent;
import org.wso2.carbon.apimgt.api.model.ResourceFile;
import org.wso2.carbon.apimgt.api.model.SubscribedAPI;
import org.wso2.carbon.apimgt.impl.importexport.APIImportExportException;
import org.wso2.carbon.apimgt.impl.importexport.ExportFormat;
import org.wso2.carbon.apimgt.impl.importexport.ImportExportAPI;
import org.wso2.carbon.apimgt.impl.importexport.utils.APIImportExportUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.rest.api.common.RestApiCommonUtil;
import org.wso2.carbon.apimgt.rest.api.common.RestApiConstants;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.ApiProductsApiService;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.common.mappings.APIMappingUtil;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.common.mappings.DocumentationMappingUtil;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.common.mappings.PublisherCommonUtils;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.APIProductDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.APIProductListDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.DocumentDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.DocumentListDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.FileInfoDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.HistoryEventListDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.PaginationDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.utils.RestApiPublisherUtils;
import org.wso2.carbon.apimgt.rest.api.util.exception.BadRequestException;
import org.wso2.carbon.apimgt.rest.api.util.utils.RestApiUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.wso2.carbon.apimgt.impl.APIConstants.DOCUMENTATION_INLINE_CONTENT_TYPE;
import static org.wso2.carbon.apimgt.impl.APIConstants.DOCUMENTATION_RESOURCE_MAP_CONTENT_TYPE;
import static org.wso2.carbon.apimgt.impl.APIConstants.DOCUMENTATION_RESOURCE_MAP_DATA;
import static org.wso2.carbon.apimgt.impl.APIConstants.DOCUMENTATION_RESOURCE_MAP_NAME;
import static org.wso2.carbon.apimgt.impl.APIConstants.SEARCH_AND_TAG;
import static org.wso2.carbon.apimgt.impl.APIConstants.UN_AUTHORIZED_ERROR_MESSAGE;

public class ApiProductsApiServiceImpl implements ApiProductsApiService {
    private static final Log log = LogFactory.getLog(ApiProductsApiServiceImpl.class);

    @Override public Response deleteAPIProduct(String apiProductId, String ifMatch,
            MessageContext messageContext) {
        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            String username = RestApiCommonUtil.getLoggedInUsername();
            String tenantDomain = MultitenantUtils.getTenantDomain(APIUtil.replaceEmailDomainBack(username));
            APIProductIdentifier apiProductIdentifier = APIMappingUtil.getAPIProductIdentifierFromUUID(apiProductId, tenantDomain);
            if (log.isDebugEnabled()) {
                log.debug("Delete API Product request: Id " +apiProductId + " by " + username);
            }
            APIProduct apiProduct = apiProvider.getAPIProductbyUUID(apiProductId, tenantDomain);
            if (apiProduct == null) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_API_PRODUCT, apiProductId, log);
            }

            List<SubscribedAPI> apiUsages = apiProvider.getAPIProductUsageByAPIProductId(apiProductIdentifier);
            if (apiUsages != null && apiUsages.size() > 0) {
                RestApiUtil.handleConflict("Cannot remove the API " + apiProductIdentifier + " as active subscriptions exist", log);
            }

            apiProvider.deleteAPIProduct(apiProduct.getId(), apiProductId);
            return Response.ok().build();
        } catch (APIManagementException e) {
            String errorMessage = "Error while deleting API Product : " + apiProductId;
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        }
        return null;
    }

    @Override
    public Response getAPIProductDocumentContent(String apiProductId,
            String documentId, String accept, String ifNoneMatch, MessageContext messageContext) {
        Documentation documentation;
        try {
            String username = RestApiCommonUtil.getLoggedInUsername();
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();

            //this will fail if user does not have access to the API Product or the API Product does not exist
            APIProductIdentifier productIdentifier = APIMappingUtil.getAPIProductIdentifierFromUUID(apiProductId, tenantDomain);
            documentation = apiProvider.getProductDocumentation(documentId, tenantDomain);
            if (documentation == null) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_PRODUCT_DOCUMENTATION, documentId, log);
                return null;
            }

            //gets the content depending on the type of the document
            if (documentation.getSourceType().equals(Documentation.DocumentSourceType.FILE)) {
                String resource = documentation.getFilePath();
                Map<String, Object> docResourceMap = APIUtil.getDocument(username, resource, tenantDomain);
                Object fileDataStream = docResourceMap.get(DOCUMENTATION_RESOURCE_MAP_DATA);
                Object contentType = docResourceMap.get(DOCUMENTATION_RESOURCE_MAP_CONTENT_TYPE);
                contentType = contentType == null ? RestApiConstants.APPLICATION_OCTET_STREAM : contentType;
                String name = docResourceMap.get(DOCUMENTATION_RESOURCE_MAP_NAME).toString();
                return Response.ok(fileDataStream)
                        .header(RestApiConstants.HEADER_CONTENT_TYPE, contentType)
                        .header(RestApiConstants.HEADER_CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                        .build();
            } else if (documentation.getSourceType().equals(Documentation.DocumentSourceType.INLINE) || documentation.getSourceType().equals(Documentation.DocumentSourceType.MARKDOWN)) {
                String content = apiProvider.getDocumentationContent(productIdentifier, documentation.getName());
                return Response.ok(content)
                        .header(RestApiConstants.HEADER_CONTENT_TYPE, DOCUMENTATION_INLINE_CONTENT_TYPE)
                        .build();
            } else if (documentation.getSourceType().equals(Documentation.DocumentSourceType.URL)) {
                String sourceUrl = documentation.getSourceUrl();
                return Response.seeOther(new URI(sourceUrl)).build();
            }
        } catch (APIManagementException e) {
            //Auth failure occurs when cross tenant accessing APIs. Sends 404, since we don't need to expose the existence of the resource
            if (RestApiUtil.isDueToResourceNotFound(e) || RestApiUtil.isDueToAuthorizationFailure(e)) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_PRODUCT_DOCUMENTATION, apiProductId, e, log);
            } else if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure(
                        "Authorization failure while retrieving document : " + documentId + " of API Product " + apiProductId, e, log);
            } else {
                String errorMessage = "Error while retrieving document " + documentId + " of the API Product" + apiProductId;
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        } catch (URISyntaxException e) {
            String errorMessage = "Error while retrieving source URI location of " + documentId;
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        }
        return null;
    }

    @Override
    public Response addAPIProductDocumentContent(String apiProductId, String documentId,
                              String ifMatch, InputStream fileInputStream, Attachment fileDetail, String inlineContent,
                                                                          MessageContext messageContext) {
        try {
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            APIProductIdentifier productIdentifier = APIMappingUtil
                    .getAPIProductIdentifierFromUUID(apiProductId, tenantDomain);
            APIProduct product = apiProvider.getAPIProduct(productIdentifier);
            if (fileInputStream != null && inlineContent != null) {
                RestApiUtil.handleBadRequest("Only one of 'file' and 'inlineContent' should be specified", log);
            }

            //retrieves the document and send 404 if not found
            Documentation documentation = apiProvider.getProductDocumentation(documentId, tenantDomain);
            if (documentation == null) {
                RestApiUtil
                        .handleResourceNotFoundError(RestApiConstants.RESOURCE_PRODUCT_DOCUMENTATION, documentId, log);
                return null;
            }

            //add content depending on the availability of either input stream or inline content
            if (fileInputStream != null) {
                if (!documentation.getSourceType().equals(Documentation.DocumentSourceType.FILE)) {
                    RestApiUtil.handleBadRequest("Source type of product document " + documentId + " is not FILE", log);
                }
                RestApiPublisherUtils
                        .attachFileToProductDocument(apiProductId, documentation, fileInputStream, fileDetail);
            } else if (inlineContent != null) {
                if (!documentation.getSourceType().equals(Documentation.DocumentSourceType.INLINE) && !documentation
                        .getSourceType().equals(Documentation.DocumentSourceType.MARKDOWN)) {
                    RestApiUtil.handleBadRequest(
                            "Source type of product document " + documentId + " is not INLINE " + "or MARKDOWN", log);
                }
                apiProvider.addProductDocumentationContent(product, documentation.getName(), inlineContent);
            } else {
                RestApiUtil.handleBadRequest("Either 'file' or 'inlineContent' should be specified", log);
            }

            //retrieving the updated doc and the URI
            Documentation updatedDoc = apiProvider.getProductDocumentation(documentId, tenantDomain);
            DocumentDTO documentDTO = DocumentationMappingUtil.fromDocumentationToDTO(updatedDoc);
            String uriString = RestApiConstants.RESOURCE_PATH_PRODUCT_DOCUMENT_CONTENT
                    .replace(RestApiConstants.APIPRODUCTID_PARAM, apiProductId)
                    .replace(RestApiConstants.DOCUMENTID_PARAM, documentId);
            URI uri = new URI(uriString);
            return Response.created(uri).entity(documentDTO).build();
        } catch (APIManagementException e) {
            //Auth failure occurs when cross tenant accessing APIs. Sends 404, since we don't need to expose the existence of the resource
            if (RestApiUtil.isDueToResourceNotFound(e) || RestApiUtil.isDueToAuthorizationFailure(e)) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_API_PRODUCT, apiProductId, e, log);
            } else if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure(
                        "Authorization failure while adding content to the document: " + documentId + " of API Product "
                                + apiProductId, e, log);
            } else {
                RestApiUtil.handleInternalServerError("Failed to add content to the document " + documentId, e, log);
            }
        } catch (URISyntaxException e) {
            String errorMessage = "Error while retrieving document content location : " + documentId;
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
        return null;
    }

    @Override
    public Response deleteAPIProductDocument(String apiProductId, String documentId,
            String ifMatch, MessageContext messageContext) {
        Documentation documentation;
        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();

            //this will fail if user does not have access to the API Product or the API Product does not exist
            APIProductIdentifier productIdentifier = APIMappingUtil
                    .getAPIProductIdentifierFromUUID(apiProductId, tenantDomain);
            documentation = apiProvider.getProductDocumentation(documentId, tenantDomain);
            if (documentation == null) {
                RestApiUtil
                        .handleResourceNotFoundError(RestApiConstants.RESOURCE_PRODUCT_DOCUMENTATION, documentId, log);
            }
            apiProvider.removeDocumentation(productIdentifier, documentId);
            return Response.ok().build();
        } catch (APIManagementException e) {
            //Auth failure occurs when cross tenant accessing API Products. Sends 404, since we don't need to expose the existence of the resource
            if (RestApiUtil.isDueToResourceNotFound(e) || RestApiUtil.isDueToAuthorizationFailure(e)) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_API_PRODUCT, apiProductId, e, log);
            } else if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure(
                        "Authorization failure while deleting : " + documentId + " of API Product " + apiProductId, e,
                        log);
            } else {
                String errorMessage = "Error while retrieving API Product : " + apiProductId;
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    @Override
    public Response getAPIProductDocument(String apiProductId, String documentId,
            String accept, String ifNoneMatch, MessageContext messageContext) {
        Documentation documentation;
        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            documentation = apiProvider.getProductDocumentation(documentId, tenantDomain);
            APIMappingUtil.getAPIProductIdentifierFromUUID(apiProductId, tenantDomain);
            if (documentation == null) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_PRODUCT_DOCUMENTATION, documentId, log);
            }

            DocumentDTO documentDTO = DocumentationMappingUtil.fromDocumentationToDTO(documentation);
            return Response.ok().entity(documentDTO).build();
        } catch (APIManagementException e) {
            //Auth failure occurs when cross tenant accessing API Products. Sends 404, since we don't need to expose the existence of the resource
            if (RestApiUtil.isDueToResourceNotFound(e) || RestApiUtil.isDueToAuthorizationFailure(e)) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_API_PRODUCT, apiProductId, e, log);
            } else if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure(
                        "Authorization failure while retrieving document : " + documentId + " of API Product "
                                + apiProductId, e, log);
            } else {
                String errorMessage = "Error while retrieving document : " + documentId;
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    @Override
    public Response updateAPIProductDocument(String apiProductId, String documentId,
            DocumentDTO body, String ifMatch, MessageContext messageContext) {
        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            String sourceUrl = body.getSourceUrl();
            Documentation oldDocument = apiProvider.getProductDocumentation(documentId, tenantDomain);

            //validation checks for existence of the document
            if (oldDocument == null) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_PRODUCT_DOCUMENTATION, documentId, log);
                return null;
            }
            if (body.getType() == null) {
                throw new BadRequestException();
            }
            if (body.getType() == DocumentDTO.TypeEnum.OTHER && org.apache.commons.lang3.StringUtils.isBlank(body.getOtherTypeName())) {
                //check otherTypeName for not null if doc type is OTHER
                RestApiUtil.handleBadRequest("otherTypeName cannot be empty if type is OTHER.", log);
                return null;
            }
            if (body.getSourceType() == DocumentDTO.SourceTypeEnum.URL &&
                    (org.apache.commons.lang3.StringUtils.isBlank(sourceUrl) || !RestApiCommonUtil.isURL(sourceUrl))) {
                RestApiUtil.handleBadRequest("Invalid document sourceUrl Format", log);
                return null;
            }

            //overriding some properties
            body.setName(oldDocument.getName());

            Documentation newDocumentation = DocumentationMappingUtil.fromDTOtoDocumentation(body);
            //this will fail if user does not have access to the API or the API does not exist
            APIProductIdentifier apiIdentifier = APIMappingUtil.getAPIProductIdentifierFromUUID(apiProductId, tenantDomain);
            newDocumentation.setFilePath(oldDocument.getFilePath());
            apiProvider.updateDocumentation(apiIdentifier, newDocumentation);

            //retrieve the updated documentation
            newDocumentation = apiProvider.getProductDocumentation(documentId, tenantDomain);
            return Response.ok().entity(DocumentationMappingUtil.fromDocumentationToDTO(newDocumentation)).build();
        } catch (APIManagementException e) {
            //Auth failure occurs when cross tenant accessing APIs. Sends 404, since we don't need to expose the existence of the resource
            if (RestApiUtil.isDueToResourceNotFound(e) || RestApiUtil.isDueToAuthorizationFailure(e)) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_API_PRODUCT, apiProductId, e, log);
            } else if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure(
                        "Authorization failure while updating document : " + documentId + " of API Product " + apiProductId, e, log);
            } else {
                String errorMessage = "Error while updating the document " + documentId + " for API Product : " + apiProductId;
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    @Override
    public Response getAPIProductDocuments(String apiProductId, Integer limit, Integer offset,
            String accept, String ifNoneMatch, MessageContext messageContext) {

        limit = limit != null ? limit : RestApiConstants.PAGINATION_LIMIT_DEFAULT;
        offset = offset != null ? offset : RestApiConstants.PAGINATION_OFFSET_DEFAULT;

        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            //this will fail if user does not have access to the API Product or the API Product does not exist
            APIProductIdentifier productIdentifier = APIMappingUtil.getAPIProductIdentifierFromUUID(apiProductId, tenantDomain);
            List<Documentation> allDocumentation = apiProvider.getAllDocumentation(productIdentifier);
            DocumentListDTO documentListDTO = DocumentationMappingUtil.fromDocumentationListToDTO(allDocumentation,
                    offset, limit);
            DocumentationMappingUtil
                    .setPaginationParams(documentListDTO, apiProductId, offset, limit, allDocumentation.size());
            return Response.ok().entity(documentListDTO).build();
        } catch (APIManagementException e) {
            //Auth failure occurs when cross tenant accessing APIs. Sends 404, since we don't need to expose the existence of the resource
            if (RestApiUtil.isDueToResourceNotFound(e) || RestApiUtil.isDueToAuthorizationFailure(e)) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_API_PRODUCT, apiProductId, e, log);
            } else if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure(
                        "Authorization failure while retrieving documents of API Product : " + apiProductId, e, log);
            } else {
                String msg = "Error while retrieving documents of API Product " + apiProductId;
                RestApiUtil.handleInternalServerError(msg, e, log);
            }
        }
        return null;
    }

    @Override
    public Response addAPIProductDocument(String apiProductId, DocumentDTO body,
            MessageContext messageContext) {
        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            if (body.getType() == null) {
                throw new BadRequestException();
            }
            if (body.getType() == DocumentDTO.TypeEnum.OTHER && org.apache.commons.lang3.StringUtils.isBlank(body.getOtherTypeName())) {
                //check otherTypeName for not null if doc type is OTHER
                RestApiUtil.handleBadRequest("otherTypeName cannot be empty if type is OTHER.", log);
            }
            String sourceUrl = body.getSourceUrl();
            if (body.getSourceType() == DocumentDTO.SourceTypeEnum.URL &&
                    (org.apache.commons.lang3.StringUtils.isBlank(sourceUrl) || !RestApiCommonUtil.isURL(sourceUrl))) {
                RestApiUtil.handleBadRequest("Invalid document sourceUrl Format", log);
            }
            Documentation documentation = DocumentationMappingUtil.fromDTOtoDocumentation(body);
            String documentName = body.getName();
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            //this will fail if user does not have access to the API Product or the API Product does not exist
            APIProductIdentifier productIdentifier = APIMappingUtil.getAPIProductIdentifierFromUUID(apiProductId, tenantDomain);
            if (apiProvider.isDocumentationExist(productIdentifier, documentName)) {
                String errorMessage = "Requested document '" + documentName + "' already exists";
                RestApiUtil.handleResourceAlreadyExistsError(errorMessage, log);
            }
            apiProvider.addDocumentation(productIdentifier, documentation);

            //retrieve the newly added document
            String newDocumentId = documentation.getId();
            documentation = apiProvider.getProductDocumentation(newDocumentId, tenantDomain);
            DocumentDTO newDocumentDTO = DocumentationMappingUtil.fromDocumentationToDTO(documentation);
            String uriString = RestApiConstants.RESOURCE_PATH_PRODUCT_DOCUMENTS_DOCUMENT_ID
                    .replace(RestApiConstants.APIPRODUCTID_PARAM, apiProductId)
                    .replace(RestApiConstants.DOCUMENTID_PARAM, newDocumentId);
            URI uri = new URI(uriString);
            return Response.created(uri).entity(newDocumentDTO).build();
        } catch (APIManagementException e) {
            //Auth failure occurs when cross tenant accessing API Products. Sends 404, since we don't need to expose the existence of the resource
            if (RestApiUtil.isDueToResourceNotFound(e) || RestApiUtil.isDueToAuthorizationFailure(e)) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_API_PRODUCT, apiProductId, e, log);
            } else if (isAuthorizationFailure(e)) {
                RestApiUtil
                        .handleAuthorizationFailure("Authorization failure while adding documents of API : " + apiProductId, e,
                                log);
            } else {
                String errorMessage = "Error while adding the document for API : " + apiProductId;
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        } catch (URISyntaxException e) {
            String errorMessage = "Error while retrieving location for document " + body.getName() + " of API " + apiProductId;
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        }
        return null;
    }

    @Override public Response getAPIProduct(String apiProductId, String accept, String ifNoneMatch,
            MessageContext messageContext) {
        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            String username = RestApiCommonUtil.getLoggedInUsername();
            String tenantDomain = MultitenantUtils.getTenantDomain(APIUtil.replaceEmailDomainBack(username));
            if (log.isDebugEnabled()) {
                log.debug("API Product request: Id " +apiProductId + " by " + username);
            }
            APIProduct apiProduct = apiProvider.getAPIProductbyUUID(apiProductId, tenantDomain);
            if (apiProduct == null) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_API_PRODUCT, apiProductId, log);
            }

            APIProductDTO createdApiProductDTO = APIMappingUtil.fromAPIProducttoDTO(apiProduct);
            return Response.ok().entity(createdApiProductDTO).build();
        } catch (APIManagementException e) {
            String errorMessage = "Error while retrieving API Product from Id  : " + apiProductId ;
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        }
        return null;
    }

    @Override
    public Response getIsAPIProductOutdated(String apiProductId, String accept, String ifNoneMatch,
                                                         MessageContext messageContext) throws APIManagementException {
        return null;
    }

    @Override
    public Response updateAPIProduct(String apiProductId, APIProductDTO body, String ifMatch,
            MessageContext messageContext) {
        try {
            String username = RestApiCommonUtil.getLoggedInUsername();
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            APIProvider apiProvider = RestApiCommonUtil.getProvider(username);
            APIProduct retrievedProduct = apiProvider.getAPIProductbyUUID(apiProductId, tenantDomain);
            if (retrievedProduct == null) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_API_PRODUCT, apiProductId, log);
            }
            APIProduct updatedProduct = PublisherCommonUtils.updateApiProduct(retrievedProduct, body, apiProvider, username);
            APIProductDTO updatedProductDTO = APIMappingUtil.fromAPIProducttoDTO(updatedProduct);
            return Response.ok().entity(updatedProductDTO).build();
        } catch (APIManagementException | FaultGatewaysException e) {
            String errorMessage = "Error while updating API Product : " + apiProductId;
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        }
        return null;
    }

    @Override public Response getAPIProductSwagger(String apiProductId, String accept, String ifNoneMatch,
            MessageContext messageContext) {
        try {
            String username = RestApiCommonUtil.getLoggedInUsername();
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            APIProvider apiProvider = RestApiCommonUtil.getProvider(username);
            APIProduct retrievedProduct = apiProvider.getAPIProductbyUUID(apiProductId, tenantDomain);
            if (retrievedProduct == null) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_API_PRODUCT, apiProductId, log);
            }
            String apiSwagger = apiProvider.getAPIDefinitionOfAPIProduct(retrievedProduct);
            
            if (StringUtils.isEmpty(apiSwagger)) {
                apiSwagger = "";
            }
            return Response.ok().entity(apiSwagger).build();
        } catch (APIManagementException e) {
            String errorMessage = "Error while retrieving API Product from Id  : " + apiProductId;
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        }
        return null;
    }

    @Override
    public Response getAPIProductThumbnail(String apiProductId, String accept,
            String ifNoneMatch, MessageContext messageContext) {
        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            //this will fail if user does not have access to the API or the API does not exist
            APIProductIdentifier productIdentifier = APIMappingUtil
                    .getAPIProductIdentifierFromUUID(apiProductId, tenantDomain);
            ResourceFile thumbnailResource = apiProvider.getProductIcon(productIdentifier);

            if (thumbnailResource != null) {
                return Response
                        .ok(thumbnailResource.getContent(), MediaType.valueOf(thumbnailResource.getContentType()))
                        .build();
            } else {
                return Response.noContent().build();
            }
        } catch (APIManagementException e) {
            //Auth failure occurs when cross tenant accessing API Products. Sends 404, since we don't need to expose the
            // existence of the resource
            if (RestApiUtil.isDueToResourceNotFound(e) || RestApiUtil.isDueToAuthorizationFailure(e)) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_API_PRODUCT, apiProductId, e, log);
            } else if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure(
                        "Authorization failure while retrieving thumbnail of API Product : " + apiProductId, e, log);
            } else {
                String errorMessage = "Error while retrieving thumbnail of API Product : " + apiProductId;
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    @Override
    public Response updateAPIProductThumbnail(String apiProductId, InputStream fileInputStream,
            Attachment fileDetail, String ifMatch, MessageContext messageContext) {
        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            String fileName = fileDetail.getDataHandler().getName();
            String fileContentType = URLConnection.guessContentTypeFromName(fileName);
            if (org.apache.commons.lang3.StringUtils.isBlank(fileContentType)) {
                fileContentType = fileDetail.getContentType().toString();
            }

            //this will fail if user does not have access to the API or the API does not exist
            APIProduct apiProduct = apiProvider.getAPIProductbyUUID(apiProductId, tenantDomain);
            ResourceFile apiImage = new ResourceFile(fileInputStream, fileContentType);
            String thumbPath = APIUtil.getProductIconPath(apiProduct.getId());
            String thumbnailUrl = apiProvider.addProductResourceFile(apiProduct.getId(), thumbPath, apiImage);
            apiProduct.setThumbnailUrl(APIUtil.prependTenantPrefix(thumbnailUrl, apiProduct.getId().getProviderName()));
            APIUtil.setResourcePermissions(apiProduct.getId().getProviderName(), null, null, thumbPath);

            //need to set product resource mappings before updating product, otherwise existing mappings will be lost
            List<APIProductResource> resources = apiProvider.getResourcesOfAPIProduct(apiProduct.getId());
            apiProduct.setProductResources(resources);
            apiProvider.updateAPIProduct(apiProduct);

            String uriString = RestApiConstants.RESOURCE_PATH_THUMBNAIL
                    .replace(RestApiConstants.APIID_PARAM, apiProductId);
            URI uri = new URI(uriString);
            FileInfoDTO infoDTO = new FileInfoDTO();
            infoDTO.setRelativePath(uriString);
            infoDTO.setMediaType(apiImage.getContentType());
            return Response.created(uri).entity(infoDTO).build();
        } catch (APIManagementException | FaultGatewaysException e) {
            String errorMessage = "Error while updating API Product : " + apiProductId;
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        } catch (URISyntaxException e) {
            String errorMessage = "Error while retrieving thumbnail location of API Product : " + apiProductId;
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        }
        return null;
    }

    /**
     * Exports an API Product from API Manager. Meta information, API Product icon, documentation, client certificates
     * and dependent APIs are exported. This service generates a zipped archive which contains all the above mentioned
     * resources for a given API Product.
     *
     * @param name           Name of the API Product that needs to be exported
     * @param version        Version of the API Product that needs to be exported
     * @param providerName   Provider name of the API Product that needs to be exported
     * @param format         Format of output documents. Can be YAML or JSON
     * @param preserveStatus Preserve API Product status on export
     * @return Zipped file containing exported API Product
     */
    @Override
    public Response exportAPIProduct(String name, String version, String providerName, String format,
                                         Boolean preserveStatus, MessageContext messageContext)
            throws APIManagementException {

        //If not specified status is preserved by default
        preserveStatus = preserveStatus == null || preserveStatus;

        // Default export format is YAML
        ExportFormat exportFormat = StringUtils.isNotEmpty(format) ? ExportFormat.valueOf(format.toUpperCase()) :
                ExportFormat.YAML;
        ImportExportAPI importExportAPI = APIImportExportUtil.getImportExportAPI();
        try {
            File file =
                    importExportAPI.exportAPIProduct(null, name, version, providerName, exportFormat, preserveStatus,
                            true, true);
            return Response.ok(file)
                    .header(RestApiConstants.HEADER_CONTENT_DISPOSITION, "attachment; filename=\""
                            + file.getName() + "\"")
                    .build();
        } catch (APIManagementException | APIImportExportException e) {
            RestApiUtil.handleInternalServerError("Error while exporting " +
                    RestApiConstants.RESOURCE_API_PRODUCT, e, log);
        }
        return null;
    }

    @Override
    public Response getAllAPIProducts(Integer limit, Integer offset, String query, String accept,
            String ifNoneMatch, MessageContext messageContext) {

        List<APIProduct> allMatchedProducts = new ArrayList<>();
        APIProductListDTO apiProductListDTO;

        //setting default limit and offset values if they are not set
        limit = limit != null ? limit : RestApiConstants.PAGINATION_LIMIT_DEFAULT;
        offset = offset != null ? offset : RestApiConstants.PAGINATION_OFFSET_DEFAULT;
        query = query == null ? "" : query;

        try {
            //for now one criterea is supported
            String searchQuery = StringUtils.replace(query, ":", "=");
            searchQuery = searchQuery.equals("") ? RestApiConstants.GET_API_PRODUCT_QUERY : query + SEARCH_AND_TAG +
                    RestApiConstants.GET_API_PRODUCT_QUERY;

            String username = RestApiCommonUtil.getLoggedInUsername();
            String tenantDomain = MultitenantUtils.getTenantDomain(APIUtil.replaceEmailDomainBack(username));
            if (log.isDebugEnabled()) {
                log.debug("API Product list request by " + username);
            }
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            Map<String, Object> result = apiProvider.searchPaginatedAPIProducts(searchQuery, tenantDomain, offset, limit);

            Set<APIProduct> apiProducts = (Set<APIProduct>) result.get("products");
            allMatchedProducts.addAll(apiProducts);

            apiProductListDTO = APIMappingUtil.fromAPIProductListtoDTO(allMatchedProducts);

            //Add pagination section in the response
            Object totalLength = result.get("length");
            Integer length = 0;
            if (totalLength != null) {
                length = (Integer) totalLength;
            }
            APIMappingUtil.setPaginationParams(apiProductListDTO, query, offset, limit, length);

            return Response.ok().entity(apiProductListDTO).build();
        } catch (APIManagementException e) {
            String errorMessage = "Error while retrieving API Products ";
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        }
        return null;
    }

    /**
     * Import an API Product by uploading an archive file. All relevant API Product data will be included upon the creation of
     * the API Product. Depending on the choice of the user, provider of the imported API Product will be preserved or modified.
     *
     * @param fileInputStream       UploadedInputStream input stream from the REST request
     * @param fileDetail            File details as Attachment
     * @param preserveProvider      User choice to keep or replace the API Product provider
     * @param importAPIs            Whether to import the dependent APIs or not.
     * @param overwriteAPIProduct   Whether to update the API Product or not. This is used when updating already existing API Products.
     * @param overwriteAPIs         Whether to update the dependent APIs or not. This is used when updating already existing dependent APIs of an API Product.
     * @return API Product import response
     */
    @Override public Response importAPIProduct(InputStream fileInputStream, Attachment fileDetail,
            Boolean preserveProvider, Boolean importAPIs, Boolean overwriteAPIProduct, Boolean overwriteAPIs,
            MessageContext messageContext) throws APIManagementException {
        // If importAPIs flag is not set, the default value is false
        importAPIs = importAPIs == null ? false : importAPIs;

        // Check if the URL parameter value is specified, otherwise the default value is true.
        preserveProvider = preserveProvider == null || preserveProvider;

        String[] tokenScopes = (String[]) PhaseInterceptorChain.getCurrentMessage().getExchange()
                .get(RestApiConstants.USER_REST_API_SCOPES);
        ImportExportAPI importExportAPI = APIImportExportUtil.getImportExportAPI();

        // Validate if the USER_REST_API_SCOPES is not set in WebAppAuthenticator when scopes are validated
        // If the user need to import dependent APIs and the user has the required scope for that, allow the user to do it
        if (tokenScopes == null) {
            RestApiUtil.handleInternalServerError("Error occurred while importing the API Product", log);
            return null;
        } else {
            Boolean isRequiredScopesAvailable = Arrays.asList(tokenScopes)
                    .contains(RestApiConstants.API_IMPORT_EXPORT_SCOPE);
            if (!isRequiredScopesAvailable) {
                log.info("Since the user does not have required scope: " + RestApiConstants.API_IMPORT_EXPORT_SCOPE
                        + ", importAPIs will be set to false");
            }
            importAPIs = importAPIs && isRequiredScopesAvailable;
        }

        // Check whether to update the API Product. If not specified, default value is false.
        overwriteAPIProduct = overwriteAPIProduct == null ? false : overwriteAPIProduct;

        // Check whether to update the dependent APIs. If not specified, default value is false.
        overwriteAPIs = overwriteAPIs == null ? false : overwriteAPIs;

        // Check if the URL parameter value is specified, otherwise the default value is true.
        preserveProvider = preserveProvider == null || preserveProvider;

        importExportAPI.importAPIProduct(fileInputStream, preserveProvider, overwriteAPIProduct, overwriteAPIs, importAPIs,
                        tokenScopes);
        return Response.status(Response.Status.OK).entity("API Product imported successfully.").build();
    }

    @Override public Response createAPIProduct(APIProductDTO body, MessageContext messageContext) {
        String provider = null;
        try {
            APIProduct createdProduct = PublisherCommonUtils.addAPIProductWithGeneratedSwaggerDefinition(body, provider,
                    RestApiCommonUtil.getLoggedInUsername());
            APIProductDTO createdApiProductDTO = APIMappingUtil.fromAPIProducttoDTO(createdProduct);
            URI createdApiProductUri = new URI(
                    RestApiConstants.RESOURCE_PATH_API_PRODUCTS + "/" + createdApiProductDTO.getId());
            return Response.created(createdApiProductUri).entity(createdApiProductDTO).build();

        } catch (APIManagementException | FaultGatewaysException e) {
            String errorMessage = "Error while adding new API Product : " + provider + "-" + body.getName()
                    + " - " + e.getMessage();
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        } catch (URISyntaxException e) {
            String errorMessage = "Error while retrieving API Product location : " + provider + "-"
                    + body.getName();
            RestApiUtil.handleInternalServerError(errorMessage, e, log);
        }
        return null;
    }

    /**
     * To check whether a particular exception is due to access control restriction.
     *
     * @param e Exception object.
     * @return true if the the exception is caused due to authorization failure.
     */
    private boolean isAuthorizationFailure(Exception e) {
        String errorMessage = e.getMessage();
        return errorMessage != null && errorMessage.contains(UN_AUTHORIZED_ERROR_MESSAGE);
    }

    @Override
    public Response getAPIProductHistory(String apiProductId, Integer limit, Integer offset, String revisionId,
                                         String startTime, String endTime, MessageContext messageContext)
            throws APIManagementException {

        HistoryEventListDTO historyEventListDTO = new HistoryEventListDTO();
        Date startDate;
        Date endDate;
        String revisionKey = null;
        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            APIProductIdentifier apiProductIdentifier = APIUtil.getAPIProductIdentifierFromUUID(apiProductId);
            if (apiProductIdentifier == null) {
                throw new APIMgtResourceNotFoundException("Failed to get API. API artifact corresponding to artifactId "
                        + apiProductId + " does not exist", ExceptionCodes.from(ExceptionCodes.API_PRODUCT_NOT_FOUND,
                        apiProductId));
            }
            // pre-processing
            // setting default limit and offset values if they are not set
            limit = limit != null ? limit : RestApiConstants.PAGINATION_LIMIT_DEFAULT;
            offset = offset != null ? offset : RestApiConstants.PAGINATION_OFFSET_DEFAULT;
            revisionId = revisionId == null ? "" : revisionId;
            startDate = StringUtils.isNotBlank(startTime) ?
                    Date.from(OffsetDateTime.parse(startTime).toInstant()) : null;
            endDate = StringUtils.isNotBlank(endTime) ?
                    Date.from(OffsetDateTime.parse(endTime).toInstant()) : null;
            if (StringUtils.isNotBlank(revisionId)) {
                revisionKey = apiProvider.getRevisionKeyFromRevisionUUID(revisionId);
                if (revisionKey == null) {
                    throw new APIManagementException("Invalid Revision Id: " + revisionId,
                            ExceptionCodes.from(ExceptionCodes.INVALID_REVISION_ID));
                }
            }
            List<HistoryEvent> historyEvents = apiProvider
                    .getAPIOrAPIProductHistoryWithPagination(apiProductIdentifier, revisionKey, startDate, endDate,
                            offset, limit);
            int eventCount =
                    apiProvider
                            .getAllAPIOrAPIProductHistoryCount(apiProductIdentifier, revisionKey, startDate, endDate);
            historyEventListDTO = APIMappingUtil.fromHistoryEventListToDTO(historyEvents);
            APIMappingUtil
                    .setAPIProductHistoryPaginationParams(historyEventListDTO, apiProductId, revisionId, startTime,
                            endTime, limit, offset, eventCount);
            return Response.ok().entity(historyEventListDTO).build();
        } catch (DateTimeParseException e) {
            throw new APIManagementException("Invalid timestamp format. Timestamp format must be in ISO8601 standard " +
                    "(YYYY-MM-DDThh:mm:ss.fff±hh:mm).", ExceptionCodes.from(ExceptionCodes.INVALID_TIMESTAMP_FORMAT));
        } catch (APIManagementException e) {
            if (RestApiUtil.rootCauseMessageMatches(e, "start index seems to be greater than the limit count")) {
                // This is not an error of the user as he does not know the total number of events available.
                // Thus sends an empty response
                historyEventListDTO.setCount(0);
                historyEventListDTO.setPagination(new PaginationDTO());
                return Response.ok().entity(historyEventListDTO).build();
            }
            if (isAuthorizationFailure(e)) {
                throw new APIManagementException("Authorization failure while retrieving history for API Product: "
                        + apiProductId, ExceptionCodes.from(ExceptionCodes.HISTORY_AUTHORIZATION_FAILURE));
            }
            throw e;
        }
    }

    @Override
    public Response getAPIProductHistoryEventPayload(String apiProductId, String eventId, MessageContext messageContext)
            throws APIManagementException {

        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            APIProductIdentifier apiProductIdentifier = APIUtil.getAPIProductIdentifierFromUUID(apiProductId);
            if (apiProductIdentifier == null) {
                throw new APIMgtResourceNotFoundException("Failed to get API. API artifact corresponding to artifactId "
                        + apiProductId + " does not exist", ExceptionCodes.from(ExceptionCodes.API_PRODUCT_NOT_FOUND,
                        apiProductId));
            }
            String payload = apiProvider.getAPIOrAPIProductHistoryEventPayload(apiProductIdentifier, eventId);
            if (StringUtils.isBlank(payload)) {
                throw new APIManagementException("API Payload Not Found for: " + eventId,
                        ExceptionCodes.from(ExceptionCodes.HISTORY_EVENT_PAYLOAD_NOT_FOUND, eventId));
            }
            return Response.ok().entity(payload).build();
        } catch (APIManagementException e) {
            if (isAuthorizationFailure(e)) {
                throw new APIManagementException("Authorization failure while retrieving history for API Product: "
                        + apiProductId, ExceptionCodes.from(ExceptionCodes.HISTORY_AUTHORIZATION_FAILURE));
            }
            throw e;
        }
    }

}
