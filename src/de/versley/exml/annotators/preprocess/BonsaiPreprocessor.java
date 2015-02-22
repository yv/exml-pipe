package de.versley.exml.annotators.preprocess;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import de.versley.exml.config.FileReference;

public class BonsaiPreprocessor extends ProcessingChain {
	public FileReference bonsaiDir;
	
	public void loadModels() {
		if (stages == null) {
			stages = new ArrayList<LineProcessor>();
			stages.add(new LineBasedWrapper(Arrays.asList(
					"python", "-u",
					new File(bonsaiDir.toFile(), "src/do_desinflect.py").toString(),
					"--serializedlex", "--inputformat", "tok",
					new File(bonsaiDir.toFile(), "resources/lefff/lefff").toString())));
			stages.add(new LineBasedWrapper(Arrays.asList(
					"python", "-u",
					new File(bonsaiDir.toFile(), "src/do_substitute_tokens.py").toString(),
					"--inputformat", "tok", "--ldelim", "-K", "--rdelim", "K-",
					new File(bonsaiDir.toFile(), "resources/clusters/EP.tcs.dfl-c1000-min20-v2.utf8").toString())));
		}
		super.loadModels();
	}
	
	public static void main(String[] args) {
		String mybonsaiDir = "/home/yannick/sources/bonsai_v3.2";
		BonsaiPreprocessor proc = new BonsaiPreprocessor();
		proc.bonsaiDir = new FileReference(mybonsaiDir);
		proc.loadModels();
		for (String s: new String[]{"Salut , tout le monde", "Le bonsai n' est pas un épice ."}) {
			final String ss = s;
			proc.preprocess_line(s, new LineConsumer() {
				@Override
				public void consume(String line) {
					System.out.println("Input: "+ss);
					System.out.println("Output:"+line);
				}
			});
		}
		proc.close();
	}
}
