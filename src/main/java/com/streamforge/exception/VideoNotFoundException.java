package com.streamforge.exception;

public class VideoNotFoundException extends RuntimeException{
    public VideoNotFoundException(String message){
        super(message);
    }
}
