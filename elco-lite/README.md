# ELCO Lite

A very small, Hebrew-first Android infrared remote for the OnePlus Nord 5.

- Native UI, no external dependencies, no Internet permission, no tracking.
- IR output via \`ConsumerIrManager\` (38 kHz).
- Two selectable candidate profiles: Electra/AUX 13-byte code and Airwell RC04/RC08W 34-bit Manchester.
- Temperature 16–30°C, on/off, heating/cooling, automatic, fan mode.

**Compatibility is not guaranteed.** ELCO used different remote models and protocols. Verify the original remote/model; the app shows only that IR was transmitted, not that the AC accepted it. Airwell uses a **power-toggle bit**, so on/off state can go out of sync if other remotes were used.

Build: \`gradle :app:assembleDebug\` with Android SDK 35 + JDK 17.

IR protocol reference: IRremoteESP8266 (Electra and Airwell). No third-party SDK or binaries included.
