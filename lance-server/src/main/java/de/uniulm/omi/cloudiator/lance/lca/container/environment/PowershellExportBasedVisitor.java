package de.uniulm.omi.cloudiator.lance.lca.container.environment;

import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortHierarchyLevel;
import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShell;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;

import java.util.List;

/**
 * Created by Daniel Seybold on 10.09.2015.
 */
public class PowershellExportBasedVisitor implements NetworkVisitor, PropertyVisitor{

    private final ShellLikeInterface shellLikeInterface;

    public PowershellExportBasedVisitor(PlainShell plainShell) {
        shellLikeInterface = plainShell;

    }

    public void addEnvironmentVariable(String name, String value) {
        //fixme: change this to powershell commands, check splitting of commands for blanks inside '' ";
        ExecutionResult result = shellLikeInterface.executeCommand("SETX " + name + " " + value + " ");
        if(result.isSuccess()) {
            shellLikeInterface.executeCommand("SETX " + name + " " + value + " /m");
            return;
        }
        throw new IllegalStateException("could not set environment variables: " + name + "=" + value);
    }


    @Override
    public void visitNetworkAddress(PortHierarchyLevel level, String address) {
        addEnvironmentVariable(level.getName().toUpperCase() + "_IP", address);
    }

    @Override
    public void visitInPort(String portName, PortHierarchyLevel level, Integer portNr) {
        addEnvironmentVariable(level.getName().toUpperCase() + "_" + portName.toUpperCase(), portNr.toString());
    }

    @Override
    public void visitOutPort(String portName, PortHierarchyLevel level, List<DownstreamAddress> sinks) {
        String value = "";
        for(DownstreamAddress element : sinks) {
            if(!value.isEmpty()) {
                value = value + ",";
            }
            value = value + element.toString();
        }
        addEnvironmentVariable(level.getName().toUpperCase() + "_" + portName, value);
    }

    @Override
    public void visit(String propertyName, String propertyValue) {
        addEnvironmentVariable(propertyName, propertyValue);
    }
}
