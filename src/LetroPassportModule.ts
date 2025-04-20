import { NativeModule, requireNativeModule } from "expo";

declare class LetroPassportModule extends NativeModule {
	getApiKey(): string;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<LetroPassportModule>("LetroPassport");
