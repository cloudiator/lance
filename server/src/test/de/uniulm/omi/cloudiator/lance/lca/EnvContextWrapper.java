package de.uniulm.omi.cloudiator.lance.lca;

public class EnvContextWrapper {

	private static String cloudIp = "127.0.0.1";
	private static String publicIp = "129.0.0.1";
	private static String containerIp = "128.0.0.1";

	public static HostContext create() {
		System.setProperty("host.ip.public", getPublicIp());
		System.setProperty("host.ip.private", getCloudIp());
		System.setProperty("host.vm.cloud.tenant.id", "tenant: 33033");
		System.setProperty("host.vm.id", "vm: 33033");
		System.setProperty("host.vm.cloud.id", "cloud: 33033");
		
		return EnvContext.fromEnvironment();
	}

	public static void setCloudIp(String cIp) {
		cloudIp = cIp;
	}

	public static void setPublicIp(String pIp) {
		publicIp = pIp;
	}

	public static void setContainerIp(String cIp) {
		containerIp = cIp;
	}

	public static String getCloudIp() {
		return cloudIp;
	}
	
	public static String getPublicIp() {
		return publicIp;
	}

	public static String getContainerIp() {
		return containerIp;
	}
}
