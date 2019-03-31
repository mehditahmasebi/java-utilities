import sun.management.ConnectorAddressLink;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import com.sun.tools.attach.*;


/**
 * A client JMX to test the connection with WildFly (based on host and port of
 * wildfly)
 *
 * @author psouda
 */
public class JMXExportMBeans {

    public static void main(String[] args) {
        try {
//            JMXConnector jmxConnector = getJMXConnectorByJMXPort(args);
            JMXConnector jmxConnector = getJMXConnectorByPID(args);
            MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();
            String domains[] = connection.getDomains();
            Arrays.sort(domains);
            Set<ObjectName> names = new TreeSet<ObjectName>(connection.queryNames(null, null));

            // var for last domain
            String lastDomain = "";

            for (int i = 0; i < domains.length; i++) {
                for (ObjectName mbeanName : names) {
                    if (mbeanName.getDomain().equals(domains[i])) {
                        MBeanInfo mbeanInfo = connection.getMBeanInfo(mbeanName);

                        if (mbeanInfo.getAttributes() != null && mbeanInfo.getAttributes().length > 0) {

                            if (!domains[i].equals(lastDomain)) {

                                // get current domain
                                lastDomain = domains[i];

                                // print separator
                                echo("___________________________");

                                // domain
                                echo(domains[i]);

                            }

                            // object name
                            String type = mbeanName.toString().substring(mbeanName.toString().lastIndexOf("type") + 5);

                            if (!type.contains("name")) {
                                // type
                                echo("\t" + type);
                                // attributes
                                echo("\t\tAttributes");
                                for (MBeanAttributeInfo mBeanAttributeInfo : mbeanInfo.getAttributes()) {
                                    printAttributesKeyValues(mBeanAttributeInfo, "\t\t\t");
                                }

                            } else {
                                // get name
                                String name = type.substring(type.lastIndexOf("name") + 5);

                                // type
                                echo("\t" + type.substring(0, type.lastIndexOf("name") - 1));

                                // name
                                echo("\t\t" + name);

                                // attributes
                                echo("\t\t\tAttributes");
                                for (MBeanAttributeInfo mBeanAttributeInfo : mbeanInfo.getAttributes()) {
                                    printAttributesKeyValues(mBeanAttributeInfo, "\t\t\t\t");
                                }
                            }
                        }
                    }
                }
            }

            // close connector
            jmxConnector.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static JMXConnector getJMXConnectorByJMXPort(String[] args) throws IOException {
        // Get a connection to the WildFly MBean server on localhost (by default) on
        // port 9990
        String host = "localhost";
        int port = 9990;

        // get host from args
        if (args.length > 1) {
            host = args[0];
            port = Integer.valueOf(args[1]);
        }

        String urlString = System.getProperty("jmx.service.url",
                "service:jmx:http-remoting-jmx://" + host + ":" + port);

        JMXServiceURL serviceURL = new JMXServiceURL(urlString);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
        return jmxConnector;
    }

    private static JMXConnector getJMXConnectorByPID(String[] args) throws IOException {
        int pid = Integer.parseInt(args[0]);
        String address = ConnectorAddressLink.importFrom(pid);
        if(address != null) {
            JMXServiceURL jmxUrl = new JMXServiceURL(address);
            return JMXConnectorFactory.connect(jmxUrl);
        }
        else {
            /*
             * JAR file normally in ${java.home}/jre/lib but may be in ${java.home}/lib
             * with development/non-images builds
             */
            String home = System.getProperty("java.home");
            String agent = home + File.separator + "jre" + File.separator + "lib"
                    + File.separator + "management-agent.jar";
            File f = new File(agent);
            if (!f.exists()) {
                agent = home + File.separator + "lib" + File.separator +
                        "management-agent.jar";
                f = new File(agent);
                if (!f.exists()) {
                    throw new RuntimeException("management-agent.jar missing");
                }
            }
            agent = f.getCanonicalPath();

            System.out.println("Loading " + agent + " into target VM ...");

            try {
                VirtualMachine.attach(pid+"").loadAgent(agent);
                address = ConnectorAddressLink.importFrom(pid);
                if(address != null) {
                    JMXServiceURL jmxUrl = new JMXServiceURL(address);
                    return JMXConnectorFactory.connect(jmxUrl);
                }
                else
                    throw new RuntimeException("Can't connect to JMX Service of PID");
            } catch (Exception x) {
                throw new IOException(x.getMessage());
            }
        }
    }

    private static void printAttributesKeyValues(MBeanAttributeInfo mBeanAttributeInfo, String spaces) {
        echo(spaces + mBeanAttributeInfo.getName());
        echo(spaces + "\t - Name -> " + mBeanAttributeInfo.getName());
        echo(spaces + "\t - Description -> " + mBeanAttributeInfo.getDescription());
        echo(spaces + "\t - Readable -> " + mBeanAttributeInfo.isReadable());
        echo(spaces + "\t - Writable -> " + mBeanAttributeInfo.isWritable());
        echo(spaces + "\t - Is -> " + mBeanAttributeInfo.isIs());
        echo(spaces + "\t - Type -> " + mBeanAttributeInfo.getType());
    }

    private static void echo(String s) {
        System.out.println(s);
    }
}