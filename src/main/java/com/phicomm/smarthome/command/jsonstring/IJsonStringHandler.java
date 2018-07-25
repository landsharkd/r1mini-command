package com.phicomm.smarthome.command.jsonstring;

import java.util.List;

/**
 *
 * package: com.phicomm.smarthome.command.jsonstring
 * class: IJsonStringHandler.java
 * date: 2018年5月9日 下午4:07:58
 * author: wen.xia
 * description: json字符串处理器接口
 */
public interface IJsonStringHandler {
    
    /**
     * 
     * @param str 输入字符串
     * @param field 需要处理的字段对象
     * @param valIfEmpty 经过处理后，如果json对象为空则返回预定义的值valIfEmpty
     * @return 返回经过处理的字符串,若未经过处理则返回源字符串
     */
    public String handle(String str, SimpleField field, String valIfEmpty);
    /**
     * 
     * @param str 输入字符串
     * @param fields 需要处理的字段对象列表
     * @param valIfEmpty 经过处理后，如果json对象为空则返回预定义的值valIfEmpty
     * @return 返回经过处理的字符串,若未经过处理则返回源字符串
     */
    public String handle(String str, List<SimpleField> fields, String valIfEmpty);
    /**
     * 
     * @param str 输入字符串
     * @param field 需要处理的字段对象
     * @return 返回经过处理的字符串,若未经过处理则返回源字符串,经过处理后，如果json对象为空则返回默认空值"{}"
     */
    public String handle(String str, SimpleField field);
    /**
     * 
     * @param str 输入字符串
     * @param fields 需要处理的字段对象列表
     * @return 返回经过处理的字符串,若未经过处理则返回源字符串,经过处理后，如果json对象为空则返回默认空值"{}"
     */
    public String handle(String str, List<SimpleField> fields);

}
