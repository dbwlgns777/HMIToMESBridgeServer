package com.zes.bridge.common;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class ZesReturnService {

    public JSONObject ZES_returnToFormat(ZesEnum code, String message, Object data) {
        JSONObject result = new JSONObject();
        result.put("code", code.name());
        result.put("message", message);
        result.put("data", data);
        return result;
    }
}
