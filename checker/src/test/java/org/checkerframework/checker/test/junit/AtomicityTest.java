package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class AtomicityTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create an AtomicityTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public AtomicityTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.atomicity.AtomicityChecker.class,
                "atomicity",
                "-Anomsgtext", // Disable full error message printing
        	"-Alint=debugSpew");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"atomicity" /*, "all-systems"*/};
    }
}
