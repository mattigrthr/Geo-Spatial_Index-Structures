package benchMark;

import common.PublishedMessage;
import common.Subscription;
import geoBroker.GeoBrokerStorage;
import geoBroker.TransformedSubscription;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs benchmark on BTree with given subscriptions
 */
public class GeoBrokerBenchMark implements Benchmark {
    public GeoBrokerStorage geoBrokerStorage;

    public GeoBrokerBenchMark() {
        this.geoBrokerStorage = new GeoBrokerStorage();
    }

    public void writeData(List<Subscription> subscriptionList) {
        List<TransformedSubscription> transformedSubscriptions = new ArrayList<>();

        subscriptionList.forEach(s -> {
            TransformedSubscription transformedSubscription = new TransformedSubscription(s.id, s.geoFence, s.topic);

            transformedSubscriptions.add(transformedSubscription);
        });

        long startTime = System.currentTimeMillis();

        transformedSubscriptions.forEach(s -> this.geoBrokerStorage.addTransformedSubscription(s.id, s.geoFence, s.topic));

        long stopTime = System.currentTimeMillis();
        long executionTime = stopTime - startTime;

        System.out.println(">>>GeoBroker<<<");
        System.out.println(subscriptionList.size() + " subscriptions added in " + executionTime + " ms");
    }

    public void queryData(List<PublishedMessage> messageList) {
        System.out.println(">>>GeoBroker<<<");
        long startTime = System.currentTimeMillis();

        messageList.forEach(m -> {
            List<String> matchingSubscriptions = this.geoBrokerStorage.getMatchingSubscriptions(m.location, m.topic);

            System.out.println(m + ": " + matchingSubscriptions.size() + " matching subscriptions");
        });

        long stopTime = System.currentTimeMillis();
        long executionTime = stopTime - startTime;

        System.out.println("All queries executed in " + executionTime + " ms");
    }
}
