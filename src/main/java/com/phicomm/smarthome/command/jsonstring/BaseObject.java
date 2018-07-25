package com.phicomm.smarthome.command.jsonstring;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * package: com.phicomm.smarthome.command.jsonstring
 * class: BaseObject.java
 * date: 2018年5月10日 上午11:47:03
 * author: wen.xia
 * description: 基础对象
 */
public abstract class BaseObject {
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
