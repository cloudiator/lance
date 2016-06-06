package de.uniulm.omi.cloudiator.lance.lifecycle;

import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorState;

interface StopDetectorCallback {
	
	void state(DetectorState theState);

	DetectorState getDetectedState();

	void exceptionOccurred(Exception ex);

}
