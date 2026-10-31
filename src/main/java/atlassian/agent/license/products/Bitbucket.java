package atlassian.agent.license.products;

import atlassian.agent.license.LicenseProperty;

public class Bitbucket extends LicenseProperty {
    public Bitbucket(String ContactName, String ContactEMail, String ServerID, String Organisation, boolean dataCenter) {
        super(ContactName, ContactEMail, ServerID, Organisation, dataCenter);
    }

    public Bitbucket(String ContactName, String ContactEMail, String ServerID, String Organisation) {
        this(ContactName, ContactEMail, ServerID, Organisation, false);
    }

    @Override
    public String getProductName() {
        return "stash";
    }
}
