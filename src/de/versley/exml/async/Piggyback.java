package de.versley.exml.async;

public class Piggyback<E> implements Consumer<E> {
	private Consumer<E> _consumer;
	private Runnable _runnable;
	
	public Piggyback(Consumer<E> cons, Runnable runn) {
		_consumer = cons;
		_runnable = runn;
	}

	@Override
	public void consume(E t) {
		_consumer.consume(t);
		_runnable.run();
	}
}
