package de.versley.exml.async;

public class NullConsumer<E> implements Consumer<E> {

	@Override
	public void consume(E t) {
		// we do exactly nothing.
	}

}
