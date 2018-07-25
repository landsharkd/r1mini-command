package com.phicomm.smarthome.command.jsonstring;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.phicomm.smarthome.command.jsonstring.SimpleField.FieldOpt;

/**
 * package: com.phicomm.smarthome.command.jsonstring
 * class: JsonStringHandler.java
 * date: 2018年5月9日 下午4:21:48
 * author: wen.xia
 * description: json字符串处理实现类
 */

@Component
public class JsonStringHandler  implements IJsonStringHandler{

    //日志
    private static Logger logger = LogManager.getLogger(JsonStringHandler.class);

    public String handle(String str, SimpleField field, String valIfEmpty) {
        if(field == null) {
            logger.error("handle error : fields can't be empty, str = [{}], field=[{}], valIfEmpty=[{}]", str, field, valIfEmpty);
            return str;
        }
        return handle(str, Arrays.asList(field), valIfEmpty);
    }
    
    public String handle(String str, List<SimpleField> fields, String valIfEmpty) {
        if(CollectionUtils.isNotEmpty(fields)) {
            
            JSONObject jo = null;
            if(StringUtils.isBlank(str)) {
                jo = new JSONObject();
            }
            try {
                 jo = JSON.parseObject(str);
            }catch(Exception e) {
                logger.error("JSON parseObject error : str=" + str, e);
                return str;
            }
            
            if(jo == null) {
                jo = new JSONObject();
            }
            
            //是否被处理过
            boolean optioned = false;
            
            for(SimpleField field : fields) {
                if(field.isSuccess()) {
                    logger.warn("field has been processed, field = " + field);
                    continue;
                }
                if(field.getOpt() == FieldOpt.ADD) {
                    if(field != null && StringUtils.isNotBlank(field.getName())) {
                        jo.put(field.getName(), field.getVal());
                        field.setSuccess(true);
                        optioned = true;
                    }
                }else if(field.getOpt() == FieldOpt.DEL) {
                    if(field != null && StringUtils.isNotBlank(field.getName()) && jo.containsKey(field.getName())) {
                            field.setVal(jo.remove(field.getName()));
                            field.setSuccess(true);
                            optioned = true;
                    }
                }else {
                    logger.warn("unsupported opt : field [{}]", field.getOpt());
                }
            }
            
            //如果什么都没做，则返回原字符串
            if(!optioned) {
                return str;
            }
            
            //如果操作过后，json为空则返回指定空值
            if(jo.isEmpty()) {
                return valIfEmpty;
            }
            
            return JSON.toJSONString(jo, SerializerFeature.WriteMapNullValue);
        }
        logger.error("handle error : fields can't be empty, str = [{}], fields=[{}], valIfEmpty=[{}]", str, fields, valIfEmpty);
        return str;
    }
    
    public static void main(String[] args) {
        //System.out.println(new JSONObject().isEmpty());
        //System.out.println(new JSONObject().toJSONString());
        JSON.parseObject("");
        String str = "{\"val\":\"t1\", \"nap\": 1}";
        //str = "{}";
        SimpleField delsf = new SimpleField(FieldOpt.DEL);
        delsf.setName("name");

        JsonStringHandler rj = new JsonStringHandler();
        long start = System.currentTimeMillis();
        System.out.println(delsf);
        System.out.println("del : " + (str = rj.handle(str, delsf,"")));
        System.out.println("del : " +  delsf.isSuccess());

        SimpleField addsf = new SimpleField(delsf.getName(), delsf.getVal(), FieldOpt.ADD);
        System.out.println(addsf);
        System.out.println("add : " + (str = rj.handle(str, addsf, "")));
        System.out.println("addsf : " +  addsf.isSuccess());

        delsf.setSuccess(false);
        System.out.println(delsf);
        System.out.println("del : " + rj.handle(str, delsf, ""));
        System.out.println("del : " +  delsf.isSuccess());

        System.out.println("all time:" + (System.currentTimeMillis() - start) + " ms");
        
        JSONObject j = new JSONObject();
        j.put("A", null);
        System.out.println(JSONObject.toJSONString(j,SerializerFeature.WriteMapNullValue));
        System.out.println(j.isEmpty());
        System.out.println(j.toJSONString());
        System.out.println(j.containsKey("A"));
        JSONObject j2 = new JSONObject();
        System.out.println(j2.containsKey("A"));
    }

    @Override
    public String handle(String str, SimpleField field) {
        return handle(str, field, new JSONObject().toJSONString());
    }

    @Override
    public String handle(String str, List<SimpleField> fields) {
        return handle(str, fields, new JSONObject().toJSONString());
    }
    

}
