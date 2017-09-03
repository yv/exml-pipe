package de.versley.exml.service;

import de.versley.exml.annotators.Annotator;
import de.versley.exml.async.Pipeline;
import de.versley.exml.config.GlobalConfig;
import de.versley.exml.pipe.ExmlDocBuilder;
import exml.tueba.TuebaDocument;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Path("/")
public class PipelineResource {
    GlobalConfig conf;

    public PipelineResource() {
        conf = GlobalConfig.load("exmlpipe_config.yaml");

    }

    /**
     * retrieves the pipeline associated with the pipeline called <i>name</i>
     *
     * @param name designates a pipeline
     * @return empty or the pipeline
     */
    Optional<Pipeline<TuebaDocument>> getPipeline(String name) {
        Optional<List<Annotator>> annotators = conf.createAnnotators(name);
        if (annotators.isPresent()) {
            Pipeline<TuebaDocument> pipeline = new Pipeline<TuebaDocument>();

            for (Annotator anno: conf.createAnnotators()) {
                pipeline.addStage(anno);
            }
            pipeline.loadModels();
            return Optional.of(pipeline);
        } else {
            return Optional.empty();
        }
    }

    @POST
    @Path("/pipe/{name}")
    @Consumes("text/plain")
    @Produces("application/exml+xml")
    public TuebaDocument pipeText(String body, @PathParam("name") String name)
    {
        System.err.println("pipeText:"+name);
        Optional<Pipeline<TuebaDocument>> pipeline = getPipeline(name);
        if (!pipeline.isPresent()) {
            throw new NotFoundException("Pipeline not found");
        }
        ExmlDocBuilder builder = new ExmlDocBuilder("de");
        // TODO detect paragraphs and split on them
        builder.addText(body);
        TuebaDocument doc = builder.getDocument();
        final TuebaDocument[] out = new TuebaDocument[] { null };

        pipeline.get().process(doc, result -> out[0]=result);
        return out[0];
    }

    @POST
    @Path("/pipe/{name}")
    @Consumes("application/exml+xml")
    @Produces("application/exml+xml")
    public TuebaDocument pipeExml(TuebaDocument doc, @PathParam("name") String name)
    {
        System.err.println("pipeExml:"+name);
        Optional<Pipeline<TuebaDocument>> pipeline = getPipeline(name);
        if (!pipeline.isPresent()) {
            throw new NotFoundException("Pipeline not found");
        }
        final TuebaDocument[] out = new TuebaDocument[] { null };
        pipeline.get().process(doc, result -> out[0]=result);
        return out[0];
    }

    @GET
    @Path("/pipe")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> pipeGet() {
        Map<String, Object> result = new HashMap<String, Object>();
        List<String> tokenLangs = new ArrayList<String>();
        tokenLangs.add("de");
        result.put("tokenize", tokenLangs);
        List<PipeInfo> pipelines = new ArrayList<>();
        for (Map.Entry<String,List<Annotator>> pipe: conf.pipelines.entrySet()) {
            pipelines.add(new PipeInfo(pipe.getKey(), pipe.getValue()));
        }
        result.put("pipelines", pipelines);
        return result;
    }

    @GET @Path("/status")
    @Produces("application/json")
    public String status() {
        return "{\"status\":\"ok\"}";
    }

    public static void main(String[] args) {
        final URI BASE_URI = URI.create("http://localhost:8080/");
        try {
            ResourceConfig config = new ResourceConfig(
                    PipelineResource.class, EXMLMessageWriter.class, EXMLMessageReader.class);
            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, config, false);
            Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

            // set up grizzly to produce proper error messages
            Logger l = Logger.getLogger("org.glassfish.grizzly.http.server.HttpHandler");
            l.setLevel(Level.FINE);
            l.setUseParentHandlers(false);
            ConsoleHandler ch = new ConsoleHandler();
            ch.setLevel(Level.ALL);
            l.addHandler(ch);

            server.start();

            System.out.println(String.format("Application started.\nTry out %s\nStop the application using CTRL+C",
                    BASE_URI));
            Thread.currentThread().join();
        } catch (IOException|InterruptedException e) {
            e.printStackTrace();
        }
    }
}