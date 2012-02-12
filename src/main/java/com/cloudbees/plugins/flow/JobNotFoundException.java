package com.cloudbees.plugins.flow;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(String mess) {
        super(mess);
    }

    public JobNotFoundException(Exception e) {
        super(e);
    }

    public JobNotFoundException(String mess, Exception e) {
        super(mess, e);
    }
}
