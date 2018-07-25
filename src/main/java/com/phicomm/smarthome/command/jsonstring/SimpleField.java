package com.phicomm.smarthome.command.jsonstring;
/**
 * package: com.phicomm.smarthome.command.jsonstring
 * class: SimpleField.java
 * date: 2018年5月9日 下午7:36:07
 * author: wen.xia
 * description: json字符串的字段处理对象
 */
public class SimpleField extends BaseObject{
    //字段名称
    private String name;
    //字段值
    private Object val;
    //字段操作类型，增加或删除
    private FieldOpt opt;
    //是否处理成功
    private boolean success;

    public SimpleField(FieldOpt opt) {
        super();
        this.opt = opt;
    }
    
    public SimpleField(String name, FieldOpt opt) {
        super();
        this.name = name;
        this.opt = opt;
    }

    public SimpleField(String name, Object val, FieldOpt opt) {
        super();
        this.name = name;
        this.val = val;
        this.opt = opt;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Object getVal() {
        return val;
    }
    public void setVal(Object val) {
        this.val = val;
    }
    
    public FieldOpt getOpt() {
        return opt;
    }
    public void setOpt(FieldOpt opt) {
        this.opt = opt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }


    /**
     * 字段操作枚举类型，增加或删除
     */
    public enum FieldOpt{
        DEL, ADD;
    }
    
}