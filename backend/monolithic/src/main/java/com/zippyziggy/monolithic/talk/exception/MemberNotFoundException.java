package com.zippyziggy.monolithic.talk.exception;

public class MemberNotFoundException extends RuntimeException{

    private String message;

    public MemberNotFoundException() {
        this.message = "존재하지 않는 회원입니다.";
    }
}
