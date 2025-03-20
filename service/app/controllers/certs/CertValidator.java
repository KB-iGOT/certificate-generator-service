package controllers.certs;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.UrlValidator;
import org.sunbird.incredible.processor.JsonKey;
import org.sunbird.BaseException;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This class contains method to validate certificate api request
 */
public class CertValidator {

    /**
     * This method will validate generate certificate request
     *
     * @param request
     * @throws BaseException
     */
    public void validateGenerateCertRequest(Request request) throws BaseException {

        Map<String, Object> certReq = request.getRequest();
        checkMandatoryParamsPresent(certReq, JsonKey.REQUEST, Arrays.asList(JsonKey.USER_ID, JsonKey.COURSE_ID, JsonKey.BATCH_ID));
    }

    private void checkMandatoryParamsPresent(
            Map<String, Object> data, String parentKey, List<String> keys) throws BaseException {
        if (MapUtils.isEmpty(data)) {
            throw new BaseException("MANDATORY_PARAMETER_MISSING",
                    MessageFormat.format(IResponseMessage.MANDATORY_PARAMETER_MISSING, parentKey),
                    ResponseCode.CLIENT_ERROR.getCode());
        }
        checkChildrenMapMandatoryParams(data, keys, parentKey);
    }

    private void checkChildrenMapMandatoryParams(Map<String, Object> data, List<String> keys, String parentKey) throws BaseException {

        for (String key : keys) {
            if (StringUtils.isBlank((String) data.get(key))) {
                throw new BaseException("MANDATORY_PARAMETER_MISSING",
                        MessageFormat.format(IResponseMessage.MANDATORY_PARAMETER_MISSING, parentKey + "." + key),
                        ResponseCode.CLIENT_ERROR.getCode());
            }
        }
    }
}

