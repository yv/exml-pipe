package de.versley.exml.config;

import de.versley.exml.annotators.Annotator;
import de.versley.exml.annotators.MATEAnnotator;
import de.versley.exml.util.StreamUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestFileref {

    @Test
    public void testFileref() throws IOException {
        InputStream example_in = StreamUtils.openResource(getClass(), "example_config.yaml");
        GlobalConfig conf = GlobalConfig.load(example_in, "example_config.yaml");
        conf.modelDir = "/different/path";
        List<Annotator> annotators = conf.createAnnotators();
        MATEAnnotator anno = (MATEAnnotator)annotators.get(0);
        assertEquals(anno.lemma_fname.toPath(), "/different/path/mate_models/lemma-ger-3.6.model");
    }
}
