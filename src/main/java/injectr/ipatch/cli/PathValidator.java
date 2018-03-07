package injectr.ipatch.cli;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

import java.nio.file.Path;

public class PathValidator implements IValueValidator<Path> {

    @Override
    public void validate(String name, Path value) throws ParameterException {
        if (!value.toFile().exists())
            throw new ParameterException(String.format("%s does not exist!", value));
    }
}
