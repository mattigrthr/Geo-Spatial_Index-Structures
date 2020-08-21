package common;

import java.util.Arrays;
import java.util.List;

/**
 * Representation of a topic consisting of a list of tokens
 */
public class Topic {
    public List<String> tokens;
    public String tokenString;

    public Topic(List<String> tokens) {
        this.tokens = tokens;

        String tokenString = "";

        for (String token: tokens) {
            tokenString += "/" + token;
        }

        this.tokenString = tokenString.substring(1);
    }

    public static Topic generateRandomTopic() {
        return new Topic(Arrays.asList("sensor", "temperature", "data"));
    }

    /**
     * Check if the message tokens match with the subscription
     * @param subscriptionTokens Tokens the subscriber is interested in
     * @param messageTokens Tokens that are contained in the message
     * @return
     */
    public static boolean validateTopic(List<String> subscriptionTokens, List<String> messageTokens) {
        for (int i = 0; i < subscriptionTokens.size(); i++) {
            if (i == messageTokens.size()) {
                return false;
            }

            if (subscriptionTokens.get(i).equals("#")) {
                return true;
            } else if (subscriptionTokens.get(i).equals("+")) {
                continue;
            } else if (subscriptionTokens.get(i).equals(messageTokens.get(i))) {
                continue;
            }

            return false;
        }

        if (subscriptionTokens.size() < messageTokens.size()) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return this.tokenString;
    }
}
