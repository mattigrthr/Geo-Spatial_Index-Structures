package geoBroker;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class Id {
    public ImmutablePair<String, Integer> id;

    public Id (String id) {
        String[] idComponents = id.split("_");

        this.id = new ImmutablePair<>(
            idComponents[0],
            idComponents.length > 1 ? Integer.valueOf(idComponents[1]) : 1
        );
    }
}
