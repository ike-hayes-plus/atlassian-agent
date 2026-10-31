package atlassian.agent.license.products;

import atlassian.agent.license.LicenseProperty;

public class FishEye extends LicenseProperty {
    public FishEye(String ContactName, String ContactEMail, String ServerID, String Organisation) {
        super(ContactName, ContactEMail, ServerID, Organisation);
    }

    @Override
    public String getProductName() {
        return "fisheye";
    }
}
