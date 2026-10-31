package atlassian.agent.license.products;

public class Questions extends Plugin {
    public Questions(String ContactName, String ContactEMail, String ServerID, String Organisation, boolean dataCenter) {
        super(ContactName, ContactEMail, ServerID, Organisation, dataCenter);
    }

    public Questions(String ContactName, String ContactEMail, String ServerID, String Organisation) {
        this(ContactName, ContactEMail, ServerID, Organisation, false);
    }

    @Override
    public String getProductName() {
        return "com.atlassian.confluence.plugins.confluence-questions";
    }
}
