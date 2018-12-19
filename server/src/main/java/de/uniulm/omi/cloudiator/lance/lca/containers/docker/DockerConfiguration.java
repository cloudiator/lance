package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerConfiguration {

  public static final DockerConfiguration INSTANCE = new DockerConfiguration(false);

  private final String hostname;
  private final int port;
  private final boolean useRegistry;
  private final boolean portRequired;
  private final String userName;
  private final String password;
  private final boolean useCredentialsParam;

  DockerConfiguration(
      String userName, String password, boolean portRequiredParam, boolean useCredentialsParam) {
    useRegistry = DockerConfigurationFields.isEnabled();
    hostname = DockerConfigurationFields.getHostname();
    if (portRequiredParam) {
      port = DockerConfigurationFields.getPort();
    } else {
      port = -1;
    }
    if (shouldBeUsedButCant()) {
      DockerConfigurationFields.LOGGER.error(
          "cannot make use of registry@ " + hostname + ":" + port);
    }
    this.userName = userName;
    this.password = password;
    this.portRequired = portRequiredParam;
    this.useCredentialsParam = useCredentialsParam;
  }

  DockerConfiguration(boolean portRequiredParam) {
    this("", "", portRequiredParam, false);
  }

  private boolean shouldBeUsedButCant() {
    if (!useRegistry) return false;
    if (hostname == null
        || hostname.isEmpty()
        || port < 0
        || port > 65555) // whatever the right number is
    return true;
    return false;
  }

  public boolean useCredentials() {
    return useCredentialsParam;
  }

  public boolean isUseRegistry() {
    return useRegistry;
  }

  public String getUserName() {
    return userName;
  }

  public String getPassword() {
    return password;
  }

  public boolean registryCanBeUsed() {
    return registryEnabled() && !shouldBeUsedButCant();
  }

  public boolean registryEnabled() {
    return useRegistry;
  }

  public String prependRegistry(String pre) {
    if (useRegistry && !shouldBeUsedButCant()) {
      if (portRequired) {
        return hostname + ":" + port + "/" + pre;
      } else {
        return hostname + "/" + pre;
      }
    }
    return pre;
  }

  static class DockerConfigurationFields {
    public static final String DOCKER_REGISTRY_USE_KEY = "host.docker.registry.use";
    public static final String DOCKER_REGISTRY_HOST_KEY = "host.docker.registry.host";
    public static final String DOCKER_REGISTRY_PORT_KEY = "host.docker.registry.port";
    public static final Boolean DOCKER_REGISTRY_USE_DEFAULT = Boolean.FALSE;
    public static final String DOCKER_REGISTRY_HOST_DEFAULT = null;
    public static final int DOCKER_REGISTRY_PORT_DEFAULT = 5000;
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerConfigurationFields.class);

    static boolean isEnabled() {
      System.out.println(System.getProperties());
      String s =
          System.getProperty(
              DockerConfigurationFields.DOCKER_REGISTRY_USE_KEY,
              DockerConfigurationFields.DOCKER_REGISTRY_USE_DEFAULT.toString());
      if ("true".equals(s)) {
        LOGGER.debug("use of docker registry enabled.");
        return true;
      }
      LOGGER.debug("use of docker registry disabled.");
      return false;
    }

    static String getHostname() {
      return System.getProperty(
          DockerConfigurationFields.DOCKER_REGISTRY_HOST_KEY,
          DockerConfigurationFields.DOCKER_REGISTRY_HOST_DEFAULT);
    }

    static int getPort() {
      String s =
          System.getProperty(
              DockerConfigurationFields.DOCKER_REGISTRY_PORT_KEY,
              Integer.toString(DockerConfigurationFields.DOCKER_REGISTRY_PORT_DEFAULT));
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException ex) {
        if (s != null && !s.isEmpty()) LOGGER.warn("cannot set port nunmber");
        return -1;
      }
    }
  }
}
