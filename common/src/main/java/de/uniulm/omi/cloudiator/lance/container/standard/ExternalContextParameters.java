package de.uniulm.omi.cloudiator.lance.container.standard;

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import java.util.List;

public class ExternalContextParameters {
  private static final String pubIpHostString = "HOST_PUBLIC_IP";
  private static final String cloudIpHostString = "HOST_CLOUD_IP";
  private static final String containerIpHostString = "HOST_CONTAINER_IP";
  private final String name;
  private final ApplicationInstanceId appId;
  private final ComponentId cId;
  private final ComponentInstanceId cInstId;
  private final String publicIp;
  //todo: make these adresses settable
  private final String cloudIp;
  private final String containerIp;
  private final List<InPortContext> inpContext;
  //private final List<OutPortContext> outpContext;
  private final ContainerStatus status;

  private ExternalContextParameters(Builder builder) {
    this.name = builder.name_;
    this.appId = builder.appInstId;
    this.cId = builder.cId;
    this.cInstId = builder.cInstId;
    this.publicIp = builder.publicIp;
    //todo: Use builder vars
    cloudIp = "0.0.0.0";
    //todo: Use builder vars
    containerIp = "0.0.0.0";
    this.inpContext = builder.inpContext;
    //this.outpContext = builder.outpContext;
    this.status = builder.status_;
  }

  public String getName() {
    return name;
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

  public String getCloudIp() {
    return cloudIp;
  }

  public String getContainerIp() {
    return containerIp;
  }

  public List<InPortContext> getInpContext() {
    return inpContext;
  }

  /*public List<OutPortContext> getOutpContext() {
    return outpContext;
  }*/

  public ContainerStatus getStatus() {
    return status;
  }

  public static String getPublicIpHostString() {
    return pubIpHostString;
  }

  public static String getCloudIpHostString() {
    return cloudIpHostString;
  }
  public static String getContainerIpHostString() {
    return containerIpHostString;
  }

  //Todo: Expand this class with "CONTAINER" and "CLOUD" port
  public static class InPortContext {
    private static final String fullPortPublicPrefixString = "ACCESS_PUBLIC_";
    private static final String fullPortCloudPrefixString = "ACCESS_CLOUD_";
    private static final String fullPortContainerPrefixString = "ACCESS_CONTAINER_";
    String portName;
    int inernalInPortNmbr;

    public InPortContext(String portName, int inernalInPortNmbr) {
      this.portName = portName;
      this.inernalInPortNmbr = inernalInPortNmbr;
    }

    public Integer getInernalInPortNmbr() {
      return inernalInPortNmbr;
    }

    public String getFullPublicPortName() {
      final String fullPortName = fullPortPublicPrefixString + portName;
      return fullPortName;
    }

    public String getFullCloudPortName() {
      final String fullPortName = fullPortCloudPrefixString + portName;
      return fullPortName;
    }

    public String getFullContainerPortName() {
      final String fullPortName = fullPortContainerPrefixString + portName;
      return fullPortName;
    }
  }

  /*public static class OutPortContext {
    String portName;
    ComponentId sinkComponentId;
    int sinkInPortNmbr;
  }*/

  public static class Builder {
    private String name_;
    private ApplicationInstanceId appInstId;
    private ComponentId cId;
    private ComponentInstanceId cInstId;
    private String publicIp;
    /* todo: integrate those vars
     private final String cloudIp;
     private final String containerIp;
    */
    private List<InPortContext> inpContext;
    //private List<OutPortContext> outpContext;
    private ContainerStatus status_;

    public Builder() {}

    public Builder name(String name_) {
      this.name_ = name_;
      return this;
    }

    public Builder appInstanceId(ApplicationInstanceId appInstId) {
      this.appInstId = appInstId;
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

    public Builder inPortContext(List<InPortContext> inpContext) {
      this.inpContext = inpContext;
      return this;
    }

    /*public Builder outPortContext(List<OutPortContext> outpContext) {
      this.outpContext = outpContext;
      return this;
    }*/

    public Builder status(ContainerStatus status_) {
      this.status_ = status_;
      return this;
    }

    public ExternalContextParameters build() {
      return new ExternalContextParameters(this);
    }
  }
}
