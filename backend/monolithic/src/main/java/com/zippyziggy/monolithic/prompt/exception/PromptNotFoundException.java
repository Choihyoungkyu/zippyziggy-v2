package com.zippyziggy.monolithic.prompt.exception;

public class PromptNotFoundException extends RuntimeException{
	public PromptNotFoundException() {
		super("존재하지 않는 프롬프트입니다.");
	}
}
