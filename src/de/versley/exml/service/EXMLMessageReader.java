package de.versley.exml.service;

import exml.Document;
import exml.io.DocumentReader;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaNodeMarkable;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class EXMLMessageReader implements MessageBodyReader<Document<?>> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Document.class.isAssignableFrom(type);
    }

    @Override
    public Document<?> readFrom(Class<Document<?>> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        //TODO refactor TuebaDocument API and simplify here
        //TODO support binary formats
        TuebaDocument doc=new TuebaDocument();
        try {
            DocumentReader.readDocument(doc, entityStream);
        } catch (XMLStreamException ex) {
            throw new BadRequestException("Invalid EXML XML");
        }
        doc.nodes.<TuebaNodeMarkable> addToChildList("parent", doc.node_children);
        doc.addToChildList("parent", doc.node_children);
        doc.nodes.sortChildList(doc.node_children);
        return doc;
    }
}
