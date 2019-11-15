package de.uniulm.omi.cloudiator.lance.application.component;

import java.io.Serializable;

public class OutLambda implements Serializable {

    private final String name;

    public OutLambda(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
