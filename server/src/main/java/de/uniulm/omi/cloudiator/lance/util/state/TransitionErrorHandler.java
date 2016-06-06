package de.uniulm.omi.cloudiator.lance.util.state;

public interface TransitionErrorHandler<T extends Enum<?> & State> {

	void run(TransitionException te, T from, T to);

}
