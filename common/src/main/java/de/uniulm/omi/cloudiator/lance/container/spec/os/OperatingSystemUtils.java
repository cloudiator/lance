package de.uniulm.omi.cloudiator.lance.container.spec.os;

import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;

public class OperatingSystemUtils {

  // do not instantiate
  private OperatingSystemUtils() {
  }

  public static boolean isLinux(OperatingSystem os) {
    OperatingSystemFamily family = os.operatingSystemFamily();
    return isLinux(family);
  }

  public static boolean isLinux(OperatingSystemFamily family) {
    return family == OperatingSystemFamily.AMZN_LINUX
        || family == OperatingSystemFamily.ARCH
        || family == OperatingSystemFamily.CENTOS
        || family == OperatingSystemFamily.CLOUD_LINUX
        || family == OperatingSystemFamily.COREOS
        || family == OperatingSystemFamily.DEBIAN
        || family == OperatingSystemFamily.FEDORA
        || family == OperatingSystemFamily.GENTOO
        || family == OperatingSystemFamily.MANDRIVA
        || family == OperatingSystemFamily.RHEL
        || family == OperatingSystemFamily.OEL
        || family == OperatingSystemFamily.SLACKWARE
        || family == OperatingSystemFamily.SUSE
        || family == OperatingSystemFamily.TURBOLINUX
        || family == OperatingSystemFamily.SCIENTIFIC;
  }
}
