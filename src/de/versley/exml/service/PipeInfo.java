package de.versley.exml.service;


import de.versley.exml.annotators.Annotator;

import java.util.ArrayList;
import java.util.List;

public class PipeInfo {
    public final String name;
    public final String language;
    public List<String> components;

    public PipeInfo(String name, List<Annotator> annotators) {
        this.name = name;
        if (name.startsWith("de.")) {
            this.language = "de";
        } else if (name.startsWith("en.")) {
            this.language = "en";
        } else if (name.startsWith("fr.")) {
            this.language = "fr";
        } else {
            this.language = "null";
        }
        components = new ArrayList<>(annotators.size());
        for (Annotator anno: annotators) {
            components.add(anno.getClass().getSimpleName());
        }
    }
}
