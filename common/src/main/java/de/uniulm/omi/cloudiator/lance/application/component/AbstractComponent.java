/*
 * Copyright (c) 2014-2015 University of Ulm
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.uniulm.omi.cloudiator.lance.application.component;

import de.uniulm.omi.cloudiator.lance.application.component.PortProperties.PortType;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.DynamicEnvVars;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.DynamicEnvVarsImpl;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.PropertyVisitor;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

public abstract class AbstractComponent implements Serializable, DynamicEnvVars {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractComponent.class);
    private static final long serialVersionUID = -8078692212700712671L;

    private final String name;
    private final ComponentId myId;
    private final List<InPort> inPorts;
    private final List<OutPort> outPorts;
    private final HashMap<String, Class<?>> properties;
    private final HashMap<String, ? extends Serializable> defaultValues;
    private DeploymentContext ctx;
    private DynamicEnvVarsImpl currentEnvVarsDynamic;

    AbstractComponent(Builder<?> builder) {
        name = builder.nameParam;
        myId = builder.myIdParam;
        inPorts = new ArrayList<>(builder.inPortsParam);
        outPorts = new ArrayList<>(builder.outPortsParam);
        properties = new HashMap<>(builder.propertiesParam);
        defaultValues = new HashMap<>(builder.defaultValuesParam);
    }

    public ComponentId getComponentId() {
    	return myId; 
    }

    public List<InPort> getExposedPorts() {
        List<InPort> ports = new ArrayList<>(inPorts.size());
        for(InPort i : inPorts) {
            ports.add(i);
        }
        return ports;
    }
    
    @Override
    public String toString() {
        return name + ": -> " + inPorts + "@" + myId; 
    }

    public List<OutPort> getDownstreamPorts() {
        return new ArrayList<>(outPorts); 
    }
    
    public void accept(PropertyVisitor visitor) {
        for(Entry<String, String> entry : currentEnvVarsDynamic.getEnvVars().entrySet()) {
            visitor.visit(entry.getKey(), entry.getValue());
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getEnvVars() {
        return getMatchingValsFromDContext();
    }

    public void injectDeploymentContext(DeploymentContext ctx) {
        this.ctx = ctx;
    }

    private Map<String,String> getMatchingValsFromDContext() {
        final Map<String,String> vals = new HashMap<>();

        for(Entry<String, Class<?>> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Class<?> type = entry.getValue();
            if(type == OutPort.class)
                continue;
            Object o = ctx.getProperty(propertyName, type);
            if(o == null) {
                o = defaultValues.get(propertyName);
            }
            if(o == null) {
                LOGGER.warn("propery '" + propertyName + "' has not been defined for the application");
                continue;
            }
            vals.put(propertyName, o.toString());
        }

        return vals;
    }

  @Override
  public void generateDynamicEnvVars() {
      final Map<String,String> vals = getMatchingValsFromDContext();
      DynamicEnvVarsImpl impl = DynamicEnvVarsImpl.DEPL_COMPONENT;
      impl.setEnvVars(vals);
      this.currentEnvVarsDynamic = impl;
  }

  //todo: exception handling if wrong enum type
  @Override
  public void injectDynamicEnvVars(DynamicEnvVarsImpl vars) throws ContainerException {
      this.currentEnvVarsDynamic = vars;
  }

  //todo: exception handling if wrong enum type
  @Override
  public void removeDynamicEnvVars(DynamicEnvVars vars) throws ContainerException {
      if(!currentEnvVarsDynamic.equals(vars))
          LOGGER.error("Cannot remove vars " + vars + " as they are not currently available.");

      //todo: Check if vars Map in DynamicEnvVarsImpl is reset
      this.currentEnvVarsDynamic = DynamicEnvVarsImpl.NETWORK_PORTS;
  }

  abstract static class Builder<T extends Builder<T>> {
      protected String nameParam;
      protected ComponentId myIdParam;
      private List<InPort> inPortsParam = new ArrayList<>();
      private List<OutPort> outPortsParam = new ArrayList<>();
      private HashMap<String, Class<?>> propertiesParam = new HashMap<>();
      private HashMap<String, Serializable> defaultValuesParam = new HashMap<>();

      public T name(String name) {
          nameParam = name;
          return self();
      }

      public T myId(ComponentId myId) {
          myIdParam = myId;
          return self();
      }

      public T inPorts(List<InPort> inPorts) {
          inPortsParam = new ArrayList<>(inPorts);
          return self();
      }

      public T outPorts(List<OutPort> outPorts) {
          outPortsParam = new ArrayList<>(outPorts);
          return self();
      }

      public T properties(HashMap<String, Class<?>> properties) {
          propertiesParam = new HashMap<>(properties);
          return self();
      }

      public T defaultValues(HashMap<String, ? extends Serializable> defaultValues) {
          defaultValuesParam = new HashMap<>(defaultValues);
          return self();
      }


      private Builder(String nameParam, ComponentId idParam) {
          this.nameParam = nameParam;
          this.myIdParam = idParam;
      }

      public T addInport(String portname, PortType type, int cardinality) {
          inPortsParam.add(new InPort(portname, type, cardinality));
          addProperty(portname, InPort.class);
          return self();
      }

      public T addInport(String portname, PortType type, int cardinality, int defaultPortNr) {
          inPortsParam.add(new InPort(portname, type, cardinality));
          addProperty(portname, InPort.class, Integer.valueOf(defaultPortNr));
          return self();
      }

      public T addOutport(String portname, PortUpdateHandler handler, int cardinality) {
          outPortsParam.add(new OutPort(portname, handler, cardinality, OutPort.NO_SINKS, OutPort.INFINITE_SINKS));
          addProperty(portname, OutPort.class);
          return self();
      }

      public T addOutport(String portname, PortUpdateHandler handler, int cardinality, int minSinks) {
          outPortsParam.add(new OutPort(portname, handler, cardinality, minSinks, OutPort.INFINITE_SINKS));
          addProperty(portname, OutPort.class);
          return self();
      }

      public T addOutport(String portname, PortUpdateHandler handler, int cardinality, int minSinks, int maxSinks) {
          if(minSinks < 0 || maxSinks < 1 || minSinks > maxSinks) {
              throw new IllegalArgumentException("minSinks and maxSinks have non-fitting values: " + minSinks + ", " + maxSinks);
          }
          outPortsParam.add(new OutPort(portname, handler, cardinality, minSinks, maxSinks));
          addProperty(portname, OutPort.class);
          return self();
      }

      public T addProperty(String propertyName, Class<?> propertyType, Serializable defaultValue) {
          addProperty(propertyName, propertyType);
          defaultValuesParam.put(propertyName, defaultValue);
          return self();
      }

      public void addProperty(String propertyName, Class<?> propertyType) {
          propertiesParam.put(propertyName, propertyType);
      }

      @SuppressWarnings("static-method")
      public void deploySequentially(boolean b) {
          if(b == false)
              throw new UnsupportedOperationException("parallel deployment not supported yet.");
      }

      Builder(){};

      abstract AbstractComponent build();

      protected abstract T self();
  }
}
