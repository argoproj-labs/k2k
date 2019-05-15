package com.intuit.dev.patterns.yaml2kpatch;

import java.io.FileNotFoundException;

import org.junit.Test;

import com.intuit.dev.patterns.yaml2kpatch.Main;

public class MainTest {
    private static final String KSONNET_MANIFEST = MainTest.class.getClassLoader()
            .getResource("ksonet-default-manifest.yaml").getPath();
    private static final String KUSTOMIZE_MANIFEST = MainTest.class.getClassLoader()
            .getResource("kustomize-default-manifest.yaml").getPath();

    @Test(expected = NullPointerException.class)
    public void incorrectArgs1() throws Exception {
        Main.main(null);
    }

    @Test(expected = Exception.class)
    public void incorrectArgs2() throws Exception {
        Main.main(new String[] { null });
    }

    @Test(expected = Exception.class)
    public void incorrectArgs3() throws Exception {
        Main.main(new String[] {});
    }

    @Test(expected = Exception.class)
    public void incorrectArgs4() throws Exception {
        Main.main(new String[] { "file1" });
    }

    @Test(expected = Exception.class)
    public void incorrectArgs5() throws Exception {
        Main.main(new String[] { "file1", "file2", "file3" });
    }

    @Test(expected = FileNotFoundException.class)
    public void incorrectArgs6() throws Exception {
        Main.main(new String[] { "file1", "file2" });
    }

    @Test
    public void correctArgs() throws Exception {
        Main.main(new String[] { KUSTOMIZE_MANIFEST, KSONNET_MANIFEST });
    }

    @Test
    public void correctArgs2() throws Exception {
        Main.main(new String[] { MainTest.class.getClassLoader().getResource("original.yaml").getPath(),
                MainTest.class.getClassLoader().getResource("target.yaml").getPath() });
    }
}
