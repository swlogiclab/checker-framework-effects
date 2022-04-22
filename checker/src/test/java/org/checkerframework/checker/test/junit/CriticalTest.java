package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class CriticalTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create an CriticalTest
   *
   * @parm testFiles the files containing test code, which will be type-checked
   */
  public CriticalTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.critical.CriticalChecker.class,
        "critical",
        "-Anomsgtext"); // Disable full error message printing
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"critical"};
  }
}
