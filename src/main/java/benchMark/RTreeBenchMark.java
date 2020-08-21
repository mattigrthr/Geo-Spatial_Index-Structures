package benchMark;

import common.PublishedMessage;
import common.Subscription;
import rTree.RTreeStorage;

import java.util.List;

/**
 * Runs benchmark on RTree with given subscriptions
 */
public class RTreeBenchMark implements Benchmark {
    public RTreeStorage rTreeStorage;

    public RTreeBenchMark() {
        this.rTreeStorage = new RTreeStorage();
    }

    public void writeData(List<Subscription> subscriptionList) {
        long startTime = System.currentTimeMillis();

        subscriptionList.forEach(s -> this.rTreeStorage.addSubscription(s.id, s.geoFence, s.topic));

        long stopTime = System.currentTimeMillis();
        long executionTime = stopTime - startTime;

        System.out.println(">>>RTree<<<");
        System.out.println(subscriptionList.size() + " subscriptions added in " + executionTime + " ms");
    }

    public void queryData(List<PublishedMessage> messageList) {
        System.out.println(">>>RTree<<<");
        long startTime = System.currentTimeMillis();

        messageList.forEach(m -> {
            List<String> matchingSubscriptions = this.rTreeStorage.getMatchingSubscriptions(m.location, m.topic);

            System.out.println(m + ": " + matchingSubscriptions.size() + " matching subscriptions");
        });

        long stopTime = System.currentTimeMillis();
        long executionTime = stopTime - startTime;

        System.out.println("All queries executed in " + executionTime + " ms");
    }
}
