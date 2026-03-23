package com.example.demo.DTO;


import lombok.Data;

@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static Result<Void> ok(String message) {
        Result<Void> r = new Result<>();
        r.code = 0;
        r.message = message;
        return r;
    }

    public static Result<Void> fail(int code, String message) {
        Result<Void> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}