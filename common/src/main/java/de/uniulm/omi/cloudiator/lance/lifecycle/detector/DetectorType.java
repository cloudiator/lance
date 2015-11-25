package de.uniulm.omi.cloudiator.lance.lifecycle.detector;

import de.uniulm.omi.cloudiator.lance.lifecycle.HandlerType;

public enum DetectorType implements HandlerType {

	START,
	STOP,
	PORT_UPDATE;
}
