package com.horrortricks.phantomjoin.client;

import com.horrortricks.hauntedcore.presence.PresenceEndEvent;
import com.horrortricks.hauntedcore.presence.PresenceState;
import com.horrortricks.hauntedcore.client.HauntedCoreClientNetworking;
import com.horrortricks.phantomjoin.PhantomJoinConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import org.slf4j.Logger;

@EventBusSubscriber(modid = "phantomjoin", value = Dist.CLIENT)
public final class PhantomJoinClientEvents {
   private static final int DEBUG_COMMAND_PERMISSION_LEVEL = 0;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int COMBAT_AVOIDANCE_TICKS = 600;
   private static final double HOSTILE_SCAN_RADIUS = 16.0;
   private static final double STANDING_STILL_SPEED_SQ_THRESHOLD = 2.5E-5;
   private static long tickCounter = 0L;
   private static long nextJoinTick = -1L;
   private static String pendingLeaveName;
   private static float lastObservedHealth = -1.0F;
   private static long lastDamageTick = -4611686018427387904L;

   private PhantomJoinClientEvents() {
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer player = minecraft.player;
      if (player != null && minecraft.level != null) {
         if ((Boolean)PhantomJoinConfig.ENABLED.get()) {
            tickCounter++;
            trackDamage(player);
            RandomSource random = minecraft.level.random;
            if (nextJoinTick < 0L) {
               scheduleNextJoin(random);
            } else {
               if (tickCounter >= nextJoinTick && isQuietMoment(minecraft, player)) {
                  triggerJoin(minecraft, random);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPresenceEnd(PresenceEndEvent event) {
      if (pendingLeaveName != null) {
         HauntedCoreClientNetworking.syncPresence(null);
         sendLeaveMessage(pendingLeaveName);
         pendingLeaveName = null;
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.level != null) {
            scheduleNextJoin(minecraft.level.random);
         }
      }
   }

   @SubscribeEvent
   public static void onLoggingIn(LoggingIn event) {
      tickCounter = 0L;
      nextJoinTick = -1L;
      pendingLeaveName = null;
      lastObservedHealth = -1.0F;
      lastDamageTick = -4611686018427387904L;
   }

   @SubscribeEvent
   public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("phantomjoin").requires(source -> source.hasPermission(0)))
            .then(Commands.literal("trigger").executes(context -> triggerNow((CommandSourceStack)context.getSource())))
      );
   }

   private static int triggerNow(CommandSourceStack source) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) {
         LOGGER.info("[phantomjoin debug] No world loaded - nothing to trigger.");
         return 0;
      }

      if (PresenceState.isPresent()) {
         PresenceState.end();
      }

      triggerJoin(minecraft, minecraft.level.random);
      LOGGER.info("[phantomjoin debug] Triggered a phantom join for {}.", pendingLeaveName);
      return 1;
   }

   private static void triggerJoin(Minecraft minecraft, RandomSource random) {
      String currentPlayerName = minecraft.player != null ? minecraft.player.getGameProfile().getName() : null;
      PresenceState.begin(random, currentPlayerName);
      String name = PresenceState.getFakeName();
      HauntedCoreClientNetworking.syncPresence(name);
      sendJoinMessage(name);
      pendingLeaveName = name;
   }

   private static void scheduleNextJoin(RandomSource random) {
      int min = (Integer)PhantomJoinConfig.MIN_INTERVAL_TICKS.get();
      int max = Math.max(min, (Integer)PhantomJoinConfig.MAX_INTERVAL_TICKS.get());
      int delay = randomInclusive(random, min, max);
      nextJoinTick = tickCounter + delay;
   }

   private static int randomInclusive(RandomSource random, int min, int max) {
      long range = (long)max - min + 1L;
      return min + (int)Math.floorMod(random.nextLong(), range);
   }

   private static void trackDamage(LocalPlayer player) {
      float health = player.getHealth();
      if (lastObservedHealth >= 0.0F && health < lastObservedHealth) {
         lastDamageTick = tickCounter;
      }

      lastObservedHealth = health;
   }

   private static boolean isQuietMoment(Minecraft minecraft, LocalPlayer player) {
      return !recentlyDamaged() && !hostileMobNearby(minecraft, player) && isStandingStill(player);
   }

   private static boolean recentlyDamaged() {
      return tickCounter - lastDamageTick < 600L;
   }

   private static boolean hostileMobNearby(Minecraft minecraft, LocalPlayer player) {
      AABB area = player.getBoundingBox().inflate(16.0);
      return !minecraft.level.getEntitiesOfClass(Mob.class, area, mob -> mob instanceof Enemy && mob.isAlive()).isEmpty();
   }

   private static boolean isStandingStill(LocalPlayer player) {
      return player.getDeltaMovement().horizontalDistanceSqr() < 2.5E-5;
   }

   private static void sendJoinMessage(String name) {
      Component message = Component.translatable("multiplayer.player.joined", new Object[]{Component.literal(name)}).withStyle(ChatFormatting.YELLOW);
      Minecraft.getInstance().gui.getChat().addMessage(message);
   }

   private static void sendLeaveMessage(String name) {
      Component message = Component.translatable("multiplayer.player.left", new Object[]{Component.literal(name)}).withStyle(ChatFormatting.YELLOW);
      Minecraft.getInstance().gui.getChat().addMessage(message);
   }
}
