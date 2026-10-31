package atlassian.agent.license.products;

import atlassian.agent.license.LicenseProperty;

public class Confluence extends LicenseProperty {
    public Confluence(String ContactName, String ContactEMail, String ServerID, String Organisation, boolean dataCenter) {
        super(ContactName, ContactEMail, ServerID, Organisation, dataCenter);
        // Do not set NumberOfClusterNodes: non-zero max nodes triggers LegacyClusterLicenseValidator on Confluence 11.
    }

    public Confluence(String ContactName, String ContactEMail, String ServerID, String Organisation) {
        this(ContactName, ContactEMail, ServerID, Organisation, false);
    }

    @Override
    public String getProductName() {
        return "conf";
    }
}
