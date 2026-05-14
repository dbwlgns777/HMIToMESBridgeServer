package com.zes.authentication.common;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class ZES_returnService
{
    public JSONObject ZES_returnToFormat(ZES_Enum code, String message, Object data)
    {
        JSONObject ZES_lv_result = new JSONObject();
        ZES_lv_result.put("code", code.name());
        ZES_lv_result.put("message", message);
        ZES_lv_result.put("data", data);
        return ZES_lv_result;
    }
}
