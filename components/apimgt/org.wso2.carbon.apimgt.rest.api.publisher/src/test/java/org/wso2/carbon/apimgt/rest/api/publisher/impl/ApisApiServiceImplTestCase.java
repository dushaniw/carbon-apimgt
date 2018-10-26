/*
 *
 *   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.apimgt.rest.api.publisher.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apimgt.core.api.APIPublisher;
import org.wso2.carbon.apimgt.core.api.UserNameMapper;
import org.wso2.carbon.apimgt.core.api.WorkflowResponse;
import org.wso2.carbon.apimgt.core.exception.APICommentException;
import org.wso2.carbon.apimgt.core.exception.APIManagementException;
import org.wso2.carbon.apimgt.core.exception.APIMgtDAOException;
import org.wso2.carbon.apimgt.core.exception.APIMgtWSDLException;
import org.wso2.carbon.apimgt.core.exception.ExceptionCodes;
import org.wso2.carbon.apimgt.core.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.core.impl.APIPublisherImpl;
import org.wso2.carbon.apimgt.core.models.API;
import org.wso2.carbon.apimgt.core.models.Comment;
import org.wso2.carbon.apimgt.core.models.DedicatedGateway;
import org.wso2.carbon.apimgt.core.models.DocumentContent;
import org.wso2.carbon.apimgt.core.models.DocumentInfo;
import org.wso2.carbon.apimgt.core.models.Scope;
import org.wso2.carbon.apimgt.core.models.WSDLArchiveInfo;
import org.wso2.carbon.apimgt.core.models.WSDLInfo;
import org.wso2.carbon.apimgt.core.models.WorkflowStatus;
import org.wso2.carbon.apimgt.core.util.APIUtils;
import org.wso2.carbon.apimgt.core.workflow.GeneralWorkflowResponse;
import org.wso2.carbon.apimgt.rest.api.common.exception.APIMgtSecurityException;
import org.wso2.carbon.apimgt.rest.api.common.exception.BadRequestException;
import org.wso2.carbon.apimgt.rest.api.common.util.RestApiUtil;
import org.wso2.carbon.apimgt.rest.api.publisher.NotFoundException;
import org.wso2.carbon.apimgt.rest.api.publisher.common.SampleTestObjectCreator;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIDefinitionValidationResponseDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.CommentDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.DedicatedGatewayDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.DocumentDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.mappings.CommentMappingUtil;
import org.wso2.carbon.apimgt.rest.api.publisher.utils.MappingUtil;
import org.wso2.carbon.apimgt.rest.api.publisher.utils.RestAPIPublisherUtil;
import org.wso2.carbon.lcm.core.impl.LifecycleState;
import org.wso2.msf4j.Request;
import org.wso2.msf4j.formparam.FileInfo;
import org.wso2.transport.http.netty.message.HTTPCarbonMessage;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;


@RunWith(PowerMockRunner.class)
@PrepareForTest({RestAPIPublisherUtil.class, CommentMappingUtil.class, APIManagerFactory.class})
public class ApisApiServiceImplTestCase {

    private static final Logger log = LoggerFactory.getLogger(ApisApiServiceImplTestCase.class);
    private static final String USER = "admin";
    private static final String IF_MATCH = null;
    private static final String IF_UNMODIFIED_SINCE = null;
    private static final String WSDL = "WSDL";
    private static final String SWAGGER = "SWAGGER";
    private static final String WSDL_FILE = "stockQuote.wsdl";
    private static final String WSDL_FILE_LOCATION = "wsdl" + File.separator + WSDL_FILE;
    private static final String WSDL_ZIP = "WSDLFiles.zip";
    private static final String WSDL_ZIP_LOCATION = "wsdl" + File.separator + WSDL_ZIP;
    private static final String WSDL_FILE_INVALID = "stockQuote_invalid.wsdl";
    private static final String WSDL_FILE_INVALID_LOCATION = "wsdl" + File.separator + WSDL_FILE_INVALID;
    UserNameMapper userNameMapper = Mockito.mock(UserNameMapper.class);

    @Test
    public void testDeleteApi() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String api1Id = UUID.randomUUID().toString();
        Mockito.doNothing().doThrow(new IllegalArgumentException()).when(apiPublisher).deleteAPI(api1Id);
        Response response = apisApiService.apisApiIdDelete(api1Id, null, null, getRequest());
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testDeleteApiErrorCase() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String api1Id = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error Occurred", ExceptionCodes.API_NOT_FOUND))
                .when(apiPublisher).deleteAPI(api1Id);
        Response response = apisApiService.apisApiIdDelete(api1Id, null, null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("API not found"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdContentGetInline() throws Exception {
        String inlineContent = "INLINE CONTENT";
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String api1Id = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultInlineDocumentationInfo().build();
        DocumentContent documentContent = DocumentContent.newDocumentContent().documentInfo(documentInfo).build();
        Mockito.doReturn(documentContent).doThrow(new IllegalArgumentException()).when(apiPublisher).
                getDocumentationContent(documentId);
        Response response = apisApiService.
                apisApiIdDocumentsDocumentIdContentGet(api1Id, documentId, null, null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertEquals(inlineContent, response.getEntity().toString());
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdContentGetURL() throws Exception {
        String URL = "https://www.testapidoc.com/docs/123.pdf";
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String api1Id = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultURLDocumentationInfo().content(URL).build();
        DocumentContent documentContent = DocumentContent.newDocumentContent().documentInfo(documentInfo).build();
        Mockito.doReturn(documentContent).doThrow(new IllegalArgumentException()).when(apiPublisher).
                getDocumentationContent(documentId);
        Response response = apisApiService.
                apisApiIdDocumentsDocumentIdContentGet(api1Id, documentId, null, null, getRequest());
        assertEquals(response.getStatus(), 303);
        assertEquals(URL, response.getHeaderString("Location"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdContentGetFile() throws Exception {
        String fileName = "mytext.txt";
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String api1Id = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultFileDocumentationInfo()
                .sourceType(DocumentInfo.SourceType.FILE).fileName(fileName).build();
        DocumentContent documentContent = DocumentContent.newDocumentContent().documentInfo(documentInfo).build();
        Mockito.doReturn(documentContent).doThrow(new IllegalArgumentException()).when(apiPublisher).
                getDocumentationContent(documentId);
        Response response = apisApiService.
                apisApiIdDocumentsDocumentIdContentGet(api1Id, documentId, null, null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getStringHeaders().get("Content-Disposition").toString().contains(fileName));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdContentGetErrorAPIManagementException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String api1Id = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error Occurred", ExceptionCodes.DOCUMENT_CONTENT_NOT_FOUND))
                .when(apiPublisher).getDocumentationContent(documentId);
        Response response = apisApiService.
                apisApiIdDocumentsDocumentIdContentGet(api1Id, documentId, null, null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("Document content not found"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdContentPostNotFound() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String api1Id = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        Mockito.doReturn(null).doThrow(new IllegalArgumentException()).when(apiPublisher).
                getDocumentationSummary(documentId);
        Response response = apisApiService.
                apisApiIdDocumentsDocumentIdContentPost(api1Id, documentId,
                        null, null,null, null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("Documentation not found"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdContentPostFile() throws Exception {
        String fileName = "swagger.json";
        String contentType = "text/json";
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        FileInfo fileDetail = new FileInfo();
        fileDetail.setFileName(fileName);
        fileDetail.setContentType(contentType);
        FileInputStream fis = new FileInputStream(file);
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String api1Id = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultFileDocumentationInfo().fileName(fileName)
                .build();
        Mockito.doReturn(documentInfo).doThrow(new IllegalArgumentException()).when(apiPublisher).
                getDocumentationSummary(documentId);
        Mockito.doNothing().doThrow(new IllegalArgumentException()).when(apiPublisher).
                uploadAPIDocumentationFile(documentId, fis);
        Response response = apisApiService.
                apisApiIdDocumentsDocumentIdContentPost(api1Id, documentId, fis, fileDetail, null,
                        null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 201);
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdContentPostFileWrongSource() throws Exception {
        String fileName = "swagger.json";
        String contentType = "text/json";
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        FileInfo fileDetail = new FileInfo();
        fileDetail.setFileName(fileName);
        fileDetail.setContentType(contentType);
        FileInputStream fis = new FileInputStream(file);
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String api1Id = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultFileDocumentationInfo().fileName(fileName)
                .sourceType(DocumentInfo.SourceType.INLINE).build();
        Mockito.doReturn(documentInfo).doThrow(new IllegalArgumentException()).when(apiPublisher)
                .getDocumentationSummary(documentId);
        Mockito.doNothing().doThrow(new IllegalArgumentException()).when(apiPublisher)
                .uploadAPIDocumentationFile(documentId, fis);
        Response response = apisApiService.
                apisApiIdDocumentsDocumentIdContentPost(api1Id, documentId, fis, fileDetail, null,
                        null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("is not FILE"));

    }

    @Test
    public void testApisApiIdDocumentsDocumentIdContentPostNullContent() throws Exception {
        String fileName = "swagger.json";
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String api1Id = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultFileDocumentationInfo().fileName(fileName)
                .build();
        Mockito.doReturn(documentInfo).doThrow(new IllegalArgumentException()).when(apiPublisher).
                getDocumentationSummary(documentId);
        Response response = apisApiService.
                apisApiIdDocumentsDocumentIdContentPost(api1Id, documentId, null, null,
                        null, null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("The 'file' content should be specified"));

    }

    @Test
    public void testApisApiIdDocumentsDocumentIdContentPostAPIManagementException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String api1Id = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error Occurred", ExceptionCodes.DOCUMENT_CONTENT_NOT_FOUND))
                .when(apiPublisher).getDocumentationSummary(documentId);
        Response response = apisApiService.
                apisApiIdDocumentsDocumentIdContentPost(api1Id, documentId,
                        null, null, null, null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("Document content not found"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdDelete() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        Mockito.doNothing().doThrow(new IllegalArgumentException()).when(apiPublisher)
                .removeAPIDocumentation(documentId);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdDelete(apiId, documentId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdDeleteErrorCase() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error Occurred", ExceptionCodes.DOCUMENT_NOT_FOUND))
                .when(apiPublisher).removeAPIDocumentation(documentId);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdDelete(apiId, documentId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("Document not found"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdGetURLDocument() throws Exception {
        String URL = "https://www.apidoc.com/docs/123.pdf";
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultURLDocumentationInfo().content(URL).build();
        Mockito.doReturn(documentInfo).doThrow(new IllegalArgumentException()).when(apiPublisher)
                .getDocumentationSummary(documentId);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdGet(apiId, documentId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("Summary of Calculator URL Documentation"));
        assertTrue(response.getEntity().toString().contains("https://www.apidoc.com/docs/123.pdf"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdGetInlineDocument() throws Exception {
        String inlineText = "INLINE TEXT";
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultInlineDocumentationInfo().content(inlineText)
                .build();
        Mockito.doReturn(documentInfo).doThrow(new IllegalArgumentException()).when(apiPublisher)
                .getDocumentationSummary(documentId);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdGet(apiId, documentId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("Summary of Calculator Inline Documentation"));
        assertTrue(response.getEntity().toString().contains("INLINE TEXT"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdGetFileDocument() throws Exception {
        String fileName = "swagger.json";
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultFileDocumentationInfo().fileName(fileName)
                .build();
        Mockito.doReturn(documentInfo).doThrow(new IllegalArgumentException()).when(apiPublisher)
                .getDocumentationSummary(documentId);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdGet(apiId, documentId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("Summary of Calculator File Documentation"));
        assertTrue(response.getEntity().toString().contains("swagger.json"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdGetNotFound() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(null).doThrow(new IllegalArgumentException()).when(apiPublisher)
                .getDocumentationSummary(documentId);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdGet(apiId, documentId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("Documentation not found"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error Occurred", ExceptionCodes.INVALID_DOCUMENT_CONTENT_DATA))
                .when(apiPublisher).getDocumentationSummary(documentId);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdGet(apiId, documentId,
                null, null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("Invalid document content data provided"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdPutNotFound() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultInlineDocumentationInfo().build();
        DocumentDTO documentDTO = MappingUtil.toDocumentDTO(documentInfo);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(null).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getDocumentationSummary(documentId);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdPut(apiId, documentId,
                documentDTO, null, null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("Error while getting document"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdPutOtherTypeNameEmpty() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultInlineDocumentationInfo().otherType("")
                .type(DocumentInfo.DocType.OTHER).build();
        DocumentDTO documentDTO = MappingUtil.toDocumentDTO(documentInfo);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(documentInfo).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getDocumentationSummary(documentId);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdPut(apiId, documentId,
                documentDTO, null, null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("otherTypeName cannot be empty if type is OTHER."));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdPutUrlEmpty() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultURLDocumentationInfo().build();
        DocumentDTO documentDTO = MappingUtil.toDocumentDTO(documentInfo);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(documentInfo).doThrow(new IllegalArgumentException()).when(apiPublisher)
                .getDocumentationSummary(documentId);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdPut(apiId, documentId,
                documentDTO, null, null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("Invalid document sourceUrl Format"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdPut() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        DocumentInfo documentInfo1 = SampleTestObjectCreator.createDefaultInlineDocumentationInfo().build();
        DocumentInfo documentInfo2 = SampleTestObjectCreator.createDefaultInlineDocumentationInfo()
                .id(documentInfo1.getId()).summary("My new summary").build();
        DocumentDTO documentDTO = MappingUtil.toDocumentDTO(documentInfo2);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(documentInfo1).doReturn(documentInfo2).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getDocumentationSummary(documentId);
        Mockito.doReturn("updated").doThrow(new IllegalArgumentException())
                .when(apiPublisher).updateDocumentation(apiId, documentInfo1);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdPut(apiId, documentId,
                documentDTO, null, null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("My new summary"));
    }

    @Test
    public void testApisApiIdDocumentsDocumentIdPutException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        DocumentInfo documentInfo1 = SampleTestObjectCreator.createDefaultInlineDocumentationInfo().build();
        DocumentInfo documentInfo2 = SampleTestObjectCreator.createDefaultInlineDocumentationInfo()
                .id(documentInfo1.getId()).summary("My new summary").build();
        DocumentDTO documentDTO = MappingUtil.toDocumentDTO(documentInfo2);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String documentId = UUID.randomUUID().toString();
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(documentInfo1).doReturn(documentInfo2).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getDocumentationSummary(documentId);
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.INVALID_DOCUMENT_CONTENT_DATA))
                .when(apiPublisher).updateDocumentation(apiId, documentInfo1);
        Response response = apisApiService.apisApiIdDocumentsDocumentIdPut(apiId, documentId,
                documentDTO, null, null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("Invalid document content data provided"));
    }

    @Test
    public void testApisApiIdDocumentsGet() throws Exception {
        printTestMethodName();
        Integer offset = 0;
        Integer limit = 10;
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        List<DocumentInfo> documentInfos = new ArrayList<>();
        documentInfos.add(SampleTestObjectCreator.createDefaultInlineDocumentationInfo().name("NewName1").build());
        documentInfos.add(SampleTestObjectCreator.createDefaultInlineDocumentationInfo().name("NewName2").build());
        Mockito.doReturn(documentInfos).doThrow(new IllegalArgumentException()).when(apiPublisher)
                .getAllDocumentation(apiId, offset, limit);
        Response response = apisApiService.apisApiIdDocumentsGet(apiId, 10, 0, null,
                getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("NewName1"));
    }

    @Test
    public void testApisApiIdDocumentsGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error Occurred", ExceptionCodes.INVALID_DOCUMENT_CONTENT_DATA))
                .when(apiPublisher).getAllDocumentation(apiId, 0, 10);
        Response response = apisApiService.apisApiIdDocumentsGet(apiId, 10, 0, null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("Invalid document content data provided"));
    }

    @Test(expected = BadRequestException.class)
    public void testApisApiIdDocumentsPostEmptyOtherType() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultURLDocumentationInfo()
                .type(DocumentInfo.DocType.OTHER).otherType("").build();
        DocumentDTO documentDTO = MappingUtil.toDocumentDTO(documentInfo);
        Response response = apisApiService.apisApiIdDocumentsPost(apiId, documentDTO,
                null, null, getRequest());
    }

    @Test(expected = BadRequestException.class)
    public void testApisApiIdDocumentsPostEmptyContentUrlType() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultURLDocumentationInfo()
                .sourceType(DocumentInfo.SourceType.URL).content("").build();
        DocumentDTO documentDTO = MappingUtil.toDocumentDTO(documentInfo);
        Response response = apisApiService.apisApiIdDocumentsPost(apiId, documentDTO, null, null,
                getRequest());
    }

    @Test
    public void testApisApiIdDocumentsPost() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultInlineDocumentationInfo()
                .sourceType(DocumentInfo.SourceType.INLINE).build();
        DocumentDTO documentDTO = MappingUtil.toDocumentDTO(documentInfo);
        Mockito.doReturn(documentDTO.getDocumentId()).doThrow(new IllegalArgumentException())
                .when(apiPublisher).addDocumentationInfo(apiId, documentInfo);
        Mockito.doReturn(documentInfo).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getDocumentationSummary(documentDTO.getDocumentId());
        Response response = apisApiService.apisApiIdDocumentsPost(apiId, documentDTO, null, null,
                getRequest());
        assertEquals(response.getStatus(), 201);
        assertTrue(response.getEntity().toString().contains(documentDTO.getDocumentId()));
    }

    @Test
    public void testApisApiIdDocumentsPostException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        DocumentInfo documentInfo = SampleTestObjectCreator.createDefaultInlineDocumentationInfo()
                .sourceType(DocumentInfo.SourceType.INLINE).build();
        DocumentDTO documentDTO = MappingUtil.toDocumentDTO(documentInfo);
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.INVALID_DOCUMENT_CONTENT_DATA))
                .when(apiPublisher).addDocumentationInfo(apiId, documentInfo);
        Response response = apisApiService.apisApiIdDocumentsPost(apiId, documentDTO,
                null, null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("Invalid document content data provided"));
    }

    @Test
    public void testApisApiIdGatewayConfigGet() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn("Sample Config").doThrow(new IllegalArgumentException())
                .when(apiPublisher).getApiGatewayConfig(apiId);
        Response response = apisApiService.apisApiIdGatewayConfigGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("Sample Config"));
    }

    @Test
    public void testApisApiIdGatewayConfigGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.GATEWAY_EXCEPTION))
                .when(apiPublisher).getApiGatewayConfig(apiId);
        Response response = apisApiService.apisApiIdGatewayConfigGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 500);
        assertTrue(response.getEntity().toString().contains("Gateway publishing Error"));
    }


    @Test
    public void testApisApiIdGatewayConfigPut() throws Exception {
        printTestMethodName();
        String gatewayConfig = "Test Config";
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doNothing().doThrow(new IllegalArgumentException())
                .when(apiPublisher).updateApiGatewayConfig(apiId, gatewayConfig);
        Response response = apisApiService.apisApiIdGatewayConfigPut(apiId, gatewayConfig,
                null, null, getRequest());
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testApisApiIdGatewayConfigPutException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        String gatewayConfig = "Test Config";
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.GATEWAY_EXCEPTION))
                .when(apiPublisher).updateApiGatewayConfig(apiId, gatewayConfig);
        Response response = apisApiService.apisApiIdGatewayConfigPut(apiId, gatewayConfig, null,
                null, getRequest());
        assertEquals(response.getStatus(), 500);
        assertTrue(response.getEntity().toString().contains("Gateway publishing Error"));
    }

    @Test
    public void testApisApiIdGetNotExist() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(false).doThrow(new IllegalArgumentException())
                .when(apiPublisher).isAPIExists(apiId);
        Response response = apisApiService.apisApiIdGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("API not found"));
    }

    @Test
    public void testApisApiIdGet() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        API api = SampleTestObjectCreator.createDefaultAPI().id(apiId).build();
        Mockito.doReturn(true).doThrow(new IllegalArgumentException())
                .when(apiPublisher).isAPIExists(apiId);
        Mockito.doReturn(api).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getAPIbyUUID(apiId);
        Response response = apisApiService.apisApiIdGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains(api.getId()));
        assertTrue(response.getEntity().toString().contains(api.getName()));
    }

    @Test
    public void testApisApiIdGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).isAPIExists(apiId);
        Response response = apisApiService.apisApiIdGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }

    @Test
    public void testApisApiIdLifecycleGet() throws Exception {
        printTestMethodName();
        LifecycleState lifecycleState = new LifecycleState();
        lifecycleState.setLcName("APIMDefault");
        lifecycleState.setState("PUBLISHED");
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(lifecycleState).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getAPILifeCycleData(apiId);
        Response response = apisApiService.apisApiIdLifecycleGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 200);
        LifecycleState lifecycleStateRetrieve = (LifecycleState) response.getEntity();
        assertEquals(lifecycleStateRetrieve.getLcName(), lifecycleState.getLcName());
        assertEquals(lifecycleStateRetrieve.getState(), lifecycleState.getState());
    }

    @Test
    public void testApisApiIdLifecycleGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).getAPILifeCycleData(apiId);
        Response response = apisApiService.apisApiIdLifecycleGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }


    @Test
    public void testApisApiIdLifecycleHistoryGetAPINotExist() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(false).doThrow(new IllegalArgumentException())
                .when(apiPublisher).isAPIExists(apiId);
        Response response = apisApiService.apisApiIdLifecycleHistoryGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("API not found"));
    }

    @Test
    public void testApisApiIdLifecycleHistoryGet() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        API api = SampleTestObjectCreator.createDefaultAPI().id(apiId).build();
        Mockito.doReturn(true).doThrow(new IllegalArgumentException())
                .when(apiPublisher).isAPIExists(apiId);
        Mockito.doReturn(api).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getAPIbyUUID(apiId);
        Response response = apisApiService.apisApiIdLifecycleHistoryGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testApisApiIdLifecycleHistoryGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).isAPIExists(apiId);
        Response response = apisApiService.apisApiIdLifecycleHistoryGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }


    @Test
    public void testApisApiIdPut() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.when(apiPublisher.getApiSwaggerDefinition(apiId)).thenReturn(SampleTestObjectCreator.apiDefinition);
        API.APIBuilder apiBuilder = SampleTestObjectCreator.createDefaultAPI();
        API api = apiBuilder.id(apiId).build();
        APIDTO apidto = MappingUtil.toAPIDto(api);
        Mockito.doReturn(api).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getAPIbyUUID(apiId);
        Mockito.doNothing().doThrow(new IllegalArgumentException())
                .when(apiPublisher).updateAPI(apiBuilder);
        Response response = apisApiService.apisApiIdPut(apiId, apidto, null,
                null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains(api.getId()));
        assertTrue(response.getEntity().toString().contains(api.getName()));
    }

    @Test
    public void testApisApiIdPutException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        API.APIBuilder apiBuilder = SampleTestObjectCreator.createDefaultAPI();
        API api = apiBuilder.id(apiId).build();
        APIDTO apidto = MappingUtil.toAPIDto(api);
        Mockito.when(apiPublisher.getApiSwaggerDefinition(apiId)).thenReturn(SampleTestObjectCreator.apiDefinition);
        Mockito.doNothing().doThrow(new IllegalArgumentException())
                .when(apiPublisher).updateAPI(apiBuilder);
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).getAPIbyUUID(apiId);
        Response response = apisApiService.apisApiIdPut(apiId, apidto, null,
                null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }

    @Test
    public void testApisApiIdSwaggerGet() throws Exception {
        printTestMethodName();
        String swagger = "sample swagger";
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(swagger).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getApiSwaggerDefinition(apiId);
        Response response = apisApiService.apisApiIdSwaggerGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains(swagger));
    }

    @Test
    public void testApisApiIdSwaggerGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).getApiSwaggerDefinition(apiId);
        Response response = apisApiService.apisApiIdSwaggerGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }

    @Test
    public void testApisApiIdSwaggerPut() throws Exception {
        printTestMethodName();
        String swagger = IOUtils.toString(new FileInputStream("src" + File.separator + "test" + File
                .separator + "resources" + File.separator + "swaggerWithAuthorization.yaml"));
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doNothing().doThrow(new IllegalArgumentException())
                .when(apiPublisher).saveSwagger20Definition(apiId, swagger);
        Mockito.doReturn(swagger).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getApiSwaggerDefinition(apiId);
        Response response = apisApiService.apisApiIdSwaggerPut(apiId, swagger, null,
                null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains(swagger));
    }

    @Test
    public void testApisApiIdSwaggerPutException() throws Exception {
        printTestMethodName();
        String swagger = "sample swagger";
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doNothing().doThrow(new IllegalArgumentException())
                .when(apiPublisher).saveSwagger20Definition(apiId, swagger);
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).getApiSwaggerDefinition(apiId);
        Response response = apisApiService.apisApiIdSwaggerGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }


    @Test
    public void testApisApiIdThumbnailGet() throws Exception {
        printTestMethodName();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("api1_thumbnail.png").getFile());
        FileInputStream fis = null;
        fis = new FileInputStream(file);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(fis).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getThumbnailImage(apiId);
        Response response = apisApiService.apisApiIdThumbnailGet(apiId, null,
                null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getStringHeaders().get("Content-Disposition").toString().contains("filename"));
    }


    @Test
    public void testApisApiIdThumbnailGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).getThumbnailImage(apiId);
        Response response = apisApiService.apisApiIdThumbnailGet(apiId, null,
                null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }

    @Test
    public void testApisApiIdThumbnailPost() throws Exception {
        printTestMethodName();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("api1_thumbnail.png").getFile());
        FileInputStream fis = null;
        fis = new FileInputStream(file);
        FileInfo fileDetail = new FileInfo();
        fileDetail.setFileName("test.png");
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doNothing().doThrow(new IllegalArgumentException())
                .when(apiPublisher).saveThumbnailImage(apiId, fis, fileDetail.getFileName());
        Response response = apisApiService.apisApiIdThumbnailPost(apiId, fis, fileDetail, null,
                null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 201);
        assertTrue(response.getEntity().toString().contains("application/octet-strea"));
    }


    @Test
    public void testApisApiIdThumbnailPostException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        FileInfo fileDetail = new FileInfo();
        fileDetail.setFileName("test.png");
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).saveThumbnailImage(apiId, null, fileDetail.getFileName());
        Response response = apisApiService.apisApiIdThumbnailPost(apiId, null, fileDetail, null,
                null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }


    @Test
    public void testApisChangeLifecyclePostWithChecklistItemChange() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        String checklist = "test1:test1,test2:test2";
        String action = "CheckListItemChange";
        WorkflowResponse workflowResponse = new GeneralWorkflowResponse();
        workflowResponse.setWorkflowStatus(WorkflowStatus.APPROVED);

        Map<String, Boolean> lifecycleChecklistMap = new HashMap<>();
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doNothing().doThrow(new IllegalArgumentException())
                .when(apiPublisher).updateCheckListItem(apiId, action, lifecycleChecklistMap);
        Response response = apisApiService.apisChangeLifecyclePost(action, apiId, checklist,
                null, null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("APPROVED"));
    }

    @Test
    public void testApisChangeLifecyclePostWithoutChecklistItemNonChange() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        String checklist = "test1:test1,test2:test2";
        String action = "CheckListItemChangeDifferent";
        Map<String, Boolean> lifecycleChecklistMap = new HashMap<>();
        if (checklist != null) {
            String[] checkList = checklist.split(",");
            for (String checkList1 : checkList) {
                StringTokenizer attributeTokens = new StringTokenizer(checkList1, ":");
                String attributeName = attributeTokens.nextToken();
                Boolean attributeValue = Boolean.valueOf(attributeTokens.nextToken());
                lifecycleChecklistMap.put(attributeName, attributeValue);
            }
        }

        WorkflowResponse workflowResponse = new GeneralWorkflowResponse();
        workflowResponse.setWorkflowStatus(WorkflowStatus.CREATED);

        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(workflowResponse).doThrow(new IllegalArgumentException())
                .when(apiPublisher).updateAPIStatus(apiId, action, lifecycleChecklistMap);
        Response response = apisApiService.apisChangeLifecyclePost(action, apiId, checklist,
                null, null, getRequest());
        assertEquals(response.getStatus(), 202);
        assertTrue(response.getEntity().toString().contains("CREATED"));
    }

    @Test
    public void testApisChangeLifecyclePostException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        String action = "CheckListItemChange";
        WorkflowResponse workflowResponse = new GeneralWorkflowResponse();
        workflowResponse.setWorkflowStatus(WorkflowStatus.APPROVED);

        Map<String, Boolean> lifecycleChecklistMap = new HashMap<>();
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).updateCheckListItem(apiId, action, lifecycleChecklistMap);
        Response response = apisApiService.apisChangeLifecyclePost(action, apiId, null,
                null, null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }

    @Test
    public void testApisCopyApiPost() throws Exception {
        printTestMethodName();
        String newVersion = "1.0.0";
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        String newAPIId = UUID.randomUUID().toString();
        API newAPI = SampleTestObjectCreator.createDefaultAPI().id(newAPIId).build();
        Mockito.doReturn(newAPIId).doThrow(new IllegalArgumentException())
                .when(apiPublisher).createNewAPIVersion(apiId, newVersion);
        Mockito.doReturn(newAPI).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getAPIbyUUID(newAPIId);
        Response response = apisApiService.apisCopyApiPost(newVersion, apiId, getRequest());
        assertEquals(response.getStatus(), 201);
        assertTrue(response.getEntity().toString().contains(newAPIId));
    }


    @Test
    public void testApisCopyApiPostException() throws Exception {
        printTestMethodName();
        String newVersion = "1.0.0";
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        String newAPIId = UUID.randomUUID().toString();
        Mockito.doReturn(newAPIId).doThrow(new IllegalArgumentException())
                .when(apiPublisher).createNewAPIVersion(apiId, newVersion);
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).getAPIbyUUID(newAPIId);
        Response response = apisApiService.apisCopyApiPost(newVersion, apiId, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }

    @Test
    public void testApisGet() throws Exception {
        printTestMethodName();
        List<API> apis = new ArrayList<>();
        apis.add(SampleTestObjectCreator.createDefaultAPI().name("newAPI1").build());
        apis.add(SampleTestObjectCreator.createDefaultAPI().name("newAPI2").build());
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Mockito.doReturn(apis).doThrow(new IllegalArgumentException())
                .when(apiPublisher).searchAPIs(10, 0, "", false);
        Response response = apisApiService.apisGet(10, 0, "", null, false, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("newAPI1"));
        assertTrue(response.getEntity().toString().contains("newAPI2"));
    }


    @Test
    public void testApisGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).searchAPIs(10, 0, "", false);
        Response response = apisApiService.apisGet(10, 0, "", null, false, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }


    @Test
    public void testApisHeadNameMatch() throws Exception {
        printTestMethodName();
        String word = "name:testName";
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Mockito.doReturn(true).doThrow(new IllegalArgumentException())
                .when(apiPublisher).checkIfAPINameExists("testName");
        Response response = apisApiService.apisHead(word, null, getRequest());
        assertEquals(response.getStatus(), 200);
    }


    @Test
    public void testApisHeadContextMatch() throws Exception {
        printTestMethodName();
        String word = "context:testContext";
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Mockito.doReturn(true).doThrow(new IllegalArgumentException())
                .when(apiPublisher).checkIfAPIContextExists("testContext");
        Response response = apisApiService.apisHead(word, null, getRequest());
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testApisHeadNoWord() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Mockito.doReturn(true).doThrow(new IllegalArgumentException())
                .when(apiPublisher).checkIfAPIContextExists("testContext");
        Response response = apisApiService.apisHead("", null, getRequest());
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testApisHeadNotFound() throws Exception {
        printTestMethodName();
        String word = "abc:testContext";
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Mockito.doReturn(true).doThrow(new IllegalArgumentException())
                .when(apiPublisher).checkIfAPIContextExists("testContext");
        Response response = apisApiService.apisHead(word, null, getRequest());
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void testApisHeadException() throws Exception {
        printTestMethodName();
        String word = "context:testContext";
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).checkIfAPIContextExists("testContext");
        Response response = apisApiService.apisHead(word, null, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }

    @Test
    public void testApisImportDefinitionPostNonEmpty() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("swagger.json").getFile());
        FileInputStream fis = null;
        fis = new FileInputStream(file);

        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Response response = apisApiService
                .apisImportDefinitionPost(SWAGGER, fis, null, "test", null, null, null, null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("Only one of 'file' and 'url' should be specified"));
    }


    @Test
    public void testApisImportDefinitionPostByFile() throws Exception {

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("swagger.json").getFile());
        FileInputStream fis = null;
        fis = new FileInputStream(file);
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        String apiId = UUID.randomUUID().toString();
        API api = SampleTestObjectCreator.createDefaultAPI().id(apiId).build();
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Mockito.doReturn(apiId).doThrow(new IllegalArgumentException()).when(apiPublisher).
                addApiFromDefinition(fis);
        Mockito.doReturn(api).doThrow(new IllegalArgumentException()).when(apiPublisher).
                getAPIbyUUID(apiId);
        Response response = apisApiService.
                apisImportDefinitionPost(SWAGGER, fis, null, null, null, null, null, null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 201);
        assertTrue(response.getEntity().toString().contains(apiId));
    }

    @Test
    public void testApisImportDefinitionPostException() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("swagger.json").getFile());
        FileInputStream fis = null;
        fis = new FileInputStream(file);
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).addApiFromDefinition(fis);
        Response response = apisApiService.
                apisImportDefinitionPost(null, fis, null, null, null, null, null, null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }

    @Test
    public void testApisImportDefinitionPostWSDLFile() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_FILE_LOCATION).getFile());
        FileInputStream fis = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(WSDL_FILE);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = powerMockDefaultAPIPublisher();
        String apiId = UUID.randomUUID().toString();
        API.APIBuilder apiBuilder = SampleTestObjectCreator.createDefaultAPI().id(apiId);
        API api = apiBuilder.build();
        APIDTO apiDto = MappingUtil.toAPIDto(api);
        ObjectMapper objectMapper = new ObjectMapper();
        String additionalProperties = objectMapper.writeValueAsString(apiDto);
        Mockito.doReturn(api).doThrow(new IllegalArgumentException()).when(apiPublisher).
                getAPIbyUUID(apiId);
        Mockito.doReturn(apiId).when(apiPublisher)
                .addAPIFromWSDLFile(Mockito.argThat(getAPIBuilderMatcher(apiBuilder)), Mockito.eq(fis),
                        Mockito.eq(false));
        Response response = apisApiService.
                apisImportDefinitionPost(WSDL, fis, fileInfo, null, additionalProperties, null, null, null,
                        getRequest());
        fis.close();
        assertEquals(response.getStatus(), 201);
        assertTrue(response.getEntity().toString().contains(apiId));
    }

    @Test
    public void testApisImportDefinitionPostWSDLArchive() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_ZIP_LOCATION).getFile());
        FileInputStream fis = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(WSDL_ZIP);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = powerMockDefaultAPIPublisher();
        String apiId = UUID.randomUUID().toString();
        API.APIBuilder apiBuilder = SampleTestObjectCreator.createDefaultAPI().id(apiId);
        API api = apiBuilder.build();
        APIDTO apiDto = MappingUtil.toAPIDto(api);
        ObjectMapper objectMapper = new ObjectMapper();
        String additionalProperties = objectMapper.writeValueAsString(apiDto);
        Mockito.doReturn(api).doThrow(new IllegalArgumentException()).when(apiPublisher).
                getAPIbyUUID(apiId);
        Mockito.doReturn(apiId).when(apiPublisher)
                .addAPIFromWSDLArchive(Mockito.argThat(getAPIBuilderMatcher(apiBuilder)), Mockito.eq(fis),
                        Mockito.eq(false));
        Response response = apisApiService.
                apisImportDefinitionPost(WSDL, fis, fileInfo, null, additionalProperties, null, null, null,
                        getRequest());
        fis.close();
        assertEquals(response.getStatus(), 201);
        assertTrue(response.getEntity().toString().contains(apiId));
    }

    @Test
    public void testApisImportDefinitionPostWSDLWithNoAdditionalProperties() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_ZIP_LOCATION).getFile());
        FileInputStream fis = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(WSDL_ZIP);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        powerMockDefaultAPIPublisher();
        Response response = apisApiService.
                apisImportDefinitionPost(WSDL, fis, fileInfo, null, null, null, null, null,
                        getRequest());
        fis.close();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testApisImportDefinitionPostWSDLWithNoFilename() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_ZIP_LOCATION).getFile());
        FileInputStream fis = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        String apiId = UUID.randomUUID().toString();
        API.APIBuilder apiBuilder = SampleTestObjectCreator.createDefaultAPI().id(apiId);
        API api = apiBuilder.build();
        APIDTO apiDto = MappingUtil.toAPIDto(api);
        ObjectMapper objectMapper = new ObjectMapper();
        String additionalProperties = objectMapper.writeValueAsString(apiDto);
        powerMockDefaultAPIPublisher();
        Response response = apisApiService.
                apisImportDefinitionPost(WSDL, fis, fileInfo, null, additionalProperties, null, null, null,
                        getRequest());
        fis.close();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testApisImportDefinitionPostWSDLWithInvalidFileExtension() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource("swagger.json").getFile());
        FileInputStream fis = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName("swagger.json");
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        String apiId = UUID.randomUUID().toString();
        API.APIBuilder apiBuilder = SampleTestObjectCreator.createDefaultAPI().id(apiId);
        API api = apiBuilder.build();
        APIDTO apiDto = MappingUtil.toAPIDto(api);
        ObjectMapper objectMapper = new ObjectMapper();
        String additionalProperties = objectMapper.writeValueAsString(apiDto);
        powerMockDefaultAPIPublisher();
        Response response = apisApiService.
                apisImportDefinitionPost(WSDL, fis, fileInfo, null, additionalProperties, null, null, null,
                        getRequest());
        fis.close();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testApisImportDefinitionPostWSDLWithInvalidImplementationType() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_FILE_LOCATION).getFile());
        FileInputStream fis = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(WSDL_FILE);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        String apiId = UUID.randomUUID().toString();
        API.APIBuilder apiBuilder = SampleTestObjectCreator.createDefaultAPI().id(apiId);
        API api = apiBuilder.build();
        APIDTO apiDto = MappingUtil.toAPIDto(api);
        ObjectMapper objectMapper = new ObjectMapper();
        String additionalProperties = objectMapper.writeValueAsString(apiDto);
        powerMockDefaultAPIPublisher();
        Response response = apisApiService.
                apisImportDefinitionPost(WSDL, fis, fileInfo, null, additionalProperties, "InvalidType", null, null,
                        getRequest());
        fis.close();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testApisValidateDefinitionPostWSDLFile() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_FILE_LOCATION).getFile());
        FileInputStream fis = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(WSDL_FILE);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        powerMockDefaultAPIPublisher();
        Response response = apisApiService.
                apisValidateDefinitionPost(WSDL, fis, fileInfo, null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testApisValidateDefinitionPostWSDLArchive() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_ZIP_LOCATION).getFile());
        FileInputStream fis = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(WSDL_ZIP);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = powerMockDefaultAPIPublisher();
        WSDLArchiveInfo archiveInfo = new WSDLArchiveInfo("/tmp", "sample.zip");
        WSDLInfo wsdlInfo = new WSDLInfo();
        wsdlInfo.setVersion("1.1");
        archiveInfo.setWsdlInfo(wsdlInfo);
        Mockito.doReturn(archiveInfo).doThrow(new IllegalArgumentException()).when(apiPublisher)
                .extractAndValidateWSDLArchive(fis);
        Response response = apisApiService.
                apisValidateDefinitionPost(WSDL, fis, fileInfo, null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity() instanceof APIDefinitionValidationResponseDTO);
        assertTrue(((APIDefinitionValidationResponseDTO) response.getEntity()).getIsValid());
    }

    @Test
    public void testApisValidateDefinitionPostWSDLFileInvalid() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_FILE_INVALID_LOCATION).getFile());
        FileInputStream fis = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(WSDL_FILE_INVALID);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        powerMockDefaultAPIPublisher();
        Response response = apisApiService.
                apisValidateDefinitionPost(WSDL, fis, fileInfo, null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testApisValidateDefinitionPostWSDLFileInvalidExtension() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource("swagger.json").getFile());
        FileInputStream fis = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName("swagger.json");
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        powerMockDefaultAPIPublisher();
        Response response = apisApiService.
                apisValidateDefinitionPost(WSDL, fis, fileInfo, null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testApisValidateDefinitionPostException() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_ZIP_LOCATION).getFile());
        FileInputStream fis = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(WSDL_ZIP);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = powerMockDefaultAPIPublisher();
        Mockito.doThrow(new APIMgtWSDLException("Error while validation", ExceptionCodes.INTERNAL_WSDL_EXCEPTION))
                .when(apiPublisher).extractAndValidateWSDLArchive(fis);
        Response response = apisApiService.
                apisValidateDefinitionPost(WSDL, fis, fileInfo, null, getRequest());
        fis.close();
        assertEquals(response.getStatus(), 500);
    }

    @Test
    public void testApisApiIdWsdlGetFile() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_FILE_LOCATION).getFile());
        String wsdlContent = IOUtils.toString(new FileInputStream(file));
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = powerMockDefaultAPIPublisher();
        API api = SampleTestObjectCreator.createDefaultAPI().build();
        Mockito.doReturn(true).when(apiPublisher).isWSDLExists(api.getId());
        Mockito.doReturn(false).when(apiPublisher).isWSDLArchiveExists(api.getId());
        Mockito.doReturn(wsdlContent).when(apiPublisher).getAPIWSDL(api.getId());
        Response response = apisApiService.apisApiIdWsdlGet(api.getId(), null, null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("StockQuote"));
    }

    @Test
    public void testApisApiIdWsdlGetArchive() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_ZIP_LOCATION).getFile());
        FileInputStream fileInputStream = new FileInputStream(file);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = powerMockDefaultAPIPublisher();
        API api = SampleTestObjectCreator.createDefaultAPI().build();
        Mockito.doReturn(true).when(apiPublisher).isWSDLExists(api.getId());
        Mockito.doReturn(true).when(apiPublisher).isWSDLArchiveExists(api.getId());
        Mockito.doReturn(api).when(apiPublisher).getAPIbyUUID(api.getId());
        Mockito.doReturn(fileInputStream).when(apiPublisher).getAPIWSDLArchive(api.getId());
        Response response = apisApiService.apisApiIdWsdlGet(api.getId(), null, null, getRequest());
        fileInputStream.close();
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity() instanceof InputStream);
    }

    @Test
    public void testApisApiIdWsdlGetNone() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = powerMockDefaultAPIPublisher();
        API api = SampleTestObjectCreator.createDefaultAPI().build();
        Mockito.doReturn(false).when(apiPublisher).isWSDLExists(api.getId());
        Response response = apisApiService.apisApiIdWsdlGet(api.getId(), null, null, getRequest());
        assertEquals(response.getStatus(), 204);
    }

    @Test
    public void testApisApiIdWsdlGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = powerMockDefaultAPIPublisher();
        API api = SampleTestObjectCreator.createDefaultAPI().build();
        Mockito.doReturn(true).when(apiPublisher).isWSDLExists(api.getId());
        Mockito.doReturn(false).when(apiPublisher).isWSDLArchiveExists(api.getId());
        Mockito.doThrow(
                new APIMgtDAOException("Error while retreiving WSDL", ExceptionCodes.INTERNAL_WSDL_EXCEPTION))
                .when(apiPublisher).getAPIWSDL(api.getId());
        Response response = apisApiService.apisApiIdWsdlGet(api.getId(), null, null, getRequest());
        assertEquals(response.getStatus(), 500);
    }

    @Test
    public void testApisApiIdWsdlPutFile() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_FILE_LOCATION).getFile());
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(WSDL_FILE);
        InputStream inputStream = new FileInputStream(file);
        String fileContent = IOUtils.toString(inputStream);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = powerMockDefaultAPIPublisher();
        API api = SampleTestObjectCreator.createDefaultAPI().build();
        Mockito.doReturn(fileContent).when(apiPublisher).updateAPIWSDL(api.getId(), inputStream);
        Response response = apisApiService
                .apisApiIdWsdlPut(api.getId(), inputStream, fileInfo, null, null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("StockQuote"));
    }

    @Test
    public void testApisApiIdWsdlPutArchive() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_ZIP_LOCATION).getFile());
        FileInputStream fileInputStream = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(WSDL_ZIP);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = powerMockDefaultAPIPublisher();
        API api = SampleTestObjectCreator.createDefaultAPI().build();
        Mockito.doReturn(api).when(apiPublisher).getAPIbyUUID(api.getId());
        Mockito.doReturn(fileInputStream).when(apiPublisher).getAPIWSDLArchive(api.getId());
        Response response = apisApiService
                .apisApiIdWsdlPut(api.getId(), fileInputStream, fileInfo, null, null, getRequest());
        fileInputStream.close();
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testApisApiIdWsdlPutUnsupported() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource("swagger.json").getFile());
        FileInputStream fileInputStream = new FileInputStream(file);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName("swagger.json");
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        powerMockDefaultAPIPublisher();
        API api = SampleTestObjectCreator.createDefaultAPI().build();
        Response response = apisApiService
                .apisApiIdWsdlPut(api.getId(), fileInputStream, fileInfo, null, null, getRequest());
        fileInputStream.close();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testApisApiIdWsdlPutException() throws Exception {
        printTestMethodName();
        File file = new File(getClass().getClassLoader().getResource(WSDL_FILE_LOCATION).getFile());
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(WSDL_FILE);
        InputStream inputStream = new FileInputStream(file);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = powerMockDefaultAPIPublisher();
        API api = SampleTestObjectCreator.createDefaultAPI().build();
        Mockito.doThrow(new APIMgtWSDLException("Error while updating WSDL", ExceptionCodes.INTERNAL_WSDL_EXCEPTION))
                .when(apiPublisher).updateAPIWSDL(api.getId(), inputStream);
        Response response = apisApiService
                .apisApiIdWsdlPut(api.getId(), inputStream, fileInfo, null, null, getRequest());
        assertEquals(response.getStatus(), 500);
    }

    @Test
    public void testApisPost() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        API.APIBuilder apiBuilder = SampleTestObjectCreator.createDefaultAPI();
        API api = apiBuilder.id(apiId).build();
        APIDTO apidto = MappingUtil.toAPIDto(api);
        Mockito.doReturn(apiId).doThrow(new IllegalArgumentException())
                .when(apiPublisher).addAPI(apiBuilder);
        Mockito.doReturn(api).doThrow(new IllegalArgumentException())
                .when(apiPublisher).getAPIbyUUID(apiId);
        Response response = apisApiService.apisPost(apidto, getRequest());
        assertEquals(response.getStatus(), 201);
        assertTrue(response.getEntity().toString().contains(api.getId()));
        assertTrue(response.getEntity().toString().contains(api.getName()));
    }

    @Test
    public void testApisPostException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        API.APIBuilder apiBuilder = SampleTestObjectCreator.createDefaultAPI();
        API api = apiBuilder.id(apiId).build();
        APIDTO apidto = MappingUtil.toAPIDto(api);
        Mockito.doReturn(apiId).doThrow(new IllegalArgumentException())
                .when(apiPublisher).addAPI(apiBuilder);
        Mockito.doThrow(new APIManagementException("Error occurred", ExceptionCodes.API_TYPE_INVALID))
                .when(apiPublisher).getAPIbyUUID(apiId);
        Response response = apisApiService.apisPost(apidto, getRequest());
        assertEquals(response.getStatus(), 400);
        assertTrue(response.getEntity().toString().contains("API Type specified is invalid"));
    }

    @Test
    public void testApisApiIdScopesGet() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Map<String, String> scopes = new HashMap<>();
        scopes.put("apim:api_read", "read apis");
        Mockito.when(apiPublisher.getScopesForApi(apiId)).thenReturn(scopes);
        Response response = apisApiService.apisApiIdScopesGet(apiId, null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("apim:api_read"));
    }

    @Test
    public void testApisApiIdScopesGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Set<String> scopes = new HashSet<>();
        scopes.add("apim:api_read");
        Mockito.when(apiPublisher.getScopesForApi(apiId)).thenThrow(new APIMgtDAOException("Swagger Definition " +
                "of API: " + apiId + ", does not exist", ExceptionCodes.SWAGGER_NOT_FOUND));
        Response response = apisApiService.apisApiIdScopesGet(apiId, null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("Swagger definition not found"));
    }

    @Test
    public void testApisApiIdScopesNameDelete() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doNothing().when(apiPublisher).deleteScopeFromApi(apiId, "apim:api_view");
        Response response = apisApiService.apisApiIdScopesNameDelete(apiId, "apim:api_view", null, null, getRequest());
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testApisApiIdScopesNameDeleteException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doThrow(new APIManagementException("Scope couldn't found by name: apim:api_view", ExceptionCodes
                .SCOPE_NOT_FOUND)).when(apiPublisher).deleteScopeFromApi(apiId, "apim:api_view");
        Response response = apisApiService.apisApiIdScopesNameDelete(apiId, "apim:api_view", null, null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("Scope not found"));
    }

    @Test
    public void testApisApiIdScopesNameGet() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.when(apiPublisher.getScopeInformationOfApi(apiId, "apim:api_view")).thenReturn(new Scope
                ("apim:api_view", "api create"));
        Response response = apisApiService.apisApiIdScopesNameGet(apiId, "apim:api_view", null, null, getRequest());
        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("apim:api_view"));
        assertTrue(response.getEntity().toString().contains("api create"));
    }

    @Test
    public void testApisApiIdScopesNameGetException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.when(apiPublisher.getScopeInformationOfApi(apiId, "apim:api_view")).thenThrow(new
                APIManagementException("Scope couldn't found by name: apim:api_view", ExceptionCodes
                .SCOPE_NOT_FOUND));
        Response response = apisApiService.apisApiIdScopesNameGet(apiId, "apim:api_view", null, null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("Scope not found"));
    }

    @Test
    public void testApisApiIdScopesNamePut() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Scope scope = new Scope("apim:api_view", "api view");
        Mockito.doNothing().when(apiPublisher).updateScopeOfTheApi(apiId, scope);
        Response response = apisApiService.apisApiIdScopesNamePut(apiId, scope.getName(), MappingUtil.scopeDto(scope,
                "roles"), null, null, getRequest());
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testApisApiIdScopesNamePutException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Scope scope = new Scope("apim:api_view", "api view");
        Mockito.doThrow(new APIManagementException("Scope couldn't found by name: apim:api_view", ExceptionCodes
                .SCOPE_NOT_FOUND)).when(apiPublisher).updateScopeOfTheApi(apiId, scope);
        Response response = apisApiService.apisApiIdScopesNamePut(apiId, "apim:api_view", MappingUtil.scopeDto(scope,
                "role"), null, null, getRequest());
        assertEquals(response.getStatus(), 404);
        assertTrue(response.getEntity().toString().contains("Scope not found"));
    }

    @Test
    public void testApisApiIdScopesPost() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Scope scope = new Scope("api_view", "api view");
        Mockito.doNothing().when(apiPublisher).addScopeToTheApi(apiId, scope);
        Response response = apisApiService.apisApiIdScopesPost(apiId, MappingUtil.scopeDto(scope, "role"), null,
                null, getRequest());
        assertEquals(response.getStatus(), 201);
    }

    @Test
    public void testApisApiIdScopesPostWithInvalidscopebindingType() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Scope scope = new Scope("api_view", "api view");
        Mockito.doNothing().when(apiPublisher).addScopeToTheApi(apiId, scope);
        Response response = apisApiService.apisApiIdScopesPost(apiId, MappingUtil.scopeDto(scope, "permission"), null,
                null, getRequest());
        assertEquals(response.getStatus(), 412);
    }

    @Test
    public void testApisApiIdScopesNamePOSTException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Scope scope = new Scope("api_view", "api view");
        Mockito.doThrow(new APIManagementException("Scope already registered", ExceptionCodes
                .SCOPE_ALREADY_REGISTERED)).when(apiPublisher).addScopeToTheApi(apiId, scope);
        Response response = apisApiService.apisApiIdScopesPost(apiId, MappingUtil.scopeDto(scope, "role"), null,
                null, getRequest());
        assertEquals(response.getStatus(), 409);
        assertTrue(response.getEntity().toString().contains("Scope already exist"));
    }

    @Test
    public void testApisApiIdDedicatedGatewayGetForInvalidAPI() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(false).when(apiPublisher).isAPIExists(apiId);
        Response response = apisApiService.apisApiIdDedicatedGatewayGet(apiId, null, null, getRequest());
        assertEquals(ExceptionCodes.API_NOT_FOUND.getHttpStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains(ExceptionCodes.API_NOT_FOUND.getErrorMessage()));
    }

    @Test
    public void testApisApiIdDedicatedGatewayGet() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(true).when(apiPublisher).isAPIExists(apiId);
        DedicatedGateway dedicatedGateway = new DedicatedGateway();
        dedicatedGateway.setEnabled(true);
        Mockito.doReturn(dedicatedGateway).when(apiPublisher).getDedicatedGateway(apiId);
        Response response = apisApiService.apisApiIdDedicatedGatewayGet(apiId, null, null, getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testApisApiIdDedicatedGatewayGetForNull() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(true).when(apiPublisher).isAPIExists(apiId);
        Mockito.doReturn(null).when(apiPublisher).getDedicatedGateway(apiId);
        Response response = apisApiService.apisApiIdDedicatedGatewayGet(apiId, null, null, getRequest());
        assertEquals(404, response.getStatus());
        assertTrue(response.getEntity().toString().contains(ExceptionCodes.DEDICATED_GATEWAY_DETAILS_NOT_FOUND
                .getErrorMessage()));
    }

    @Test
    public void testApisApiIdDedicatedGatewayGetForException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(true).when(apiPublisher).isAPIExists(apiId);
        Mockito.doThrow(new APIManagementException("Dedicated gateway details not found for the API",
                ExceptionCodes.DEDICATED_GATEWAY_DETAILS_NOT_FOUND)).when(apiPublisher).getDedicatedGateway(apiId);
        Response response = apisApiService.apisApiIdDedicatedGatewayGet(apiId, null, null, getRequest());
        assertEquals(ExceptionCodes.DEDICATED_GATEWAY_DETAILS_NOT_FOUND.getHttpStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains(ExceptionCodes.DEDICATED_GATEWAY_DETAILS_NOT_FOUND
                .getErrorMessage()));
    }

    @Test
    public void testApisApiIdDedicatedGatewayPutForInvalidAPI() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(false).when(apiPublisher).isAPIExists(apiId);
        Response response = apisApiService.apisApiIdDedicatedGatewayPut(apiId, new DedicatedGatewayDTO(), null, null,
                getRequest());
        assertEquals(ExceptionCodes.API_NOT_FOUND.getHttpStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains(ExceptionCodes.API_NOT_FOUND.getErrorMessage()));
    }

    @Test
    public void testApisApiIdDedicatedGatewayPut() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(true).when(apiPublisher).isAPIExists(apiId);
        DedicatedGateway dedicatedGateway = new DedicatedGateway();
        dedicatedGateway.setEnabled(true);
        Mockito.doNothing().when(apiPublisher).updateDedicatedGateway(Mockito.any());
        Mockito.doReturn(dedicatedGateway).when(apiPublisher).getDedicatedGateway(apiId);
        DedicatedGatewayDTO dedicatedGatewayDTO = new DedicatedGatewayDTO();
        dedicatedGatewayDTO.setIsEnabled(true);
        Response response = apisApiService.apisApiIdDedicatedGatewayPut(apiId, dedicatedGatewayDTO, null, null,
                getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testApisApiIdDedicatedGatewayPutForException() throws Exception {
        printTestMethodName();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).thenReturn(apiPublisher);
        String apiId = UUID.randomUUID().toString();
        Mockito.doReturn(true).when(apiPublisher).isAPIExists(apiId);
        Mockito.doThrow(new APIManagementException("Error while creating dedicated container based gateway",
                ExceptionCodes.DEDICATED_CONTAINER_GATEWAY_CREATION_FAILED)).when(apiPublisher)
                .updateDedicatedGateway(Mockito.any());
        Response response = apisApiService.apisApiIdDedicatedGatewayPut(apiId, new DedicatedGatewayDTO(), null, null,
                getRequest());
        assertEquals(ExceptionCodes.DEDICATED_CONTAINER_GATEWAY_CREATION_FAILED.getHttpStatusCode(),
                response.getStatus());
        assertTrue(response.getEntity().toString().contains(ExceptionCodes
                .DEDICATED_CONTAINER_GATEWAY_CREATION_FAILED.getErrorMessage()));
    }

    @Test
    public void testApisApiIdCommentsCommentIdDelete() throws Exception {
        printTestMethodName();
        String apiId = UUID.randomUUID().toString();
        String commentId = UUID.randomUUID().toString();

        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);

        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Request request = getRequest();
        PowerMockito.when(RestApiUtil.getLoggedInUsername(request)).thenReturn(USER);

        Mockito.doNothing().doThrow(new IllegalArgumentException()).when(apiPublisher)
                .deleteComment(commentId, apiId, USER);

        javax.ws.rs.core.Response response1 =
                apisApiService.apisApiIdCommentsCommentIdDelete
                        (null, apiId, null, null, request);

        Assert.assertEquals(200, response1.getStatus());

        // Error Case when commentId is not found
        Mockito.doThrow(new APICommentException("Error occurred", ExceptionCodes.COMMENT_NOT_FOUND))
                .when(apiPublisher).deleteComment(commentId, apiId, USER);

        Response response2 = apisApiService.apisApiIdCommentsCommentIdDelete
                (commentId, apiId, IF_MATCH, IF_UNMODIFIED_SINCE, request);

        assertEquals(response2.getStatus(), 404);
    }

    @Test
    public void testApisApiIdCommentsCommentIdDeleteIfMatchStringExistingFingerprintCheck()
            throws APIManagementException, NotFoundException {
        printTestMethodName();
        String apiId = UUID.randomUUID().toString();
        String commentId = UUID.randomUUID().toString();

        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);

        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Request request = getRequest();

        PowerMockito.when(RestApiUtil.getLoggedInUsername(request)).thenReturn(USER);

        String existingFingerprint = "existingFingerprint";

        Mockito.when(apisApiService.apisApiIdCommentsCommentIdDeleteFingerprint
                (commentId, apiId, "test", "test", request))
                .thenReturn(existingFingerprint);
        Mockito.doNothing().doThrow(new IllegalArgumentException()).when(apiPublisher)
                .deleteComment(commentId, apiId, USER);

        Response response = apisApiService.apisApiIdCommentsCommentIdDelete
                (commentId, apiId, "test", "test", request);

        assertEquals(response.getStatus(), 412);
    }


    @Test
    public void testApisApiIdCommentsPost() throws APIManagementException, NotFoundException {
        printTestMethodName();
        String apiId = UUID.randomUUID().toString();
        String commentId = UUID.randomUUID().toString();
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);

        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);

        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setApiId(apiId);
        commentDTO.setCommentId(commentId);
        commentDTO.setCommentText("comment text");
        commentDTO.setCategory("testCategory");
        commentDTO.setParentCommentId("");
        commentDTO.setEntryPoint("APIPublisher");
        commentDTO.setCreatedBy("creater");
        commentDTO.setLastUpdatedBy("updater");

        Instant time = APIUtils.getCurrentUTCTime();
        Comment comment = new Comment();
        comment.setUuid(commentId);
        comment.setApiId(apiId);
        comment.setCommentedUser("commentedUser");
        comment.setCommentText("this is a comment");
        comment.setCategory("testCategory");
        comment.setParentCommentId("");
        comment.setEntryPoint("APIPublisher");
        comment.setCreatedUser("createdUser");
        comment.setUpdatedUser("updatedUser");
        comment.setCreatedTime(time);
        comment.setUpdatedTime(time);

        Mockito.when(apiPublisher.addComment(comment,apiId)).thenReturn(commentId);
        Mockito.when(apiPublisher.getCommentByUUID(commentId, apiId)).thenReturn(comment);

        PowerMockito.mockStatic(CommentMappingUtil.class);
        PowerMockito.when(CommentMappingUtil.fromDTOToComment(commentDTO,USER)).thenReturn(comment);
        PowerMockito.when(CommentMappingUtil.fromCommentToDTO(comment)).thenReturn(commentDTO);

        Response response1 = apisApiService.apisApiIdCommentsPost
                (apiId,commentDTO,getRequest());

        Assert.assertEquals(201, response1.getStatus());

        // Error Case
        Mockito.doThrow(new APICommentException("Error occurred", ExceptionCodes.INTERNAL_ERROR))
                .when(apiPublisher).addComment(comment,apiId);
        Mockito.doThrow(new APICommentException("Error occurred", ExceptionCodes.INTERNAL_ERROR))
                .when(apiPublisher).getCommentByUUID(commentId, apiId);

        Response response2 = apisApiService.apisApiIdCommentsPost
                (apiId,commentDTO,getRequest());

        Assert.assertEquals(ExceptionCodes.INTERNAL_ERROR.getHttpStatusCode(), response2.getStatus());
    }


    @Test
    public void testApisApiIdCommentsCommentIdGet() throws APIManagementException, NotFoundException {
        printTestMethodName();
        String apiId = UUID.randomUUID().toString();
        String commentId = UUID.randomUUID().toString();
        APIManagerFactory instance = Mockito.mock(APIManagerFactory.class);
        PowerMockito.mockStatic(APIManagerFactory.class);
        PowerMockito.when(APIManagerFactory.getInstance()).thenReturn(instance);
        PowerMockito.when(APIManagerFactory.getInstance().getUserNameMapper()).thenReturn(userNameMapper);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);

        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Request request = getRequest();
        PowerMockito.when(RestApiUtil.getLoggedInUsername(request)).thenReturn(USER);

        Instant time = APIUtils.getCurrentUTCTime();
        Comment comment = new Comment();
        comment.setUuid(commentId);
        comment.setApiId(apiId);
        comment.setCommentedUser("commentedUser");
        comment.setCommentText("this is a comment");
        comment.setCategory("testCategory");
        comment.setParentCommentId("");
        comment.setEntryPoint("APIPublisher");
        comment.setCreatedUser("createdUser");
        comment.setUpdatedUser("updatedUser");
        comment.setCreatedTime(time);
        comment.setUpdatedTime(time);

        Mockito.when(apiPublisher.getCommentByUUID(commentId, apiId)).thenReturn(comment);

        Response response1 = apisApiService.apisApiIdCommentsCommentIdGet
                (commentId, apiId, null, null, request);

        Assert.assertEquals(200, response1.getStatus());

        // Error Case when commentId is not found
        Mockito.doThrow(new APICommentException("Error occurred", ExceptionCodes.COMMENT_NOT_FOUND))
                .when(apiPublisher).getCommentByUUID(commentId, apiId);

        Response response2 = apisApiService.apisApiIdCommentsCommentIdGet
                (commentId, apiId, null, null, request);

        Assert.assertEquals(404, response2.getStatus());
    }

    @Test
    public void testApisApiIdCommentsGet() throws Exception {
        printTestMethodName();
        String apiId = UUID.randomUUID().toString();

        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);

        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Request request = getRequest();
        PowerMockito.when(RestApiUtil.getLoggedInUsername(request)).thenReturn(USER);

        Instant time = APIUtils.getCurrentUTCTime();
        Comment comment1 = new Comment();
        comment1.setUuid(UUID.randomUUID().toString());
        comment1.setCommentedUser("commentedUser1");
        comment1.setCommentText("this is a comment 1");
        comment1.setCategory("testCategory1");
        comment1.setParentCommentId("");
        comment1.setEntryPoint("APIPublisher");
        comment1.setCreatedUser("createdUser1");
        comment1.setUpdatedUser("updatedUser1");
        comment1.setCreatedTime(time);
        comment1.setUpdatedTime(time);

        time = APIUtils.getCurrentUTCTime();
        Comment comment2 = new Comment();
        comment2.setUuid(UUID.randomUUID().toString());
        comment2.setCommentedUser("commentedUser2");
        comment2.setCommentText("this is a comment 2");
        comment2.setCategory("testCategory2");
        comment2.setParentCommentId("");
        comment2.setEntryPoint("APIPublisher");
        comment2.setCreatedUser("createdUser2");
        comment2.setUpdatedUser("updatedUser2");
        comment2.setCreatedTime(time);
        comment2.setUpdatedTime(time);

        List<Comment> commentList = new ArrayList<>();
        commentList.add(comment1);
        commentList.add(comment2);

        Mockito.when(apiPublisher.getCommentsForApi(apiId)).thenReturn(commentList);
        Response response1 = apisApiService.apisApiIdCommentsGet(apiId, 3, 0, request);

        Assert.assertEquals(200, response1.getStatus());

        // Error Case when commentId is not found
        Mockito.doThrow(new APICommentException("Error occurred", ExceptionCodes.COMMENT_NOT_FOUND))
                .when(apiPublisher).getCommentsForApi(apiId);

        Response response2 = apisApiService.apisApiIdCommentsGet(apiId, 3, 0, request);

        Assert.assertEquals(404, response2.getStatus());
    }

    @Test
    public void testApisApiIdCommentsCommentIdPut() throws Exception {
        printTestMethodName();
        String apiId = UUID.randomUUID().toString();
        String commentId = UUID.randomUUID().toString();
        APIManagerFactory instance = Mockito.mock(APIManagerFactory.class);
        PowerMockito.mockStatic(APIManagerFactory.class);
        PowerMockito.when(APIManagerFactory.getInstance()).thenReturn(instance);
        PowerMockito.when(APIManagerFactory.getInstance().getUserNameMapper()).thenReturn(userNameMapper);
        ApisApiServiceImpl apisApiService = new ApisApiServiceImpl();
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);

        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        Request request = getRequest();
        PowerMockito.when(RestApiUtil.getLoggedInUsername(request)).thenReturn(USER);

        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setApiId(apiId);
        commentDTO.setCommentText("comment text");
        commentDTO.setCategory("testCategory");
        commentDTO.setParentCommentId("");
        commentDTO.setEntryPoint("APIPublisher");
        commentDTO.setCreatedBy("creater");
        commentDTO.setLastUpdatedBy("updater");

        Instant time = APIUtils.getCurrentUTCTime();
        Comment comment = new Comment();
        comment.setCommentedUser("commentedUser");
        comment.setCommentText("this is a comment");
        comment.setCategory("testCategory");
        comment.setParentCommentId("");
        comment.setEntryPoint("APIPublisher");
        comment.setCreatedUser("createdUser");
        comment.setUpdatedUser("updatedUser");
        comment.setCreatedTime(time);
        comment.setUpdatedTime(time);

        Mockito.doNothing().doThrow(new IllegalArgumentException()).when(apiPublisher)
                .updateComment(comment, commentId, apiId, USER);
        Mockito.when(apiPublisher.getCommentByUUID(commentId, apiId)).thenReturn(comment);

        Response response1 = apisApiService.apisApiIdCommentsCommentIdPut
                (commentId, apiId, commentDTO, null, null, request);

        Assert.assertEquals(200, response1.getStatus());

        // Error Case
        Mockito.doThrow(new APICommentException("Error occurred", ExceptionCodes.INTERNAL_ERROR))
                .when(apiPublisher).updateComment(comment, commentId, apiId, USER);
        Mockito.doThrow(new APICommentException("Error occurred", ExceptionCodes.INTERNAL_ERROR))
                .when(apiPublisher).getCommentByUUID(commentId, apiId);


        Response response2 = apisApiService.apisApiIdCommentsCommentIdPut
                (commentId, apiId, commentDTO, IF_MATCH, IF_UNMODIFIED_SINCE, request);

        Assert.assertEquals(ExceptionCodes.INTERNAL_ERROR.getHttpStatusCode(), response2.getStatus());
    }

    private Matcher<API.APIBuilder> getAPIBuilderMatcher(API.APIBuilder apiBuilder) {
        return new BaseMatcher<API.APIBuilder>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof API.APIBuilder && ((API.APIBuilder) o).getId() != null && ((API.APIBuilder) o)
                        .getId().equals(apiBuilder.getId());
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }

    private APIPublisher powerMockDefaultAPIPublisher() throws APIManagementException {
        APIPublisher apiPublisher = Mockito.mock(APIPublisherImpl.class);
        PowerMockito.mockStatic(RestAPIPublisherUtil.class);
        PowerMockito.when(RestAPIPublisherUtil.getApiPublisher(USER)).
                thenReturn(apiPublisher);
        return apiPublisher;
    }

    // Sample request to be used by tests
    private Request getRequest() throws APIMgtSecurityException {
        HTTPCarbonMessage carbonMessage = Mockito.mock(HTTPCarbonMessage.class);
        Mockito.when(carbonMessage.getProperty("LOGGED_IN_USER")).thenReturn(USER);
        Request request = new Request(carbonMessage);
        return request;
    }

    private static void printTestMethodName() {
        log.info("------------------ Test method: " + Thread.currentThread().getStackTrace()[2].getMethodName() +
                " ------------------");
    }
}
