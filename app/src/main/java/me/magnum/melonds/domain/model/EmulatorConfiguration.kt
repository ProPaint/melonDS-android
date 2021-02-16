package me.magnum.melonds.domain.model

data class EmulatorConfiguration(
        val dsConfigDirectory: String,
        val dsiConfigDirectory: String,
        val fastForwardSpeedMultiplier: Float,
        val useJit: Boolean,
        val consoleType: ConsoleType,
        val micSource: MicSource,
        val rendererConfiguration: RendererConfiguration
)