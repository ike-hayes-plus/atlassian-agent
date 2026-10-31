package atlassian.agent.license.products;

public class Portfolio extends Plugin {
    public Portfolio(String ContactName, String ContactEMail, String ServerID, String Organisation, boolean dataCenter) {
        super(ContactName, ContactEMail, ServerID, Organisation, dataCenter);
    }

    public Portfolio(String ContactName, String ContactEMail, String ServerID, String Organisation) {
        this(ContactName, ContactEMail, ServerID, Organisation, false);
    }

    @Override
    public String getProductName() {
        return "com.radiantminds.roadmaps-jira";
    }
}
