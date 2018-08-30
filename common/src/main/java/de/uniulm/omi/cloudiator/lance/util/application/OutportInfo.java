package de.uniulm.omi.cloudiator.lance.util.application;

import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;

import java.io.Serializable;

public class OutportInfo implements Serializable {

    private static final long serialVersionUID = -4836138962934247464L;
    private final String outportName;
    private final transient PortUpdateHandler puHandler;
    private final int providedPortNumber;
    private final int cardinality;
    private final int min;

    public OutportInfo(String outportName, PortUpdateHandler puHandler, int providedPortNumber, int cardinality, int min) {
        this.outportName = outportName;
        this.puHandler = puHandler;
        this.cardinality = cardinality;
        this.min = min;
        this.providedPortNumber = providedPortNumber;
    }

    public String getOutportName() {
        return outportName;
    }

    public PortUpdateHandler getPuHandler() {
        return puHandler;
    }

    public int getProvidedPortNumber() {
        return providedPortNumber;
    }

    public int getCardinality() {
        return cardinality;
    }

    public int getMin() {
        return min;
    }

    public OutportInfo(String outportName, PortUpdateHandler puHandler, int providedPortNumber) {
        this(outportName, puHandler, providedPortNumber, 1, OutPort.NO_SINKS);
    }
}
