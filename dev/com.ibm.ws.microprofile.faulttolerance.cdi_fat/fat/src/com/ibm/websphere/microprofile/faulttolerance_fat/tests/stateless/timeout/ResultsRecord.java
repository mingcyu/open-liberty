package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.timeout;

public class ResultsRecord {

    public boolean testMethodCalled = false;
    public boolean testMethodRecievedInteruptException = false;
    public boolean testMethodContinuedPastInterruptException = false;
}