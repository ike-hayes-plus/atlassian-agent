package atlassian.agent.agent;

import java.lang.instrument.Instrumentation;

public class Agent {
    public static void premain(String args, Instrumentation inst) {
        try {
            inst.addTransformer(new KeyTransformer());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
