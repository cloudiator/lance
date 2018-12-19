package de.uniulm.omi.cloudiator.lance.util;

public final class Version {

  public static final String LANCE_VERSION_NUMBER = "v0.3";
  public static final String LANCE_VERSION_BRANCH = "main";

  private Version() {
    // no instances of this class //
  }

  public static final String getVersionString() {
    return LANCE_VERSION_NUMBER + "@" + LANCE_VERSION_BRANCH;
  }
}
