package org.radargun.cachewrappers;

public class Jvstm2Wrapper extends Jvstm1Wrapper {

    @Override
    public String getInfo() {
        return "JVSTM lock-free commit";
    }

}