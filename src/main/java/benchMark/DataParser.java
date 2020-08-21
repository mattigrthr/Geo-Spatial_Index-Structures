package benchMark;

import common.Message;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Transforms csv entries to message object
 */
public class DataParser {
    private String path;

    public DataParser (String filePath) {
        path = filePath;
    }

    /**
     * Read lines of csv file and transform them into message objects
     * @return
     * @throws IOException
     */
    public List<Message> parseMessages() throws IOException {
        List<Message> messages = new ArrayList<Message>();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;

        while ((line = br.readLine()) != null) {
            String[] values = line.split(";");

            Message message = new Message(values[0], values[1], values[2], values[3], values[4]);
            messages.add(message);
        }

        return messages;
    }
}
