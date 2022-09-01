package com.mineinabyss.example

import com.mineinabyss.example.helpers.examplePlugin
import com.mineinabyss.idofront.commands.execution.IdofrontCommandExecutor

class ExampleCommands : IdofrontCommandExecutor() {
    override val commands = commands(examplePlugin) {
        "example" {

        }
    }
}
