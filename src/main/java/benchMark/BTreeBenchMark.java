package benchMark;

import bTree.BTreeStorage;
import common.PublishedMessage;
import common.Subscription;

import java.util.List;

/**
 * Runs benchmark on BTree with given subscriptions
 */
public class BTreeBenchMark implements Benchmark {
    public BTreeStorage bTreeStorage;

    public BTreeBenchMark() {
        this.bTreeStorage = new BTreeStorage();
    }

    public void writeData(List<Subscription> subscriptionList) {
        long startTime = System.currentTimeMillis();

        subscriptionList.forEach(s -> this.bTreeStorage.addSubscription(s.id, s.geoFence, s.topic));

        long stopTime = System.currentTimeMillis();
        long executionTime = stopTime - startTime;

        System.out.println(">>>BTree<<<");
        System.out.println(subscriptionList.size() + " subscriptions added in " + executionTime + " ms");
    }

    public void queryData(List<PublishedMessage> messageList) {
        System.out.println(">>>BTree<<<");
        long startTime = System.currentTimeMillis();

        messageList.forEach(m -> {
            List<String> matchingSubscriptions = this.bTreeStorage.getMatchingSubscriptions(m.location, m.topic);

            System.out.println(m + ": " + matchingSubscriptions.size() + " matching subscriptions");
        });

        long stopTime = System.currentTimeMillis();
        long executionTime = stopTime - startTime;

        System.out.println("All queries executed in " + executionTime + " ms");
    }
}
