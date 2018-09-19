package de.uniulm.omi.cloudiator.lance.lca.container.environment;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum DynamicEnvVarsImpl implements DynamicEnvVars {
  NETWORK_ADDR((String str) ->
    {
      try {
        InetAddresses.forString(str);
        return true;
      } catch (IllegalArgumentException ex) {
        return false;
      }
  }),
  NETWORK_PORT((String str) ->
  {
    try {
      int n = Integer.parseInt(str);
      if(n<0 || n>65535)
        return false;
      else
        return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }),
  //todo: implement -> see OutPortHandler
  NETWORK_PORTS((String str) ->
  {
    return true;
  });


  private final static Logger LOGGER = LoggerFactory.getLogger(DynamicEnvVarsImpl.class);
  ValidateString func;
  private Map<String,String> vars;

  private DynamicEnvVarsImpl(ValidateString func) {
    this.func = func;
    vars = new HashMap<>();
  }

  public void setEnvVars(Map<String,String> vars) {
    for(Entry<String, String> entry : vars.entrySet()) {
      if(func.check(entry.getValue()))
        vars.put(entry.getKey(),entry.getValue());
      else
        LOGGER.warn("Cannot set Dynamic Environment variable: " + entry.getKey() + ". Value: " + entry.getValue() + " has wrong format");
    }

    this.vars = vars;
  }

  @Override
  public Map<String, String> getEnvVars() {
    return vars;
  }
}
