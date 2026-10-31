package atlassian.agent;

import atlassian.agent.keygen.Encoder;
import atlassian.agent.license.LicenseProperty;
import atlassian.agent.license.products.*;
import org.apache.commons.cli.*;

import java.util.HashMap;
import java.util.Map;

public class Usage {
    private static final String SYS_PROP_PREFIX = "atlassian.agent.";
    private static final Options OPTIONS = new Options();
    private static final Map<String, String> PRODUCTS = new HashMap<>(16);
    private static final String PRODUCTS_DESC;

    static {
        PRODUCTS.put("conf", "Confluence");
        PRODUCTS.put("jira", "JIRA Software(common jira)");
        PRODUCTS.put("questions", "Questions plugin for Confluence");
        PRODUCTS.put("tc", "Team Calendars plugin for Confluence");
        PRODUCTS.put("bamboo", "Bamboo");
        PRODUCTS.put("bitbucket", "Bitbucket");
        PRODUCTS.put("fisheye", "FishEye");
        PRODUCTS.put("crucible", "Crucible");
        PRODUCTS.put("crowd", "Crowd");
        PRODUCTS.put("jc", "JIRA Core");
        PRODUCTS.put("portfolio", "Portfolio plugin for JIRA");
        PRODUCTS.put("jsd", "JIRA Service Desk");
        PRODUCTS.put("training", "Training plugin for JIRA");
        PRODUCTS.put("capture", "Capture plugin for JIRA");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : PRODUCTS.entrySet()) {
            sb.append("\n[");
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append("]");
        }

        PRODUCTS_DESC = sb.toString();

