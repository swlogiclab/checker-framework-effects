package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class GenericEffectsTest extends CheckerFrameworkPerDirectoryTest {

    public GenericEffectsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.genericeffects.GenericEffectChecker.class,
                "genericeffects",
                "-Anomsgtext");
        // , "-Alint=debugSpew");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"genericeffects"}; // TODO: re-enable, "all-systems"};
    }
}
