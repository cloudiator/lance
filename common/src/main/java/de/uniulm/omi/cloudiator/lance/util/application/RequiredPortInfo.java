package de.uniulm.omi.cloudiator.lance.util.application;

import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;

import java.io.Serializable;

public class RequiredPortInfo implements Serializable {

    private static final long serialVersionUID = -4836138962934247464L;
    private final String requiredPortName;
    private final transient PortUpdateHandler puHandler;
    private final int requiredRefNumber;
    private final int cardinality;
    private final int min;

    public RequiredPortInfo(String requiredPortName, PortUpdateHandler puHandler, int requiredPortNumber, int cardinality, int min) {
        this.requiredPortName = requiredPortName;
        this.puHandler = puHandler;
        this.cardinality = cardinality;
        this.min = min;
        this.requiredRefNumber= requiredPortNumber;
    }

    public String getRequiredPortName() {
        return requiredPortName;
    }

    public PortUpdateHandler getPuHandler() {
        return puHandler;
    }

    public int getRequiredPortNumber() {
        return requiredRefNumber;
    }

    public int getCardinality() {
        return cardinality;
    }

    public int getMin() {
        return min;
    }

    public RequiredPortInfo(String requiredPortName, PortUpdateHandler puHandler, int requiredPortNumber) {
        this(requiredPortName, puHandler, requiredPortNumber, 1, OutPort.NO_SINKS);
    }
}
