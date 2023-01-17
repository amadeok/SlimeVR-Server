package dev.slimevr.vr.trackers;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import dev.slimevr.config.TrackerConfig;
import dev.slimevr.vr.Device;

import java.util.concurrent.atomic.AtomicInteger;


public interface Tracker {

	AtomicInteger nextLocalTrackerId = new AtomicInteger();

	static int getNextLocalTrackerId() {
		return nextLocalTrackerId.incrementAndGet();
	}

	public Quaternion AprilResetCorrection = new Quaternion();
	public  Quaternion PrevAprilQuat = new Quaternion();
	public  Quaternion aprilQuat = new Quaternion();
	public  Quaternion CorrectionQuat = new Quaternion();
	public  Quaternion CorrectedQuat = new Quaternion();
	//public  Byte aprilDataAvailable = 0;
	//byte aprilDataAvailable = 0;

	// public default void SetAprilDataAvailable(Byte b)
	// {
	// 	aprilDataAvailable = b;
	// }

	boolean getPosition(Vector3f store);

	boolean getRotation(Quaternion store);
	boolean getAprilRotation(Quaternion store);

	boolean getAcceleration(Vector3f store);

	String getName();

	TrackerStatus getStatus();

	void readConfig(TrackerConfig config);

	void writeConfig(TrackerConfig config);

	float getConfidenceLevel();

	void resetFull(Quaternion reference);

	void resetYaw(Quaternion reference);

	void tick();

	TrackerPosition getBodyPosition();

	void setBodyPosition(TrackerPosition position);

	boolean userEditable();

	boolean hasRotation();

	boolean hasPosition();

	boolean isComputed();

	int getTrackerId();

	int getTrackerNum();

	Device getDevice();

	String getDisplayName();

	String getCustomName();


	/**
	 * Returns the real tracker behind this tracker. In case this tracker
	 * transforms another tracker like {@link ReferenceAdjustedTracker}, this
	 * will return the tracker it modifies. Otherwise, it will return itself.
	 */
	Tracker get();

    default void SetAprilDataAvailable(Byte b)
	{
	}

    default Byte IsAprilDataAvailable()
	{
		return 0;
	}


}
