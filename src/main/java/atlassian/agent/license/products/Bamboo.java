package atlassian.agent.license.products;

import atlassian.agent.license.LicenseEdition;
import atlassian.agent.license.LicenseProperty;

public class Bamboo extends LicenseProperty {
    public Bamboo(String ContactName, String ContactEMail, String ServerID, String Organisation) {
        super(ContactName, ContactEMail, ServerID, Organisation);

        setLicenseEdition(LicenseEdition.UNLIMITED);
        setNumberOfBambooLocalAgents(-1);
        setNumberOfBambooRemoteAgents(1000);
        setNumberOfBambooPlans(-1);
    }

    @Override
    public String getProductName() {
        return "bamboo";
    }

    @Override
    public void setNumberOfUsers(int numberOfUsers) {
        //
    }

    public void setLicenseEdition(LicenseEdition licenseEdition) {
        data.put(productProperty("LicenseEdition"), licenseEdition.toString());
    }

    public void setNumberOfBambooLocalAgents(int number) {
        data.put(productProperty("NumberOfBambooLocalAgents"), String.valueOf(number));
    }

    public void setNumberOfBambooRemoteAgents(int number) {
        data.put(productProperty("NumberOfBambooRemoteAgents"), String.valueOf(number));
    }

    public void setNumberOfBambooPlans(int number) {
        data.put(productProperty("NumberOfBambooPlans"), String.valueOf(number));
    }
}
