package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum DockerConfiguration {
	
	INSTANCE{
		
	},
	;
	
	private final String hostname;
	private final int port;
	private final boolean useRegistry;
	
	DockerConfiguration(){
		useRegistry = DockerConfigurationFields.isEnabled();
		hostname = DockerConfigurationFields.getHostname();
		port = DockerConfigurationFields.getPort();
		if(shouldBeUsedButCant()) {
			DockerConfigurationFields.LOGGER.error("cannot make use of registry@ " + hostname + ":" + port);
		}
	}
	
	private boolean shouldBeUsedButCant() {
		if(!useRegistry) return false;
		if(hostname == null || hostname.isEmpty() || port < 0 || port > 65555) // whatever the right number is
			return true;
		return false;
	}

	static class DockerConfigurationFields {
		private static final Logger LOGGER = LoggerFactory.getLogger(DockerConfigurationFields.class);
		
	 public static final String DOCKER_REGISTRY_USE_KEY = "host.docker.registry.use";
	 public static final String DOCKER_REGISTRY_HOST_KEY = "host.docker.registry.host";
	 public static final String DOCKER_REGISTRY_PORT_KEY = "host.docker.registry.port";
	 
	 public static final Boolean DOCKER_REGISTRY_USE_DEFAULT = Boolean.FALSE;
	 public static final String DOCKER_REGISTRY_HOST_DEFAULT = null;
	 public static final int DOCKER_REGISTRY_PORT_DEFAULT = 5000;
	 
	 static boolean isEnabled() {
		 System.out.println(System.getProperties());
		 String s = System.getProperty(DockerConfigurationFields.DOCKER_REGISTRY_USE_KEY, DockerConfigurationFields.DOCKER_REGISTRY_USE_DEFAULT.toString());
		 if("true".equals(s)) {
			 LOGGER.debug("use of docker registry enabled.");
			 return true;
		 }
		 LOGGER.debug("use of docker registry disabled.");
		 return false;
	 }
	 
	 static String getHostname() {
		 return System.getProperty(DockerConfigurationFields.DOCKER_REGISTRY_HOST_KEY, DockerConfigurationFields.DOCKER_REGISTRY_HOST_DEFAULT);
	 }
	 
	 static int getPort() {
		 String s = System.getProperty(DockerConfigurationFields.DOCKER_REGISTRY_PORT_KEY, Integer.toString(DockerConfigurationFields.DOCKER_REGISTRY_PORT_DEFAULT));
		 try {
			 return Integer.parseInt(s);
		 } catch(NumberFormatException ex) {
			 if(s != null && !s.isEmpty())
				 LOGGER.warn("cannot set port nunmber");
			 return -1;
		 }
	 }
	}

	public boolean registryCanBeUsed() {
		return registryEnabled() && !shouldBeUsedButCant();
	}
	
	public boolean registryEnabled() {
		return useRegistry;
	}
	
	public String prependRegistry(String pre) {
		if(useRegistry && !shouldBeUsedButCant()) {
			return hostname + ":" + port + "/" + pre;
		}
		return pre;
	}
}
