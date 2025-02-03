package org.point85.domain.opc.ua.packml;

/**
 * The state machine states defined in the PackML OPC UA companion specification
 */
public enum PackMLState {
	Resetting, Idle, Starting, Unsuspending, Suspended, Suspending, Execute, Holding, Held, Unholding, Completing,
	Complete, Cleared, Aborting, Aborted, Clearing, Stopping, Stopped, Unknown

}
