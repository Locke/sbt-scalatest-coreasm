package de.athalis.sbt.testcoreasm;

/*

This is a customized copy from: https://github.com/CoreASM/coreasm.core/blob/master/org.coreasm.engine/test/org/coreasm/engine/test/TestEngineDriver.java @ cf7010d

*/

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.coreasm.engine.CoreASMEngine.EngineMode;
import org.coreasm.engine.CoreASMEngineFactory;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.Engine;
import org.coreasm.engine.EngineErrorEvent;
import org.coreasm.engine.EngineErrorObserver;
import org.coreasm.engine.EngineEvent;
import org.coreasm.engine.EngineProperties;
import org.coreasm.engine.EngineStepObserver;
import org.coreasm.engine.StepFailedEvent;
import org.coreasm.engine.absstorage.Update;
import org.coreasm.engine.plugin.PluginServiceInterface;
import org.coreasm.engine.plugins.debuginfo.DebugInfoPlugin.DebugInfoPSI;
import org.coreasm.engine.plugins.io.IOPlugin.IOPluginPSI;
import org.coreasm.util.CoreASMGlobal;
import org.coreasm.util.Logger;
import org.coreasm.util.Tools;

public class TestEngineDriver implements EngineStepObserver, EngineErrorObserver {

	protected static List<TestEngineDriver> runningInstances = new LinkedList<>();

	protected Engine engine;

	public enum TestEngineDriverStatus {
		stopped, running, paused
	};

	private TestEngineDriverStatus status = TestEngineDriverStatus.paused;

	private boolean updateFailed;
	protected CoreASMError lastError;
	private Exception exception = null;

	private boolean stopOnEmptyUpdates;
	private boolean stopOnStableUpdates;
	private boolean stopOnEmptyActiveAgents;
	private boolean stopOnFailedUpdates;

	public TestEngineDriver(String pluginFolders, java.util.Properties properties) {
		runningInstances.add(this);
		CoreASMGlobal.setRootFolder(Tools.getRootFolder());
		engine = (Engine) org.coreasm.engine.CoreASMEngineFactory.createEngine(properties);
		engine.addObserver(this);

		if (pluginFolders != null) {
			if (System.getProperty(EngineProperties.PLUGIN_FOLDERS_PROPERTY) != null)
				pluginFolders += EngineProperties.PLUGIN_FOLDERS_DELIM
						+ System.getProperty(EngineProperties.PLUGIN_FOLDERS_PROPERTY);
			engine.setProperty(EngineProperties.PLUGIN_FOLDERS_PROPERTY, pluginFolders);
		}
		engine.setClassLoader(CoreASMEngineFactory.class.getClassLoader());
		engine.initialize();
		engine.waitWhileBusy();
	}

	public TestEngineDriverStatus getStatus() {
		return status;
	}

	public void setOutputStream(PrintStream outputStream) {
		PluginServiceInterface ioPluginInterface = engine.getPluginInterface("IOPlugin");
		if (ioPluginInterface != null)
			((IOPluginPSI)ioPluginInterface).setOutputStream(outputStream);

		PluginServiceInterface debugInfoPluginInterface = engine.getPluginInterface("DebugInfoPlugin");
		if (debugInfoPluginInterface != null)
			((DebugInfoPSI)debugInfoPluginInterface).setOutputStream(outputStream);
	}

	public void setDefaultConfig()
	{
		Logger.verbosityLevel = Logger.ERROR;
		stopOnEmptyUpdates = false;
		stopOnStableUpdates = false;
		stopOnEmptyActiveAgents = true;
		stopOnFailedUpdates = false;
	}

	public Engine getEngine() {
		return engine;
	}

	public boolean isRunning() {
		return runningInstances.contains(this);
	}

	public static TestEngineDriver newLaunch(String abspathname, String pluginFolders) throws IOException {
		Path path = new File(abspathname).toPath();

		return TestEngineDriver.newLaunch(path, pluginFolders);
	}

