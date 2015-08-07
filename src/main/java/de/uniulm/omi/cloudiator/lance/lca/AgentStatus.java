package de.uniulm.omi.cloudiator.lance.lca;

import de.uniulm.omi.cloudiator.lance.util.state.State;

public enum AgentStatus implements State {

	NEW,
	CREATING,
	CREATED,
	INITIALISING,
	READY,
	SHUTTING_DOWN,
	CLEARED,
	DESTROYED,
	;
}
