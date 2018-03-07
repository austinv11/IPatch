package injectr.ipatch.diff;

import java.io.BufferedReader;
import java.util.List;

public interface DiffGenerator {

    List<StringChange> stringDiff(BufferedReader reader);
}
