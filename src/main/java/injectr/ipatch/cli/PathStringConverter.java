package injectr.ipatch.cli;

import com.beust.jcommander.converters.BaseConverter;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathStringConverter extends BaseConverter<Path> {

    public PathStringConverter(String optionName) {
        super(optionName);
    }

    @Override
    public Path convert(String value) {
        return Paths.get(value);
    }
}
