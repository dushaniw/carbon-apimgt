package org.wso2.carbon.apimgt.gateway.handlers.ext.listener;

import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.RESTConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.common.gateway.dto.ExtensionType;
import org.wso2.carbon.apimgt.common.gateway.extensionlistener.ExtensionListener;
import org.wso2.carbon.apimgt.gateway.APIMgtGatewayConstants;
import org.wso2.carbon.apimgt.gateway.handlers.security.APISecurityUtils;
import org.wso2.carbon.apimgt.gateway.handlers.security.AuthenticationContext;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for ExtensionListenerUtil.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceReferenceHolder.class)
public class ExtensionListenerUtilTest {

    private Map<String, ExtensionListener> extensionListenerMap = new HashMap<>();
    private String apiContext = "pizzashack";
    private String apiVersion = "v1";
    private String httpVerb = "PUT";
    private String resource = "/order/123456";
    private String resourceUriTemplate = "/order/{orderId}";
    private String apiUUID = "da856ad5-0441-499a-85c5-c6490ca4ceca";
    private String msgId = "dkfs23n2nrn21";
    private String username = "admin";
    private String consumerKey = "f32ndassfnQdW";
    private String requestFullPath = "/pizzashack/v1/order/123456";
    private javax.security.cert.X509Certificate[] clientCerts;
    private Map<String, Object> customProperty;
    private Map<String, String> transportHeaders = new HashMap<>();
    private MessageContext messageContext;
    private org.apache.axis2.context.MessageContext axis2MsgContext;
    private AuthenticationContext authenticationContext;

    @Before
    public void setup() {

        ServiceReferenceHolder serviceReferenceHolder =
                Mockito.mock(org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder.class);
        PowerMockito.mockStatic(org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder.class);
        Mockito.when(org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder
                .getInstance()).thenReturn(serviceReferenceHolder);
        APIManagerConfigurationService apiManagerConfigurationService = Mockito.mock(APIManagerConfigurationService
                .class);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn
                (apiManagerConfigurationService);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        extensionListenerMap.put(ExtensionType.AUTHENTICATION.toString(), new TestExtensionListenerImpl());
        extensionListenerMap.put(ExtensionType.THROTTLING.toString(), new TestExtensionListenerImpl());
        Mockito.when(apiManagerConfiguration.getExtensionListenerMap()).thenReturn(extensionListenerMap);
        messageContext = Mockito.mock(Axis2MessageContext.class);
        axis2MsgContext = Mockito.mock(org.apache.axis2.context.MessageContext.class);
        Mockito.when(((Axis2MessageContext) messageContext).getAxis2MessageContext()).thenReturn(axis2MsgContext);
        transportHeaders.put(APIConstants.USER_AGENT, "");
        transportHeaders.put(APIMgtGatewayConstants.AUTHORIZATION, "gsu64r874tcin7ry8oe");
    }

    @Test
    public void testPreProcessRequestReturnNullExtensionResponse() {

        Mockito.when(axis2MsgContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                .thenReturn(transportHeaders);
        Mockito.when(messageContext.getProperty(RESTConstants.REST_FULL_REQUEST_PATH)).thenReturn(requestFullPath);
        Mockito.when(messageContext.getProperty(APIMgtGatewayConstants.API_ELECTED_RESOURCE))
                .thenReturn(resourceUriTemplate);
        Mockito.when(messageContext.getProperty(APIMgtGatewayConstants.HTTP_METHOD)).thenReturn(httpVerb);
        Mockito.when(axis2MsgContext.getLogCorrelationID()).thenReturn(msgId);
        Mockito.when(messageContext.getProperty(RESTConstants.REST_API_CONTEXT)).thenReturn(apiContext);
        Mockito.when(messageContext.getProperty(RESTConstants.SYNAPSE_REST_API_VERSION)).thenReturn(apiVersion);
        Mockito.when(messageContext.getProperty(APIMgtGatewayConstants.API_UUID_PROPERTY)).thenReturn(apiUUID);
        AuthenticationContext authenticationContext = new AuthenticationContext();
        authenticationContext.setConsumerKey(consumerKey);
        authenticationContext.setUsername(username);
        Mockito.when(messageContext.getProperty(APISecurityUtils.API_AUTH_CONTEXT)).thenReturn(authenticationContext);
        Assert.assertTrue(ExtensionListenerUtil.preProcessRequest(messageContext, ExtensionType.THROTTLING.toString()));
    }

    @Test
    public void testPostProcessRequest() {

    }

    @Test
    public void testPreProcessResponse() {

    }

    @Test
    public void testPostProcessResponse() {

    }
}
