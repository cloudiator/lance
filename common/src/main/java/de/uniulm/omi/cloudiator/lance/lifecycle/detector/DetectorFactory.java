package de.uniulm.omi.cloudiator.lance.lifecycle.detector;

import de.uniulm.omi.cloudiator.lance.deployment.Deployment;

public interface DetectorFactory<T extends Detector> {

  T getDefault();

  T getDeploymentHandler(Deployment d);
}
