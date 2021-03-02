package org.wso2.carbon.apimgt.gateway.handlers.ext.listener;

import org.wso2.carbon.apimgt.common.gateway.dto.ExtensionResponseDTO;
import org.wso2.carbon.apimgt.common.gateway.dto.RequestContextDTO;
import org.wso2.carbon.apimgt.common.gateway.dto.ResponseContextDTO;
import org.wso2.carbon.apimgt.common.gateway.extensionlistener.ExtensionListener;

/**
 * ExtensionListener wrapper for ExtensionListener Util test cases.
 */
public class TestExtensionListenerImpl implements ExtensionListener {

    /***
     * Pre process Request.
     * @param requestContextDTO RequestContextDTO
     */
    @Override
    public ExtensionResponseDTO preProcessRequest(RequestContextDTO requestContextDTO) {

        return null;
    }

    /***
     * Post process Request.
     * @param requestContextDTO RequestContextDTO
     */
    @Override
    public ExtensionResponseDTO postProcessRequest(RequestContextDTO requestContextDTO) {

        return null;
    }

    /***
     * Pre process Response.
     * @param responseContextDTO ResponseContextDTO
     */
    @Override
    public ExtensionResponseDTO preProcessResponse(ResponseContextDTO responseContextDTO) {

        return null;
    }

    /***
     * Post process Response.
     * @param responseContextDTO ResponseContextDTO
     */
    @Override
    public ExtensionResponseDTO postProcessResponse(ResponseContextDTO responseContextDTO) {

        return null;
    }

    /**
     * Returns the extension listener type. This should be a value from ExtensionType enum.
     *
     * @return ExtensionType enum value
     */
    @Override
    public String getType() {

        return null;
    }
}
