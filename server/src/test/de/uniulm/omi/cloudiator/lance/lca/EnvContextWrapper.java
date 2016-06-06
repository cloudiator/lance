package de.uniulm.omi.cloudiator.lance.lca;

public class EnvContextWrapper {

	public static HostContext create() {
		System.setProperty("host.ip.public", getPublicIp());
		System.setProperty("host.ip.private", getCloudIp());
		System.setProperty("host.vm.cloud.tenant.id", "tenant: 33033");
		System.setProperty("host.vm.id", "vm: 33033");
		System.setProperty("host.vm.cloud.id", "cloud: 33033");
		
		return EnvContext.fromEnvironment();
	}

	public static String getCloudIp() {
		return "127.0.0.1";
	}
	
	public static String getPublicIp() {
		return "129.0.0.1";
	}

	public static String getContainerIp() {
		return "128.0.0.1";
	}
}
