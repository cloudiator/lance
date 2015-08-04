import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.DockerShell;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.ConnectorFactory;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;


public final class ApachePortUpdateTester {

    public final static String APACHELB_OUT_PORT_NAME = "APACHELB_OUTPORT";
    private final static String UBUNTU_DEFAULT_SITE_NAME = "000-default.conf";
    private final static String UBUNTU_CONFIG_LOCATION = "/etc/apache2";
    private final static String UBUNTU_DEFAULT_PORT = "80";
    private final static String UBUNTU_PORTS_FILE = "ports.conf";
    private final static String UBUNTU_PROXY_CONFIG_FILE = "proxy_balancer.conf";

    private static final String MAIN_COMMAND =
        "for el in $(echo \"$" + APACHELB_OUT_PORT_NAME + "\" | sed -e \"s/,/\\n/g\") ; do " +
            "sed -i -e \"s/# Generated code - DO NOT MODIFY.*/# Generated code - DO NOT MODIFY\\n\\t\\t"
            +
            "BalancerMember http:\\/\\/${el} # Generated line/g\" " +
            UBUNTU_CONFIG_LOCATION + "/mods-available/" + UBUNTU_PROXY_CONFIG_FILE +
            " ; done";

    private static final String TEST_COMMAND_0 =
        "$(echo \"$" + APACHELB_OUT_PORT_NAME + "\" | sed -e \"s/,/\\n/g\")";
    private static final String TEST_COMMAND_1 =
        "for el in $(echo \"$" + APACHELB_OUT_PORT_NAME + "\" | sed -e \"s/,/\\n/g\") ; do " +
            "touch /tmp/$el ; done";

    public static void main(String[] main) throws DockerException {

        ComponentInstanceId myId =
            ComponentInstanceId.fromString("db5f920e-1404-44eb-9a9d-68d9f375cbdd");
        DockerConnector client = ConnectorFactory.INSTANCE.createConnector("local");
        DockerShell dshell = client.getSideShell(myId);

        ExecutionResult result = dshell.executeCommand(
            "export " + APACHELB_OUT_PORT_NAME + "=192.168.1.4:8000,192.168.1.5:8900");
        result = dshell.executeCommand("env");
        result = dshell.executeCommand(
            "sed -i -e \"/.*# Generated line/d\" " + UBUNTU_CONFIG_LOCATION + "/mods-available/"
                + UBUNTU_PROXY_CONFIG_FILE);
        result = dshell.executeCommand(MAIN_COMMAND);
        System.err.println(result);
    }

}
