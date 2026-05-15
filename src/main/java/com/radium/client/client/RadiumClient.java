package com.radium.client.client;
// radium client

import com.radium.client.events.EventManager;
import com.radium.client.gui.GuiManager;
import com.radium.client.modules.client.*;
import com.radium.client.modules.combat.*;
import com.radium.client.modules.donut.*;
import com.radium.client.modules.misc.*;
import com.radium.client.modules.visual.*;
import com.radium.client.systems.accounts.AccountManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class RadiumClient {
    public static ModuleManager moduleManager;
    public static ConfigManager configManager;
    public static GuiManager guiManager;
    public static EventManager eventManager;
    public static KeybindManager keybindManager;
    public static AccountManager accountManager;
    public static MinecraftClient mc;
    public static volatile RadiumClient instance;
    public static String ssid;
    public static String username;
    public static CapeManager capeManager;
    public static String discordId = "";
    private static ClickGUI clickGui;
    private final Object initializationLock = new Object();
    private boolean initialized = false;
    private final boolean configLoaded = false;
    private final ExecutorService executorService;

    public RadiumClient() {
        instance = this;

        this.executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread thread = new Thread(null, r, "Reference Handler", 8 * 1024 * 1024);
                    thread.setDaemon(true);
                    thread.setPriority(Thread.NORM_PRIORITY);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );

        executorService.submit(this::initialize);

        // Non-blocking initialization - wait with timeout
        synchronized (initializationLock) {
            long startTime = System.currentTimeMillis();
            while (!initialized && (System.currentTimeMillis() - startTime) < 10000) {
                try {
                    initializationLock.wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (!initialized) {
                System.err.println("[RubyClient] Warning: Initialization did not complete within timeout");
            }
        }

    }

    public static AccountManager getAccountManager() {
        return accountManager;
    }

    public static RadiumClient getInstance() {
        return instance;
    }



    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static EventManager getEventManager() {
        return eventManager;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static KeybindManager getKeybindManager() {
        return keybindManager;
    }

    public static GuiManager getGuiManager() {
        return guiManager;
    }

    public static CapeManager getCapeManager() {
        return capeManager;
    }

    public static ClickGUI getClickGui() {
        return clickGui;
    }


    public static void sendKeepAliveIfAllowed() {
        // Removed auth functionality
    }

    public void initialize() {
        try {
            mc = MinecraftClient.getInstance();
            
            // Wait for Minecraft client to be ready
            if (mc == null) {
                // Retry after a short delay
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                mc = MinecraftClient.getInstance();
            }
            
            if (mc == null || mc.runDirectory == null) {
                System.err.println("[RubyClient] Minecraft client not ready, initialization will be retried");
                synchronized (initializationLock) {
                    initialized = true;
                    initializationLock.notifyAll();
                }
                return;
            }

            File radiumDir = new File(mc.runDirectory, "radium");
            accountManager = new AccountManager(radiumDir);

            moduleManager = new ModuleManager();
            configManager = new ConfigManager();
            eventManager = new EventManager();
            keybindManager = new KeybindManager();
            capeManager = new CapeManager();
            guiManager = new GuiManager();



            clickGui = new ClickGUI();
            moduleManager.register(clickGui);
            moduleManager.register(new HUD());
            moduleManager.register(new Themes());
            moduleManager.register(new Cape());
            moduleManager.register(new MediaPlayer());
            moduleManager.register(new TabDetector());
            moduleManager.register(new Freecam());
            moduleManager.register(new Freelook());
            moduleManager.register(new SpawnerProtect());
            moduleManager.register(new PearlThrow());
            moduleManager.register(new AutoFirework());
            moduleManager.register(new AutoTotem());

            moduleManager.register(new SnapTap());

            moduleManager.register(new AutoEat());
            moduleManager.register(new AutoVillagerTrade());
            moduleManager.register(new FakeScoreboard());
            moduleManager.register(new AutoTool());
            moduleManager.register(new FastPlace());
            moduleManager.register(new ClusterFinder());
            moduleManager.register(new AutoReplenish());
            moduleManager.register(new PlayerDetection());
            moduleManager.register(new BaseDigger());
            moduleManager.register(new AutoJumpReset());
            moduleManager.register(new StorageESP());
            moduleManager.register(new PlayerESP());
            moduleManager.register(new FakePlayer());
            moduleManager.register(new LegitTridentFly());
            moduleManager.register(new NameProtect());
            moduleManager.register(new BaseESP());
            moduleManager.register(new LightESP());
            moduleManager.register(new MobESP());
            moduleManager.register(new CrystalESP());
            moduleManager.register(new BeehiveESP());
            moduleManager.register(new NoRender());
            moduleManager.register(new AntiBlockRotate());
            moduleManager.register(new HideScoreboard());
            moduleManager.register(new AntiTrap());
            moduleManager.register(new HoleTunnelStairsESP());
            moduleManager.register(new AhSell());
            moduleManager.register(new AutoTreeFarmer());
            moduleManager.register(new AutoSell());
            moduleManager.register(new TotemOverlay());
            moduleManager.register(new SpawnerDropper());
            moduleManager.register(new AuctionSniper());
            moduleManager.register(new RTPBaseFinder());
            moduleManager.register(new CrateBuyer());
            moduleManager.register(new SafeAnchorMacro());
            moduleManager.register(new SwingSpeed());
            moduleManager.register(new FullBright());
            moduleManager.register(new AnchorMacro());
            moduleManager.register(new CrystalMacro());
            moduleManager.register(new AutoCrystal());
            moduleManager.register(new PlacementOptimizer());
            moduleManager.register(new CrystalOptimizer());
            moduleManager.register(new AimAssist());
            moduleManager.register(new AnchorMacrov2());
            moduleManager.register(new InvTotem());
            moduleManager.register(new MaceSwap());
            moduleManager.register(new MaceTrigger());
            moduleManager.register(new AutoShulker());
            moduleManager.register(new ShopBuyer());
            moduleManager.register(new AntiAFK());
            moduleManager.register(new TriggerBot());
            moduleManager.register(new DoubleAnchor());
            moduleManager.register(new AutoBoneOrder());
            moduleManager.register(new ChunkFinder());
            moduleManager.register(new NoInteract());
            moduleManager.register(new StashFinder());
            moduleManager.register(new ItemSwap());
            moduleManager.register(new SkinChanger());
            moduleManager.register(new FakeElytra());
            moduleManager.register(new HoverTotem());
            moduleManager.register(new ExtraESP());
            moduleManager.register(new DiscordPresence());
            moduleManager.register(new AutoReconnect());
            moduleManager.register(new AutoLog());
            moduleManager.register(new Friends());
            moduleManager.register(new TunnelBaseFinder());
            moduleManager.register(new SilentHome());
            moduleManager.register(new NoPause());
            moduleManager.register(new AutoDoubleHand());
            moduleManager.register(new Baltagger());
            moduleManager.register(new AutoTPA());

            moduleManager.register(new ChatUtils());
            moduleManager.register(new Compass());
            moduleManager.register(new SeeThroughWalls());
            moduleManager.register(new ItemESP());
            moduleManager.register(new BlockESP());

            moduleManager.register(new com.radium.client.modules.misc.Sprint());
            moduleManager.register(new com.radium.client.modules.misc.InventoryWalk());
            
            moduleManager.register(new Blink());

            moduleManager.register(new AutoWeapon());
            moduleManager.register(new ChestStealer());
            moduleManager.register(new CustomCrosshair());
            moduleManager.register(new CustomFOV());
            moduleManager.register(new AutoFish());

            for (com.radium.client.modules.Module module : moduleManager.getModules()) {
                try {
                    java.lang.reflect.Field mcField = com.radium.client.modules.Module.class.getDeclaredField("mc");
                    mcField.setAccessible(true);
                    mcField.set(module, mc);
                } catch (Exception ignored) {
                }
            }

            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (keybindManager != null) {
                    keybindManager.checkKeybinds();
                }
            });

            configManager.loadProfile();
            configManager.loadHudPositions();

            synchronized (initializationLock) {
                initialized = true;
                initializationLock.notifyAll();
            }

            mc.execute(() -> {
                Thread moduleThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            if (moduleManager != null) moduleManager.tick();
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (Exception ignored) {
                        }
                    }
                });
                moduleThread.setDaemon(true);
                moduleThread.setName("RubyClient-ModuleTicker");
                moduleThread.start();


            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
