package injectr.ipatch;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import injectr.ipatch.bytecode.ClassFile;
import injectr.ipatch.cli.Args;
import injectr.ipatch.compress.CompressType;
import injectr.ipatch.diff.DiffAlgorithm;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class IPatch {

    public static final String VERSION = "1.0-SNAPSHOT";

    public final Logger LOGGER;
    private final Path base, modifier, output;
    private final boolean patchMode;
    private final CompressType compressType;
    private final DiffAlgorithm diffAlgorithm;

    public IPatch(boolean verbose, Path base, Path modifier, Path output, boolean patchMode, CompressType compressType, DiffAlgorithm diffAlgorithm) {
        LOGGER = new Logger(verbose);
        this.base = base;
        this.modifier = modifier;
        this.patchMode = patchMode;
        this.compressType = compressType;
        this.diffAlgorithm = diffAlgorithm;
        if (output == null) {
            if (patchMode) {
                this.output = modifier.resolveSibling(modifier.getFileName() + ".patch" + compressType.toString());
            } else {
                this.output = modifier;
            }
        } else {
            this.output = output;
        }
    }

    public void run() {
        LOGGER.info("IPatch v%s", VERSION);
        LOGGER.debug("Base: %s", base);
        LOGGER.debug("Modifier: %s", modifier);
        LOGGER.debug("Output: %s", output);
        LOGGER.debug("Patching?: %s", patchMode);
        LOGGER.debug("Compression Type: %s", compressType);
        LOGGER.debug("Diff Algorithm: %s", diffAlgorithm);

        try {
            ClassFile file = ClassFile.readFrom(new FileInputStream("E:\\austi\\Development\\IntelliJ\\IPatch\\out\\production\\classes\\injectr\\ipatch\\Logger.class"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();
        //TODO
    }

    public static void main(String[] argv) {
        Args args = new Args();
        JCommander commander = JCommander.newBuilder()
                .addObject(args)
                .programName(String.format("IPatch v%s", VERSION))
                .build();

        try {
            commander.parse(argv);

            if (args.newFile != null && args.patchFile != null)
                throw new ParameterException("--new-file and --patch-file are mutually exclusive!");
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            commander.usage();
            return;
        }

        if (args.help) {
            commander.usage();
        } else {
            if (args.file.size() != 1) {
                commander.usage();
            } else {
                Path base = args.file.get(0);
                new IPatch(args.verbose,
                        base,
                        args.newFile == null ? args.patchFile : args.newFile,
                        args.outFile,
                        args.newFile == null,
                        args.compressType,
                        args.diffAlgorithm).run();
            }
        }
    }
}
