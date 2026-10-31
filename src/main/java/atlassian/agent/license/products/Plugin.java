package atlassian.agent.license.products;

import atlassian.agent.license.LicenseProperty;

abstract public class Plugin extends LicenseProperty {
    Plugin(String ContactName, String ContactEMail, String ServerID, String Organisation, boolean dataCenter) {
        super(ContactName, ContactEMail, ServerID, Organisation, dataCenter);

        setLicenseID(null);
        setEnterprise(true);
    }

    Plugin(String ContactName, String ContactEMail, String ServerID, String Organisation) {
        this(ContactName, ContactEMail, ServerID, Organisation, false);
    }

    @Override
    public void setNumberOfUsers(int numberOfUsers) {
        data.put("NumberOfUsers", String.valueOf(numberOfUsers));
    }

    public void setEnterprise(boolean enterprise) {
        data.put(productProperty("enterprise"), String.valueOf(enterprise));
    }
}
