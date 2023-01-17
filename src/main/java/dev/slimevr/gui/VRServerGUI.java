package dev.slimevr.gui;

import dev.slimevr.Main;
import dev.slimevr.VRServer;
import dev.slimevr.config.WindowConfig;
import dev.slimevr.gui.swing.ButtonTimer;
import dev.slimevr.gui.swing.EJBagNoStretch;
import dev.slimevr.gui.swing.EJBox;
import dev.slimevr.gui.swing.EJBoxNoStretch;
import dev.slimevr.platform.windows.WindowsNamedPipeBridge;
import dev.slimevr.vr.processor.skeleton.SkeletonConfigToggles;
import dev.slimevr.vr.trackers.TrackerRole;
import io.eiren.util.MacOSX;
import io.eiren.util.OperatingSystem;
import io.eiren.util.StringUtils;
import io.eiren.util.ann.AWTThread;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.BoxLayout.LINE_AXIS;
import static javax.swing.BoxLayout.PAGE_AXIS;


public class VRServerGUI extends JFrame {

	public static final String TITLE = "SlimeVR Server (" + Main.VERSION + ")";

	public final VRServer server;
	private final TrackersList trackersList;
	private final TrackersFiltersGUI trackersFiltersGUI;
	private final SkeletonList skeletonList;
	private final EJBox pane;
	private JButton resetButton;
	private JButton floorClipButton;
	private JButton skatingCorrectionButton;
	private JButton ConnectToAprilTag;
	private JButton ConnectToVisualizer;

	private WindowConfig config;

	@AWTThread
	public VRServerGUI(VRServer server) {
		super(TITLE);

		this.config = server.getConfigManager().getVrConfig().getWindow();

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (OperatingSystem.getCurrentPlatform() == OperatingSystem.OSX)
			MacOSX.setTitle(TITLE);
		try {
			List<BufferedImage> images = new ArrayList<BufferedImage>(6);
			images.add(ImageIO.read(VRServerGUI.class.getResource("/icon16.png")));
			images.add(ImageIO.read(VRServerGUI.class.getResource("/icon32.png")));
			images.add(ImageIO.read(VRServerGUI.class.getResource("/icon48.png")));
			images.add(ImageIO.read(VRServerGUI.class.getResource("/icon64.png")));
			images.add(ImageIO.read(VRServerGUI.class.getResource("/icon128.png")));
			images.add(ImageIO.read(VRServerGUI.class.getResource("/icon256.png")));
			setIconImages(images);
			if (OperatingSystem.getCurrentPlatform() == OperatingSystem.OSX) {
				MacOSX.setIcons(images);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		this.server = server;

		setDefaultFontSize(config.getZoom());
		// All components should be constructed to the current zoom level by
		// default

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		getContentPane().setLayout(new BoxLayout(getContentPane(), PAGE_AXIS));

		this.trackersList = new TrackersList(server, this);
		trackersFiltersGUI = new TrackersFiltersGUI(server, this);
		this.skeletonList = new SkeletonList(server, this);

		JScrollPane scrollPane = (JScrollPane) add(
			new JScrollPane(
				pane = new EJBox(PAGE_AXIS),
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
			)
		);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		GraphicsConfiguration gc = getGraphicsConfiguration();
		Rectangle screenBounds = gc.getBounds();
		setMinimumSize(new Dimension(100, 100));
		setSize(
			Math.min(config.getWidth(), screenBounds.width),
			Math.min(config.getHeight(), screenBounds.height)
		);

		int posx = config.getPosx();
		if (posx == -1) {
			posx = screenBounds.x + (screenBounds.width - getSize().width) / 2;
			config.setPosx(posx);
		}

		int posy = config.getPosy();
		if (posy == -1) {
			posy = (screenBounds.height - getSize().height) / 2;
			config.setPosy(posy);
		}

		setLocation(posx, posy);

		// Resize and close listeners to save position and size betwen launcher
		// starts
		addComponentListener(new AbstractComponentListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				saveFrameInfo();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				saveFrameInfo();
			}
		});

		build();
	}

