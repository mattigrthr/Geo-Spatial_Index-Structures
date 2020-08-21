package benchMark;

import common.PublishedMessage;
import common.Subscription;

import java.util.List;

public interface Benchmark {
    /**
     * Write a list of subscriptions to the data store
     * @param subscriptionList
     */
    void writeData(List<Subscription> subscriptionList);

    /**
     * Get matching subscriptions from a list of queries
     * @param messageList
     */
    void queryData(List<PublishedMessage> messageList);
}
