package de.versley.exml.service;

import exml.Document;
import exml.io.DocumentWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class EXMLMessageWriter implements MessageBodyWriter<Document<?>> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Document.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(Document<?> document, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Document<?> document, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        try {
            DocumentWriter.writeDocument(document, entityStream);
        } catch (XMLStreamException e) {
            throw new WebApplicationException("Cannot write EXML", e);
        }
    }
}
