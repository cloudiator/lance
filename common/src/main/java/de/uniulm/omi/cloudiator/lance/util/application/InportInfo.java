package de.uniulm.omi.cloudiator.lance.util.application;

import de.uniulm.omi.cloudiator.lance.application.component.PortProperties;

import java.io.Serializable;

public class InportInfo implements Serializable {
    private final String inportName;
    private final transient PortProperties.PortType portType;
    private final int inPort;
    private final int requiredPortNumber;
    private int cardinality;

    public InportInfo(String inportName, PortProperties.PortType portType, int inPort, int requiredPortNumber, int cardinality) {
        this.inportName = inportName;
        this.portType = portType;
        this.cardinality = cardinality;
        this.inPort = inPort;
        this.requiredPortNumber = requiredPortNumber;
    }

    public String getInportName() {
        return inportName;
    }

    public PortProperties.PortType getPortType() {
        return portType;
    }

    public int getInPort() {
        return inPort;
    }

    public int getRequiredPortNumber() {
        return requiredPortNumber;
    }

    public int getCardinality() {
        return cardinality;
    }

    public InportInfo(String inportName, PortProperties.PortType portType, int inPort, int requiredPortNumber) {
        this(inportName, portType, inPort, requiredPortNumber, 1);
    }
}