	public static TestEngineDriver newLaunch(Path path, String pluginFolders) throws IOException {
		Reader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path)));

		return TestEngineDriver.newLaunch(path.getFileName().toString(), reader, pluginFolders, new java.util.Properties());
	}

	public static TestEngineDriver newLaunch(String name, Reader src) {
		return TestEngineDriver.newLaunch(name, src, null, new java.util.Properties());
	}

	public static TestEngineDriver newLaunch(String name, Reader src, String pluginFolders, java.util.Properties properties) {
		TestEngineDriver td = new TestEngineDriver(pluginFolders, properties);
		td.setDefaultConfig();
		td.doLaunch(name, src);
		return td;
	}

	private void doLaunch(String name, Reader src) {
		if (engine.getEngineMode() == EngineMode.emError) {
			engine.recover();
			engine.waitWhileBusy();
		}

		engine.loadSpecification(name, src);
		engine.waitWhileBusy();
	}

	private void executeStepsImpl(int stepsLimit)
	{
		boolean doShutdown = false;

		Set<Update> updates, prevupdates = null;

		try {

			if (engine.getEngineMode() != EngineMode.emIdle) {
				handleError();
				return;
			}

			int step = 0;

			while (engine.getEngineMode() == EngineMode.emIdle) {
				status = TestEngineDriverStatus.running;

				//execute a step
				engine.waitWhileBusy();
				engine.step();
				step++;
				engine.waitWhileBusy();

				updates = engine.getUpdateSet(0);
				if (terminated(updates, prevupdates)) {
					doShutdown = true;
					break;
				}
				prevupdates = updates;
				if (step == stepsLimit) {
					break;
				}
			}

			if (engine.getEngineMode() != EngineMode.emIdle)
				handleError();
		}
		catch (Exception e) {
			doShutdown = true;
			exception = e;
			e.printStackTrace();
		}
		finally {
			if (doShutdown) {
				stop();
			}
			else {
				status = TestEngineDriverStatus.paused;
			}
		}
	}

	public void stop() {
		if (runningInstances.contains(this)) {
			runningInstances.remove(this);
			this.engine.removeObserver(this);

			if (exception != null)
				System.err.println("[!] Run is terminated with exception " + exception);

			this.engine.terminate();
			this.engine.hardInterrupt();

			engine.waitWhileBusy();

			status = TestEngineDriverStatus.stopped;
		}
	}

	public void executeSteps(int numberOfSteps) {
		if (numberOfSteps == 0)
			numberOfSteps = -1; //means infinite steps

		executeStepsImpl(numberOfSteps);
	}


	private boolean terminated(Set<Update> updates, Set<Update> prevupdates) {
		if (stopOnEmptyUpdates && updates.isEmpty())
			return true;
		if (stopOnStableUpdates && updates.equals(prevupdates))
			return true;
		if (stopOnEmptyActiveAgents && engine.getAgentSet().size() < 1)
			return true;
		if (stopOnFailedUpdates && updateFailed)
			return true;
		return false;
	}

	@Override
	public void update(EngineEvent event) {

		// Looking for StepFailed
		if (event instanceof StepFailedEvent) {
			synchronized (this) {
				updateFailed = true;
			}
		}

		// Looking for errors
		else if (event instanceof EngineErrorEvent) {
			synchronized (this) {
				lastError = ((EngineErrorEvent) event).getError();
			}
		}

	}

	protected void handleError() {
		String message;

		if (lastError != null) {
			message = lastError.showError();
		}
		else {
			message = "Enginemode should be " + EngineMode.emIdle + " but is " + engine.getEngineMode();
		}

		showErrorDialog("CoreASM Engine Error", message);

		lastError = null;
		engine.recover();
		engine.waitWhileBusy();
	}

	private void showErrorDialog(String title, String message) {
		System.err.println(title + "\n" + message);
	}
}
