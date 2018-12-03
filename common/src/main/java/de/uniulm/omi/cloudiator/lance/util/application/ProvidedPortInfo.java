package de.uniulm.omi.cloudiator.lance.util.application;

import de.uniulm.omi.cloudiator.lance.application.component.PortProperties;

import java.io.Serializable;

public class ProvidedPortInfo implements Serializable {

    private static final long serialVersionUID = 9167263731953331611L;
    private final String providedPortName;
    private final transient PortProperties.PortType portType;
    private final int providedPort;
    private final int portRefNumber;
    private int cardinality;

    public ProvidedPortInfo(String providedPortName, PortProperties.PortType portType, int providedPort, int portRefNumber, int cardinality) {
        this.providedPortName = providedPortName;
        this.portType = portType;
        this.cardinality = cardinality;
        this.providedPort = providedPort;
        this.portRefNumber = portRefNumber;
    }

    public String getProvidedPortName() {
        return providedPortName;
    }

    public PortProperties.PortType getPortType() {
        return portType;
    }

    public int getProvidedPort() {
        return providedPort;
    }

    public int getPortRefNumber() {
        return portRefNumber;
    }

    public int getCardinality() {
        return cardinality;
    }

    public ProvidedPortInfo(String providedPortName, PortProperties.PortType portType, int providedPort, int portRefNumber) {
        this(providedPortName, portType, providedPort, portRefNumber, 1);
    }
}
