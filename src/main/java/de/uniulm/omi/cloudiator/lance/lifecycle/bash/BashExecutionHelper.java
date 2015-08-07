package de.uniulm.omi.cloudiator.lance.lifecycle.bash;

import java.util.List;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.Shell;

final class BashExecutionHelper {
    
    private BashExecutionHelper() {
        // no instances of this class //
    }
    
    private static boolean osMatches(OperatingSystem osParam, ExecutionContext ec) {
        return osParam.equals(ec.getOperatingSystem());
    }
    
    private static String buildStringFromCommandLine(String[] cmd) {
        String res = "";
        for(String s : cmd) { 
            res = res + " " + s;
        }
        return res;
    }
    
    private static void doExecuteCommand(boolean blocking, String command, Shell shell) {
        if(blocking) {
            shell.executeBlockingCommand(command);
        } else { 
            shell.executeCommand(command); 
        }
    }
    
    static void executeCommands(OperatingSystem osParam, ExecutionContext ec, List<String[]> commands) {
          if(!osMatches(osParam, ec))
              return;
          
          Shell shell = ec.getShell();
          for(String[] cmd : commands) {
              String res = buildStringFromCommandLine(cmd);
              doExecuteCommand(false, res, shell);
          }
    }
    
    static void executeBlockingCommands(OperatingSystem osParam, ExecutionContext ec, List<String[]> commands) {
        if(!osMatches(osParam, ec))
            return;
        
        Shell shell = ec.getShell();
        final int commandSize = commands.size();
        int counter = 0;
        
        for(String[] cmd : commands) {
            String res = buildStringFromCommandLine(cmd);
            counter++;
            doExecuteCommand(counter == commandSize, res, shell);
        }
    }
}
