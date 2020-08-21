package benchMark;

import common.*;
import geoBroker.GeoBrokerStorage;
import geoBroker.Id;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Class that will start different benchmarkings
 */
public class BenchmarkManager {
    private List<Subscription> subscriptionList;
    private BTreeBenchMark bTreeBenchMark;
    private RTreeBenchMark rTreeBenchMark;
    private GeoBrokerBenchMark geoBrokerBenchMark;
    private static Scanner in;

    public BenchmarkManager() {
        this.subscriptionList = new ArrayList<>();
        this.bTreeBenchMark = new BTreeBenchMark();
        this.rTreeBenchMark = new RTreeBenchMark();
        this.geoBrokerBenchMark = new GeoBrokerBenchMark();
        in = new Scanner (System.in);
    }

    public static void main(String[] args) {
        System.out.println("Benchmark manager started\n");
        BenchmarkManager benchmarkManager = new BenchmarkManager();

        while (true) {
            System.out.println("What do you want to do?");
            System.out.println("[1]: Run full workload\n[2]: Exit");
            int option = in.nextInt();

            switch (option) {
                case 1:
                    benchmarkManager.runFullWorkload();
                    break;
                case 2:
                    System.exit(0);
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }
    }

    private void runFullWorkload() {
        boolean correctInputFile = false;
        String filePath = new File("").getAbsolutePath();
        File folder = new File(filePath + "/data/generatedWorkload/workloads");
        File[] files = folder.listFiles();
        File selectedFile = null;
        SubscriptionStorage storage = null;
        boolean geoBroker = false;
        GeoBrokerStorage geoBrokerStorage = null;

        while (!correctInputFile) {
            System.out.println("Which workload do you want to run?");

            for (int i = 0; i < files.length; i++) {
                System.out.println("[" + (i + 1) + "]: " + files[i].getName());
            }

            int option = in.nextInt();

            if (option > 0 && option < (files.length + 1)) {
                selectedFile = files[option - 1];
                correctInputFile = true;
            } else {
                System.out.println("Wrong input. Please try again!");
            }
        }

        boolean correctInput = false;

        while (!correctInput) {
            System.out.println("On which data index do you want to run the workload?");
            System.out.println("[1]: BTree\n[2]: RTree\n[3]: GeoBroker");
            int option = in.nextInt();

            switch (option) {
                case 1:
                    storage = bTreeBenchMark.bTreeStorage;
                    correctInput = true;
                    break;
                case 2:
                    storage = rTreeBenchMark.rTreeStorage;
                    correctInput = true;
                    break;
                case 3:
                    geoBrokerStorage = geoBrokerBenchMark.geoBrokerStorage;
                    geoBroker = true;
                    correctInput = true;
                    break;
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }

        SubscriptionStorage finalStorage = storage;
        GeoBrokerStorage finalGeoBrokerStorage = geoBrokerStorage;

        List<List<Object>> transformedSubscriptions = transformSubscriptions(geoBroker, selectedFile.getAbsolutePath());

        System.out.println("Transformed operations. Running now on subscription storage.");

        long startTime = System.currentTimeMillis();

        if (!geoBroker) {
            transformedSubscriptions.forEach(operation -> {

                switch ((String) operation.get(0)) {
                    case "A":
                        finalStorage.addSubscription((String) operation.get(1), (GeoFence) operation.get(2), (Topic) operation.get(3));
                        break;
                    case "U":
                        finalStorage.updateSubscription((String) operation.get(1), (GeoFence) operation.get(2), (Topic) operation.get(3));
                        break;
                    case "G":
                        finalStorage.getMatchingSubscriptions((Location) operation.get(1), (Topic) operation.get(2));
                        break;
                }
            });
        } else {
            transformedSubscriptions.forEach(operation -> {
                switch ((String) operation.get(0)) {
                    case "A":
                        finalGeoBrokerStorage.addTransformedSubscription((Id) operation.get(1), (de.hasenburg.geobroker.commons.model.spatial.Geofence) operation.get(2), (de.hasenburg.geobroker.commons.model.message.Topic) operation.get(3));
                        break;
                    case "U":
                        finalGeoBrokerStorage.updateTransformedSubscription((Id) operation.get(1), (de.hasenburg.geobroker.commons.model.spatial.Geofence) operation.get(2), (de.hasenburg.geobroker.commons.model.message.Topic) operation.get(3));
                        break;
                    case "G":
                        finalGeoBrokerStorage.getTransformedMatchingSubscriptions((de.hasenburg.geobroker.commons.model.spatial.Location) operation.get(1), (de.hasenburg.geobroker.commons.model.message.Topic) operation.get(2));
                        break;
                }
            });
        }

        long stopTime = System.currentTimeMillis();
        long executionTime = stopTime - startTime;

        System.out.println("Execution took: " + executionTime + " ms");
    }

    private List<List<Object>> transformSubscriptions(boolean geoBroker, String filePath) {
        List<List<Object>> transformedSubscriptions = new ArrayList<>();

        try {
            Files.lines(Paths.get(filePath)).forEach(operation -> {
                List<Object> transformedComponents = new ArrayList<>();
                String[] operationComponents = operation.split(";");

                switch (operationComponents[0]) {
                    case "A":
                        transformedComponents.add(0, "A");

                        if (geoBroker) {
                            de.hasenburg.geobroker.commons.model.spatial.Location center = new de.hasenburg.geobroker.commons.model.spatial.Location(Double.valueOf(operationComponents[3]), Double.valueOf(operationComponents[4]));
                            transformedComponents.add(1, new Id(operationComponents[1]));
                            transformedComponents.add(2, de.hasenburg.geobroker.commons.model.spatial.Geofence.circle(center,Double.valueOf(operationComponents[5])));
                            transformedComponents.add(3, new de.hasenburg.geobroker.commons.model.message.Topic(operationComponents[2]));
                        } else {
                            transformedComponents.add(1, operationComponents[1]);
                            transformedComponents.add(2, new GeoFence(Double.valueOf(operationComponents[3]), Double.valueOf(operationComponents[4]), Double.valueOf(operationComponents[5])));
                            transformedComponents.add(3, new Topic(Arrays.asList(operationComponents[2].split("/"))));
                        }

                        transformedSubscriptions.add(transformedComponents);
                        break;
                    case "U":
                        transformedComponents.add(0, "U");

                        if (geoBroker) {
                            de.hasenburg.geobroker.commons.model.spatial.Location center = new de.hasenburg.geobroker.commons.model.spatial.Location(Double.valueOf(operationComponents[3]), Double.valueOf(operationComponents[4]));
                            transformedComponents.add(1, new Id(operationComponents[1]));
                            transformedComponents.add(2, de.hasenburg.geobroker.commons.model.spatial.Geofence.circle(center,Double.valueOf(operationComponents[5])));
                            transformedComponents.add(3, new de.hasenburg.geobroker.commons.model.message.Topic(operationComponents[2]));
                        } else {
                            transformedComponents.add(1, operationComponents[1]);
                            transformedComponents.add(2, new GeoFence(Double.valueOf(operationComponents[3]), Double.valueOf(operationComponents[4]), Double.valueOf(operationComponents[5])));
                            transformedComponents.add(3, new Topic(Arrays.asList(operationComponents[2].split("/"))));
                        }

                        transformedSubscriptions.add(transformedComponents);
                        break;
                    case "G":
                        transformedComponents.add(0, "G");

                        if (geoBroker) {
                            transformedComponents.add(1, new de.hasenburg.geobroker.commons.model.spatial.Location(Double.valueOf(operationComponents[2]), Double.valueOf(operationComponents[3])));
                            transformedComponents.add(2, new de.hasenburg.geobroker.commons.model.message.Topic(operationComponents[1]));
                        } else {
                            transformedComponents.add(1, new Location(Double.valueOf(operationComponents[2]), Double.valueOf(operationComponents[3])));
                            transformedComponents.add(2, new Topic(Arrays.asList(operationComponents[1].split("/"))));
                        }

                        transformedSubscriptions.add(transformedComponents);
                        break;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return transformedSubscriptions;
    }

    /**
     * Reads csv data and creates subscriptions
     */
    private void createSubscriptionList() {
        boolean correctInput = false;
        String filePath = new File("").getAbsolutePath();

        while (!correctInput) {
            System.out.println("Which datasource do you want to run the benchmark on?");
            System.out.println("[1]: 20-300\n[2]: 250-300\n[3]: 250-3600\n[4]: 250-86400");

            int option = in.nextInt();


            switch (option) {
                case 1:
                    filePath += "/data/message_schedule_20-300.csv";
                    System.out.println("Selected [1]");
                    correctInput = true;
                    break;
                case 2:
                    filePath += "/data/message_schedule_250-300.csv";
                    System.out.println("Selected [2]");
                    correctInput = true;
                    break;
                case 3:
                    filePath += "/data/message_schedule_250_3600.csv";
                    System.out.println("Selected [3]");
                    correctInput = true;
                    break;
                case 4:
                    filePath += "/data/message_schedule_250-86400.csv";
                    System.out.println("Selected [4]");
                    correctInput = true;
                    break;
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }

        DataParser data_20_300 = new DataParser(filePath);
        List<Message> messageList = null;

        try {
            messageList = data_20_300.parseMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Subscription> subscriptionList = new ArrayList<>();

        for (Message m: messageList) {
            subscriptionList.add(new Subscription(m.thingId + "_" + m.tupleNr, m.lat, m.lng));
        }

        this.subscriptionList = subscriptionList;

        System.out.println("Subscription list created\n");
    }

    /**
     * Write subscriptions to a specific data store
     */
    private void writeToIndex() {
        boolean correctInput = false;

        while (!correctInput) {
            System.out.println("To which index do you want to write the subscriptions?");
            System.out.println("[1]: BTree\n[2]: RTree\n[3]: GeoBroker\n[4]: All\n[5]: Go back");
            int option = in.nextInt();

            switch (option) {
                case 1:
                    bTreeBenchMark.writeData(subscriptionList);
                    correctInput = true;
                    break;
                case 2:
                    rTreeBenchMark.writeData(subscriptionList);
                    correctInput = true;
                    break;
                case 3:
                    geoBrokerBenchMark.writeData(subscriptionList);
                    correctInput = true;
                    break;
                case 4:
                    bTreeBenchMark.writeData(subscriptionList);
                    rTreeBenchMark.writeData(subscriptionList);
                    geoBrokerBenchMark.writeData(subscriptionList);
                    correctInput = true;
                    break;
                case 5:
                    correctInput = true;
                    break;
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }
    }

    private void runQueries() {
        List<PublishedMessage> queries = choseQueries();

        if (queries.size() < 1) {
            return;
        }

        boolean correctInput = false;

        while (!correctInput) {
            System.out.println("On which index do you want to run the queries?");
            System.out.println("[1]: BTree\n[2]: RTree\n[3]: GeoBroker\n[4]: All\n[5]: Go back");
            int option = in.nextInt();

            switch (option) {
                case 1:
                    bTreeBenchMark.queryData(queries);
                    correctInput = true;
                    break;
                case 2:
                    rTreeBenchMark.queryData(queries);
                    correctInput = true;
                    break;
                case 3:
                    geoBrokerBenchMark.queryData(queries);
                    correctInput = true;
                    break;
                case 4:
                    bTreeBenchMark.queryData(queries);
                    rTreeBenchMark.queryData(queries);
                    geoBrokerBenchMark.queryData(queries);
                    correctInput = true;
                    break;
                case 5:
                    correctInput = true;
                    break;
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }
    }

    private List<PublishedMessage> choseQueries() {
        List<PublishedMessage> messageList = new ArrayList<>();
        boolean correctInput = false;

        while (!correctInput) {
            System.out.println("How many messages do you want to send?");
            System.out.println("[1]: 1 message\n[2]: 10 messages\n[3]: 100 messages\n[4]: Go back");
            int option = in.nextInt();

            switch (option) {
                case 1:
                    subscriptionList.stream().limit(1).forEach(s -> {
                        double lat = s.geoFence.center.location.getLat();
                        double lng = s.geoFence.center.location.getLon();
                        messageList.add(new PublishedMessage(lat, lng));
                    });
                    correctInput = true;
                    break;
                case 2:
                    subscriptionList.stream().limit(10).forEach(s -> {
                        double lat = s.geoFence.center.location.getLat();
                        double lng = s.geoFence.center.location.getLon();
                        messageList.add(new PublishedMessage(lat, lng));
                    });
                    correctInput = true;
                    break;
                case 3:
                    subscriptionList.stream().limit(100).forEach(s -> {
                        double lat = s.geoFence.center.location.getLat();
                        double lng = s.geoFence.center.location.getLon();
                        messageList.add(new PublishedMessage(lat, lng));
                    });
                    correctInput = true;
                    break;
                case 4:
                    correctInput = true;
                    break;
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }

        return messageList;
    }
}