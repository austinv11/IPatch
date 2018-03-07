package injectr.ipatch.cli;

import com.beust.jcommander.Parameter;
import injectr.ipatch.compress.CompressType;
import injectr.ipatch.diff.DiffAlgorithm;

import java.nio.file.Path;
import java.util.List;

public class Args {

    @Parameter(required = true, arity = 1, converter = PathStringConverter.class, validateValueWith = PathValidator.class,
            description = "[The file to interact with]")
    public List<Path> file;

    @Parameter(converter = PathStringConverter.class, validateValueWith = PathValidator.class, arity = 1,
            names = {"-p", "--patch-file"},
            description = "The patch file to use in order to patch the passed file. This is mutually exclusive with --new-file!")
    public Path patchFile;

    @Parameter(converter = PathStringConverter.class, validateValueWith = PathValidator.class, arity = 1,
            names = {"-n", "--new-file"},
            description = "The new file to generate the patch file to convert the old file from. This is mutually exclusive with --patch-file!")
    public Path newFile;

    @Parameter(converter = PathStringConverter.class, arity = 1, names = {"-o", "--out"},
            description = "The output file path.")
    public Path outFile;

    @Parameter(names = {"-c", "--compress"}, description = "Sets the compression of the output file.")
    public CompressType compressType = CompressType.NONE;

    @Parameter(names = {"-d", "--diff-type"}, description = "The algorithm for diff generation.")
    public DiffAlgorithm diffAlgorithm = DiffAlgorithm.DYNAMIC;

    @Parameter(names = {"-v", "--verbose"}, description = "Prints logs to the console.")
    public boolean verbose = false;

    @Parameter(names = {"-h", "--help"}, help = true, description = "Displays help.")
    public boolean help = false;
}
