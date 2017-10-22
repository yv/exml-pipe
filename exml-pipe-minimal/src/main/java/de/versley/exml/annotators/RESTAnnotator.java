package de.versley.exml.annotators;

import de.versley.exml.async.Consumer;
import exml.io.DocumentReader;
import exml.io.DocumentWriter;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaTerminal;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class RESTAnnotator implements Annotator, Serializable {
    public String pipeUrl;

    transient CloseableHttpClient httpClient = HttpClients.createDefault();

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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            DocumentWriter.<TuebaTerminal>writeDocument(input, out);
            //TODO add async processing
            HttpPost post = new HttpPost(pipeUrl);
            post.setEntity(new BasicHttpEntity());
            TuebaDocument result = httpClient.execute(post, response -> {
                HttpEntity response_entity = response.getEntity();
                TuebaDocument doc_out = new TuebaDocument();
                try {
                    DocumentReader.readDocument(doc_out, response_entity.getContent());
                    //TODO check input/output consistency
                    output.consume(doc_out);
                } catch (XMLStreamException ex) {
                    throw new RuntimeException("Cannot write input document", ex);
                }
                return doc_out;
            });
        } catch (XMLStreamException ex) {
            throw new RuntimeException("Cannot write input document", ex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {

    }
}
