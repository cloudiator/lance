package de.uniulm.omi.cloudiator.lance.lca.container.environment;

import com.google.common.net.InetAddresses;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum DynamicEnvVarsImpl implements DynamicEnvVars {
  NETWORK_ADDR(
      (String str) -> {
        try {
          if (str.equals("<unknown>")) return true;

          InetAddresses.forString(str);
          return true;
        } catch (IllegalArgumentException ex) {
          return false;
        }
      }),
  NETWORK_PORT(
      (String str) -> {
        try {
          int n = Integer.parseInt(str);
          if (n < 0 || n > 65535) return false;
          else return true;
        } catch (NumberFormatException ex) {
          return false;
        }
      }),
  // todo: implement -> see OutPortHandler
  NETWORK_PORTS(
      (String str) -> {
        return true;
      }),
  // todo: implement
  DEPL_COMPONENT(
      (String str) -> {
        return true;
      });

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEnvVarsImpl.class);
  ValidateString func;
  private Map<String, String> vars;

  private DynamicEnvVarsImpl(ValidateString func) {
    this.func = func;
    vars = new HashMap<>();
  }

  @Override
  public Map<String, String> getEnvVars() {
    return vars;
  }

  public void setEnvVars(Map<String, String> vars) {
    for (Entry<String, String> entry : vars.entrySet()) {
      if (func.check(entry.getValue())) this.vars.put(entry.getKey(), entry.getValue());
      else
        LOGGER.warn(
            "Cannot set Dynamic Environment variable: "
                + entry.getKey()
                + ". Value: "
                + entry.getValue()
                + " has wrong format");
    }
  }

  @Override
  public void generateDynamicEnvVars() {
    vars = new HashMap<>();
  }

  @Override
  public void injectDynamicEnvVars(DynamicEnvVarsImpl vars) {
    setEnvVars(vars.getEnvVars());
  }

  @Override
  public void removeDynamicEnvVars(DynamicEnvVars vars) {
    for (Entry<String, String> entry : vars.getEnvVars().entrySet()) {
      this.vars.remove(entry.getKey(), entry.getValue());
    }
  }
}
