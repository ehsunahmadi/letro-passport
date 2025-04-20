import ExpoModulesCore

public class LetroPassportModule: Module {
  public func definition() -> ModuleDefinition {
    Name("LetroPassport")

    Function("getApiKey") { () -> String in
      "api-key"
    }
  }
}
