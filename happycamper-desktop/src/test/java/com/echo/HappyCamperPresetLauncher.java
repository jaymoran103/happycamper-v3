package com.echo;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;

import com.echo.automation.TestFileFinder;
import com.echo.preset.Preset;
import com.echo.preset.PresetLoader;

/**
 * Test-source entry point that reads {@code -Dhappycamper.preset=<name>} (or
 * the first program argument) and launches {@link HappyCamper} pre-loaded with
 * the named preset's CSVs and feature toggles.
 *
 * <p>Lives in test sources by design — keeps {@code com.echo.preset.*} (which
 * lives in the core {@code tests} classifier jar) off the production fat-jar
 * classpath. Production {@link HappyCamper#main} is unchanged.
 *
 * <p>Invoke via the shell wrapper ({@code ./verify-desktop.sh -j -p demo-small})
 * or directly: {@code mvn -pl happycamper-desktop exec:java -Dhappycamper.preset=demo-small}.
 */
public final class HappyCamperPresetLauncher {

    private HappyCamperPresetLauncher() {}

    public static void main(String[] args) {
        TestFileFinder.clearCache();

        String presetName = System.getProperty("happycamper.preset");
        if (presetName == null && args.length > 0) {
            presetName = args[0];
        }

        if (presetName == null || presetName.isBlank()) {
            System.out.println("[preset-launcher] no -Dhappycamper.preset specified; launching default main");
            HappyCamper.setupApp(true);
            return;
        }

        Preset preset = PresetLoader.load(presetName);
        printPresetInfo(preset);
        HappyCamper.mainTest(preset.getCamperFile(), preset.getActivityFile(), preset.getFeatures());

        // Under `mvn exec:java`, Maven exits the JVM as soon as main returns — even
        // with cleanupDaemonThreads=false — which tears the Swing window down before
        // the user can interact with it. Block here until the window is closed so
        // the launch behaves like a real desktop run.
        blockUntilWindowClosed();
    }

    private static void blockUntilWindowClosed() {
        CountDownLatch latch = new CountDownLatch(1);
        HappyCamper.accessSingleWindow().addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { latch.countDown(); }
            @Override public void windowClosing(WindowEvent e) { latch.countDown(); }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printPresetInfo(Preset p) {
        System.out.println();
        System.out.println("=== HappyCamper preset launcher ===");
        System.out.println("Preset:        " + p.getName());
        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            System.out.println("Description:   " + p.getDescription().trim());
        }
        System.out.println("Camper file:   " + p.getCamperFile().getName());
        System.out.println("Activity file: " + p.getActivityFile().getName());
        System.out.println("Session:       " + p.getSession());
        if (p.getFeatures().length > 0) {
            System.out.println("Features:      " + String.join(", ", p.getFeatures()));
        }
        System.out.println("===================================");
        System.out.println();
    }
}
