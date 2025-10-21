package com.viaoa.datasource;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class OADataSourceEmptyIterator implements OADataSourceIterator {

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public Object next() throws NoSuchElementException {
		// ?? if no more elements, then this should throw NoSuchElementException
		return null;
	}

	@Override
	public String getQuery() {
		return null;
	}

	@Override
	public String getQuery2() {
		return null;
	}

	@Override
	public void remove() {
		// no-op
	}

	@Override
	public void forEachRemaining(Consumer action) {
		// no-op
	}
}