        OPTIONS.addOption(Option.builder("p")
                .longOpt("product")
                .desc("License product, support: " + PRODUCTS_DESC)
                .hasArg()
                .build());
        OPTIONS.addOption(Option.builder("s")
                .longOpt("serverid")
                .desc("License server ID")
                .hasArg()
                .build());
        OPTIONS.addOption(Option.builder("m")
                .longOpt("mail")
                .desc("License email")
                .hasArg()
                .build());
        OPTIONS.addOption(Option.builder("o")
                .longOpt("organisation")
                .desc("License organisation")
                .hasArg()
                .build());
        OPTIONS.addOption(Option.builder("n")
                .longOpt("name")
                .desc("License name[default: <license email>]")
                .hasArg()
                .build());
        OPTIONS.addOption(Option.builder("d")
                .longOpt("datacenter")
                .desc("Data center license[default: false]")
                .build());
        OPTIONS.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Print help message")
                .build());
    }

    public static void main(String[] args) {
        String usage = "\n====================================================\n" +
                "=======           Atlassian Agent            =======\n" +
                "====================================================\n\n";

        System.out.print(usage);
        System.out.flush();

        CommandLine command = null;
        if (args.length > 0) {
            try {
                command = new DefaultParser().parse(OPTIONS, args);
            } catch (ParseException e) {
                printUsage();
                return;
            }
            if (hasOption(command, "help", 'h')) {
                printUsage();
                return;
            }
        }

        String product = firstNonBlank(optionValue(command, "product", 'p'), systemProperty("product"));
        String serverID = firstNonBlank(optionValue(command, "serverid", 's'), systemProperty("serverid"));
        String contactEMail = firstNonBlank(optionValue(command, "mail", 'm'), systemProperty("mail"));
        String organisation = firstNonBlank(optionValue(command, "organisation", 'o'), systemProperty("organisation"));
        if (isBlank(product) || isBlank(serverID) || isBlank(contactEMail) || isBlank(organisation)) {
            printUsage();
            return;
        }

        String contactName = firstNonBlank(optionValue(command, "name", 'n'), systemProperty("name"), contactEMail);
        boolean dataCenter = hasOption(command, "datacenter", 'd') || systemPropertyFlag("datacenter");
        // Confluence 9+ setup rejects non-DC licenses (LegacyServerLicenseValidator / isClusteringEnabled).
        if ("conf".equals(product)) {
            dataCenter = true;
        }

        runCommand(product, serverID, contactEMail, organisation, contactName, dataCenter);
    }

    private static void printUsage() {
        String selfPath = Usage.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        System.out.print("KeyGen ");
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar " + selfPath, OPTIONS, true);

        System.out.println("\n# KeyGen example (short options, use with java -jar):");
        System.out.println("  java -jar " + selfPath
                + " -p jira -m you@example.com -o MyOrg -s XXXX-XXXX -n LicenseName");
        System.out.println("\n# KeyGen example (long options; must be AFTER main class or use java -jar):");
        System.out.println("  java -cp ... atlassian.agent.Usage"
                + " --product jira --mail you@example.com --organisation MyOrg --serverid XXXX-XXXX"
                + " --name LicenseName");
        System.out.println("\n# IntelliJ VM options (when args would appear before -classpath):");
        System.out.println("  -D" + SYS_PROP_PREFIX + "product=jira"
                + " -D" + SYS_PROP_PREFIX + "mail=you@example.com"
                + " -D" + SYS_PROP_PREFIX + "organisation=MyOrg"
                + " -D" + SYS_PROP_PREFIX + "serverid=XXXX-XXXX"
                + " -D" + SYS_PROP_PREFIX + "name=LicenseName");
        System.out.println("\n# Do NOT put --product/--mail in VM options; JVM does not recognize them.");

        System.out.println("\n================================================================================");
        System.out.println("\n# Agent usage: append -javaagent arg to system environment: CATALINA_OPTS.");
        System.out.println("# Example(execute this command or append it to setenv.sh/setenv.bat file): \n");
        System.out.println("  export CATALINA_OPTS=\"-javaagent:" + selfPath + " ${CATALINA_OPTS}\"");
        System.out.println("\n# Then start your confluence/jira server.\n");

        System.exit(1);
    }

    private static void runCommand(String product, String serverID, String contactEMail, String organisation,
                                   String contactName, boolean dataCenter) {
        LicenseProperty property;
        switch (product) {
            case "conf":
                property = new Confluence(contactName, contactEMail, serverID, organisation, dataCenter);
                break;
            case "jira":
                property = new JIRASoftware(contactName, contactEMail, serverID, organisation, dataCenter);
                break;
            case "questions":
                property = new Questions(contactName, contactEMail, serverID, organisation, dataCenter);
                break;
            case "tc":
                property = new TeamCalendars(contactName, contactEMail, serverID, organisation, dataCenter);
                break;
            case "bamboo":
                property = new Bamboo(contactName, contactEMail, serverID, organisation);
                break;
            case "bitbucket":
                property = new Bitbucket(contactName, contactEMail, serverID, organisation, dataCenter);
                break;
            case "fisheye":
                property = new FishEye(contactName, contactEMail, serverID, organisation);
                break;
            case "crucible":
                property = new Crucible(contactName, contactEMail, serverID, organisation);
                break;
            case "crowd":
                property = new Crowd(contactName, contactEMail, serverID, organisation, dataCenter);
                break;
            case "jc":
                property = new JIRACore(contactName, contactEMail, serverID, organisation);
                break;
            case "portfolio":
                property = new Portfolio(contactName, contactEMail, serverID, organisation, dataCenter);
                break;
            case "jsd":
                property = new JIRAServiceDesk(contactName, contactEMail, serverID, organisation, dataCenter);
                break;
            case "training":
                property = new Training(contactName, contactEMail, serverID, organisation);
                break;
            case "capture":
                property = new Capture(contactName, contactEMail, serverID, organisation);
                break;
            default:
                printUsage();
                return;
        }

        try {
            String licenseCode = Encoder.encode(property);

            System.out.println("Your license code(Don't copy this line!!!): \n");
            System.out.println(licenseCode);
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.flush();
    }

    private static boolean hasOption(CommandLine commandLine, String longOpt, char shortOpt) {
        if (commandLine == null) {
            return false;
        }
        return commandLine.hasOption(longOpt) || commandLine.hasOption(shortOpt);
    }

    private static String optionValue(CommandLine commandLine, String longOpt, char shortOpt) {
        if (commandLine == null) {
            return null;
        }
        String value = commandLine.getOptionValue(longOpt);
        return value != null ? value : commandLine.getOptionValue(String.valueOf(shortOpt));
    }

    private static String systemProperty(String key) {
        return System.getProperty(SYS_PROP_PREFIX + key);
    }

    private static boolean systemPropertyFlag(String key) {
        String value = systemProperty(key);
        return value != null && !value.isEmpty()
                && !"false".equalsIgnoreCase(value) && !"0".equals(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
