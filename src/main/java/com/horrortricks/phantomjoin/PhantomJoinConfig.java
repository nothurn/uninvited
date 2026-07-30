package com.horrortricks.phantomjoin;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class PhantomJoinConfig {
   private static final Builder BUILDER = new Builder();
   public static final BooleanValue ENABLED = BUILDER.comment("Master switch for the phantom join/leave effect.").define("enabled", true);
   public static final IntValue MIN_INTERVAL_TICKS = BUILDER.comment(
         new String[]{
            "Minimum ticks (20 ticks/second) between one phantom's leave message and the next phantom's join message.",
            "Default of 48000 is 40 minutes - this is meant to read as a single unsettling occurrence",
            "over a play session, not a repeatable background event."
         }
      )
      .defineInRange("minIntervalTicks", 48000, 20, Integer.MAX_VALUE);
   public static final IntValue MAX_INTERVAL_TICKS = BUILDER.comment(
         new String[]{
            "Maximum ticks (20 ticks/second) between one phantom's leave message and the next phantom's join message.", "Default of 96000 is 80 minutes."
         }
      )
      .defineInRange("maxIntervalTicks", 96000, 20, Integer.MAX_VALUE);
   public static final ModConfigSpec SPEC = BUILDER.build();

   private PhantomJoinConfig() {
   }

   static {
      BUILDER.push("general");
      BUILDER.pop();
   }
}
