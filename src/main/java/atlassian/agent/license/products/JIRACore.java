package atlassian.agent.license.products;

import atlassian.agent.license.LicenseEdition;
import atlassian.agent.license.LicenseProperty;
import atlassian.agent.license.LicenseType;

public class JIRACore extends LicenseProperty {
    public JIRACore(String ContactName, String ContactEMail, String ServerID, String Organisation) {
        super(ContactName, ContactEMail, ServerID, Organisation);

        setLicenseEdition(LicenseEdition.UNLIMITED);
    }

    @Override
    public String getProductName() {
        return "jira.product.jira-core";
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        data.put("jira.active", String.valueOf(active));
    }

    @Override
    public void setNumberOfUsers(int numberOfUsers) {
        super.setNumberOfUsers(numberOfUsers);
        data.put("jira.NumberOfUsers", String.valueOf(numberOfUsers));
        data.put("NumberOfUsers", String.valueOf(numberOfUsers));
    }

    @Override
    public void setLicenseType(LicenseType licenseType) {
        data.put("LicenseTypeName", licenseType.toString());
        data.put("jira.LicenseTypeName", licenseType.toString());
    }

    public void setLicenseEdition(LicenseEdition licenseEdition) {
        data.put("jira.LicenseEdition", licenseEdition.toString());
    }
}
