import ExpoModulesCore

public class LetroPassportModule: Module {
  public func definition() -> ModuleDefinition {
    Name("LetroPassport")

    Function("getApiKey") {
     return Bundle.main.object(forInfoDictionaryKey: "MY_CUSTOM_API_KEY") as? String
    }
  }
}
