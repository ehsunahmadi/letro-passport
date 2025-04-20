package expo.modules.letropassport

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class LetroPassportModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("LetroPassport")

    Function("getApiKey") {
      return@Function "api-key"
    }
  }
}
