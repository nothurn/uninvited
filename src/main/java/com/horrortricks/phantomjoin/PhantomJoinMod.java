package com.horrortricks.phantomjoin;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import org.slf4j.Logger;

@Mod("phantomjoin")
public class PhantomJoinMod {
   public static final String MOD_ID = "phantomjoin";
   private static final Logger LOGGER = LogUtils.getLogger();

   public PhantomJoinMod(IEventBus modEventBus, ModContainer modContainer) {
      modContainer.registerConfig(Type.CLIENT, PhantomJoinConfig.SPEC);
      LOGGER.info("Phantom Join initialized.");
   }
}
