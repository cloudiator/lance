This project implements the PaaSage lifecylce agent. A tool that is 
concerned with executing the individual steps of the lifecycle of
a particular component, mainly install, configure and run the 
component.

If you are targeting to run your application on a (virtual) machine
X, an instance of the lifecycle agent has to run there. As prerequistes
you have to install Java (version 8+) maven and git on that machine. 
Then, clone the sources from git
```
git clone git@github.com:cloudiator/lance.git
```

Then, enter the ``LifecycleAgent`` directory and run maven
```
mvn clean install
```

Now, you can run the agent with the following command:
```
java -classpath target/LifecycleAgent-1.0-SNAPSHOT.jar -Dhost.ip.public=<public ip of machine> -Dhost.ip.private=<private ip of machine> -Djava.rmi.server.hostname=<ip address to be used by RMI> -DVM_ID=<id of machine> -DTENANT_ID=<id of tenant> -Dhost.os=<operating system> -Dhost.vm.cloud.id=<id of cloud platform> de.uniulm.omi.cloudiator.lance.lca.LifecycleAgentBooter
```

In practise this may look as follows:
```
java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n -classpath target/LifecycleAgent-1.0-SNAPSHOT.jar -Dhost.ip.public=134.60.64.34 -Dhost.ip.private=192.168.6.7 -Djava.rmi.server.hostname=134.60.64.34 -DVM_ID=dokker_vm -DTENANT_ID=dslab -Dhost.os=ubuntu:14.04 -Dhost.vm.cloud.id=omistack  de.uniulm.omi.cloudiator.lance.lca.LifecycleAgentBooter
```

If debugging shall be enabled, use the following command instead. You can then connect with a remote 
debugger to the virtual machine.

```
java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n -classpath target/LifecycleAgent-1.0-SNAPSHOT.jar -Dhost.ip.public=134.60.64.34 -Dhost.ip.private=192.168.6.7 -Djava.rmi.server.hostname=134.60.64.34 -DVM_ID=dokker_vm -DTENANT_ID=dslab -Dhost.os=ubuntu:14.04 -Dhost.vm.cloud.id=omistack  de.uniulm.omi.cloudiator.lance.lca.LifecycleAgentBooter
```

Every lifecylce agent will expose itself as an RMI object over a host-local RMI registry. For that reason, the
default registry port 1099 to the machine will have to be accessible. Also, the agent itself will be addressed
over port 33034 so that this port will have to be open as well. In return, the agent communicates back to the
'home base' with the directory and hence will require open ports for outgoing connections.


