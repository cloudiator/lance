package de.uniulm.omi.cloudiator.lance.lifecycles;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ResponseHistory<T> {

	private volatile int count = 0;
	private final List<T> responses; 
	
	private ResponseHistory(T[] responses) {
		this.responses = responses == null ? null : Arrays.asList(responses);
	}
	
	public T getNext() {
		count++;
		if(responses == null) 
			return null;
		if(count > responses.size()) {
			return responses.get(responses.size() - 1);
		} 
		return responses.get(count - 1);
	}
	
	public int getCount() {
		return count;
	}
	
	public static ResponseHistory<String> forStringMethods(String[] values) {
		if(values == null || values.length == 0) {
			throw new IllegalStateException();
		}
		return new ResponseHistory<>(values);
	}
	
	public static ResponseHistory<Object> forObjectMethods(Object[] values) {
		if(values == null || values.length == 0) {
			throw new IllegalStateException();
		}
		return new ResponseHistory<>(values);
	}

	public static ResponseHistory<Void> forVoidMethods() {
		return new ResponseHistory<>(null);
	}
	
	@Override
	public String toString() {
		return count + "(of " + (responses == null ? "<none>" : responses.size()) + ")";
	}
}
