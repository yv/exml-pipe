package de.versley.exml.annotators;

import de.versley.exml.async.Consumer;
import de.versley.exml.service.EXMLMessageReader;
import de.versley.exml.service.EXMLMessageWriter;
import exml.tueba.TuebaDocument;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Response;
import java.io.Serializable;

public class JerseyAnnotator implements Annotator, Serializable {
    public String pipeUrl;

    private static ClientConfig clientConfig = new ClientConfig();
    static {
        clientConfig.register(EXMLMessageWriter.class);
        clientConfig.register(EXMLMessageReader.class);
    }
    transient private Client client = ClientBuilder.newClient(clientConfig);

    @Override
    public void annotate(TuebaDocument doc) {
        throw new RuntimeException("annotate() called by itself");
    }

    @Override
    public void loadModels() {
        // not much to do.
    }

    @Override
    public void process(TuebaDocument input, Consumer<TuebaDocument> output) {
        WebTarget target = client.target(pipeUrl);
        System.err.println("REST invocation:"+pipeUrl);
        Response resp = target.request()
                .accept("application/exml+xml")
                .buildPost(Entity.entity(input, "application/exml+xml"))
                .invoke();
        if (resp.getStatus() != 200) {
            throw new RuntimeException("Wrong response:"+resp.getStatus());
        }
        output.consume(resp.readEntity(TuebaDocument.class));
    }

    @Override
    public void close() {

    }
}
