package de.versley.exml.config;

import de.versley.exml.annotators.Annotator;
import de.versley.exml.util.StreamUtils;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.*;
import java.util.List;

/**
 * Tests useful properties of GlobalConfig and (empty) Annotator objects
 */
public class TestConfig {

    public static <T extends Object> T doRoundtrip(T val) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objout = new ObjectOutputStream(out);
        objout.writeObject(val);
        objout.close();
        out.close();
        byte[] data = out.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream objin = new ObjectInputStream(in);
        T val_after = (T)objin.readObject();
        return val_after;
    }

    @Test
    public void testSerializationBasic() throws IOException, ClassNotFoundException {
        GlobalConfig conf = GlobalConfig.fromDefaults();
        doRoundtrip(conf);
    }

    @Test
    public void testSerializationFile() throws IOException, ClassNotFoundException {
        InputStream example_in = StreamUtils.openResource(getClass(), "example_config.yaml");
        GlobalConfig conf = GlobalConfig.load(example_in, "example_config.yaml");
        GlobalConfig conf2 = doRoundtrip(conf);
        assertEquals(conf.language, conf2.language);
        assertEquals(conf.default_pipeline, conf2.default_pipeline);
        // List<Annotator> annotators = conf2.pipelines.default_pipeline()
        List<Annotator> annotators = conf2.pipelines.get("de.corenlp");
        List<Annotator> annotators2 = doRoundtrip(annotators);
        assertEquals(annotators.size(), annotators2.size());
    }
}
