package dev.slimevr.vr.trackers;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import dev.slimevr.config.TrackerConfig;
import dev.slimevr.vr.Device;


public class ReferenceAdjustedTracker<E extends Tracker> implements Tracker {

	public final E tracker;
	public final Quaternion yawFix = new Quaternion();
	public final Quaternion gyroFix = new Quaternion();
	public final Quaternion attachmentFix = new Quaternion();

	public final Quaternion AprilYawFix = new Quaternion();
	public final Quaternion AprilGyroFix = new Quaternion();
	public final Quaternion AprilAttachmentFix = new Quaternion();

	//public Byte aprilDataAvailable = 0;

	protected float confidenceMultiplier = 1.0f;
	public  Byte aprilDataAvailable = 0;

	//@Override
	public void SetAprilDataAvailable(Byte b)
	{
		aprilDataAvailable = b;
	}

	public Byte IsAprilDataAvailable()
	{
		return aprilDataAvailable;
	}

	public ReferenceAdjustedTracker(E tracker) {
		this.tracker = tracker;
	}

	@Override
	public boolean userEditable() {
		return this.tracker.userEditable();
	}

	@Override
	public void readConfig(TrackerConfig config) {
		this.tracker.readConfig(config);
	}

	@Override
	public void writeConfig(TrackerConfig config) {
		this.tracker.writeConfig(config);
	}

	/**
	 * Reset the tracker so that it's current rotation is counted as (0, <HMD
	 * Yaw>, 0). This allows tracker to be strapped to body at any pitch and
	 * roll.
	 * <p>
	 * Performs {@link #resetYaw(Quaternion)} for yaw drift correction.
	 */
	@Override
	public void resetFull(Quaternion reference) 
	{
		tracker.resetFull(reference);
		fixGyroscope();
		Quaternion sensorRotation = new Quaternion();
		tracker.getRotation(sensorRotation);
		gyroFix.mult(sensorRotation, sensorRotation);
		attachmentFix.set(sensorRotation).inverseLocal();

		Quaternion AprilSensorRotation = new Quaternion();
		AprilSensorRotation.set(tracker.aprilQuat);
		AprilGyroFix.mult(AprilSensorRotation, AprilSensorRotation);
		AprilAttachmentFix.set(AprilSensorRotation).inverseLocal();
		
	//AprilResetCorrection.set(reference.mult(aprilQuat.inverse()));

		fixYaw(reference);
	}

	/**
	 * Reset the tracker so that it's current yaw rotation is counted as <HMD
	 * Yaw>. This allows the tracker to have yaw independent of the HMD. Tracker
	 * should still report yaw as if it was mounted facing HMD, mounting
	 * position should be corrected in the source.
	 */
	@Override
	public void resetYaw(Quaternion reference) {
		tracker.resetYaw(reference);
		fixYaw(reference);
	}

	private void fixYaw(Quaternion reference) {
		// Use only yaw HMD rotation
		Quaternion targetTrackerRotation = new Quaternion(reference);
		float[] angles = new float[3];
		targetTrackerRotation.toAngles(angles);
		targetTrackerRotation.fromAngles(0, angles[1], 0);

		Quaternion sensorRotation = new Quaternion();
		tracker.getRotation(sensorRotation);
		gyroFix.mult(sensorRotation, sensorRotation);
		sensorRotation.multLocal(attachmentFix);

		sensorRotation.toAngles(angles);
		sensorRotation.fromAngles(0, angles[1], 0);

		yawFix.set(sensorRotation).inverseLocal().multLocal(targetTrackerRotation);



		Quaternion AprilTargetTrackerRotation = new Quaternion(reference);
		AprilTargetTrackerRotation.toAngles(angles);
		AprilTargetTrackerRotation.fromAngles(0, angles[1], 0);

		Quaternion AprilSensorRotation = new Quaternion();
		AprilSensorRotation.set(tracker.aprilQuat);
		AprilGyroFix.mult(AprilSensorRotation, AprilSensorRotation);
		AprilSensorRotation.multLocal(AprilAttachmentFix);

		AprilSensorRotation.toAngles(angles);
		AprilSensorRotation.fromAngles(0, angles[1], 0);

		AprilYawFix.set(AprilSensorRotation).inverseLocal().multLocal(AprilTargetTrackerRotation);

		
	}

	private void fixGyroscope() {
		float[] angles = new float[3];

		Quaternion sensorRotation = new Quaternion();
		tracker.getRotation(sensorRotation);

		sensorRotation.toAngles(angles);
		sensorRotation.fromAngles(0, angles[1], 0);

		gyroFix.set(sensorRotation).inverseLocal();



		Quaternion AprilSensorRotation = new Quaternion();
		AprilSensorRotation.set(tracker.aprilQuat);

		AprilSensorRotation.toAngles(angles);
		AprilSensorRotation.fromAngles(0, angles[1], 0);

		AprilGyroFix.set(AprilSensorRotation).inverseLocal();
	}

	protected void AprilAdjustInternal(Quaternion store) {
		AprilGyroFix.mult(store, store);
		store.multLocal(AprilAttachmentFix);
		AprilYawFix.mult(store, store);
	}
	protected void adjustInternal(Quaternion store) {
		gyroFix.mult(store, store);
		store.multLocal(attachmentFix);
		yawFix.mult(store, store);
	}

	//@Override
	public boolean getAprilRotation(Quaternion store) {
		store.set(tracker.aprilQuat);
		AprilAdjustInternal(store);
		return true;
	}

	@Override
	public boolean getRotation(Quaternion store) {
		tracker.getRotation(store);
		adjustInternal(store);
		return true;
	}

	@Override
	public boolean getAcceleration(Vector3f store) {
		return tracker.getAcceleration(store);
	}

	@Override
	public boolean getPosition(Vector3f store) {
		return tracker.getPosition(store);
	}

	@Override
	public String getName() {
		return tracker.getName();
	}

	@Override
	public TrackerStatus getStatus() {
		return tracker.getStatus();
	}

	@Override
	public float getConfidenceLevel() {
		return tracker.getConfidenceLevel() * confidenceMultiplier;
	}

	@Override
	public TrackerPosition getBodyPosition() {
		return tracker.getBodyPosition();
	}

	@Override
	public void setBodyPosition(TrackerPosition position) {
		tracker.setBodyPosition(position);
	}

	@Override
	public void tick() {
		tracker.tick();
	}

	@Override
	public boolean hasRotation() {
		return tracker.hasRotation();
	}

	@Override
	public boolean hasPosition() {
		return tracker.hasPosition();
	}

	@Override
	public boolean isComputed() {
		return tracker.isComputed();
	}

	@Override
	public int getTrackerId() {
		return tracker.getTrackerId();
	}

	@Override
	public int getTrackerNum() {
		return tracker.getTrackerNum();
	}

	@Override
	public Device getDevice() {
		return tracker.getDevice();
	}

	@Override
	public Tracker get() {
		return this.tracker;
	}

	@Override
	public String getDisplayName() {
		return this.tracker.getDisplayName();
	}

	@Override
	public String getCustomName() {
		return this.tracker.getCustomName();
	}
}
