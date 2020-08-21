package benchMark;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.locationtech.spatial4j.distance.DistanceUtils.KM_TO_DEG;

public class WorkLoadGenerator {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        
        while (true) {
            System.out.println("What do you want to do?");
            System.out.println("[1]: Create topics\n[2]: Create subscriptions\n[3]: Generate workload\n[4]: Exit");
            int option = in.nextInt();

            switch (option) {
                case 1:
                    WorkLoadGenerator.createTopics();
                    System.out.println("Created topics successfully");
                    break;
                case 2:
                    WorkLoadGenerator.createSubscriptions();
                    System.out.println("Created subscriptions successfully");
                    break;
                case 3:
                    WorkLoadGenerator.createWorkload();
                    System.out.println("Created workload successfully");
                    break;
                case 4:
                    System.exit(0);
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }
    }
    
    private static void createTopics() {
        Scanner in = new Scanner(System.in);
        int numberOfTopics = 0;
        int numberOfTokens = 0;
        int tokenLength = 0;

        boolean correctInputSubscriptionNumber = false;

        while (!correctInputSubscriptionNumber) {
            System.out.println("How many topics do you want to create?");
            System.out.println("[1]: 50\n[2]: 100\n[3]: 1000");

            int option = in.nextInt();

            switch (option) {
                case 1:
                    numberOfTopics = 50;
                    correctInputSubscriptionNumber = true;
                    break;
                case 2:
                    numberOfTopics = 100;
                    correctInputSubscriptionNumber = true;
                    break;
                case 3:
                    numberOfTopics = 1000;
                    correctInputSubscriptionNumber = true;
                    break;
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }

        boolean correctInputTokenNumber = false;

        while (!correctInputTokenNumber) {
            System.out.println("How many tokens should a topic have?");
            System.out.println("[1]: 5\n[2]: 10\n[3]: 20");

            int option = in.nextInt();

            switch (option) {
                case 1:
                    numberOfTokens = 5;
                    correctInputTokenNumber = true;
                    break;
                case 2:
                    numberOfTokens = 10;
                    correctInputTokenNumber = true;
                    break;
                case 3:
                    numberOfTokens = 20;
                    correctInputTokenNumber = true;
                    break;
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }

        WorkLoadGenerator.generateTopics(numberOfTopics, numberOfTokens);
    }

    private static void generateTopics(int numberOfTopics, int numberOfTokens) {
        String filePath = new File("").getAbsolutePath();
        String fileName = numberOfTopics + "-topics_" + numberOfTokens + "-tokens";

        try {
            Stream<String> tokenStream = Files.lines(Paths.get(filePath + "/data/generatedWorkload/tokens/10-tokens.csv"));
            List<String> tokens = tokenStream.collect(Collectors.toList());
            FileWriter fileWriter = new FileWriter(new File(filePath + "/data/generatedWorkload/topics", fileName + ".csv"));

            for (int i = 0; i < numberOfTopics; i++) {
                String topic = "";
                boolean terminatingWildcard = false;

                for (int j = 0; j < numberOfTokens; j++) {
                    if (!terminatingWildcard) {
                        String token;

                        double wildCardDecider = Math.random();

                        /**
                         * 10 % wildcards
                         */
                        if (wildCardDecider > 0.9) {
                            double specificWildCardDecider = Math.random();

                            /**
                             * 90 % of wildcards are '+'
                             * 10 % of wildcards are '#'
                             */
                            if (specificWildCardDecider < 0.9) {
                                token = "+";
                            } else {
                                token = "#";
                                terminatingWildcard = true;
                            }
                        } else {
                            Random rnd = new Random();

                            token = tokens.get(rnd.nextInt(tokens.size() - 1));
                        }

                        topic += token;

                        if ((j < (numberOfTokens - 1)) && !terminatingWildcard) {
                            topic += "/";
                        }
                    }
                }

                fileWriter.append(topic + "\n");
            }

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createSubscriptions() {
        Scanner in = new Scanner(System.in);
        int numberOfSubscriptions = 0;

        boolean correctInputNumber = false;

        while (!correctInputNumber) {
            System.out.println("How many subscriptions do you want to create");
            System.out.println("[1]: 1.000\n[2]: 50.000\n[3]: 100.000\n[4]: 1.000.000\n[5]: 10.000.000");

            int option = in.nextInt();

            switch (option) {
                case 1:
                    numberOfSubscriptions = 1000;
                    correctInputNumber = true;
                    break;
                case 2:
                    numberOfSubscriptions = 50000;
                    correctInputNumber = true;
                    break;
                case 3:
                    numberOfSubscriptions = 100000;
                    correctInputNumber = true;
                    break;
                case 4:
                    numberOfSubscriptions = 1000000;
                    correctInputNumber = true;
                    break;
                case 5:
                    numberOfSubscriptions = 1000000000;
                    correctInputNumber = true;
                    break;
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }

        boolean correctInputFile = false;
        String filePath = new File("").getAbsolutePath();
        File folder = new File(filePath + "/data/generatedWorkload/topics");
        File[] files = folder.listFiles();
        File selectedFile = null;

        while (!correctInputFile) {
            System.out.println("On which topic file do you want to base your subscriptions?");

            for (int i = 0; i < files.length; i++) {
                System.out.println("[" + i + "]: " + files[i].getName());
            }

            int option = in.nextInt();

            if (option < files.length) {
                selectedFile = files[option];
                correctInputFile = true;
            } else {
                System.out.println("Wrong input. Please try again!");
            }
        }

        WorkLoadGenerator.generateSubscriptions(numberOfSubscriptions, selectedFile);
    }

    private static void generateSubscriptions(int numberOfSubscriptions, File topics) {
        String filePath = new File("").getAbsolutePath();
        String fileName = numberOfSubscriptions + "-subscriptions_based-on-" + topics.getName();
        List<String> topicStrings;
        Random rnd = new Random();

        try {
            FileWriter fileWriter = new FileWriter(new File(filePath + "/data/generatedWorkload/subscriptions", fileName));
            Stream<String> fileStream = Files.lines(Paths.get(topics.getAbsolutePath()));

            topicStrings = fileStream.collect(Collectors.toList());

            for (int i = 0; i < numberOfSubscriptions; i++) {
                String subscription = "s" + (i + 1) + ";";
                subscription += (topicStrings.get(rnd.nextInt(topicStrings.size() - 1)) + ";");
                subscription += (Double.parseDouble(String.format("%.5f", (47 + 8 * rnd.nextDouble()))) + ";");
                subscription += (Double.parseDouble(String.format("%.5f", (5 + 11 * rnd.nextDouble()))) + ";");

                double radiusProb = rnd.nextDouble();

                if (radiusProb > 0.8) {
                    subscription += Double.parseDouble(String.format("%.5f", (50 * KM_TO_DEG)));
                } else if (radiusProb > 0.6) {
                    subscription += Double.parseDouble(String.format("%.5f", (30 * KM_TO_DEG)));
                } else if (radiusProb > 0.3) {
                    subscription += Double.parseDouble(String.format("%.5f", (10 * KM_TO_DEG)));
                } else {
                    subscription += Double.parseDouble(String.format("%.5f", (1 * KM_TO_DEG)));
                }

                fileWriter.append(subscription + "\n");
            }

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createWorkload() {
        Scanner in = new Scanner(System.in);
        int numberOfOperations = 0;
        int updatePercentage = 0;

        boolean correctOperationsNumber = false;

        while (!correctOperationsNumber) {
            System.out.println("How many operations do you want to create?");
            System.out.println("[1]: 100.000\n[2]: 250.000\n[3]: 500.000\n[4]: 1.000.000\n[5]: 10.000.000\n[6]: 50.000.000");

            int option = in.nextInt();

            switch (option) {
                case 1:
                    numberOfOperations = 100000;
                    correctOperationsNumber = true;
                    break;
                case 2:
                    numberOfOperations = 250000;
                    correctOperationsNumber = true;
                    break;
                case 3:
                    numberOfOperations = 500000;
                    correctOperationsNumber = true;
                    break;
                case 4:
                    numberOfOperations = 1000000;
                    correctOperationsNumber = true;
                    break;
                case 5:
                    numberOfOperations = 10000000;
                    correctOperationsNumber = true;
                    break;
                case 6:
                    numberOfOperations = 50000000;
                    correctOperationsNumber = true;
                    break;
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }

        boolean correctUpdateNumber = false;

        while (!correctUpdateNumber) {
            System.out.println("How high is the share of update operations?");
            System.out.println("[1]: 0 %\n[2]: 20 %\n[3]: 40 %\n[4]: 60 %\n[5]: 80 %");

            int option = in.nextInt();

            switch (option) {
                case 1:
                    updatePercentage = 0;
                    correctUpdateNumber = true;
                    break;
                case 2:
                    updatePercentage = 20;
                    correctUpdateNumber = true;
                    break;
                case 3:
                    updatePercentage = 40;
                    correctUpdateNumber = true;
                    break;
                case 4:
                    updatePercentage = 60;
                    correctUpdateNumber = true;
                    break;
                case 5:
                    updatePercentage = 80;
                    correctUpdateNumber = true;
                    break;
                default:
                    System.out.println("Wrong input. Please try again!");
            }
        }

        boolean correctInputFile = false;
        String filePath = new File("").getAbsolutePath();
        File folder = new File(filePath + "/data/generatedWorkload/subscriptions");
        File[] files = folder.listFiles();
        File selectedFile = null;

        while (!correctInputFile) {
            System.out.println("On which subscription file do you want to base your workload?");

            for (int i = 0; i < files.length; i++) {
                System.out.println("[" + i + "]: " + files[i].getName());
            }

            int option = in.nextInt();

            if (option < files.length) {
                selectedFile = files[option];
                correctInputFile = true;
            } else {
                System.out.println("Wrong input. Please try again!");
            }
        }

        WorkLoadGenerator.generateWorkload(selectedFile, numberOfOperations, updatePercentage);
    }

    private static void generateWorkload(File subscriptions, int numberOfOperations, int updatePercentage) {
        String filePath = new File("").getAbsolutePath();
        String fileName = numberOfOperations + "-operations_with-" + updatePercentage + "%-updates" + "_based-on-" + subscriptions.getName();
        List<String> topicStrings;
        List<String> subscriptionStrings;
        int numUpdate = 0;
        Random rnd = new Random();


        try {
            Stream<String> tokenStream = Files.lines(Paths.get(filePath + "/data/generatedWorkload/tokens/10-tokens.csv"));
            List<String> tokens = tokenStream.collect(Collectors.toList());
            FileWriter fileWriter = new FileWriter(new File(filePath + "/data/generatedWorkload/workloads", fileName));
            Stream<String> topicStream = Files.lines(Paths.get(filePath + "/data/generatedWorkload/topics/" + subscriptions.getName().split("based-on-")[1]));
            Stream<String> subcriptionStream = Files.lines(Paths.get(subscriptions.getAbsolutePath()));

            topicStrings = topicStream.collect(Collectors.toList());
            subscriptionStrings = subcriptionStream.collect(Collectors.toList());

            for (String subscription: subscriptionStrings) {
                fileWriter.append("A;" + subscription + "\n");
            }

            for (int i = 0; i < numberOfOperations - subscriptionStrings.size(); i++) {
                double operationDecider = rnd.nextDouble();

                if ((operationDecider > (1 - 0.01 * updatePercentage)) && ((numUpdate / (numberOfOperations - subscriptionStrings.size())) <= 0.01 * updatePercentage)) {
                    String[] updatedComponents = subscriptionStrings.get(rnd.nextInt(subscriptionStrings.size())).split(";");
                    double updatedLng = Double.parseDouble(String.format("%.5f", Double.valueOf(updatedComponents[updatedComponents.length - 2]) + (rnd.nextBoolean() ? 1 : -1) * Double.valueOf(updatedComponents[updatedComponents.length - 2]) * rnd.nextDouble() * 0.5));
                    double updatedLat = Double.parseDouble(String.format("%.5f", Double.valueOf(updatedComponents[updatedComponents.length - 3]) + (rnd.nextBoolean() ? 1 : -1) * Double.valueOf(updatedComponents[updatedComponents.length - 3]) * rnd.nextDouble() * 0.5));
                    updatedComponents[updatedComponents.length - 2] = String.valueOf(updatedLng);
                    updatedComponents[updatedComponents.length - 3] = String.valueOf(updatedLat);

                    String updateOperation = "U";

                    for (String component: updatedComponents) {
                        updateOperation += ";" + component;
                    }

                    fileWriter.append(updateOperation + "\n");
                    numUpdate++;
                } else  {
                    String getOperation = "G;" + generateMessage(tokens, topicStrings);
                    getOperation += (";" + Double.parseDouble(String.format("%.5f", (47 + 8 * rnd.nextDouble()))));
                    getOperation += (";" + Double.parseDouble(String.format("%.5f", (5 + 11 * rnd.nextDouble()))));

                    fileWriter.append(getOperation + "\n");
                }
            }

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String generateMessage(List<String> tokens, List<String> topicStrings) {
        Random rnd = new Random();
        String rndTopic = topicStrings.get(rnd.nextInt(topicStrings.size() - 1));
        String[] topicTokens = rndTopic.split("/");
        String message = "";

        for (String topicToken: topicTokens) {
            if (!topicToken.equals("+") && !topicToken.equals("#")) {
                message += ("/" + topicToken);
            } else {
                message += ("/" + tokens.get(rnd.nextInt(tokens.size() - 1)));
            }
        }

        return message.substring(1);
    }
}
