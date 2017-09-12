package com.mybatis.plugin.exception;

public class PagerException extends RuntimeException{
	  private static final long serialVersionUID = 1L;

	    public PagerException(String message) {
	        super(message);
	    }

	    public PagerException(Throwable throwable) {
	        super(throwable);
	    }

	    public PagerException(String message, Throwable throwable) {
	        super(message, throwable);
	    }
}
