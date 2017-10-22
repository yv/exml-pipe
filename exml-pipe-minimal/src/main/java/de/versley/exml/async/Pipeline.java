package de.versley.exml.async;

import java.util.ArrayList;
import java.util.List;

public class Pipeline<E> implements Channel<E,E>, Consumer<E> {
	public List<Channel<E,E>> stages = new ArrayList<Channel<E,E>>();
	
	public void addStage(Channel<E,E> stage) {
		stages.add(stage);
	}
	
	public class Stage implements Consumer<E> {
		private int _stage;
		private Consumer<E> _and_then;
		public Stage(int st, Consumer<E> th) {
			_stage = st;
			_and_then = th;
		}
		
		@Override
		public void consume(E line) {
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
		for (Channel<E,E> stage: stages) {
			stage.loadModels();
		}
	}

	@Override
	public void process(E input, Consumer<E> output) {
		new Stage(0, output).consume(input);
	}

	@Override
	public void close() {
		for (Channel<E,E> stage: stages) {
			stage.close();
		}
	}

	@Override
	public void consume(E input) {
		new Stage(0, null).consume(input);
	}
}