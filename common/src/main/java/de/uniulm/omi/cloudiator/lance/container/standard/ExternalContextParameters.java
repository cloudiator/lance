package de.uniulm.omi.cloudiator.lance.container.standard;

import com.google.common.base.MoreObjects;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;

public class ExternalContextParameters {

  private final String taskName;
  ApplicationInstanceId appId;
  private final ComponentId cId;
  private final ComponentInstanceId cInstId;
  private final String publicIp;
  private final ProvidedPortContext providedPortContext;
  //private final List<OutPortContext> outpContext;
  private final ContainerStatus contStatus;
  private final LifecycleHandlerType compInstType;

  private ExternalContextParameters(Builder builder) {
    this.taskName = builder.taskName_;
    this.appId = builder.appId;
    this.cId = builder.cId;
    this.cInstId = builder.cInstId;
    this.publicIp = builder.publicIp;
    this.providedPortContext = builder.providedPortContext;
    //this.outpContext = builder.outpContext;
    this.contStatus = builder.contStatus_;
    this.compInstType = builder.compInstType_;
  }

  public String getTaskName() {
    return taskName;
  }

  public ApplicationInstanceId getAppId() {
    return appId;
  }

  public ComponentId getcId() {
    return cId;
  }

  public ComponentInstanceId getcInstId() {
    return cInstId;
  }

  public String getPublicIp() {
    return publicIp;
  }

  public ProvidedPortContext getProvidedPortContext() {
    return providedPortContext;
  }

  /*public List<OutPortContext> getOutpContext() {
    return outpContext;
  }*/

  public ContainerStatus getStatus() {
    return contStatus;
  }

  public LifecycleHandlerType getCompInstStatus() {
    return compInstType;
  }

  public String getFullHostName() {
    final String name = "HOST_PUBLIC_IP";
    return name;
  }

  public static class ProvidedPortContext {

    String portName;
    int portNmbr;

    public ProvidedPortContext(String portName, int inernalInPortNmbr) {
      this.portName = portName;
      this.portNmbr = inernalInPortNmbr;
    }

    public Integer getPortNmbr() {
      return portNmbr;
    }

    public String getFullPortName() {
      final String fullPortName = "ACCESS_" + "PUBLIC_" + portName;
      return fullPortName;
    }
  }

  /*public static class OutPortContext {
    String portName;
    ComponentId sinkComponentId;
    int sinkInPortNmbr;
  }*/

  public static class Builder {

    private String taskName_;
    ApplicationInstanceId appId;
    private ComponentId cId;
    private ComponentInstanceId cInstId;
    private String publicIp;
    private ProvidedPortContext providedPortContext;
    //private List<OutPortContext> outpContext;
    private ContainerStatus contStatus_;
    private LifecycleHandlerType compInstType_;

    public Builder() {
    }

    public Builder taskName(String taskName_) {
      this.taskName_ = taskName_;
      return this;
    }

    public Builder appId(ApplicationInstanceId appId) {
      this.appId = appId;
      return this;
    }

    public Builder compId(ComponentId cId) {
      this.cId = cId;
      return this;
    }

    public Builder compInstId(ComponentInstanceId cInstId) {
      this.cInstId = cInstId;
      return this;
    }

    public Builder pubIp(String publicIp) {
      this.publicIp = publicIp;
      return this;
    }

    public Builder providedPortContext(ProvidedPortContext providedPortContext) {
      this.providedPortContext = providedPortContext;
      return this;
    }

    /*public Builder outPortContext(List<OutPortContext> outpContext) {
      this.outpContext = outpContext;
      return this;
    }*/

    public Builder contStatus(ContainerStatus contStatus_) {
      this.contStatus_ = contStatus_;
      return this;
    }

    public Builder compInstType(LifecycleHandlerType compInstType_) {
      this.compInstType_ = compInstType_;
      return this;
    }

    public ExternalContextParameters build() {
      return new ExternalContextParameters(this);
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("taskName", taskName)
        .add("appId", appId)
        .add("cId", cId)
        .add("cInstId", cInstId)
        .add("publicIp", publicIp)
        .add("providedPortContext", providedPortContext)
        .add("contStatus", contStatus)
        .add("compInstType", compInstType)
        .toString();
  }
}
