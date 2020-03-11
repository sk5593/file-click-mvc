package com.file.click.mvc.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public class R implements Serializable {
    public Integer code;
    public String msg;
    public Object object;

    public R(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public R(Integer code, String msg, Object object) {
        this.code = code;
        this.msg = msg;
        this.object = object;
    }
    public static R success(){
        return new R(200,"success");
    }
    public static R fail(){
        return new R(500,"fail");
    }
}
