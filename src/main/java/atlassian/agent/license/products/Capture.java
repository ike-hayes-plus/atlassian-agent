package atlassian.agent.license.products;

import atlassian.agent.license.LicenseType;

public class Capture extends Plugin {
    public Capture(String ContactName, String ContactEMail, String ServerID, String Organisation) {
        super(ContactName, ContactEMail, ServerID, Organisation);

        setServerID(null);
    }

    @Override
    public String getProductName() {
        return "bonfire";
    }

    @Override
    public void setLicenseType(LicenseType licenseType) {
        data.put("LicenseTypeName", licenseType.toString());
    }
}