	private static void processNewZoom(float zoom, Component comp) {
		if (comp.isFontSet()) {
			Font newFont = new ScalableFont(comp.getFont(), zoom);
			comp.setFont(newFont);
		}
		if (comp instanceof Container) {
			Container cont = (Container) comp;
			for (Component child : cont.getComponents())
				processNewZoom(zoom, child);
		}
	}

	private static void setDefaultFontSize(float zoom) {
		java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof javax.swing.plaf.FontUIResource) {
				javax.swing.plaf.FontUIResource f = (javax.swing.plaf.FontUIResource) value;
				javax.swing.plaf.FontUIResource f2 = new javax.swing.plaf.FontUIResource(
					f.deriveFont(f.getSize() * zoom)
				);
				UIManager.put(key, f2);
			}
		}
	}

	protected void saveFrameInfo() {
		Rectangle b = getBounds();
		config.setWidth(b.width);
		config.setHeight(b.height);
		config.setPosx(b.x);
		config.setPosy(b.y);
		server.getConfigManager().saveConfig();
	}

	public void refresh() {
		// Pack and display
		// pack();
		setVisible(true);
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				repaint();
			}
		});
	}

	@AWTThread
	private void build() {
		pane.removeAll();

		pane.add(new EJBoxNoStretch(LINE_AXIS, false, true) {
			{
				setBorder(new EmptyBorder(i(5)));

				add(Box.createHorizontalGlue());
				add(resetButton = new JButton("RESET") {
					{
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								reset();
							}
						});
					}
				});
				add(Box.createHorizontalStrut(10));
				add(new JButton("Fast Reset") {
					{
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								resetFast();
							}
						});
					}
				});
				add(Box.createHorizontalStrut(10));
				add(floorClipButton = new JButton("Toggle Floor Clip") {
					{
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								boolean[] state = server.humanPoseProcessor.getLegTweaksState();
								setFloorClipEnabled(!state[0]);
							}
						});
					}
				});

				setFloorClipEnabled(
					server
						.getConfigManager()
						.getVrConfig()
						.getSkeleton()
						.getToggles()
						.get(SkeletonConfigToggles.FLOOR_CLIP.configKey)
				);

				add(Box.createHorizontalStrut(10));
				add(skatingCorrectionButton = new JButton("Toggle Skating Correction") {
					{
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								boolean[] state = server.humanPoseProcessor.getLegTweaksState();
								setSkatingReductionEnabled(!state[1]);
							}
						});
					}
				});

				add(Box.createHorizontalStrut(10));
				add(ConnectToAprilTag = new JButton("Connect to AprilTag") {
					{
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								server.ConnectToApril();
							}
						});
					}
				});
				add(Box.createHorizontalStrut(10));
				add(ConnectToVisualizer = new JButton("Connect to AprilTag Visualizer") {
					{
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								server.connectToUE();
							}
						});
					}
				});
				add(Box.createHorizontalStrut(10));
				add(ConnectToVisualizer = new JButton("Apply") {
					{
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								if (server.ApplyOffset)
									server.ApplyOffset = false;
								else
									server.ApplyOffset = true;
							}
						});
					}
				});

				setSkatingReductionEnabled(
					server
						.getConfigManager()
						.getVrConfig()
						.getSkeleton()
						.getToggles()
						.get(SkeletonConfigToggles.SKATING_CORRECTION.configKey)
				);

				add(Box.createHorizontalGlue());
				add(new JButton("Record BVH") {
					{
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								if (!server.getBvhRecorder().isRecording()) {
									setText("Stop Recording BVH...");
									server.getBvhRecorder().startRecording();
								} else {
									server.getBvhRecorder().endRecording();
									setText("Record BVH");
								}
							}
						});
					}
				});
				add(Box.createHorizontalGlue());
				add(
					new JButton(
						"GUI Zoom (x" + StringUtils.prettyNumber(config.getZoom(), 2) + ")"
					) {
						{
							addMouseListener(new MouseInputAdapter() {
								@Override
								public void mouseClicked(MouseEvent e) {
									guiZoom();
									setText(
										"GUI Zoom (x"
											+ StringUtils.prettyNumber(config.getZoom(), 2)
											+ ")"
									);
								}
							});
						}
					}
				);
				add(Box.createHorizontalStrut(10));
				add(new JButton("WiFi") {
					{
						addMouseListener(new MouseInputAdapter() {
							@SuppressWarnings("unused")
							@Override
							public void mouseClicked(MouseEvent e) {
								new WiFiWindow(VRServerGUI.this);
							}
						});
					}
				});
				add(Box.createHorizontalStrut(10));
			}
		});

		pane.add(new EJBox(LINE_AXIS) {
			{
				setBorder(new EmptyBorder(i(5)));
				add(new EJBoxNoStretch(PAGE_AXIS, false, true) {
					{
						setAlignmentY(TOP_ALIGNMENT);
						JLabel l;
						add(l = new JLabel("Trackers list"));
						l.setFont(l.getFont().deriveFont(Font.BOLD));
						l.setAlignmentX(0.5f);
						add(trackersList);
						add(Box.createVerticalGlue());
					}
				});

				add(new EJBoxNoStretch(PAGE_AXIS, false, true) {
					{
						setAlignmentY(TOP_ALIGNMENT);

						JCheckBox debugCb;
						add(debugCb = new JCheckBox("Show debug information"));
						debugCb.setSelected(false);
						debugCb.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								trackersList.setDebug(debugCb.isSelected());
							}
						});

						JLabel l;
						add(l = new JLabel("Body proportions"));
						l.setFont(l.getFont().deriveFont(Font.BOLD));
						l.setAlignmentX(0.5f);
						add(new SkeletonConfigGUI(server, VRServerGUI.this));
						add(Box.createVerticalStrut(10));
						if (server.hasBridge(WindowsNamedPipeBridge.class)) {
							WindowsNamedPipeBridge br = server
								.getVRBridge(WindowsNamedPipeBridge.class);
							add(l = new JLabel("SteamVR Trackers"));
							l.setFont(l.getFont().deriveFont(Font.BOLD));
							l.setAlignmentX(0.5f);
							add(l = new JLabel("Changes may require restart of SteamVR"));
							l.setFont(l.getFont().deriveFont(Font.ITALIC));
							l.setAlignmentX(0.5f);

							add(new EJBagNoStretch(false, true) {
								{
									JCheckBox waistCb;
									add(waistCb = new JCheckBox("Waist"), c(1, 1));
									waistCb.setSelected(br.getShareSetting(TrackerRole.WAIST));
									waistCb.addActionListener(new ActionListener() {
										@Override
										public void actionPerformed(ActionEvent e) {
											server.queueTask(() -> {
												br
													.changeShareSettings(
														TrackerRole.WAIST,
														waistCb.isSelected()
													);
											});
										}
									});

									JCheckBox legsCb;
									add(legsCb = new JCheckBox("Feet"), c(2, 1));
									legsCb
										.setSelected(
											br.getShareSetting(TrackerRole.LEFT_FOOT)
												&& br.getShareSetting(TrackerRole.RIGHT_FOOT)
										);
									legsCb.addActionListener(new ActionListener() {
										@Override
										public void actionPerformed(ActionEvent e) {
											server.queueTask(() -> {
												br
													.changeShareSettings(
														TrackerRole.LEFT_FOOT,
														legsCb.isSelected()
													);
												br
													.changeShareSettings(
														TrackerRole.RIGHT_FOOT,
														legsCb.isSelected()
													);
											});
										}
									});

									JCheckBox chestCb;
									add(chestCb = new JCheckBox("Chest"), c(1, 2));
									chestCb.setSelected(br.getShareSetting(TrackerRole.CHEST));
									chestCb.addActionListener(new ActionListener() {
										@Override
										public void actionPerformed(ActionEvent e) {
											server.queueTask(() -> {
												br
													.changeShareSettings(
														TrackerRole.CHEST,
														chestCb.isSelected()
													);
											});
										}
									});

									JCheckBox kneesCb;
									add(kneesCb = new JCheckBox("Knees"), c(2, 2));
									kneesCb
										.setSelected(
											br.getShareSetting(TrackerRole.LEFT_KNEE)
												&& br.getShareSetting(TrackerRole.RIGHT_KNEE)
										);
									kneesCb.addActionListener(new ActionListener() {
										@Override
										public void actionPerformed(ActionEvent e) {
											server.queueTask(() -> {
												br
													.changeShareSettings(
														TrackerRole.LEFT_KNEE,
														kneesCb.isSelected()
													);
												br
													.changeShareSettings(
														TrackerRole.RIGHT_KNEE,
														kneesCb.isSelected()
													);
											});
										}
									});

									JCheckBox elbowsCb;
									add(elbowsCb = new JCheckBox("Elbows"), c(1, 3));
									elbowsCb
										.setSelected(
											br.getShareSetting(TrackerRole.LEFT_ELBOW)
												&& br.getShareSetting(TrackerRole.RIGHT_ELBOW)
										);
									elbowsCb.addActionListener(new ActionListener() {
										@Override
										public void actionPerformed(ActionEvent e) {
											server.queueTask(() -> {
												br
													.changeShareSettings(
														TrackerRole.LEFT_ELBOW,
														elbowsCb.isSelected()
													);
												br
													.changeShareSettings(
														TrackerRole.RIGHT_ELBOW,
														elbowsCb.isSelected()
													);
											});
										}
									});

									JCheckBox handsCb;
									add(handsCb = new JCheckBox("Hands"), c(2, 3));
									handsCb
										.setSelected(
											br.getShareSetting(TrackerRole.LEFT_HAND)
												&& br.getShareSetting(TrackerRole.RIGHT_HAND)
										);
									handsCb.addActionListener(new ActionListener() {
										@Override
										public void actionPerformed(ActionEvent e) {
											server.queueTask(() -> {
												br
													.changeShareSettings(
														TrackerRole.LEFT_HAND,
														handsCb.isSelected()
													);
												br
													.changeShareSettings(
														TrackerRole.RIGHT_HAND,
														handsCb.isSelected()
													);
											});
										}
									});
								}
							});

							add(Box.createVerticalStrut(10));
						}

						add(l = new JLabel("Trackers filtering"));
						l.setFont(l.getFont().deriveFont(Font.BOLD));
						l.setAlignmentX(0.5f);
						add(trackersFiltersGUI);

						add(Box.createVerticalStrut(10));

						add(new JLabel("Skeleton data"));
						add(skeletonList);
						add(Box.createVerticalGlue());
					}
				});
			}
		});
		pane.add(Box.createVerticalGlue());

		refresh();

		server.addOnTick(trackersList::updateTrackers);
		server.addOnTick(skeletonList::updateBones);
	}

	// For now only changes font size, but should change fixed components size
	// in
	// the future too
	private void guiZoom() {
		if (config.getZoom() <= 1.0f) {
			config.setZoom(1.5f);
		} else if (config.getZoom() <= 1.5f) {
			config.setZoom(1.75f);
		} else if (config.getZoom() <= 1.75f) {
			config.setZoom(2f);
		} else if (config.getZoom() <= 2.0f) {
			config.setZoom(2.5f);
		} else {
			config.setZoom(1f);
		}
		processNewZoom(config.getZoom() / WindowConfig.INITAL_ZOOM, pane);
		refresh();
		server.getConfigManager().saveConfig();
	}

	@AWTThread
	private void resetFast() {
		server.resetTrackersYaw();
	}

	@AWTThread
	private void reset() {
		ButtonTimer.runTimer(resetButton, 3, "RESET", server::resetTrackers);
	}

	@AWTThread
	private void setSkatingReductionEnabled(Boolean value) {
		if (value == null)
			value = false;

		skatingCorrectionButton.setBackground(value ? Color.GREEN : Color.RED);

		skatingCorrectionButton
			.setText("Skating Correction: " + (value ? "ON" : "OFF"));

		server.setSkatingReductionEnabled(value);
	}

	@AWTThread
	private void setFloorClipEnabled(Boolean value) {
		if (value == null)
			value = false;

		floorClipButton.setBackground(value ? Color.GREEN : Color.RED);

		// update the button
		floorClipButton.setText("Floor clip: " + (value ? "ON" : "OFF"));
		server.setFloorClipEnabled(value);
	}
}
