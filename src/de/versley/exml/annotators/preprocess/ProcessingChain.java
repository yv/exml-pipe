package de.versley.exml.annotators.preprocess;

import java.util.List;

import de.versley.exml.async.Consumer;

/**
 * utility class that arranges multiple LineProcessor instances
 * into a pipeline.
 * @author yannick
 *
 */
public class ProcessingChain implements LineProcessor {
	public List<LineProcessor> stages;
	
	public class Stage implements Consumer<String> {
		private int _stage;
		private Consumer<String> _and_then;
		public Stage(int st, Consumer<String> th) {
			_stage = st;
			_and_then = th;
		}

		@Override
		public void consume(String line) {
			if (_stage == stages.size()) {
				_and_then.consume(line);
			} else {
				stages.get(_stage).process(line,
						new Stage(_stage+1, _and_then));
			}
		}
	}

	@Override
	public void loadModels() {
		for (LineProcessor lp: stages) {
			lp.loadModels();
		}
		
	}

	@Override
	public void process(String input, Consumer<String> and_then) {
		new Stage(0, and_then).consume(input);
	}

	@Override
	public void close() {
		for (LineProcessor lp: stages) {
			//System.err.println("closing:"+lp.toString());
			lp.close();
		}
	}
}
